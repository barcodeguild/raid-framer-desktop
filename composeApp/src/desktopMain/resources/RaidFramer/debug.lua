local RF = RaidFramer
if not RF then return end  -- safety

ADDON:ImportObject(OBJECT_TYPE.BUTTON)
ADDON:ImportAPI(API_TYPE.CHAT.id)

function RF:dumpTable(t, prefix)
  prefix = prefix or ""
  for k, v in pairs(t) do
    if type(v) == "table" then
      X2Chat:DispatchChatMessage(CMF_SYSTEM, prefix .. k .. " = {")
      dumpTable(v, prefix .. "  ")
      X2Chat:DispatchChatMessage(CMF_SYSTEM, prefix .. "}")
    else
      X2Chat:DispatchChatMessage(
        CMF_SYSTEM,
        prefix .. k .. " = " .. tostring(v)
      )
    end
  end
end

function RF:dump(...)
  local args = { ... }

  for i = 1, #args do
    if type(args[i]) == "table" then
      X2Chat:DispatchChatMessage(CMF_SYSTEM, "[" .. i .. "] TABLE:")
      dumpTable(args[i], "  ")
    else
      X2Chat:DispatchChatMessage(CMF_SYSTEM, "[" .. i .. "] (" .. type(args[i]) .. ") " .. tostring(args[i]))
    end
  end
end
