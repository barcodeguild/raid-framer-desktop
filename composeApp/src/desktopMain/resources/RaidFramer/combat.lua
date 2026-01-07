--- Deals with combat events and tracking buffs, damage, heals, deaths, player metadata, etc
RF = RF or {}
RF.Combat = RF.Combat or {}

-- Cache of player last player meta calls timestamps to avoid calling too many AR API functions
-- Character Name -> Timestamp of last fetch
RF.Combat.REPORT_COOLDOWN = 30 -- seconds
RF.Combat.ENTITY_REPORT_COOLDOWNS = {}
RF.Combat.EVENTS_PER_MINUTE = 0 -- for informative purposes / monitoring addon performance
RF.Combat.EVENTS_PER_MINUTE_LAST_RESET = os.time()

-- tell desktop app when a unit dies
function RF.Combat.handleUnitDead(...)
  local args = { ... }
  local evt = RF.Parser.ParseUnitDeathEvent(args)
  RF.IPC.WriteMessage(RF.IPC.MESSAGE_TYPES.PLAYER_DEATH, X2Unit:GetUnitNameById(evt.tuuid))
end

-- duel event handlers - notify desktop app (we may treat combat events during duels differently later)
function RF.Combat.handleDuelStarted()
  RF.IPC.WriteMessage(RF.IPC.MESSAGE_TYPES.DUEL_STARTED, os.time())
end
function RF.Combat.handleDuelEnded(...)
  RF.IPC.WriteMessage(RF.IPC.MESSAGE_TYPES.DUEL_ENDED, os.time())
end


--function RF.Raid.NewPlayerMeta(slot)
--  return {
--    playerName = "",             -- string
--    gearScore = 0,               -- number
--    characterBuild = "",         -- string
--    lastZone = "",               -- string
--    distance = -1,               -- meters, -1 = unknown
--    lastUpdated = os.time(), -- used to track staleness of data at the higher layers
--  }
--end
--
--local lastReokyCid = ""


-- 24599 healing circle flower buff

-- main combat event handler
function RF.Combat.handleCombatMessage(...)
  RF.IPC.interact() -- Rate limited write of queued messages and also triggers read of incoming messages on a cooldown

  local combatEvent = { ... }
  local meta = RF.Parser.ParseCombatEventMetadata(combatEvent)

  RF.Combat.EVENTS_PER_MINUTE = RF.Combat.EVENTS_PER_MINUTE + 1

  -- if it's been a minute, reset the counter
  -- removes the need to call os.time() multiple times per event by using the ts from the previous event
  if (meta.timestamp - RF.Combat.EVENTS_PER_MINUTE_LAST_RESET) >= 60 then
    local kbps = (RF.Combat.EVENTS_PER_MINUTE * 256) / 1024 -- assuming average event size of 256 bytes
    RF:Log("[Performance] Combat event logging rate is about: " .. tostring(math.floor(kbps)) .. " KB/s)")
    RF.Combat.EVENTS_PER_MINUTE_LAST_RESET = meta.timestamp -- move timestamp pointer to now
    RF.Combat.EVENTS_PER_MINUTE = 0 -- reset counter
  end

  -- populate the local player info cache and dispatch player meta info to desktop app
  -- if it's something we've never seen before (like someone just spawned a dragon and we
  -- need to know right meow)
  if not RF.Combat.ENTITY_REPORT_COOLDOWNS[meta.target] or (meta.timestamp - RF.Combat.ENTITY_REPORT_COOLDOWNS[meta.target]) >= RF.Combat.REPORT_COOLDOWN  or meta.cid == "0" then
    local result = X2Unit:GetUnitInfoById(meta.cid)
    if (result) then
      if (result["type"] == "character") then
        RF:Log("Caching player info for " .. meta.target)
      elseif (result["type"] == "npc") then
        if (result["is_portal"]) then
          if (RF.Combat.EVENTS_PER_MINUTE >= 500) then
            RF:Log("Oh God a portal during PvP..")
          end
        else
          RF:Log("Caching NPC info for " .. meta.target)
        end
      elseif (result["type"] == "mate") then
        RF:Log("Caching companion info for " .. meta.target)
      end
    else
      RF:Log("Could not retrieve unit info for " .. meta.source .. " (CID: " .. tostring(meta.cid) .. ")")
    end
    -- send whatever the game gave us directly to the desktop app for processing on another thread (can't do that in aa lua because there is only one thread)
    RF.IPC.EnqueueWriteMessage(RF.IPC.MESSAGE_TYPES.PLAYER_INFO, RF.JSON.json_encode(result))
    RF.Combat.ENTITY_REPORT_COOLDOWNS[meta.target] = meta.timestamp
  end

  -- write combat events by type to IPC queue
  if (meta.type == "SPELL_AURA_APPLIED" or meta.type == "SPELL_AURA_REMOVED") then
    local buff = RF.Parser.ParseBuffEvent(combatEvent)
    RF.IPC.EnqueueWriteMessage(RF.IPC.MESSAGE_TYPES.COMBAT_EVENT, RF.JSON.json_encode(buff))
    RF.Combat.postEventsForBuff(buff)
    return
  end
  if (meta.type == "SPELL_DAMAGE") then
    RF.IPC.EnqueueWriteMessage(RF.IPC.MESSAGE_TYPES.COMBAT_EVENT, RF.JSON.json_encode(RF.Parser.ParseDamageEvent(combatEvent)))
  end

end

--
-- handle combat-related addon features below (implemented on the Lua side)
--

-- basically just local stuff that we want to do when certain buffs are applied/removed
function RF.Combat.postEventsForBuff(buff)
  -- charmed
  if (buff.buffName == "Charmed" and buff.type == "SPELL_AURA_APPLIED") then
    if (RF.Config.SHOW_CHARMED_IN_CHAT) then
      RF:Log(tostring(buff.source) .. " charmed " .. tostring(buff.target))
    end
    if (buff.source == RF.PLAYER_NAME) then
      X2Sound:PlayUISound("event_trade_lock", true)
    end
  end

  -- silenced
  if (buff.buffName == "Silence" and buff.type == "SPELL_AURA_APPLIED") then
    if (RF.Config.SHOW_SILENCED_IN_CHAT) then
      RF:Log(tostring(buff.source) .. " silenced " .. tostring(buff.target))
    end
  end

  -- distressed
  if (buff.buffName == "Distressed" and buff.type == "SPELL_AURA_APPLIED") then
    if (RF.Config.SHOW_DISTRESSED_IN_CHAT) then
      RF:Log(tostring(buff.source) .. " distressed " .. tostring(buff.target))
    end
  end
end