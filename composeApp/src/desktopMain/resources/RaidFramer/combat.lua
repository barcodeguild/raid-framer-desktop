--- Deals with combat events and tracking buffs, damage, heals, deaths, player metadata, etc
RF = RF or {}
RF.Combat = RF.Combat or {}

-- tell desktop app when a unit dies
function RF.Combat.handleUnitDead(...)
  local args = { ... }
  local evt = RF.Parser.ParseUnitDeathEvent(args)
  RF.IPC.WriteMessage(
      RF.IPC.MESSAGE_TYPES.DEATH,
      X2Unit:GetUnitNameById(evt.tuuid)
  )
end

-- duel event handlers - notify desktop app (we may treat combat events during duels differently later)
function RF.Combat.handleDuelStarted()
  RF.IPC.WriteMessage(RF.IPC.MESSAGE_TYPES.DUEL_STARTED, os.time(os.date("!*t")))
end
function RF.Combat.handleDuelEnded(...)
  RF.IPC.WriteMessage(RF.IPC.MESSAGE_TYPES.DUEL_ENDED, os.time(os.date("!*t")))
end

-- main combat event handler
function RF.Combat.handleCombatMessage(...)
  RF.IPC.ReadMessages() -- Check if there's config changes (rate limited to every 5s!)

  local args = { ... }
  local evt = RF.Parser.ParseCombatEvent(args)

  --
  -- send all buff related events to desktop app to build us pretty graphs
  --

  -- debuffs
  if (evt.auraType == "DEBUFF") then
    RF.IPC.WriteMessage(RF.IPC.MESSAGE_TYPES.DEBUFF, RF.JSON.json_encode(evt))
  end

  -- buffs
  if (evt.auraType == "BUFF") then
    RF.IPC.WriteMessage(RF.IPC.MESSAGE_TYPES.BUFF, RF.JSON.json_encode(evt))
  end

  --
  -- handle combat-related addon features below (implemented on the Lua side)
  --

  -- charmed
  if (evt.buffName == "Charmed" and evt.eventType == "SPELL_AURA_APPLIED") then
    if (RF.Config.SHOW_CHARMED_IN_CHAT) then
      RF:Log(tostring(evt.source) .. " charmed " .. tostring(evt.target))
    end
    if (evt.source == RF.PLAYER_NAME) then
      X2Sound:PlayUISound("event_trade_lock", true)
    end
  end

  -- silenced
  if (evt.buffName == "Silence" and evt.eventType == "SPELL_AURA_APPLIED") then
    if (RF.Config.SHOW_SILENCED_IN_CHAT) then
      RF:Log(tostring(evt.source) .. " silenced " .. tostring(evt.target))
    end
  end

  -- distressed
  if (evt.buffName == "Distressed" and evt.eventType == "SPELL_AURA_APPLIED") then
    if (RF.Config.SHOW_DISTRESSED_IN_CHAT) then
      RF:Log(tostring(evt.source) .. " distressed " .. tostring(evt.target))
    end
  end
end
