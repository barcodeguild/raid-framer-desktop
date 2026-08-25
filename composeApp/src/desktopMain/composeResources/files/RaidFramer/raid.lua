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
-- Note: this is intentionally 5 seconds to keep CPU usage down inside the game process.
RF.Raid.BUFF_SCAN_INTERVAL = 5 -- seconds between buff/distance/gearScore scans
RF.Raid.LastBuffScan = 0

-- whitelist of buff IDs to include in FRAMES_UPDATE (all others are skipped to reduce IPC payload)
-- add new buff IDs here as the Kotlin side needs to track them
RF.Raid.INTERESTING_BUFF_IDS = {
  [25875] = true,  -- Life Mend (healAmount tracking)
  [2385] = true,   -- Rebirth Trauma (timeLeft tracking for the raid caller overlay)
  -- Goblet: orange, blue, yellow, purple, pink, gray variants
  [24469] = true, [24470] = true, [24471] = true, [24472] = true, [24473] = true, [24474] = true,
  [21796] = true, [21801] = true, [21806] = true, [21811] = true, [21819] = true, [21846] = true,
  [21797] = true, [21802] = true, [21807] = true, [21812] = true, [21820] = true,
  [21798] = true, [21803] = true, [21808] = true, [21813] = true, [21821] = true,
  [21799] = true, [21804] = true, [21809] = true, [21814] = true, [21822] = true,
  [21800] = true, [21805] = true, [21810] = true, [21815] = true, [21823] = true,
  -- Feast table, ribs, and lower-level meatballs
  [21791] = true, [21792] = true, [21793] = true, [21794] = true, [2305] = true,
  [685] = true, [689] = true, [693] = true, [697] = true,
  [680] = true, [686] = true, [690] = true, [694] = true,
  -- Longing: regular and enhanced
  [20552] = true, [32381] = true, [32382] = true, [21795] = true, [26581] = true, [26582] = true,
  [9001811] = true, -- Whisper
  [31306] = true, -- Blessed Elixir
  [9000906] = true, [9001797] = true, -- Ancient's Potion
  [8318] = true, -- Jinhui's Wish
  [8209] = true, -- Secret Gift
  [26764] = true, -- Fairy Protection
  [5861] = true, [5862] = true, [5863] = true, [5864] = true, [5865] = true, -- Cookfire
  [5700] = true, [32233] = true, [32234] = true, [32235] = true, [32236] = true, [32237] = true, [32238] = true, [32239] = true, -- War Drum
  [6660] = true, -- Dahuta's Bubble
  [9002009] = true, -- Monster Hunter's Dream
  [3076] = true, [3075] = true, -- Flower fruits
  -- Statue buffs: Haranya, Nuia, and Pirate variants
  [30767] = true, [30764] = true, [9002338] = true, [9002337] = true, [30773] = true, [30772] = true,
  [30766] = true, [9002340] = true, [30760] = true, [9002339] = true, [30770] = true, [30771] = true,
  [30768] = true, [30765] = true, [9002342] = true, [9002341] = true,
  [23717] = true, [32025] = true, -- Strength of the Faction / War Time
  -- Loot buffs (comprehensive whitelist). These are matched by the Kotlin loot-buff
  -- definitions so the app can sum their loot-drop percentages per player. Loot buffs
  -- only signal their presence (we skip their tooltips to save CPU inside the game).
  [23215] = true, [9002077] = true, [22516] = true, [22941] = true, [22929] = true, [31422] = true,
  [8000681] = true, [8000803] = true, [9001658] = true,
  [8000726] = true, [9001956] = true, [8000779] = true, [8000794] = true, [8000795] = true, [8000796] = true,
  [23492] = true, [23491] = true, [22292] = true,
  [23094] = true, [23093] = true, [31322] = true, [8002787] = true,
  -- Comprehensive loot-buff metadata table (buff IDs from the ArcheRage wiki)
  [8000792] = true, [8000793] = true, [23493] = true, [23095] = true,
  [8000731] = true, [13834] = true, [22678] = true, [11075] = true, [11074] = true, [23097] = true,
  [25791] = true, [25861] = true, [20195] = true, [24362] = true, [29609] = true, [29913] = true,
  [30062] = true, [21009] = true, [21008] = true, [3142] = true, [31424] = true, [31425] = true,
  [31426] = true, [31427] = true, [8000473] = true, [8000623] = true, [9001428] = true, [6442] = true,
  [6769] = true, [6918] = true, [7149] = true, [7150] = true, [7151] = true, [7152] = true, [7153] = true,
  [15779] = true, [15780] = true, [15781] = true, [15782] = true, [15783] = true, [15784] = true,
  [15789] = true, [15790] = true, [15791] = true, [15792] = true, [15793] = true, [15794] = true,
  [16248] = true, [16249] = true, [16250] = true, [16270] = true, [7480] = true, [8246] = true,
  [8247] = true, [16394] = true, [16935] = true, [8338] = true, [8341] = true, [11225] = true,
  [14783] = true, [14796] = true, [14825] = true, [14826] = true, [14827] = true, [14828] = true,
  [15394] = true, [15481] = true, [17940] = true, [22209] = true, [22696] = true, [22714] = true,
  [23139] = true, [23140] = true, [23141] = true, [23142] = true, [23143] = true, [23144] = true,
  [23448] = true, [25109] = true, [25330] = true, [25340] = true, [20072] = true, [20073] = true,
  [20074] = true, [20075] = true, [20076] = true, [28063] = true, [28064] = true, [28065] = true,
  [28066] = true, [28618] = true, [28619] = true, [28622] = true, [28658] = true, [21284] = true,
  [21285] = true, [21286] = true, [21287] = true, [21288] = true, [21303] = true, [21450] = true,
  [21486] = true, [21577] = true, [21715] = true, [30770] = true, [30771] = true, [30772] = true,
  [30773] = true, [31439] = true, [8000009] = true, [8000011] = true, [8000185] = true, [8000187] = true,
  [8000188] = true, [8000218] = true, [8000221] = true, [8000225] = true, [8000233] = true,
  [8000290] = true, [8000291] = true, [8000356] = true, [8000362] = true, [8000363] = true,
  [8000364] = true, [8000365] = true, [8000382] = true, [8000385] = true, [8000439] = true,
  [8000469] = true, [8000481] = true, [8000483] = true, [8000627] = true, [8000669] = true,
  [8000671] = true, [8000752] = true, [8000753] = true, [8000190] = true, [8000195] = true,
  [8000196] = true, [8000197] = true, [8000815] = true, [9000001] = true, [9000069] = true,
  [9000070] = true, [9000071] = true, [9000113] = true, [9000265] = true, [9000518] = true,
  [9000568] = true, [9000569] = true, [9000722] = true, [9000723] = true, [9000724] = true,
  [9000955] = true, [9001338] = true, [9001366] = true, [9001664] = true, [9001825] = true,
  [9001918] = true, [9002014] = true, [9002146] = true, [9002147] = true, [9002264] = true,
  [9002356] = true,
}

