--- This will serve as the chat event processor. Some future features that I have planned will require chat
--- (such as chat-based commands, and OSRS-style split-chat for ArcheAge.)
RF = RF or {}
RF.Chat = RF.Chat or {}

-- finds the faction when joining alliance chat channels
function RF.Chat.handleChatChannelJoined(channelNumber, channelName)
  local faction = ""
  if (channelName == "Nuia Alliance") then
    faction = "Nuia"
  elseif (channelName == "Haranya Alliance") then
    faction = "Haranya"
  elseif (channelName == "Pirate Alliance") then
    faction = "Pirate"
  else
    return
  end
  RF.FACTION = faction
  RF.IPC.WriteMessage(RF.IPC.MESSAGE_TYPES.SELF_FACTION, faction)
end
