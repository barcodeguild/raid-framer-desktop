-- This code establishes a type of a inter-process communication (IPC) bus for the various components
-- of Raid Framer to communicate.
RF = RF or {}
RF.IPC = RF.IPC or {}

RF.IPC.BASE_DIR = "../Documents/Addon/RaidFramer/"
RF.IPC.CHANNEL_OUT_FILE = RF.IPC.BASE_DIR .. "ipc.rfout"
RF.IPC.CHANNEL_IN_FILE  = RF.IPC.BASE_DIR .. "ipc.rfin"
RF.IPC.MESSAGE_VERSION = 1

-- queue structure that holds future messages to be written to the ipc file
RF.IPC.BATCH_SIZE = 10000 -- 10000 events per every other second
RF.IPC.BATCH_COOLDOWN = 1 -- seconds
RF.IPC.LAST_INTERACT_TIME = 0

-- Convert the simple array into a head/tail queue to avoid expensive copies when trimming the front
RF.IPC.MESSAGE_WRITE_QUEUE = {}
RF.IPC.MESSAGE_WRITE_QUEUE_HEAD = 1
RF.IPC.MESSAGE_WRITE_QUEUE_TAIL = 0

-- Persistent output file handle to avoid repeated open/close
RF.IPC.OUT_FH = nil

RF.IPC.MESSAGE_TYPES = {
  COMBAT_EVENT = "COMBAT_EVENT", -- see game monitor branch (not used yet)
  PLAYER_DEATH = "PLAYER_DEATH", -- from the callback when a unit dies
  PLAYER_INFO = "PLAYER_INFO", -- authoritative entity update (we know for sure some piece of info in the form of raw X2UnitInfo data)
  WORLD_EVENT = "WORLD_EVENT", -- thinking maybe zone changes / like going to conflict/peace could be useful to know down the line (not used yet)
  FRAMES_UPDATE = "FRAMES_UPDATE", -- full raid frames update (sent periodically when the raid roster changes)
  TARGET_UPDATE = "TARGET_UPDATE", -- used for the debuff tracker to select the target the players tabs over (similar to other debuff trackers)
  SELF_UPDATE = "SELF_UPDATE", -- sets own player name
  SELF_FACTION = "SELF_FACTION", -- sets own faction (very important for knowing who is an enemy LOL)
  DUEL_STARTED = "DUEL_STARTED", -- duel started event
  DUEL_ENDED = "DUEL_ENDED", -- duel ended event
  SOUND_ALERT = "SOUND_ALERT", -- play a sound alert on the companion (not used yet, but I want to get the unreal tournament sounds for this LOL)
  AOE_SPLAT = "AOE_SPLAT", -- hoping maybe Queen Sparkles will open the animation api someday (not used yet but I want to draw sick animations on the ground for charms and stuff!)
  TEST_PING = "TEST_PING", -- companion replies with a "pong" payload (used for the lua companion indicator LED)
  CONFIG_UPDATE = "CONFIG_UPDATE" -- app notifies addon that config has changed, prompts a reload from disk
}

-- Helper: open persistent output file handle (append mode). Returns file handle or nil.
function RF.IPC.OpenOutFile()
  -- if we already have a handle, check it's still valid
  if RF.IPC.OUT_FH then
    local ok = pcall(function() RF.IPC.OUT_FH:seek("cur") end)
    if ok then
      return RF.IPC.OUT_FH
    end
    -- if invalid, try to close it safely
    pcall(function() RF.IPC.OUT_FH:close() end)
    RF.IPC.OUT_FH = nil
  end

  local f = io.open(RF.IPC.CHANNEL_OUT_FILE, "a")
  if f then
    RF.IPC.OUT_FH = f
  end
  return RF.IPC.OUT_FH
end