-- Loot buff IDs. These only need to signal presence; we skip their tooltips to save CPU.
RF.Raid.LOOT_BUFF_IDS = {
  [23215] = true, [9002077] = true, [22516] = true, [22941] = true, [22929] = true, [31422] = true,
  [8000681] = true, [8000803] = true, [9001658] = true, [8000726] = true, [9001956] = true,
  [8000779] = true, [8000792] = true, [8000793] = true, [8000794] = true, [8000795] = true,
  [8000796] = true, [23492] = true, [23491] = true, [23493] = true, [22292] = true, [23095] = true,
  [23094] = true, [23093] = true, [31322] = true, [8000731] = true, [8002787] = true,
  [13834] = true, [22678] = true, [11075] = true, [11074] = true, [23097] = true, [25791] = true,
  [25861] = true, [20195] = true, [23717] = true, [24362] = true, [29609] = true, [29913] = true,
  [30062] = true, [21009] = true, [21008] = true, [3142] = true, [31424] = true, [31425] = true,
  [31426] = true, [31427] = true, [8000473] = true, [8000623] = true, [9001428] = true, [6442] = true,
  [6769] = true, [6918] = true, [7149] = true, [7150] = true, [7151] = true, [7152] = true,
  [7153] = true, [15779] = true, [15780] = true, [15781] = true, [15782] = true, [15783] = true,
  [15784] = true, [15789] = true, [15790] = true, [15791] = true, [15792] = true, [15793] = true,
  [15794] = true, [16248] = true, [16249] = true, [16250] = true, [16270] = true, [7480] = true,
  [8246] = true, [8247] = true, [16394] = true, [16935] = true, [8338] = true, [8341] = true,
  [11225] = true, [14783] = true, [14796] = true, [14825] = true, [14826] = true, [14827] = true,
  [14828] = true, [15394] = true, [15481] = true, [17940] = true, [22209] = true, [22696] = true,
  [22714] = true, [23139] = true, [23140] = true, [23141] = true, [23142] = true, [23143] = true,
  [23144] = true, [23448] = true, [25109] = true, [25330] = true, [25340] = true, [20072] = true,
  [20073] = true, [20074] = true, [20075] = true, [20076] = true, [28063] = true, [28064] = true,
  [28065] = true, [28066] = true, [28618] = true, [28619] = true, [28622] = true, [28658] = true,
  [21284] = true, [21285] = true, [21286] = true, [21287] = true, [21288] = true, [21303] = true,
  [21450] = true, [21486] = true, [21577] = true, [21715] = true, [30770] = true, [30771] = true,
  [30772] = true, [30773] = true, [31439] = true, [8000009] = true, [8000011] = true, [8000185] = true,
  [8000187] = true, [8000188] = true, [8000218] = true, [8000221] = true, [8000225] = true,
  [8000233] = true, [8000290] = true, [8000291] = true, [8000356] = true, [8000362] = true,
  [8000363] = true, [8000364] = true, [8000365] = true, [8000382] = true, [8000385] = true,
  [8000439] = true, [8000469] = true, [8000481] = true, [8000483] = true, [8000627] = true,
  [8000669] = true, [8000671] = true, [8000752] = true, [8000753] = true, [8000190] = true,
  [8000195] = true, [8000196] = true, [8000197] = true, [8000815] = true, [9000001] = true,
  [9000069] = true, [9000070] = true, [9000071] = true, [9000113] = true, [9000265] = true,
  [9000518] = true, [9000568] = true, [9000569] = true, [9000722] = true, [9000723] = true,
  [9000724] = true, [9000955] = true, [9001338] = true, [9001366] = true, [9001664] = true,
  [9001825] = true, [9001918] = true, [9002014] = true, [9002146] = true, [9002147] = true,
  [9002264] = true, [9002356] = true,
}

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
    buffCount = 0,               -- total active buff count (X2Unit:UnitBuffCount)
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
  if not RF.Config.PERFORMANCE_RAID_ROSTER_TRACKING then return end
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
  if not RF.Config.PERFORMANCE_RAID_ROSTER_TRACKING then return end

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

