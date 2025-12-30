-- oh friends, Lua does not have built-in JSON support...
-- so we hope this code is sufficient for our needs...
RF = RF or {} -- ensure global RF exists so this file can initialize RF.JSON
RF.JSON = RF.JSON or {}

-- no export needed, scoped to module
local function json_escape(str)
  return str:gsub('[%c\\"]', {
    ['\b'] = '\\b',
    ['\f'] = '\\f',
    ['\n'] = '\\n',
    ['\r'] = '\\r',
    ['\t'] = '\\t',
    ['\\'] = '\\\\',
    ['"']  = '\\"'
  })
end

-- no export needed, scoped to module
local function is_array(tbl)
  local i = 1
  for _ in pairs(tbl) do
    if tbl[i] == nil then return false end
    i = i + 1
  end
  return true
end

-- scoped to the module so we can call it externally mhmm!
function RF.JSON.json_encode(value)
  local t = type(value)

  if t == "nil" then
    return "null"
  elseif t == "number" or t == "boolean" then
    return tostring(value)
  elseif t == "string" then
    return '"' .. json_escape(value) .. '"'
  elseif t == "table" then
    if is_array(value) then
      local items = {}
      for i = 1, #value do
        items[#items + 1] = RF.JSON.json_encode(value[i])
      end
      return "[" .. table.concat(items, ",") .. "]"
    else
      local items = {}
      for k, v in pairs(value) do
        items[#items + 1] = RF.JSON.json_encode(tostring(k)) .. ":" .. RF.JSON.json_encode(v)
      end
      return "{" .. table.concat(items, ",") .. "}"
    end
  end

  return "null"
end
