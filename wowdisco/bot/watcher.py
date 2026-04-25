"""
Watches the WoW chat log file and parses WOWDISCO events into structured objects.

WoW writes lines like:
  4/25 12:34:56.123  WOWDISCO|1714000000|ZONE_ENTER|Thrall|WARRIOR|60|Stormwind City|Trade District|Stormwind City

Enable WoW chat logging: Settings > Interface > Help > Log Chat to File
"""

import asyncio
import logging
import os
from dataclasses import dataclass, field
from typing import Optional

import aiofiles

logger = logging.getLogger(__name__)


@dataclass
class WowEvent:
    timestamp: int
    event_type: str
    player_name: str
    player_class: str
    player_level: int
    zone: str
    subzone: str
    extra: list[str] = field(default_factory=list)

    @classmethod
    def from_log_line(cls, line: str) -> Optional["WowEvent"]:
        idx = line.find("WOWDISCO|")
        if idx == -1:
            return None

        parts = line[idx:].strip().split("|")
        if len(parts) < 8:
            return None

        try:
            return cls(
                timestamp=int(parts[1]),
                event_type=parts[2],
                player_name=parts[3],
                player_class=parts[4],
                player_level=int(parts[5]) if parts[5].isdigit() else 1,
                zone=parts[6],
                subzone=parts[7],
                extra=parts[8:] if len(parts) > 8 else [],
            )
        except (ValueError, IndexError):
            logger.debug("Failed to parse WOWDISCO line: %s", line.strip())
            return None

    def describe(self) -> str:
        name = self.player_name
        level = self.player_level
        zone = self.zone
        sub = f" ({self.subzone})" if self.subzone else ""
        x = self.extra

        mapping = {
            "LOGIN":         f"{name} (Level {level}) logged into World of Warcraft",
            "RELOAD":        f"{name} reloaded their UI",
            "LEVEL_UP":      f"{name} reached Level {x[0] if x else level}!",
            "DEATH":         f"{name} died in {zone}{sub}",
            "ALIVE":         f"{name} rose from the dead in {zone}",
            "ZONE_ENTER":    f"{name} entered {x[0] if x else zone}",
            "QUEST_COMPLETE": f"{name} completed the quest \"{x[0] if x else 'a quest'}\"",
            "QUEST_ACCEPT":  f"{name} accepted the quest \"{x[0] if x else 'a new quest'}\"",
            "ACHIEVEMENT":   f"{name} earned the achievement \"{x[0] if x else 'an achievement'}\"",
            "BOSS_KILL":     f"{name}'s group defeated {x[0] if x else 'a boss'}!",
            "GROUP_JOIN":    f"{name} joined a group of {x[0] if x else 'adventurers'}",
            "GROUP_LEAVE":   f"{name} left their group and is now adventuring solo",
            "TEST":          f"{name} sent a test: {x[0] if x else ''}",
        }
        return mapping.get(self.event_type, f"{name}: [{self.event_type}] in {zone}")


class WowLogWatcher:
    """Tails the WoW chat log and pushes parsed WowEvents onto an asyncio Queue."""

    def __init__(self, log_path: str, event_queue: asyncio.Queue):
        self.log_path = log_path
        self.event_queue = event_queue
        self._running = False

    async def start(self) -> None:
        self._running = True
        logger.info("WowLogWatcher: watching %s", self.log_path)

        # Wait for the file to appear (WoW creates it on first login session)
        while self._running and not os.path.exists(self.log_path):
            logger.warning("Log file not found: %s — retrying in 15 s…", self.log_path)
            await asyncio.sleep(15)

        if not self._running:
            return

        # Start tailing from the end so we don't replay old sessions
        file_position = os.path.getsize(self.log_path)

        while self._running:
            try:
                async with aiofiles.open(
                    self.log_path, "r", encoding="utf-8", errors="replace"
                ) as fh:
                    await fh.seek(file_position)
                    while self._running:
                        line = await fh.readline()
                        if line:
                            file_position = await fh.tell()
                            event = WowEvent.from_log_line(line)
                            if event:
                                logger.debug("Event: %s %s", event.event_type, event.player_name)
                                await self.event_queue.put(event)
                        else:
                            # Check if file was truncated (new WoW session)
                            try:
                                current_size = os.path.getsize(self.log_path)
                                if current_size < file_position:
                                    file_position = 0
                                    logger.info("Log file reset — restarting from beginning")
                            except OSError:
                                pass
                            await asyncio.sleep(0.5)
            except FileNotFoundError:
                logger.warning("Log file disappeared — waiting…")
                await asyncio.sleep(10)
            except Exception:
                logger.exception("Unexpected error in log watcher")
                await asyncio.sleep(5)

    def stop(self) -> None:
        self._running = False
