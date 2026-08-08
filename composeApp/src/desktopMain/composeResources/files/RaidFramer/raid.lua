--- Holds code pertaining to raid formation and structure
--- Module accepts updates to raid structure as events occur and maintains
--- an internal representation of the raid for use by other modules. (Like when we gotta export it all)
RF = RF or {} -- ensure global RF exists so this file can initialize RF.Raid
RF.Raid = RF.Raid or {}

RF.Raid.Roster = RF.Raid.Roster or {} -- somewhat ambiguous name but it is what it is
RF.Raid.CountRaidOne = 0 -- number of players in raid one
RF.Raid.CountRaidTwo = 0 -- number of players in raid two
RF.Raid.LastRosterUpdate = 0 -- timestamp of last roster update

RF.Raid.recentlyJoined = false
RF.Raid.isPrepared = false

-- flag for isCoraided
RF.Raid.hasCoRaid = false

-- buff scan rate limiting (called from combat loop, not from roster events)
RF.Raid.BUFF_SCAN_INTERVAL = 1 -- seconds between buff/distance/gearScore scans
RF.Raid.LastBuffScan = 0

-- enum of strings different team member change reasons we can filter by to avoid unnecessary processing
RF.TEAM_CHANGE_REASONS = {
  JOINED = "joined",
  REFRESHED = "refreshed",
  KICKED = "kicked_by_self",
  DISMISSED = "dismissed",
  JOINED_BY_SELF = "joined_by_self",
  LEAVED_BY_SELF = "leaved_by_self",
  MOVED = "moved",
  OWNER_CHANGED = "owner_changed",
  INVITATION_REJECTED = "invitation_rejected",
}

-- numeric list of roles for healer, tank, dps and ranged etc
-- literally just doing the colors because no one can agree on what colors map to what roles
RF.Raid.ROLES = {
  BLUE = 0,
  GREEN = 1,
  PINK = 2,
  RED = 3,
  PURPLE = 4
}

-- what one raid member looks like
function RF.Raid.NewRaidMember(slot)
  return {
    slot = slot,                 -- 1..100
    playerName = "",             -- string
    role = 0,                    -- raid frame color
    gearScore = 0,               -- number
    characterBuild = "",         -- string
    lastZone = "",               -- string
    distance = -1,               -- meters, -1 = unknown
    lastUpdated = os.time(), -- used to track staleness of data at the higher layers
    buffs = {},                  -- list of buff objects (each with nested tooltip)
  }
end

function RF.Raid.GetRaidRoster()
  return RF.Raid.Roster
end

-- allocates and initializes raid member structures
function RF.Raid.Prepare()
  for i = 1, 100 do
    RF.Raid.Roster[i] = RF.Raid.NewRaidMember(i)
  end
  RF.Raid.isPrepared = true
end

function RF.Raid.handleTeamRoleChanged(...)
  --RF.Debug.dumpTable({...})
end

-- allows us to set data for a raid slot from outside the module
function RF.Raid.UpdateRaidSlot(slot, data)
  local member = RF.Raid.Roster[slot]
  if not member then return end

  -- takes the key-value pairs and updates the fields of the new member (shallow copy)
  for k, v in pairs(data) do
    member[k] = v
  end

  -- important to note that this is UTC time, we don't expose local time here
  member.lastUpdated = os.time()  -- "!*t" returns a table in UTC
end

