"""
Watches WowDisco's SavedVariables file for new events.

WoW writes _retail_/WTF/Account/<account>/SavedVariables/WowDisco.lua
to disk on logout and /reload.  We poll for mtime changes and push any
events with timestamps newer than the last-seen one onto the queue.
"""

import asyncio
import glob
import logging
import os
import re
from dataclasses import dataclass, field
from typing import Optional

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
            "LOGIN":          f"{name} (Level {level}) logged into World of Warcraft",
            "RELOAD":         f"{name} reloaded their UI",
            "LEVEL_UP":       f"{name} reached Level {x[0] if x else level}!",
            "DEATH":          f"{name} died in {zone}{sub}",
            "ALIVE":          f"{name} rose from the dead in {zone}",
            "ZONE_ENTER":     f"{name} entered {x[0] if x else zone}",
            "QUEST_COMPLETE": f"{name} completed the quest \"{x[0] if x else 'a quest'}\"",
            "QUEST_ACCEPT":   f"{name} accepted the quest \"{x[0] if x else 'a new quest'}\"",
            "ACHIEVEMENT":    f"{name} earned the achievement \"{x[0] if x else 'an achievement'}\"",
            "BOSS_KILL":      f"{name}'s group defeated {x[0] if x else 'a boss'}!",
            "GROUP_JOIN":     f"{name} joined a group of {x[0] if x else 'adventurers'}",
            "GROUP_LEAVE":    f"{name} left their group and is now adventuring solo",
            "TEST":           f"{name} sent a test: {x[0] if x else ''}",
        }
        return mapping.get(self.event_type, f"{name}: [{self.event_type}] in {zone}")


def find_sv_path(wow_log_path: str) -> str:
    """Locate WowDisco.lua by walking up from the WoW log path."""
    # wow_log_path = .../World of Warcraft/_retail_/Logs/WoWChatLog.txt
    # target       = .../World of Warcraft/_retail_/WTF/Account/*/SavedVariables/WowDisco.lua
    retail_dir = os.path.dirname(os.path.dirname(wow_log_path))
    pattern = os.path.join(retail_dir, "WTF", "Account", "*", "SavedVariables", "WowDisco.lua")
    matches = glob.glob(pattern)
    return matches[0] if matches else ""


class WowSavedVarsWatcher:
    """
    Polls WowDisco.lua for new events.

    WoW flushes SavedVariables to disk on logout and /reload.  Events
    are keyed by Unix timestamp so we never replay the same event twice,
    even across bot restarts.
    """

    def __init__(self, wow_log_path: str, event_queue: asyncio.Queue):
        self._wow_log_path = wow_log_path
        self.sv_path = find_sv_path(wow_log_path)
        self.event_queue = event_queue
        self._running = False
        self._last_seen_ts = 0

    async def start(self) -> None:
        self._running = True

        if self.sv_path:
            logger.info("Watching SavedVariables: %s", self.sv_path)
            # Establish baseline so we don't replay events from before now
            self._last_seen_ts = self._latest_timestamp()
            logger.info("Baseline: last event timestamp %d", self._last_seen_ts)
        else:
            logger.info(
                "WowDisco.lua not found yet — log out of WoW once to create it "
                "(WoW writes SavedVariables on logout and /reload)"
            )

        last_mtime = 0.0
        while self._running:
            try:
                if not self.sv_path:
                    found = find_sv_path(self._wow_log_path)
                    if found:
                        self.sv_path = found
                        logger.info("Found SavedVariables: %s", self.sv_path)
                        self._last_seen_ts = self._latest_timestamp()

                if self.sv_path and os.path.exists(self.sv_path):
                    mtime = os.path.getmtime(self.sv_path)
                    if mtime > last_mtime:
                        last_mtime = mtime
                        await self._process_new_events()
            except Exception:
                logger.exception("Error polling SavedVariables")
            await asyncio.sleep(2)

    def _read_raw_events(self) -> list[str]:
        try:
            with open(self.sv_path, "r", encoding="utf-8", errors="replace") as fh:
                content = fh.read()
            return re.findall(r'"(WOWDISCO\|[^"]+)"', content)
        except Exception:
            logger.debug("Could not read SavedVariables", exc_info=True)
            return []

    def _latest_timestamp(self) -> int:
        ts = 0
        for raw in self._read_raw_events():
            ev = WowEvent.from_log_line(raw)
            if ev:
                ts = max(ts, ev.timestamp)
        return ts

    async def _process_new_events(self) -> None:
        new: list[WowEvent] = []
        for raw in self._read_raw_events():
            ev = WowEvent.from_log_line(raw)
            if ev and ev.timestamp > self._last_seen_ts:
                new.append(ev)

        if not new:
            return

        new.sort(key=lambda e: e.timestamp)
        self._last_seen_ts = new[-1].timestamp
        for ev in new:
            logger.info("New event: %s for %s", ev.event_type, ev.player_name)
            await self.event_queue.put(ev)

    def stop(self) -> None:
        self._running = False
