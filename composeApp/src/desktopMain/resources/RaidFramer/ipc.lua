-- This code establishes a type of a inter-process communication (IPC) bus for the various components
-- of Raid Framer to communicate.
RF = RF or {}
RF.IPC = RF.IPC or {}

RF.IPC.BASE_DIR = "../Documents/Addon/RaidFramer/"
RF.IPC.CHANNEL_OUT_FILE = RF.IPC.BASE_DIR .. "ipc.rfout"
RF.IPC.CHANNEL_IN_FILE  = RF.IPC.BASE_DIR .. "ipc.rfin"
RF.IPC.MESSAGE_VERSION = 1

-- queue structure that holds future messages to be written to the ipc file
RF.IPC.BATCH_SIZE = 300 -- 300 events per every other second
RF.IPC.BATCH_COOLDOWN = 1 -- seconds
RF.IPC.LAST_INTERACT_TIME = 0

RF.IPC.MESSAGE_WRITE_QUEUE = {}

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

-- Flushes batches of the write queue to the output file by calling write message
-- poke does some work and then returns; this is called periodically by the main addon loop
-- and it's meant to smooth out writes over time instead of spamming writes in the combat event handler
function RF.IPC.interact()

  local now = os.time()

  -- GUARD: Only write on odd seconds to reduce contention with desktop (that only reads on even seconds)
  if now % 2 == 0 then
    return
  end

  -- GUARD: Should we write right now? (rate limit)
  if now - RF.IPC.LAST_INTERACT_TIME < RF.IPC.BATCH_COOLDOWN then
    return
  end

  -- We've decided we're doing stuff; update last interact time
  RF.IPC.LAST_INTERACT_TIME = now

  -- First check if there's anything to read
  RF.IPC.ReadMessages()

  -- GUARD: Anything to write? (Done before call to os.time for efficiency?)
  if #RF.IPC.MESSAGE_WRITE_QUEUE == 0 then
    return
  end

  -- Determine batch size (process up to BATCH_SIZE messages)
  local batchSize = math.min(RF.IPC.BATCH_SIZE, #RF.IPC.MESSAGE_WRITE_QUEUE)

  -- Open file once for batch write
  local f = io.open(RF.IPC.CHANNEL_OUT_FILE, "a")
  if not f then return end

  -- Write batch of messages
  for i = 1, batchSize do
    local json = RF.IPC.MESSAGE_WRITE_QUEUE[i]
    f:write(json)
    f:write("\n")
  end
  f:close()

  -- Remove processed messages from queue
  for i = batchSize, 1, -1 do
    table.remove(RF.IPC.MESSAGE_WRITE_QUEUE, 1)
  end
end

-- Queues a message to be sent later; returns the JSON string
function RF.IPC.EnqueueWriteMessage(msgType, payload)
  local message = RF.IPC.BuildIPCMessage(msgType, payload)
  local json = RF.JSON.json_encode(message)
  table.insert(RF.IPC.MESSAGE_WRITE_QUEUE, json)
  return json
end

-- Immediately writes a message to the output file
-- The reason we don't call this in the interaction loop is because then writing batches
-- would constantly open and close the file, which is inefficient.
-- but this is how we used to do combat event writes before batching was implemented
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
    timestamp = os.time(),
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