-- scans buffs for a single unit, returns only buffs whose ID is in INTERESTING_BUFF_IDS
-- and (as a second return value) the unit's total buff count for the "too many buffs" ranking.
function RF.Raid.ScanUnitBuffs(unitId)
  local buffCount = X2Unit:UnitBuffCount(unitId)
  if not buffCount or buffCount <= 0 then return {}, 0 end
  local buffs = {}
  for buffId = 1, buffCount do
    local rawBuff = X2Unit:UnitBuff(unitId, buffId)
    if rawBuff then
      local buff = RF.Parser.ParseUnitBuff(rawBuff)
      if RF.Raid.INTERESTING_BUFF_IDS[buff.buff_id] then
        -- Loot buffs only signal their presence. We intentionally skip fetching their
        -- tooltip here because we don't need it and it costs CPU inside the game process.
        if not RF.Raid.LOOT_BUFF_IDS[buff.buff_id] then
          local rawTooltip = X2Unit:UnitBuffTooltip(unitId, buffId)
          if rawTooltip then
            buff.tooltip = RF.Parser.ParseUnitBuffTooltip(rawTooltip)
          end
        end
        buffs[#buffs + 1] = buff
      end
    end
  end
  return buffs, buffCount
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
           member.buffs, member.buffCount = RF.Raid.ScanUnitBuffs(unitId)
           member.buffScanTimestamp = os.time()
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
           member.buffs, member.buffCount = RF.Raid.ScanUnitBuffs(unitId)
           member.buffScanTimestamp = os.time()
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
           memberTwo.buffs, memberTwo.buffCount = RF.Raid.ScanUnitBuffs(unitId)
           memberTwo.buffScanTimestamp = os.time()
          local dist = X2Unit:UnitDistance(unitId)
          if type(dist) == "table" then
            memberTwo.distance = math.floor(dist.distance or -1)
          elseif type(dist) == "number" then
            memberTwo.distance = math.floor(dist)
          else
            memberTwo.distance = -1
          end
          memberTwo.gearScore = tonumber(X2Unit:UnitGearScore(unitId, false)) or 0
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
  if not RF.Config.PERFORMANCE_RAID_ROSTER_TRACKING then return end
  RF.Raid.hasCoRaid = true
  RF:Log("[Raid] Joint raid established")
end
function RF.Raid.handleCoraidBroken()
  if not RF.Config.PERFORMANCE_RAID_ROSTER_TRACKING then return end
  RF.Raid.hasCoRaid = false
  -- Clear the second raid slots to prevent stale data
  for i = 51, 100 do
    RF.Raid.Roster[i] = RF.Raid.NewRaidMember(i)
  end
  RF:Log("[Raid] Joint raid broken")
end
