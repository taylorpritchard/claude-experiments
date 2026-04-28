# StormQuest

A classic Final Fantasy / Dragon Warrior style turn-based RPG for Android.

## Requirements

- Android Studio Hedgehog (2023.1.1) or newer, OR
- JDK 17+ with Android SDK (API 34)
- Gradle 8.4 (downloaded automatically by wrapper)

## First-Time Setup

The Gradle wrapper jar is not included in the repo. Download it before building:

```bash
# Option 1: Use the setup script
chmod +x setup_wrapper.sh
./setup_wrapper.sh

# Option 2: Run with Gradle directly (if Gradle 8.4 installed)
gradle wrapper --gradle-version=8.4

# Option 3: Open in Android Studio — it will download the wrapper automatically
```

## Building

```bash
# Debug APK
./gradlew assembleDebug

# Release APK (requires signing config)
./gradlew assembleRelease

# The debug APK will be at:
# app/build/outputs/apk/debug/app-debug.apk
```

## Installing on Device / Emulator

```bash
./gradlew installDebug
# or
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Game Overview

**StormQuest** is a dungeon-crawling RPG where you:

1. **Create a party of 4 heroes** — choose from 6 races and 6 classes
2. **Explore a dungeon** — advance floor by floor, encountering enemies
3. **Fight in turn-based battles** — Attack, Magic, Items, Defend, or Flee
4. **Level up** your party and collect gold
5. **Shop** between floors for potions and supplies
6. **Defeat the Shadow Dragon** boss on every 5th floor

### Races
Human, Elf, Dwarf, Halfling, Half-Orc, Gnome — each with unique stat bonuses

### Classes
Warrior, Mage, Cleric, Rogue, Paladin, Ranger — each with different roles and spells

### Combat
- Turn order based on SPD stat
- Physical damage: STR × 2 − DEF + random
- Magic damage: MAG × 2 + spell power + random
- Critical hits based on LCK
- Defending halves incoming damage

## Project Structure

```
app/src/main/java/com/stormquest/
├── data/           — Game data definitions (races, classes, spells, items, enemies)
├── model/          — Game state models (GameCharacter, BattleEnemy, GameState)
├── battle/         — Battle resolution engine (BattleEngine)
├── ui/             — Activities (PartyCreation, Explore, Battle, Shop)
└── MainActivity.kt — Title screen
```

## Tech Stack

- Kotlin 1.9.22
- Android minSdk 24, targetSdk 34
- Traditional Views (no Jetpack Compose)
- Gradle 8.4 / AGP 8.1.4
- ViewBinding enabled