-- handler for when we detect changes to the raid roster
-- note that there's a lot of raid refreshes that aren't actual changes (so filter accordingly)
function RF.Raid.handleTeamMembersChanged(reason, ...)

  -- GUARD: reason is not refresh (don't waste api calls/processing on non-changes)
  if reason == RF.TEAM_CHANGE_REASONS.REFRESHED then
    if (RF.Raid.recentlyJoined) then
      scanForCoRaid() -- scan on first refresh after joining
      RF.Raid.recentlyJoined = false
    end

    -- allow scanning because of a refresh if we haven't done that for 5 seconds
    if (os.time() - RF.Raid.LastRosterUpdate) >= 5 then
      RF.Raid.LastRosterUpdate = os.time()
    else
      return -- skip processing on rapid refreshes (there's a lot of these and we're trying to be more efficient)
    end
  end

  -- GUARD: make sure there's empty raid slots to fill with players
  if not RF.Raid.isPrepared then
    RF.Raid.Prepare()
  end

  -- set flag to scan on next refresh (scanning immediately here is too early, game hasn't populated slots yet)
  if reason == RF.TEAM_CHANGE_REASONS.JOINED_BY_SELF then
    RF.Raid.recentlyJoined = true
    return -- wait for refresh to do the firstscan
  end

  -- if we just left a raid, clear co-raid status
  if reason == RF.TEAM_CHANGE_REASONS.LEAVED_BY_SELF or reason == RF.TEAM_CHANGE_REASONS.KICKED then
    RF.Raid.hasCoRaid = false
    RF.Raid.recentlyJoined = false
    RF.Raid.Prepare() -- reset roster
    RF:Log("Left raid, clearing co-raid status.")
  end
  
  -- something must have changed: scan raid slots and update roster (scan both raids if co-raid)
  -- note: only updates playerName and role, buffs/distance/gearScore are handled by ScanBuffs
  if not RF.Raid.hasCoRaid then
    for position = 1, 50 do
      local raidMember = X2Unit:UnitName(string.format("team%02d", position))
      local raidRole = X2Team:GetRole(0, position)
      if raidMember then
        --RF:Log(string.format("Raid Slot %02d: %s", position, raidMember))
        RF.Raid.UpdateRaidSlot(position, { playerName = raidMember, role = raidRole })
      end
    end
  else
    for position = 1, 50 do
      local raidMember = X2Unit:UnitName(string.format("team_01_%02d", position))
      local raidRole = X2Team:GetRole(1, position)
      if raidMember then
        --RF:Log(string.format("Main Raid Raid Slot %02d: %s", position, raidMember))
        RF.Raid.UpdateRaidSlot(position, { playerName = raidMember, role = raidRole })
      end
      local raidTwoMember = X2Unit:UnitName(string.format("team_02_%02d", position))
      local raidRole = X2Team:GetRole(2, position)
      if raidTwoMember then
        --RF:Log(string.format("Co-Raid Raid Slot %02d: %s", position + 50, raidTwoMember))
        RF.Raid.UpdateRaidSlot(position + 50, { playerName = raidTwoMember, role = raidRole })
      end
    end
  end

  -- ipc export updated raid roster
  RF.IPC.WriteMessage(
    RF.IPC.MESSAGE_TYPES.FRAMES_UPDATE,
    RF.Raid.GetRaidRoster()
  )

  if not RF.Config.SHOW_RAID_STATUS then
    return
  end

  -- output the number of players in raid one and raid two for logging purposes
  local countRaidOne = 0
  local countRaidTwo = 0
  for i = 1, 50 do
    local r1 = RF.Raid.Roster[i]
    if r1 and r1.playerName and r1.playerName ~= "" then
      countRaidOne = countRaidOne + 1
    end

    local r2 = RF.Raid.Roster[i + 50]
    if r2 and r2.playerName and r2.playerName ~= "" then
      countRaidTwo = countRaidTwo + 1
    end
  end

  -- helper to format player counts nicely
  local function fmtPlayers(n)
    if n == 0 then return "no players" end
    if n == 1 then return "1 player" end
    return string.format("%d players", n)
  end

  if countRaidTwo > 0 then
    if (countRaidOne ~= RF.Raid.CountRaidOne or countRaidTwo ~= RF.Raid.CountRaidTwo) then
      RF:Log(string.format("Currently there are %s in raid one, and %s in raid two. (%s total)", fmtPlayers(countRaidOne), fmtPlayers(countRaidTwo), fmtPlayers(countRaidOne + countRaidTwo)))
    end
  else
    if (countRaidOne ~= RF.Raid.CountRaidOne) then
      RF:Log(string.format("Currently there are %s in the raid.", fmtPlayers(countRaidOne)))
    end
  end

  RF.Raid.CountRaidOne = countRaidOne
  RF.Raid.CountRaidTwo = countRaidTwo
end

-- scans for co-raid presence if we just joined a raid and the events for it have not fired yet
-- afterwards the events are trusted to keep the state updated so we aren't scanning repeatedly
function scanForCoRaid()
  local hasCoRaid = false
  for position = 1, 50 do
    local raidMember = X2Unit:UnitName(string.format("team_02_%02d", position))
    if raidMember then
      hasCoRaid = true
      break
    end
  end
  if hasCoRaid then
    RF.Raid.hasCoRaid = true
    RF:Log("Joined a raid that has a co-raid.")
  else
    RF.Raid.hasCoRaid = false
    RF:Log("Joined a raid that does not have a co-raid.")
  end
end

-- scans buffs for a single unit, returns the buffs table
function RF.Raid.ScanUnitBuffs(unitId)
  local buffCount = X2Unit:UnitBuffCount(unitId)
  if not buffCount or buffCount <= 0 then return {} end
  local buffs = {}
  for buffId = 1, buffCount do
    local rawBuff = X2Unit:UnitBuff(unitId, buffId)
    if rawBuff then
      local rawTooltip = X2Unit:UnitBuffTooltip(unitId, buffId)
      local buff = RF.Parser.ParseUnitBuff(rawBuff)
      if rawTooltip then
        buff.tooltip = RF.Parser.ParseUnitBuffTooltip(rawTooltip)
      end
      buffs[#buffs + 1] = buff
    end
  end
  return buffs
end

-- periodic scan of buffs, distance and gearScore for all occupied roster slots
-- called from the combat event loop (the only periodic hook in the ArcheAge API)
function RF.Raid.ScanBuffs()
  local now = os.time()
  if (now - RF.Raid.LastBuffScan) < RF.Raid.BUFF_SCAN_INTERVAL then
    return
  end
  RF.Raid.LastBuffScan = now

  if not RF.Raid.isPrepared then return end

  local scanned = 0
  local withBuffs = 0

  if not RF.Raid.hasCoRaid then
    for position = 1, 50 do
      local member = RF.Raid.Roster[position]
      if member then
        local raidMember = X2Unit:UnitName(string.format("team%02d", position))
        if raidMember then
          local unitId = string.format("team%02d", position)
          member.playerName = raidMember
          member.role = X2Team:GetRole(0, position)
          member.buffs = RF.Raid.ScanUnitBuffs(unitId)
          local dist = X2Unit:UnitDistance(unitId)
          if type(dist) == "table" then
            member.distance = math.floor(dist.distance or -1)
          elseif type(dist) == "number" then
            member.distance = math.floor(dist)
          else
            member.distance = -1
          end
          member.gearScore = tonumber(X2Unit:UnitGearScore(unitId, false)) or 0
          scanned = scanned + 1
          if #member.buffs > 0 then withBuffs = withBuffs + 1 end
        else
          RF.Raid.Roster[position] = RF.Raid.NewRaidMember(position)
        end
      end
    end
  else
    for position = 1, 50 do
      local member = RF.Raid.Roster[position]
      if member then
        local raidMember = X2Unit:UnitName(string.format("team_01_%02d", position))
        if raidMember then
          local unitId = string.format("team_01_%02d", position)
          member.playerName = raidMember
          member.role = X2Team:GetRole(1, position)
          member.buffs = RF.Raid.ScanUnitBuffs(unitId)
          local dist = X2Unit:UnitDistance(unitId)
          if type(dist) == "table" then
            member.distance = math.floor(dist.distance or -1)
          elseif type(dist) == "number" then
            member.distance = math.floor(dist)
          else
            member.distance = -1
          end
          member.gearScore = tonumber(X2Unit:UnitGearScore(unitId, false)) or 0
          scanned = scanned + 1
          if #member.buffs > 0 then withBuffs = withBuffs + 1 end
        else
          RF.Raid.Roster[position] = RF.Raid.NewRaidMember(position)
        end
      end
      local memberTwo = RF.Raid.Roster[position + 50]
      if memberTwo then
        local raidTwoMember = X2Unit:UnitName(string.format("team_02_%02d", position))
        if raidTwoMember then
          local unitId = string.format("team_02_%02d", position)
          memberTwo.playerName = raidTwoMember
          memberTwo.role = X2Team:GetRole(2, position)
          memberTwo.buffs = RF.Raid.ScanUnitBuffs(unitId)
          local dist = X2Unit:UnitDistance(unitId)
          memberTwo.distance = (type(dist) == "table") and dist.distance or dist
          memberTwo.gearScore = X2Unit:UnitGearScore(unitId, false)
          scanned = scanned + 1
          if #memberTwo.buffs > 0 then withBuffs = withBuffs + 1 end
        else
          RF.Raid.Roster[position + 50] = RF.Raid.NewRaidMember(position + 50)
        end
      end
    end
  end

  -- send updated roster to the app immediately
  RF.IPC.WriteMessage(
    RF.IPC.MESSAGE_TYPES.FRAMES_UPDATE,
    RF.Raid.GetRaidRoster()
  )

  --RF:Log(string.format("[Raid] Buff scan: %d players scanned, %d with buffs", scanned, withBuffs))
end

-- these are updated when the event fires -or- when we detect players in the second raid
function RF.Raid.handleCoraidEstablished()
  RF.Raid.hasCoRaid = true
  RF:Log("[Raid] Joint raid established")
end
function RF.Raid.handleCoraidBroken()
  RF.Raid.hasCoRaid = false
  -- Clear the second raid slots to prevent stale data
  for i = 51, 100 do
    RF.Raid.Roster[i] = RF.Raid.NewRaidMember(i)
  end
  RF:Log("[Raid] Joint raid broken")
end
