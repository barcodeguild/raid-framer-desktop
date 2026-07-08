-- Raid Framer 2.0 Lua Companion
-- May have automatically been placed by the Raid Framer Desktop App
-- https://github.com/barcodeguild/raid-framer-desktop
-- Author: Reoky
RF = RF or {}
RF.TAG = "Raid Framer 2.1.2"

RF.PLAYER_NAME = ""
RF.FACTION = ""

ADDON:ImportAPI(API_TYPE.CHAT.id)
ADDON:ImportAPI(API_TYPE.UNIT.id)
ADDON:ImportAPI(API_TYPE.PLAYER.id)
ADDON:ImportAPI(API_TYPE.TEAM.id)
ADDON:ImportAPI(API_TYPE.SOUND.id)

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

function RF:contains(v, t)
  for _, x in ipairs(t) do
    if x == v then return true end
  end
  return false
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

----------------------------
-- Main Lifecycle Methods --
----------------------------
function RF:Init()
  self:Shutdown()  -- makes Init idempotent

  local success = guardAllModulesArePresent()
  self.initialized = success

  if (not success) then
    self:Log("ERROR: " .. RF.TAG .. " failed to initialize. Please replace the missing modules, friend.")
    return
  end

  -- get player name : tell desktop app which character the user is playing
  RF.PLAYER_NAME = X2Unit:UnitName("player")
  RF.IPC.WriteMessage(RF.IPC.MESSAGE_TYPES.SELF_UPDATE, RF.PLAYER_NAME)

  self:Log("Good news, " .. RF.PLAYER_NAME .. "! If you can read this message, then the " .. RF.TAG .. " Lua component is working!")
  self:Log("Please be sure to launch the desktop app to access the multi-monitor game overlay.")

  registerForEvents()
end

function RF:Shutdown()
  if not self.initialized then return end
  self.initialized = false
  self:Log("Shutdown")
  deregisterForEvents()
end

-- Attach handlers to the game for various events we care about
function registerForEvents()
  UIParent:SetEventHandler(UIEVENT_TYPE.TEAM_MEMBERS_CHANGED, RF.Raid.handleTeamMembersChanged)
  UIParent:SetEventHandler(UIEVENT_TYPE.TEAM_ROLE_CHANGED, RF.Raid.handleTeamRoleChanged)
  UIParent:SetEventHandler(UIEVENT_TYPE.TARGET_CHANGED, RF.Combat.handleTargetChanged)
  UIParent:SetEventHandler(UIEVENT_TYPE.COMBAT_MSG, RF.Combat.handleCombatMessage)
  UIParent:SetEventHandler(UIEVENT_TYPE.UNIT_DEAD_NOTICE, RF.Combat.handleUnitDead)
  UIParent:SetEventHandler(UIEVENT_TYPE.STARTED_DUEL, RF.Combat.handleDuelStarted)
  UIParent:SetEventHandler(UIEVENT_TYPE.ENDED_DUEL, RF.Combat.handleDuelEnded)
  UIParent:SetEventHandler(UIEVENT_TYPE.CHAT_JOINED_CHANNEL, RF.Chat.handleChatChannelJoined)
  UIParent:SetEventHandler(UIEVENT_TYPE.TEAM_JOINTED, RF.Raid.handleCoraidEstablished)
  UIParent:SetEventHandler(UIEVENT_TYPE.TEAM_JOINT_BROKEN, RF.Raid.handleCoraidBroken)
end

-- Do the opposite of registerForEvents as part of the tear-down pattern
function deregisterForEvents()
  UIParent:RemoveEventHandler(UIEVENT_TYPE.TEAM_MEMBERS_CHANGED)
  UIParent:RemoveEventHandler(UIEVENT_TYPE.TEAM_ROLE_CHANGED)
  UIParent:RemoveEventHandler(UIEVENT_TYPE.TARGET_CHANGED)
  UIParent:RemoveEventHandler(UIEVENT_TYPE.COMBAT_MSG)
  UIParent:RemoveEventHandler(UIEVENT_TYPE.UNIT_DEAD_NOTICE)
  UIParent:RemoveEventHandler(UIEVENT_TYPE.STARTED_DUEL)
  UIParent:RemoveEventHandler(UIEVENT_TYPE.ENDED_DUEL)
  UIParent:RemoveEventHandler(UIEVENT_TYPE.CHAT_JOINED_CHANNEL)
  UIParent:RemoveEventHandler(UIEVENT_TYPE.TEAM_JOINTED)
  UIParent:RemoveEventHandler(UIEVENT_TYPE.TEAM_JOINT_BROKEN)
end

-------------------------
-- Game Event Handlers --
-------------------------
local function handleEnteredWorld()
  RF:Init()
end

-- GUARD: Ensure every module is initialized before proceeding
function guardAllModulesArePresent()
  if not RF.Raid then
    RF:Log("ERROR: RF.Raid module not initialized.")
    return false
  end
  if not RF.Combat then
    RF:Log("ERROR: RF.Combat module not initialized.")
    return false
  end
  if not RF.IPC then
    RF:Log("ERROR: RF.IPC module not initialized.")
    return false
  end
  if not RF.Parser then
    RF:Log("ERROR: RF.Parser module not initialized.")
    return false
  end
  if not RF.JSON then
    RF:Log("ERROR: RF.JSON module not initialized.")
    return false
  end
  if not RF.Config then
    RF:Log("ERROR: RF.Config module not initialized.")
    return false
  end
  if not RF.Debug then
    RF:Log("ERROR: RF.DEBUG module not initialized.")
    return false
  end
  if not RF.Chat then
    RF:Log("ERROR: RF.Chat module not initialized.")
    return false
  end
  return true
end

UIParent:SetEventHandler(UIEVENT_TYPE.ENTERED_WORLD, handleEnteredWorld) -- entry point

-- God I love Unix
