# WowDisco Setup Guide (Windows) 🗡️

> **No tech experience needed!** Follow these steps one at a time and you'll be done in about 15 minutes.

---

## What you'll need

Before you start, make sure you have accounts / access to these three things:

| Thing | What it is | Cost |
|-------|-----------|------|
| A Discord server you admin | Where the updates get posted | Free |
| An [Anthropic account](https://console.anthropic.com) | The AI that writes the narratives | Pay-per-use (very cheap) |
| World of Warcraft (retail) | The game being tracked | Your sub |

---

## Part 0 — Get the files (pick one method)

### Option A · Download a ZIP (simplest, one-time)

1. Go to the GitHub repository page for this project
2. Click the green **Code** button → **Download ZIP**
3. Extract the ZIP somewhere you'll remember (e.g. `C:\Users\YourName\Documents\WowDisco`)
4. Continue to Part 1 using the extracted folder

### Option B · Install via Git (recommended — easy updates forever)

Git lets you update the addon **and** the bot with a single command whenever a new version is released.

#### Install Git
1. Go to [git-scm.com/download/win](https://git-scm.com/download/win) and download the installer
2. Run it — the defaults are all fine, just keep clicking **Next** then **Install**
3. Click **Finish**

#### Clone the repository
1. Click the Windows Start button, type **cmd**, and open **Command Prompt**
2. Choose a folder to put the project in, e.g. your Documents folder:
   ```
   cd %USERPROFILE%\Documents
   ```
3. Clone the repo (replace the URL with the actual one):
   ```
   git clone https://github.com/yourname/wowdisco.git
   ```
   This creates a `wowdisco` folder with everything inside.

#### Updating later
When a new version is released, open Command Prompt and run:
```
cd %USERPROFILE%\Documents\wowdisco
git pull
```
Then re-copy the addon folder into WoW (Step 2 below) — everything else updates automatically since the bot runs directly from the cloned folder.

> 💡 **Tip:** After `git pull` you only need to re-copy the `WowDisco` addon folder if you see changes mentioned to the addon. The bot (`wowdisco\bot\`) updates in-place; just restart it.

---

## Part 1 — Install the WoW Addon

### Step 1 · Find your AddOns folder

1. Open **File Explorer** (the folder icon on your taskbar)
2. Paste this into the address bar at the top and press **Enter**:
   ```
   C:\Program Files (x86)\World of Warcraft\_retail_\Interface\AddOns
   ```
   > 💡 If WoW is installed somewhere else, look for `World of Warcraft\_retail_\Interface\AddOns` wherever that is.

### Step 2 · Copy the addon folder

1. Find the `wowdisco\addon\WowDisco` folder from this project
2. Copy the entire **`WowDisco`** folder (the one that contains `WowDisco.lua` and `WowDisco.toc`)
3. Paste it into the `AddOns` folder you opened in Step 1

It should look like this when done:
```
AddOns\
  └── WowDisco\
        ├── WowDisco.lua
        └── WowDisco.toc
```

### Step 3 · Enable the addon in-game

1. Launch WoW
2. On the character select screen, click **AddOns** (bottom-left corner)
3. Find **WowDisco** in the list and tick the checkbox ✅
4. Log in — you should see a green message: *"WowDisco loaded!"*

### Step 4 · Turn on chat logging

This is how the addon talks to the bot — WoW writes everything to a text file that the bot reads.

1. In WoW, press **Escape** → **System** → **Interface**
2. Click the **Help** tab on the left
3. Tick **"Log Chat to File"** ✅
4. Click **Okay**

> ✅ **Test it:** Type `/wowdisco test` in the chat box. You should see a long line of text appear. That line is what the bot reads!

---

## Part 2 — Create a Discord Bot

### Step 5 · Make a new Discord application

1. Go to [discord.com/developers/applications](https://discord.com/developers/applications) and log in
2. Click **New Application** (top right)
3. Give it a name like `WowDisco` and click **Create**

### Step 6 · Create the bot user

1. In the left sidebar click **Bot**
2. Click **Add Bot** → **Yes, do it!**
3. Under **Token**, click **Reset Token** → **Yes, do it!**
4. Click **Copy** — this is your **DISCORD_TOKEN** — **save it somewhere safe, treat it like a password!**

### Step 7 · Invite the bot to your server

1. In the left sidebar click **OAuth2** → **URL Generator**
2. Under **Scopes**, tick: `bot`
3. Under **Bot Permissions**, tick: `Send Messages` and `Embed Links`
4. Copy the URL at the bottom, paste it in your browser, and invite the bot to your server

### Step 8 · Get your channel ID

1. In Discord, open **User Settings** (gear icon) → **Advanced** → turn on **Developer Mode** ✅
2. Right-click the channel you want WowDisco to post in
3. Click **Copy Channel ID** — save this number, it's your **DISCORD_CHANNEL_ID**

---

## Part 3 — Set Up the Python Bot

### Step 9 · Install Python

1. Go to [python.org/downloads](https://www.python.org/downloads/)
2. Download the latest **Python 3.12** (or newer) installer
3. Run it — on the first screen, **tick "Add Python to PATH"** before clicking Install ✅
4. Click **Install Now** and let it finish

### Step 10 · Get your Anthropic API key

1. Go to [console.anthropic.com](https://console.anthropic.com) and sign in
2. Click your name (top right) → **API Keys**
3. Click **Create Key**, give it a name, and **copy the key** — save it safely!
4. Add some credit under **Billing** (a few dollars goes a very long way — each narrative costs fractions of a cent)

### Step 11 · Create the config file

1. Open the `wowdisco\bot` folder
2. Find the file called **`.env.example`**
3. Make a **copy** of it in the same folder and rename the copy to **`.env`** (just `.env`, no `.example`)

   > ⚠️ Windows might warn you about removing the file extension — click **Yes**

4. Right-click your new `.env` file → **Open with** → **Notepad**
5. Fill in your values:

```
DISCORD_TOKEN=paste-your-bot-token-here
DISCORD_CHANNEL_ID=paste-your-channel-id-here
ANTHROPIC_API_KEY=paste-your-anthropic-key-here
WOW_LOG_PATH=C:\Program Files (x86)\World of Warcraft\_retail_\Logs\WoWChatLog.txt
NARRATIVE_INTERVAL=300
MIN_EVENTS=2
```

6. Save and close Notepad

> 💡 `NARRATIVE_INTERVAL=300` means a post every 5 minutes. Change it to `600` for 10 minutes, `180` for 3 minutes, etc.

### Step 12 · Install the bot's dependencies

1. Click the Windows Start button, type **cmd**, and open **Command Prompt**
2. Navigate to the bot folder by typing (adjust the path if needed):
   ```
   cd C:\path\to\wowdisco\bot
   ```
   For example: `cd C:\Users\YourName\Downloads\wowdisco\bot`
3. Run this command:
   ```
   pip install -r requirements.txt
   ```
4. Wait for it to finish (you'll see a bunch of text scrolling — that's normal)

### Step 13 · Run the bot!

In the same Command Prompt window, type:
```
python main.py
```

You should see something like:
```
2026-04-25 12:00:00 [INFO] root: Starting WowDisco bot…
2026-04-25 12:00:01 [INFO] root: Discord bot connected as WowDisco
2026-04-25 12:00:01 [INFO] root: WowDisco is live! Posting to channel … every 300 s
2026-04-25 12:00:01 [INFO] watcher: Watching WoW log: C:\Program Files…
```

**Leave this window open while you play.** The bot only works while it's running.

---

## Part 4 — Test Everything

### Step 14 · Send a test event

1. In WoW, type in chat:
   ```
   /wowdisco test
   ```
2. Wait up to 5 minutes (or however long you set `NARRATIVE_INTERVAL`)
3. Check your Discord channel — a narrative embed should appear! 🎉

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| Addon shows as **Incompatible** | At the bottom of the AddOns list screen, tick **"Load out of date AddOns"** — then enable WowDisco. If that option isn't there, the `.toc` file may need its interface number updated (see below) |
| *"python is not recognized"* | Re-run the Python installer and make sure "Add to PATH" is ticked |
| Bot is online but nothing posts | Check that chat logging is ON in WoW (Step 4) |
| *"Log file not found"* in the console | Make sure `WOW_LOG_PATH` in `.env` matches your actual WoW install path |
| Addon doesn't appear in WoW AddOns list | Make sure the folder structure is `AddOns\WowDisco\WowDisco.lua` (not an extra folder inside) |
| *"DISCORD_TOKEN not set"* | Make sure your `.env` file has no spaces around the `=` sign |
| Nothing happens after `/wowdisco test` | Check the Command Prompt window for error messages |

### Fixing the "Incompatible" addon manually

Each WoW patch has an interface number (e.g. `120005`). If the addon's `.toc` file is behind, WoW flags it as incompatible. To fix it yourself:

1. In WoW, type `/run print(select(4, GetBuildInfo()))` in chat — the number that appears (e.g. `120005`) is your current interface number
2. Open `AddOns\WowDisco\WowDisco.toc` in Notepad
3. Change **both** `Interface:` lines to that number:
   ```
   ## Interface: 120005
   ## Interface-Retail: 120005
   ```
4. Save, then reload WoW (`/reload` or log out and back in)

If you installed via Git (Part 0 Option B), this fix will be in the next `git pull` automatically.

---

## Keeping it running automatically (optional)

The bot stops when you close the Command Prompt window. To run it in the background without a visible window:

1. In the `bot` folder, create a file called `start.bat` and paste this inside it:
   ```bat
   @echo off
   cd /d %~dp0
   pythonw main.py
   ```
2. Double-click `start.bat` to launch the bot silently
3. To stop it, open **Task Manager** → **Details** tab → find `pythonw.exe` → **End Task**

---

## Quick Reference

| Command (in WoW chat) | Does |
|-----------------------|------|
| `/wowdisco test` | Sends a test event right now |
| `/wowdisco status` | Shows how many events are stored locally |
| `/wowdisco clear` | Clears the local event log |

Happy adventuring! ⚔️✨
