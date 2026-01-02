-- This is where all the parsers can go ~
RF = RF or {}
RF.Parser = RF.Parser or {}

function RF.Parser.ParseUnitInfo(t)
  local info = {}
  info.type       = t[1]   -- "player", "npc", etc
  info.isPortal   = t[2]   -- bool
  info.classId    = t[3]   -- table [1 = x, 2 = y, 3 = z]
  info.hp         = t[4]   -- int
  info.grade      = t[5]   -- LEGENDARYYYYY
  info.kind       = t[6]   -- "humanoid", "beast", etc
  info.familyName = t[7]   -- "wolf", "dragon", etc
  info.name       = t[8]   -- Like the actual name of the unit
  info.faction    = t[9]   -- "friendly", "hostile"
  info.level      = t[10]  -- int 55
  info.heirLevel  = t[11]  -- int no idea what this is
  return info
end

function RF.Parser.ParseUnitDeathEvent(t)
  local evt = {}
  evt.tuuid      = t[1]
  evt.unknownInt = t[2]
  evt.unknownInt = t[3]
  return evt
end

-- Tries to parse the raw event args from the game into a structured table
-- the game has many event types with different arg structures under the hood
-- so this is a simplified parser that only extracts common fields for now
function RF.Parser.ParseCombatEvent(t)
  local evt = {}

  evt.cid          = t[1]
  evt.eventType    = t[2]
  evt.source       = t[3]
  evt.target       = t[4]
  evt.buffId       = t[5]
  evt.buffName     = t[6]
  evt.school       = t[7]
  evt.auraType     = t[8] -- auraType (e.g., DEBUFF, BUFF)
  evt.unknownBool  = t[9]

  -- Environmental damage special case
  if evt.eventType == "ENVIRONMENTAL_DAMAGE" then
    evt.envType = t[6] -- FALLING
    evt.amount  = tonumber(t[7])
    evt.result  = t[9] -- HIT
  end

  evt.timestamp  = os.time()

  return evt
end
