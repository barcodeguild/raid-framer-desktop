RF = RF or {}
RF.Debug = RF.Debug or {}

ADDON:ImportObject(OBJECT_TYPE.BUTTON)
ADDON:ImportAPI(API_TYPE.CHAT.id)

-- safer table dumper: guards against errors, cycles, empty/pseudo-tables and prints metatable / __index
function RF.Debug.dumpTable(t, prefix, visited, depth)
  prefix = prefix or ""
  visited = visited or {}
  depth = (depth == nil) and 5 or depth  -- default max depth

  local function safeTostring(v)
    local ok, s = pcall(tostring, v)
    return ok and s or ("<tostring error: " .. tostring(s) .. ">")
  end

  if type(t) ~= "table" then
    X2Chat:DispatchChatMessage(CMF_SYSTEM, prefix .. "Not a table: (" .. type(t) .. ") " .. safeTostring(t))
    return
  end

  if visited[t] then
    X2Chat:DispatchChatMessage(CMF_SYSTEM, prefix .. "<cycle detected>")
    return
  end
  visited[t] = true

  if depth <= 0 then
    X2Chat:DispatchChatMessage(CMF_SYSTEM, prefix .. "<max depth reached>")
    return
  end

  -- Try iterating; catch any runtime errors (some engine tables may raise)
  local ok, err = pcall(function()
    local hadAny = false
    for k, v in pairs(t) do
      hadAny = true
      local kstr = safeTostring(k)
      local vtype = type(v)
      if vtype == "table" then
        X2Chat:DispatchChatMessage(CMF_SYSTEM, prefix .. kstr .. " = {")
        RF.Debug.dumpTable(v, prefix .. "  ", visited, depth - 1)
        X2Chat:DispatchChatMessage(CMF_SYSTEM, prefix .. "}")
      else
        X2Chat:DispatchChatMessage(CMF_SYSTEM, prefix .. kstr .. " = (" .. vtype .. ") " .. safeTostring(v))
      end
    end
    if not hadAny then
      X2Chat:DispatchChatMessage(CMF_SYSTEM, prefix .. "<table has no enumerable keys>")
    end
  end)

  if not ok then
    X2Chat:DispatchChatMessage(CMF_SYSTEM, prefix .. "dumpTable iteration failed: " .. tostring(err))
  end

  -- always try to show metatable and __index (useful for engine proxy objects)
  local mt = getmetatable(t)
  if mt then
    X2Chat:DispatchChatMessage(CMF_SYSTEM, prefix .. "metatable:")
    if type(mt) == "table" then
      -- prefer showing metatable keys without recursing indefinitely
      for k, v in pairs(mt) do
        local vtype = type(v)
        X2Chat:DispatchChatMessage(CMF_SYSTEM, prefix .. "  " .. safeTostring(k) .. " = (" .. vtype .. ") " .. safeTostring(v))
      end
      if type(mt.__index) == "table" then
        X2Chat:DispatchChatMessage(CMF_SYSTEM, prefix .. "__index:")
        RF.Debug.dumpTable(mt.__index, prefix .. "  ", visited, depth - 1)
      end
    else
      X2Chat:DispatchChatMessage(CMF_SYSTEM, prefix .. "  (" .. type(mt) .. ") " .. safeTostring(mt))
    end
  end
end

function RF.Debug.dump(...)
  local args = { ... }
  for i = 1, #args do
    if type(args[i]) == "table" then
      X2Chat:DispatchChatMessage(CMF_SYSTEM, "[" .. i .. "] TABLE:")
      RF.Debug.dumpTable(args[i], "  ")
    else
      X2Chat:DispatchChatMessage(CMF_SYSTEM, "[" .. i .. "] (" .. type(args[i]) .. ") " .. tostring(args[i]))
    end
  end
end

function RF.Debug.probeObject(obj, name)
  name = name or tostring(obj)
  local function L(msg) RF:Log("PROBE " .. name .. ": " .. msg) end

  L("type = " .. tostring(type(obj)))
  L("tostring = " .. tostring(obj))

  local ok, mt = pcall(getmetatable, obj)
  if not ok then
    L("getmetatable failed: " .. tostring(mt))
    return
  end
  if not mt then
    L("no metatable")
  else
    L("metatable type = " .. tostring(type(mt)) .. " tostring = " .. tostring(mt))
    if type(mt) == "table" then
      -- show metatable keys (non-recursive)
      local any = false
      for k, v in pairs(mt) do
        any = true
        L("metatable key: " .. tostring(k) .. " = (" .. type(v) .. ") " .. tostring(v))
      end
      if not any then L("metatable has no enumerable keys") end

      -- inspect __index
      local idx = mt.__index
      if idx == nil then
        L("__index = nil")
      else
        L("__index type = " .. tostring(type(idx)) .. " tostring = " .. tostring(idx))
        if type(idx) == "table" then
          RF.Debug.dumpTable(idx, "  ")
        else
          L("__index is not a table (likely a function/proxy), can't enumerate")
        end
      end
    end
  end

  -- try some safe lookups (won't error if absent)
  local function tryLookup(key)
    local ok, val = pcall(function() return obj[key] end)
    if ok then
      L("lookup [" .. tostring(key) .. "] -> (" .. type(val) .. ") " .. tostring(val))
    else
      L("lookup [" .. tostring(key) .. "] error: " .. tostring(val))
    end
  end

  -- try common method names used by dominion API (adjust as needed)
  local probeKeys = { "GetZoneGroupName", "CanUpdateSiegeSchedule", "GetOwnerFactionName", "IsSiegeWinnerMyFaction", "GetSiegePeriodName", "GetCurPeriodRemainDate", "GetGuardTowerHp" }
  for _, k in ipairs(probeKeys) do tryLookup(k) end

  -- attempt to call a safe method if present (use pcall)
  if obj.GetMyTopLevelFaction or obj.GetMyFaction then
    local okc, cres = pcall(function()
      if obj.GetMyTopLevelFaction then return obj:GetMyTopLevelFaction() end
      if obj.GetMyFaction then return obj:GetMyFaction() end
    end)
    if okc then
      L("call result -> (" .. type(cres) .. ") " .. tostring(cres))
    else
      L("call error: " .. tostring(cres))
    end
  else
    L("no known callable methods found to probe")
  end

  -- try numeric iteration (in case it's an array-like proxy)
  local found = false
  for i = 1, 20 do
    local okn, v = pcall(function() return obj[i] end)
    if okn and v ~= nil then
      found = true
      L("numeric[" .. i .. "] = (" .. type(v) .. ") " .. tostring(v))
    end
  end
  if not found then L("no numeric indices in 1..20") end
end
