-- This code establishes a type of a inter-process communication (IPC) bus for the various components
-- of Raid Framer to communicate.
RF = RF or {}
RF.IPC = RF.IPC or {}

RF.IPC.BASE_DIR = "../Documents/Addon/RaidFramer/"
RF.IPC.CHANNEL_OUT_FILE = RF.IPC.BASE_DIR .. "ipc.rfout"
RF.IPC.CHANNEL_IN_FILE  = RF.IPC.BASE_DIR .. "ipc.rfin"
RF.IPC.LAST_READ_TIME = 0
RF.IPC.READ_COOLDOWN = 1 -- seconds

RF.IPC.MESSAGE_VERSION = 1

RF.IPC.MESSAGE_TYPES = {
  CAST = "PLAYER_CAST",
  DAMAGE = "PLAYER_DAMAGE",
  HEAL = "PLAYER_HEAL",
  DEBUFF = "PLAYER_DEBUFF",
  BUFF = "PLAYER_BUFF",
  DEATH = "PLAYER_DEATH",
  WORLD = "WORLD_EVENT",
  FRAMES_UPDATE = "FRAMES_UPDATE",
  TARGET_UPDATE = "TARGET_UPDATE",
  SELF_UPDATE = "SELF_UPDATE",
  SELF_FACTION = "SELF_FACTION",
  SOUND_ALERT = "SOUND_ALERT",
  AOE_SPLAT = "AOE_SPLAT",
  TEST_PING = "TEST_PING",
  CONFIG_UPDATE = "CONFIG_UPDATE"
}

function RF.IPC.WriteMessage(msgType, payload)
  local message = RF.IPC.BuildIPCMessage(msgType, payload)
  local json = RF.JSON.json_encode(message)
  local f = io.open(RF.IPC.CHANNEL_OUT_FILE, "a")
  if not f then return end
  f:write(json)
  f:write("\n")  -- newline-delimited JSON (important!)
  f:close()
end

-- Will clear the input file after reading because of the problem where the file
-- just keeps getting bigger and bigger aaaaaaa
function RF.IPC.ReadMessages()

  -- rate limit ( every READ_COOLDOWN seconds )
  local now = os.time(os.date("!*t"))
  if now - RF.IPC.LAST_READ_TIME < RF.IPC.READ_COOLDOWN then
    return
  end
  RF.IPC.LAST_READ_TIME = now -- almost forgot this LOL

  local f = io.open(RF.IPC.CHANNEL_IN_FILE, "r")
  if not f then return end

  local lines = {}
  for line in f:lines() do
    if line ~= "" then
      lines[#lines + 1] = line
    end
  end
  f:close()

  if #lines == 0 then return end

  -- Clear file after reading
  f = io.open(RF.IPC.CHANNEL_IN_FILE, "w")
  if f then f:close() end

  -- Process messages
  for _, line in ipairs(lines) do
    RF.IPC.HandleRawMessage(line)
  end
end

function RF.IPC.BuildIPCMessage(msgType, payload)
  return {
    version = RF.IPC.MESSAGE_VERSION,
    type = msgType,
    timestamp = os.time(os.date("!*t")),
    payload = payload or {}
  }
end

function RF.IPC.ResetOutputFile()
  local f = io.open(RF.IPC.CHANNEL_OUT_FILE, "w")
  if f then
    f:write("")  -- truncate
    f:close()
  end
end

function RF.IPC.HandleRawMessage(rawMessage)

  local success, message = pcall(RF.JSON.json_decode, rawMessage)
  if not success then
    RF:Log("IPC: Failed to decode message: " .. tostring(message))
    return
  end

  -- switch based on message.type
  if message.type == RF.IPC.MESSAGE_TYPES.TEST_PING then
    RF.IPC.WriteMessage(RF.IPC.MESSAGE_TYPES.TEST_PING, { reply = "pong" })
    if RF.PLAYER_NAME ~= "" and RF.FACTION ~= "" then
      RF.IPC.WriteMessage(RF.IPC.MESSAGE_TYPES.SELF_UPDATE, RF.PLAYER_NAME)
      RF.IPC.WriteMessage(RF.IPC.MESSAGE_TYPES.SELF_FACTION, RF.FACTION)
    end
  elseif message.type == RF.IPC.MESSAGE_TYPES.CONFIG_UPDATE then
    RF:Log("Addon configuration updated via the desktop app.")
    RF.Config.LoadConfig()
  else
    RF:Log("IPC: Received unknown message type: " .. tostring(message.type))
  end
end
