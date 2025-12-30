-- Raid Framer Main Module
-- Automatically placed addon by Raid Framer 2.0
-- https://github.com/barcodeguild/raid-framer-desktop
-- Author: Reoky
RF = RF or {}
RF.TAG = "Raid Framer 2.0"

ADDON:ImportAPI(API_TYPE.CHAT.id)
ADDON:ImportAPI(API_TYPE.UNIT.id)
ADDON:ImportAPI(API_TYPE.MAP.id)
ADDON:ImportAPI(API_TYPE.PLAYER.id)
ADDON:ImportAPI(API_TYPE.TEAM.id)

RF.COLORS = {
  regular = "|cFFFFD700",
  magenta = "|cFFE44D9D",
  cyan    = "|cFF00FFFF",
  green   = "|cFF00FF00",
  red     = "|cFFFF0000",
}

-------------------------------
-- Helpers and Factorynesses --
-------------------------------
local function colorize(text, color)
    local c = RF.COLORS[color] or RF.COLORS.regular
    return string.format("%s%s|r", c, text)
end

function RF:Log(msg)
  local time = os.date("%H:%M:%S")
  local line = string.format(
    "[%s@%s]: %s",
    colorize(self.TAG, "magenta"),
    colorize(time, "cyan"),
    msg
  )
  X2Chat:DispatchChatMessage(CMF_SYSTEM, line)
end

local function DumpTableKeys(tbl, name)
  name = name or "table"
  RF:Log(string.format("Dumping %s:", name))
  for k, v in pairs(tbl) do
    RF:Log(string.format("  %s (%s)", tostring(k), type(v)))
  end
end

----------------------------
-- Main Lifecycle Methods --
----------------------------
function RF:Init()
  self:Shutdown()  -- makes Init idempotent

  self.initialized = true

  self:Log("Good news, friend! If you can read this message, then the Raid Framer 2.0 Lua component is working! (Please be sure to launch the companion app for a multi-monitor GUI experience!)")
  --DumpTableKeys(X2Unit, "X2Unit")

  -- create UI
  -- register events
  registerForEvents()
  -- OTHER STUFF
end

function RF:Shutdown()
  if not self.initialized then return end

  self.initialized = false
  self:Log("Shutdown")


  -- hide/destroy UI
  -- unregister events
  deregisterForEvents()
  -- OTHER STUFF
end

function registerForEvents()
  -- Guard: ensure RF.Raid module is loaded before registering its handlers
  if not RF.Raid or not RF.Raid.handleTeamMembersChanged then
    RF:Log("ERROR: RF.Raid module not loaded. Cannot register event handlers.")
    return
  end
  
  UIParent:SetEventHandler(UIEVENT_TYPE.TEAM_MEMBERS_CHANGED, RF.Raid.handleTeamMembersChanged)
  UIParent:SetEventHandler(UIEVENT_TYPE.COMBAT_MSG, CombatEvents)
  UIParent:SetEventHandler(UIEVENT_TYPE.TEAM_JOINTED, RF.Raid.handleCoraidEstablished)
  UIParent:SetEventHandler(UIEVENT_TYPE.TEAM_JOINT_BROKEN, RF.Raid.handleCoraidBroken)
end

function deregisterForEvents()
  UIParent:RemoveEventHandler(UIEVENT_TYPE.TEAM_MEMBERS_CHANGED)
  UIParent:RemoveEventHandler(UIEVENT_TYPE.COMBAT_MSG)
  UIParent:RemoveEventHandler(UIEVENT_TYPE.TEAM_JOINTED)
  UIParent:RemoveEventHandler(UIEVENT_TYPE.TEAM_JOINT_BROKEN)
end

-------------------------
-- Game Event Handlers --
-------------------------
local function onEnteredWorld()
  RF:Init()
end
UIParent:SetEventHandler(UIEVENT_TYPE.ENTERED_WORLD, onEnteredWorld)

function CombatEvents(...)
    local args = { ... }
    local evt = RF:ParseInternalEvent(args)
    if (evt.spellName == "Charmed") then
      RF:Log(tostring(evt.source) .. " charmed " .. tostring(evt.target))
    end
    if (evt.spellName == "Silence") then
      RF:Log(tostring(evt.source) .. " silenced " .. tostring(evt.target))
    end
end

-- Tries to parse the raw event args from the game into a structured table
-- the game has many event types with different arg structures under the hood
-- so this is a simplified parser that only extracts common fields for now
function RF:ParseInternalEvent(t)
    local evt = {}

    evt.eventType  = t[2]
    evt.source     = t[3]
    evt.target     = t[4]
    evt.spellId    = t[5]
    evt.spellName  = t[6]
    evt.school     = t[7]
    evt.auraType   = t[8]
    evt.isHarmful  = t[9]

    -- Environmental damage special case
    if evt.eventType == "ENVIRONMENTAL_DAMAGE" then
        evt.envType = t[6] -- FALLING
        evt.amount  = tonumber(t[7])
        evt.result  = t[9] -- HIT
    end

    return evt
end