-- Flushes batches of the write queue to the output file by calling write message
-- poke does some work and then returns; this is called periodically by the main addon loop
-- and it's meant to smooth out writes over time instead of spamming writes in the combat event handler
function RF.IPC.interact()

  -- GUARD: Should we write right now? (rate limit)
  local now = os.time()
  if now - RF.IPC.LAST_INTERACT_TIME < RF.IPC.BATCH_COOLDOWN then
    return
  end

  -- We've decided we're doing stuff; update last interact time
  RF.IPC.LAST_INTERACT_TIME = now

  -- First check if there's anything to read
  RF.IPC.ReadMessages()

  -- GUARD: Anything to write? (use head/tail math rather than # which doesn't work with nil holes)
  local head = RF.IPC.MESSAGE_WRITE_QUEUE_HEAD
  local tail = RF.IPC.MESSAGE_WRITE_QUEUE_TAIL
  if tail < head then
    return
  end

  -- reset file if size is >100MB to prevent bloat
  local sizeCheck = io.open(RF.IPC.CHANNEL_OUT_FILE, "r")
  if sizeCheck then
    local size = sizeCheck:seek("end")
    sizeCheck:close()
    if size > 100 * 1024 * 1024 then -- 100MB
      RF.IPC.ResetOutputFile()
    end
  end

  -- Determine batch size (process up to BATCH_SIZE messages)
  local available = tail - head + 1
  local batchSize = math.min(RF.IPC.BATCH_SIZE, available)

  -- Open persistent file handle once for batch write
  local file = RF.IPC.OpenOutFile()
  if not file then return end

  -- Write batch of messages using head/tail indices, nil out entries so GC can reclaim them
  for i = 0, batchSize - 1 do
    local idx = head + i
    local entry = RF.IPC.MESSAGE_WRITE_QUEUE[idx]
    if entry then
      local json
      if type(entry) == "string" then
        -- already-encoded JSON (older code path); write as-is
        json = entry
      else
        -- encode table into JSON now (lazy encode)
        local ok, enc = pcall(RF.JSON.json_encode, entry)
        if ok and enc then
          json = enc
        else
          RF:Log("IPC: Failed to encode queued message: " .. tostring(enc))
          json = nil
        end
      end

      if json then
        -- write and newline; use pcall to avoid throwing into game loop
        local wrote_ok = pcall(function()
          file:write(json)
          file:write("\n")
        end)
        if not wrote_ok then
          -- if write failed, we can't do much; stop processing this batch
          break
        end
      end

      RF.IPC.MESSAGE_WRITE_QUEUE[idx] = nil
    end
  end

  -- flush to ensure data is pushed to OS (may help reduce stalls later)
  pcall(function() file:flush() end)

  -- Move head forward
  RF.IPC.MESSAGE_WRITE_QUEUE_HEAD = head + batchSize

  -- Periodic compaction to avoid head/tail numbers growing without bound and to reclaim table memory
  -- Only compact when the head index grows past a threshold to keep this cheap
  local COMPACT_THRESHOLD = 4096
  if RF.IPC.MESSAGE_WRITE_QUEUE_HEAD > COMPACT_THRESHOLD then
    local newQueue = {}
    local newTail = 0
    for i = RF.IPC.MESSAGE_WRITE_QUEUE_HEAD, RF.IPC.MESSAGE_WRITE_QUEUE_TAIL do
      newTail = newTail + 1
      newQueue[newTail] = RF.IPC.MESSAGE_WRITE_QUEUE[i]
    end
    -- log compaction for diagnostics (should be rare)
    RF:Log("IPC: Compacting write queue (old head=" .. tostring(RF.IPC.MESSAGE_WRITE_QUEUE_HEAD) .. ", old tail=" .. tostring(RF.IPC.MESSAGE_WRITE_QUEUE_TAIL) .. ")")
    RF.IPC.MESSAGE_WRITE_QUEUE = newQueue
    RF.IPC.MESSAGE_WRITE_QUEUE_HEAD = 1
    RF.IPC.MESSAGE_WRITE_QUEUE_TAIL = newTail
  end

  -- Safety: if head/tail numbers somehow grew extremely large, reset indices to avoid numeric issues
  local ABSOLUTE_INDEX_RESET = 1000000000
  if RF.IPC.MESSAGE_WRITE_QUEUE_TAIL > ABSOLUTE_INDEX_RESET then
    -- rebuild queue into a fresh one
    local rebuild = {}
    local rt = 0
    for i = RF.IPC.MESSAGE_WRITE_QUEUE_HEAD, RF.IPC.MESSAGE_WRITE_QUEUE_TAIL do
      rt = rt + 1
      rebuild[rt] = RF.IPC.MESSAGE_WRITE_QUEUE[i]
    end
    RF.IPC.MESSAGE_WRITE_QUEUE = rebuild
    RF.IPC.MESSAGE_WRITE_QUEUE_HEAD = 1
    RF.IPC.MESSAGE_WRITE_QUEUE_TAIL = rt
    RF:Log("[Performance] - Forcefully rebuilt huge IPC write queue to avoid index overflow..")
  end
end

-- Queues a message to be sent later; returns nothing (avoid heavy work on hot path)
function RF.IPC.EnqueueWriteMessage(msgType, payload)
  -- Build message table but do NOT stringify here to keep hot path cheap
  local message = RF.IPC.BuildIPCMessage(msgType, payload)

  -- Safety: warn if backlog grows large, but always enqueue (never drop).
  local backlog = RF.IPC.MESSAGE_WRITE_QUEUE_TAIL - RF.IPC.MESSAGE_WRITE_QUEUE_HEAD + 1
  if backlog < 0 then backlog = 0 end
  local MAX_BACKLOG = 300000 -- arbitrary large cap; tune if needed
  if backlog > MAX_BACKLOG then
    RF:Log("[Performance] - IPC write queue backlog exceeded " .. tostring(MAX_BACKLOG) .. " (" .. tostring(backlog) .. " pending). Consider increasing BATCH_SIZE or BATCH_COOLDOWN.")
  end

  -- push to tail (cheap table insert)
  RF.IPC.MESSAGE_WRITE_QUEUE_TAIL = RF.IPC.MESSAGE_WRITE_QUEUE_TAIL + 1
  RF.IPC.MESSAGE_WRITE_QUEUE[RF.IPC.MESSAGE_WRITE_QUEUE_TAIL] = message
  return nil
end

-- Immediately writes a message to the output file
-- The reason we don't call this in the interaction loop is because then writing batches
-- would constantly open and close the file, which is inefficient.
-- but this is how we used to do combat event writes before batching was implemented
function RF.IPC.WriteMessage(msgType, payload)
  local message = RF.IPC.BuildIPCMessage(msgType, payload)
  local json = RF.JSON.json_encode(message)
  local f = RF.IPC.OpenOutFile()
  if not f then return end
  pcall(function()
    f:write(json)
    f:write("\n")  -- newline-delimited JSON (important!)
    f:flush()
  end)
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

  -- Clear file after reading, but only after we have the content in memory.
  -- Re-opening in write mode truncates the file atomically on the addon side.
  local clear = io.open(RF.IPC.CHANNEL_IN_FILE, "w")
  if clear then
    clear:flush()
    clear:close()
  end

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
  -- close persistent handle first to ensure truncation works reliably
  if RF.IPC.OUT_FH then
    pcall(function() RF.IPC.OUT_FH:close() end)
    RF.IPC.OUT_FH = nil
  end
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
    if RF.Raid and RF.Raid.isPrepared then
      RF.IPC.WriteMessage(RF.IPC.MESSAGE_TYPES.FRAMES_UPDATE, RF.Raid.GetRaidRoster())
    end
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
