-- This is where all the parsers can go ~
RF = RF or {}
RF.Parser = RF.Parser or {}

function RF.Parser.ParseUnitDeathEvent(t)
  local evt = {}
  evt.tuuid      = t[1]
  evt.unknownInt = t[2]
  evt.unknownInt = t[3]
  return evt
end

-- Extracts just the metadata from a combat event so we can perform routing logic on it / client-side actions
-- before passing the whole unprocessed table to the desktop app as a json string. The game itself has multiple
-- sub-types combat events (damage, heal, buff, etc) and we don't want to have to parse all of those in Lua just to route them
-- because it would waste single-threaded CPU time. So we just parse the metadata we need to make decisions on the Lua side
-- and pass to the multi-threaded desktop app for full processing.
function RF.Parser.ParseCombatEventMetadata(t)
  local event = {}

  event.timestamp  = os.time()
  event.cid          = t[1] -- always entity id
  event.type         = t[2] -- always event type
  event.source       = t[3] -- always source name
  event.target       = t[4] -- always target name

  return event
end

-- SPELL_AURA_APPLIED, SPELL_AURA_REMOVED
function RF.Parser.ParseBuffEvent(t)
  local event = {}

  event.timestamp  = os.time()
  event.cid          = t[1] -- always entity id
  event.type         = t[2] -- always event type
  event.source       = t[3] -- always source name
  event.target       = t[4] -- always target name

  -- specific fields for buff events
  event.buffId       = t[5] -- buff id or 0
  event.buffName     = t[6] -- buff name or ""
  event.damageType   = t[7] -- PHYSICAL
  event.buffType     = t[8] -- DEBUFF / BUFF
  event.isActive     = t[9] -- not sure what this is yet

  return event
end

-- SPELL_DAMAGE
function RF.Parser.ParseDamageEvent(t)
  local event = {}

  event.timestamp    = os.time()
  event.cid            = t[1] -- always entity id
  event.type           = t[2] -- always event type
  event.source         = t[3] -- always source name
  event.target         = t[4] -- always target name

  -- specific fields for damage events
  event.unknownInt    = t[5] -- always seems to be zero
  event.spell         = t[6] -- name of spell
  event.damageType    = t[7] -- PHYSICAL / FIRE / etc
  event.amount        = t[8] -- integer (negative means removed health)
  event.pool          = t[9] -- HEALTH / MANA
  event.result        = t[10] -- string HIT
  event.f11           = t[11] -- zero int
  event.f12           = t[12] -- zero int
  event.f13           = t[13] -- bool
  event.f14           = t[14] -- bool
  event.f15           = t[15] -- bool

  return event
end