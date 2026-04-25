"""
Uses Claude to turn batches of WoW events into fun fantasy narrative updates.

The system prompt is cached via prompt caching to keep costs low across many calls.
"""

import logging
from typing import Optional

import anthropic

from watcher import WowEvent

logger = logging.getLogger(__name__)

# Stable system prompt — cached with cache_control so we only pay full price once
SYSTEM_PROMPT = """\
You are a dramatic fantasy narrator chronicling the adventures of a World of Warcraft hero \
for their friends on Discord. Your job is to transform raw game events into short, \
entertaining narrative updates.

Style guide:
- Write like an over-the-top fantasy novel narrator with a knowing wink — epic but self-aware
- 2–4 sentences maximum; punchy and fun
- Make mundane events sound heroic ("the weary traveler claimed yet another victory…")
- Reference the character's class where it adds colour
- Treat deaths with mock solemnity, resurrections with mock triumph
- Zone entries are "bold explorations"; quest completions are "glorious victories"
- Vary your openings — never start two updates the same way
- Avoid starting with the character's name every time; mix in "Our hero…", "The brave \
  adventurer…", "Once more into the breach…", etc.

Class flavour hints (use occasionally, not every time):
  WARRIOR / DEATHKNIGHT  – charges, steel, battle-cries
  PALADIN                – holy light, righteous judgement
  HUNTER                 – tracking, beasts, wilderness
  ROGUE                  – shadows, daggers, cunning
  PRIEST                 – divine mercy, shadow whispers
  SHAMAN                 – elements, totems, ancient spirits
  MAGE                   – arcane intellect, fire, frost
  WARLOCK                – fel bargains, demonic pacts
  MONK                   – chi, ancient techniques, serenity
  DRUID                  – nature, shapeshifting, balance
  DEMONHUNTER            – fel sight, vengeance
  EVOKER                 – draconic magic, Azeroth's hope\
"""


class WowNarrator:
    def __init__(self, api_key: str) -> None:
        self.client = anthropic.AsyncAnthropic(api_key=api_key)

    async def generate_narrative(self, events: list[WowEvent]) -> Optional[str]:
        """Generate a narrative Discord message from a batch of WoW events."""
        if not events:
            return None

        event_lines = "\n".join(f"- {e.describe()}" for e in events)
        latest = events[-1]

        user_prompt = (
            f"Recent events for {latest.player_name} "
            f"the Level {latest.player_level} {latest.player_class.title()}:\n\n"
            f"{event_lines}\n\n"
            "Write a brief, entertaining narrative update (2–4 sentences) for their Discord channel."
        )

        try:
            response = await self.client.messages.create(
                model="claude-opus-4-7",
                max_tokens=350,
                system=[
                    {
                        "type": "text",
                        "text": SYSTEM_PROMPT,
                        # Cache the stable system prompt — pays ~1.25x on first call,
                        # ~0.1x on all subsequent calls.
                        "cache_control": {"type": "ephemeral"},
                    }
                ],
                messages=[{"role": "user", "content": user_prompt}],
            )

            for block in response.content:
                if block.type == "text":
                    cache_hits = getattr(response.usage, "cache_read_input_tokens", 0)
                    logger.debug(
                        "Narrative generated. Cache hits: %d tokens", cache_hits
                    )
                    return block.text.strip()

        except anthropic.APIError:
            logger.exception("Claude API error while generating narrative")

        return None
