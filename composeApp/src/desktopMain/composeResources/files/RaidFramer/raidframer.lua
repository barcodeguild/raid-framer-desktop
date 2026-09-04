-- Raid Framer 2.0 Lua Companion
-- May have automatically been placed by the Raid Framer Desktop App
-- https://github.com/barcodeguild/raid-framer-desktop
-- Author: Reoky
RF = RF or {}
RF.TAG = "Raid Framer 2.4.2"

RF.PLAYER_NAME = ""
RF.FACTION = ""
local raidEventsEnabled = false

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

  RF.IPC.DORMANT = true
  raidEventsEnabled = true

  -- get player name : tell desktop app which character the user is playing
  RF.PLAYER_NAME = X2Unit:UnitName("player")
  RF.IPC.WriteMessage(RF.IPC.MESSAGE_TYPES.SELF_UPDATE, RF.PLAYER_NAME)

  self:Log("Good news, " .. RF.PLAYER_NAME .. "! If you can read this message, then the " .. RF.TAG .. " Lua component is working!")
  self:Log("Please be sure to launch the desktop app to access the multi-monitor game overlay.")

  -- Register every game event handler exactly once for the lifetime of the addon.
  -- Handlers stay registered across dormancy and guard their own work via the
  -- `eventHandlers` wrappers (and PERFORMANCE_COMPANION_ENABLED), so we never
  -- re-register and stack duplicate callbacks on the game's event bus.
  registerForEvents()
end

function RF:Shutdown()
  if not self.initialized then return end
  self.initialized = false
  raidEventsEnabled = false
  self:Log("Hehe bye!")
  deregisterForEvents()
end

-- Attach handlers to the game for various events we care about
-- Keep stable references for APIs that identify handlers by Lua function identity.
local eventHandlers = {
  teamMembersChanged = function(...)
    if raidEventsEnabled then RF.Raid.handleTeamMembersChanged(...) end
  end,
  teamRoleChanged = function(...)
    if raidEventsEnabled then RF.Raid.handleTeamRoleChanged(...) end
  end,
  targetChanged = function(...)
    if raidEventsEnabled then RF.Combat.handleTargetChanged(...) end
  end,
  combatMessage = function(...)
    if raidEventsEnabled then RF.Combat.handleCombatMessage(...) end
  end,
  unitDead = function(...)
    if raidEventsEnabled then RF.Combat.handleUnitDead(...) end
  end,
  duelStarted = function(...)
    if raidEventsEnabled then RF.Combat.handleDuelStarted(...) end
  end,
  duelEnded = function(...)
    if raidEventsEnabled then RF.Combat.handleDuelEnded(...) end
  end,
  chatJoinedChannel = function(...)
    if raidEventsEnabled then RF.Chat.handleChatChannelJoined(...) end
  end,
  teamJointed = function(...)
    if raidEventsEnabled then RF.Raid.handleCoraidEstablished(...) end
  end,
  teamJointBroken = function(...)
    if raidEventsEnabled then RF.Raid.handleCoraidBroken(...) end
  end,
}

function registerForEvents()
  UIParent:SetEventHandler(UIEVENT_TYPE.TEAM_MEMBERS_CHANGED, eventHandlers.teamMembersChanged)
  UIParent:SetEventHandler(UIEVENT_TYPE.TEAM_ROLE_CHANGED, eventHandlers.teamRoleChanged)
  UIParent:SetEventHandler(UIEVENT_TYPE.TARGET_CHANGED, eventHandlers.targetChanged)
  UIParent:SetEventHandler(UIEVENT_TYPE.COMBAT_MSG, eventHandlers.combatMessage)
  UIParent:SetEventHandler(UIEVENT_TYPE.UNIT_DEAD_NOTICE, eventHandlers.unitDead)
  UIParent:SetEventHandler(UIEVENT_TYPE.STARTED_DUEL, eventHandlers.duelStarted)
  UIParent:SetEventHandler(UIEVENT_TYPE.ENDED_DUEL, eventHandlers.duelEnded)
  UIParent:SetEventHandler(UIEVENT_TYPE.CHAT_JOINED_CHANNEL, eventHandlers.chatJoinedChannel)
  UIParent:SetEventHandler(UIEVENT_TYPE.TEAM_JOINTED, eventHandlers.teamJointed)
  UIParent:SetEventHandler(UIEVENT_TYPE.TEAM_JOINT_BROKEN, eventHandlers.teamJointBroken)
end

-- Do the opposite of registerForEvents as part of the tear-down pattern
function deregisterForEvents()
  -- Keep the stable wrappers registered. ArcheAge clients can crash while
  -- releasing native event registrations; disabling dispatch is sufficient.
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
