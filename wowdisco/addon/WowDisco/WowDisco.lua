-- WowDisco: Chronicles your adventures to Discord
-- Events are emitted as pipe-delimited strings to the chat log.
-- Chat logging is enabled automatically on load.

local THROTTLE_SECONDS = 3  -- minimum seconds between identical event types
local MAX_STORED_EVENTS = 200

WowDiscoData = WowDiscoData or {}
WowDiscoData.events  = WowDiscoData.events  or {}
WowDiscoData.throttle = WowDiscoData.throttle or {}

local function Sanitize(s)
    if s == nil then return "" end
    return tostring(s):gsub("|", "/"):gsub("\n", " "):gsub("\r", "")
end

local function EmitEvent(eventType, ...)
    local now = time()
    if WowDiscoData.throttle[eventType] and (now - WowDiscoData.throttle[eventType]) < THROTTLE_SECONDS then
        return
    end
    WowDiscoData.throttle[eventType] = now

    local playerName  = Sanitize(UnitName("player") or "Unknown")
    local playerClass = Sanitize(select(2, UnitClass("player")) or "WARRIOR")
    local playerLevel = UnitLevel("player") or 1
    local zone        = Sanitize(GetRealZoneText() or "Unknown")
    local subzone     = Sanitize(GetSubZoneText() or "")

    local parts = {
        "WOWDISCO",
        now,
        Sanitize(eventType),
        playerName,
        playerClass,
        playerLevel,
        zone,
        subzone,
    }

    for i = 1, select("#", ...) do
        local v = select(i, ...)
        table.insert(parts, Sanitize(tostring(v or "")))
    end

    local msg = table.concat(parts, "|")

    -- Store locally (newest at the end, trimmed to max)
    table.insert(WowDiscoData.events, msg)
    while #WowDiscoData.events > MAX_STORED_EVENTS do
        table.remove(WowDiscoData.events, 1)
    end

    -- Print to default chat frame; WoW logs this when "Log Chat to File" is enabled
    DEFAULT_CHAT_FRAME:AddMessage(msg)
end

-- ─── Event Frame ─────────────────────────────────────────────────────────────

local frame = CreateFrame("Frame")
frame:RegisterEvent("PLAYER_ENTERING_WORLD")
frame:RegisterEvent("PLAYER_LEVEL_UP")
frame:RegisterEvent("PLAYER_DEAD")
frame:RegisterEvent("PLAYER_ALIVE")
frame:RegisterEvent("PLAYER_UNGHOST")
frame:RegisterEvent("ZONE_CHANGED_NEW_AREA")
frame:RegisterEvent("QUEST_TURNED_IN")
frame:RegisterEvent("QUEST_ACCEPTED")
frame:RegisterEvent("ACHIEVEMENT_EARNED")
frame:RegisterEvent("ENCOUNTER_END")
frame:RegisterEvent("GROUP_ROSTER_UPDATE")

local lastZone  = ""
local lastGroup = 0

frame:SetScript("OnEvent", function(self, event, ...)
    if event == "PLAYER_ENTERING_WORLD" then
        local isLogin = ...
        EmitEvent(isLogin and "LOGIN" or "RELOAD")
        lastZone = GetRealZoneText() or ""

    elseif event == "PLAYER_LEVEL_UP" then
        local newLevel = ...
        EmitEvent("LEVEL_UP", newLevel)

    elseif event == "PLAYER_DEAD" then
        EmitEvent("DEATH")

    elseif event == "PLAYER_ALIVE" or event == "PLAYER_UNGHOST" then
        EmitEvent("ALIVE")

    elseif event == "ZONE_CHANGED_NEW_AREA" then
        local newZone = GetRealZoneText() or "Unknown"
        if newZone ~= lastZone then
            lastZone = newZone
            EmitEvent("ZONE_ENTER", newZone)
        end

    elseif event == "QUEST_TURNED_IN" then
        local questID = ...
        local questName = C_QuestLog.GetTitleForQuestID and C_QuestLog.GetTitleForQuestID(questID) or "a quest"
        EmitEvent("QUEST_COMPLETE", questName or "Unknown Quest")

    elseif event == "QUEST_ACCEPTED" then
        local questLogIndex, questID = ...
        -- Delay slightly so quest data is available in the log
        C_Timer.After(0.2, function()
            local questName = C_QuestLog.GetTitleForQuestID and C_QuestLog.GetTitleForQuestID(questID) or "a quest"
            EmitEvent("QUEST_ACCEPT", questName or "New Quest")
        end)

    elseif event == "ACHIEVEMENT_EARNED" then
        local achievementID, name = ...
        EmitEvent("ACHIEVEMENT", name or "an achievement")

    elseif event == "ENCOUNTER_END" then
        local encounterID, encounterName, difficultyID, groupSize, success = ...
        if success == 1 then
            EmitEvent("BOSS_KILL", encounterName or "a boss")
        end

    elseif event == "GROUP_ROSTER_UPDATE" then
        local groupSize = GetNumGroupMembers()
        if groupSize ~= lastGroup then
            if groupSize > 0 and groupSize > lastGroup then
                EmitEvent("GROUP_JOIN", groupSize)
            elseif groupSize == 0 then
                EmitEvent("GROUP_LEAVE")
            end
            lastGroup = groupSize
        end
    end
end)

-- ─── Slash Commands ───────────────────────────────────────────────────────────

SLASH_WOWDISCO1 = "/wowdisco"
SlashCmdList["WOWDISCO"] = function(msg)
    local cmd = (msg or ""):lower():match("^%s*(%S*)")

    if cmd == "test" then
        EmitEvent("TEST", "Hello from Azeroth! WowDisco is working.")
        print("|cff00ff00WowDisco|r: Test event emitted.")

    elseif cmd == "status" then
        print(string.format("|cff00ff00WowDisco|r: %d events stored locally.", #WowDiscoData.events))
        print("|cff00ff00WowDisco|r: Chat logging must be ON for events to reach Discord.")

    elseif cmd == "clear" then
        WowDiscoData.events  = {}
        WowDiscoData.throttle = {}
        print("|cff00ff00WowDisco|r: Event log cleared.")

    else
        print("|cff00ff00WowDisco|r Commands:")
        print("  /wowdisco test   - emit a test event")
        print("  /wowdisco status - show stored event count")
        print("  /wowdisco clear  - clear stored events")
    end
end

-- Enable chat logging automatically so the bot can read events
if not LoggingChat() then
    LoggingChat(true)
    print("|cff00ff00WowDisco|r: Chat logging enabled automatically.")
end

print("|cff00ff00WowDisco|r loaded! Your adventures will be narrated to Discord.")
