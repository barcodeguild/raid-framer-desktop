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

function RF.JSON.json_decode(s)
  local pos = 1
  local len = #s

  local function skip_ws()
    while pos <= len do
      local c = s:sub(pos,pos)
      if c == ' ' or c == '\t' or c == '\n' or c == '\r' then
        pos = pos + 1
      else
        break
      end
    end
  end

  local function utf8_from_codepoint(cp)
    if cp <= 0x7F then
      return string.char(cp)
    elseif cp <= 0x7FF then
      return string.char(0xC0 + math.floor(cp/0x40),
          0x80 + (cp % 0x40))
    elseif cp <= 0xFFFF then
      return string.char(0xE0 + math.floor(cp/0x1000),
          0x80 + (math.floor(cp/0x40) % 0x40),
          0x80 + (cp % 0x40))
    else
      return string.char(0xF0 + math.floor(cp/0x40000),
          0x80 + (math.floor(cp/0x1000) % 0x40),
          0x80 + (math.floor(cp/0x40) % 0x40),
          0x80 + (cp % 0x40))
    end
  end

  local function parse_string()
    -- assumes current char is \"
    pos = pos + 1
    local parts = {}
    while pos <= len do
      local c = s:sub(pos,pos)
      if c == '"' then
        pos = pos + 1
        return table.concat(parts)
      end
      if c == '\\' then
        local esc = s:sub(pos+1,pos+1)
        if esc == '"' then parts[#parts+1] = '"' ; pos = pos + 2
        elseif esc == '\\' then parts[#parts+1] = '\\' ; pos = pos + 2
        elseif esc == '/' then parts[#parts+1] = '/' ; pos = pos + 2
        elseif esc == 'b' then parts[#parts+1] = '\b' ; pos = pos + 2
        elseif esc == 'f' then parts[#parts+1] = '\f' ; pos = pos + 2
        elseif esc == 'n' then parts[#parts+1] = '\n' ; pos = pos + 2
        elseif esc == 'r' then parts[#parts+1] = '\r' ; pos = pos + 2
        elseif esc == 't' then parts[#parts+1] = '\t' ; pos = pos + 2
        elseif esc == 'u' then
          local hex = s:sub(pos+2, pos+5)
          if #hex < 4 or not hex:match('%x%x%x%x') then
            error("invalid \\u escape at position " .. pos)
          end
          local cp = tonumber(hex, 16)
          pos = pos + 6
          -- handle surrogate pairs for basic BMP support
          if cp >= 0xD800 and cp <= 0xDBFF then
            -- expect a following \u low surrogate
            if s:sub(pos,pos+1) == '\\u' then
              local lowhex = s:sub(pos+2, pos+5)
              if lowhex and lowhex:match('%x%x%x%x') then
                local low = tonumber(lowhex,16)
                if low >= 0xDC00 and low <= 0xDFFF then
                  cp = 0x10000 + ((cp - 0xD800) * 0x400) + (low - 0xDC00)
                  pos = pos + 6
                end
              end
            end
          end
          parts[#parts+1] = utf8_from_codepoint(cp)
        else
          -- unknown escape, be permissive: include the char
          parts[#parts+1] = esc
          pos = pos + 2
        end
      else
        parts[#parts+1] = c
        pos = pos + 1
      end
    end
    error("unclosed string")
  end

  local function parse_number()
    local s_sub = s:sub(pos)
    local num_str = s_sub:match('^-?%d+%.?%d*[eE]?[+-]?%d*')
    if not num_str or num_str == "" then
      error("invalid number at position " .. pos)
    end
    pos = pos + #num_str
    local n = tonumber(num_str)
    return n
  end

  local function parse_value()
    skip_ws()
    if pos > len then error("unexpected end of input") end
    local c = s:sub(pos,pos)
    if c == 'n' and s:sub(pos,pos+3) == 'null' then
      pos = pos + 4
      return nil
    elseif c == 't' and s:sub(pos,pos+3) == 'true' then
      pos = pos + 4
      return true
    elseif c == 'f' and s:sub(pos,pos+4) == 'false' then
      pos = pos + 5
      return false
    elseif c == '"' then
      return parse_string()
    elseif c == '{' then
      pos = pos + 1
      local obj = {}
      skip_ws()
      if s:sub(pos,pos) == '}' then pos = pos + 1; return obj end
      while true do
        skip_ws()
        if s:sub(pos,pos) ~= '"' then error("expected string key at position " .. pos) end
        local key = parse_string()
        skip_ws()
        if s:sub(pos,pos) ~= ':' then error("expected ':' after key at position " .. pos) end
        pos = pos + 1
        local val = parse_value()
        obj[key] = val
        skip_ws()
        local ch = s:sub(pos,pos)
        if ch == '}' then pos = pos + 1; break
        elseif ch == ',' then pos = pos + 1
        else error("expected ',' or '}' at position " .. pos) end
      end
      return obj
    elseif c == '[' then
      pos = pos + 1
      local arr = {}
      skip_ws()
      if s:sub(pos,pos) == ']' then pos = pos + 1; return arr end
      local idx = 1
      while true do
        local val = parse_value()
        arr[idx] = val
        idx = idx + 1
        skip_ws()
        local ch = s:sub(pos,pos)
        if ch == ']' then pos = pos + 1; break
        elseif ch == ',' then pos = pos + 1
        else error("expected ',' or ']' at position " .. pos) end
      end
      return arr
    else
      -- number?
      if c == '-' or c:match('%d') then
        return parse_number()
      end
      error("unexpected character '" .. c .. "' at position " .. pos)
    end
  end

  local result = parse_value()
  skip_ws()
  if pos <= len then
    error("trailing characters at position " .. pos)
  end
  return result
end
