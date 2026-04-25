"""
WowDisco bot — watches your WoW chat log and posts fun narrative updates to Discord.

Usage:
  1. Copy .env.example to .env and fill in your tokens/paths
  2. pip install -r requirements.txt
  3. python main.py
"""

import asyncio
import logging
import os
import sys

import discord
from discord.ext import tasks
from dotenv import load_dotenv

from narrator import WowNarrator
from watcher import WowEvent, WowLogWatcher

load_dotenv()

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    handlers=[logging.StreamHandler()],
)
logger = logging.getLogger(__name__)

# ─── Config ──────────────────────────────────────────────────────────────────

DISCORD_TOKEN      = os.getenv("DISCORD_TOKEN", "")
DISCORD_CHANNEL_ID = int(os.getenv("DISCORD_CHANNEL_ID", "0"))
ANTHROPIC_API_KEY  = os.getenv("ANTHROPIC_API_KEY", "")
WOW_LOG_PATH       = os.getenv("WOW_LOG_PATH", "")
NARRATIVE_INTERVAL = int(os.getenv("NARRATIVE_INTERVAL", "300"))
MIN_EVENTS         = int(os.getenv("MIN_EVENTS", "2"))

# WoW class colours for Discord embeds
CLASS_COLORS: dict[str, int] = {
    "WARRIOR":     0xC69B3A,
    "PALADIN":     0xF48CBA,
    "HUNTER":      0xAAD372,
    "ROGUE":       0xFFF468,
    "PRIEST":      0xFFFFFF,
    "SHAMAN":      0x0070DD,
    "MAGE":        0x3FC7EB,
    "WARLOCK":     0x8788EE,
    "MONK":        0x00FF98,
    "DRUID":       0xFF7C0A,
    "DEMONHUNTER": 0xA330C9,
    "DEATHKNIGHT": 0xC41E3A,
    "EVOKER":      0x33937F,
}

# ─── Bot State ────────────────────────────────────────────────────────────────

intents = discord.Intents.default()
bot = discord.Client(intents=intents)

event_queue: asyncio.Queue[WowEvent] = asyncio.Queue()
pending_events: list[WowEvent] = []
narrator: WowNarrator | None = None
_started = False

# ─── Bot Events ───────────────────────────────────────────────────────────────

@bot.event
async def on_ready() -> None:
    global narrator, _started
    if _started:
        return
    _started = True

    logger.info("Discord bot connected as %s (id %s)", bot.user, bot.user.id if bot.user else "?")

    narrator = WowNarrator(ANTHROPIC_API_KEY)

    asyncio.create_task(_run_watcher())
    asyncio.create_task(_collect_events())

    post_narrative.start()
    logger.info(
        "WowDisco is live! Posting to channel %d every %d s (min %d events).",
        DISCORD_CHANNEL_ID,
        NARRATIVE_INTERVAL,
        MIN_EVENTS,
    )


async def _run_watcher() -> None:
    watcher = WowLogWatcher(WOW_LOG_PATH, event_queue)
    await watcher.start()


async def _collect_events() -> None:
    while True:
        try:
            event = await event_queue.get()
            pending_events.append(event)
            logger.info("Queued: %s for %s (total pending: %d)", event.event_type, event.player_name, len(pending_events))
        except Exception:
            logger.exception("Error in event collector")


# ─── Narrative Poster ─────────────────────────────────────────────────────────

@tasks.loop(seconds=NARRATIVE_INTERVAL)
async def post_narrative() -> None:
    if not pending_events:
        return

    if len(pending_events) < MIN_EVENTS:
        logger.info("Only %d events pending, waiting for more…", len(pending_events))
        return

    channel = bot.get_channel(DISCORD_CHANNEL_ID)
    if not isinstance(channel, discord.TextChannel):
        logger.error("Channel %d not found or not a text channel.", DISCORD_CHANNEL_ID)
        return

    # Drain all pending events atomically
    batch = pending_events.copy()
    pending_events.clear()

    logger.info("Generating narrative for %d events…", len(batch))
    narrative = await narrator.generate_narrative(batch)  # type: ignore[union-attr]

    if not narrative:
        logger.warning("Narrator returned nothing — skipping post")
        return

    latest = batch[-1]
    class_color = CLASS_COLORS.get(latest.player_class.upper(), 0x9B59B6)

    embed = discord.Embed(description=narrative, color=class_color)
    embed.set_footer(
        text=(
            f"⚔️  {latest.player_name}  •  "
            f"Level {latest.player_level} {latest.player_class.title()}  •  "
            f"{latest.zone}"
        )
    )

    await channel.send(embed=embed)
    logger.info("Posted narrative to #%s", channel.name)


@post_narrative.before_loop
async def _before_post() -> None:
    await bot.wait_until_ready()


# ─── Entry Point ──────────────────────────────────────────────────────────────

def _validate_config() -> bool:
    missing = []
    if not DISCORD_TOKEN:
        missing.append("DISCORD_TOKEN")
    if not DISCORD_CHANNEL_ID:
        missing.append("DISCORD_CHANNEL_ID")
    if not ANTHROPIC_API_KEY:
        missing.append("ANTHROPIC_API_KEY")
    if not WOW_LOG_PATH:
        missing.append("WOW_LOG_PATH")

    if missing:
        logger.error("Missing required environment variables: %s", ", ".join(missing))
        logger.error("Copy .env.example to .env and fill in the values.")
        return False
    return True


if __name__ == "__main__":
    if not _validate_config():
        sys.exit(1)

    logger.info("Starting WowDisco bot…")
    bot.run(DISCORD_TOKEN, log_handler=None)
