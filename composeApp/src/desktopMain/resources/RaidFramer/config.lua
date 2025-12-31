-- RaidFramer Addon Configuration File
-- The purpose of this code is to load the stored settings from settings.conf in the RaidFramer addon folder.
RF = RF or {} -- ensure global RF exists so this file can initialize RF.Raid
RF.Config = RF.Config or {}

RF.Config.BASE_DIR = "../Documents/Addon/RaidFramer/"
RF.Config.CONFIG_FILE = RF.Config.BASE_DIR .. "settings.conf"

RF.Config.SHOW_RAID_STATUS = false
RF.Config.SHOW_CHARMED_IN_CHAT = true
RF.Config.SHOW_SILENCED_IN_CHAT = true
RF.Config.MARK_HVT_HEALERS = false
RF.Config.MARK_HVT_DPS = false
RF.Config.MARK_HVT_CC = false
RF.Config.MARK_SAC_DANCERS = false
RF.Config.MARK_CHARMED_TARGETS = true
RF.Config.MARK_SILENCED_TARGETS = true
RF.Config.MARK_DISTRESSED_TARGETS = true

-- loads the configuration from the settings.conf file
 function RF.Config.LoadConfig()
   local file = io.open(RF.Config.CONFIG_FILE, "r")
   if not file then
     return -- just use the defaults cause there wasn't one
   end

   for line in file:lines() do
     -- Trim whitespace
     line = line:match("^%s*(.-)%s*$")

     -- Ignore empty lines and comments (starting with # or --)
     if line ~= "" and not line:match("^#") and not line:match("^%-%-") then
       local key, value = line:match("^(.-)=(.*)$")
       if key and value then
         -- Convert string "true"/"false" to booleans
         if value == "true" then
           value = true
         elseif value == "false" then
           value = false
         end

         -- Map the config keys to the internal RF.Config keys
         if key == "show_raid_status" then RF.Config.SHOW_RAID_STATUS = value
         elseif key == "show_charmed_in_chat" then RF.Config.SHOW_CHARMED_IN_CHAT = value
         elseif key == "show_silenced_in_chat" then RF.Config.SHOW_SILENCED_IN_CHAT = value
         elseif key == "mark_hvt_healers" then RF.Config.MARK_HVT_HEALERS = value
         elseif key == "mark_hvt_dps" then RF.Config.MARK_HVT_DPS = value
         elseif key == "mark_hvt_cc" then RF.Config.MARK_HVT_CC = value
         elseif key == "mark_sac_dancers" then RF.Config.MARK_SAC_DANCERS = value
         elseif key == "mark_charmed_targets" then RF.Config.MARK_CHARMED_TARGETS = value
         elseif key == "mark_silenced_targets" then RF.Config.MARK_SILENCED_TARGETS = value
         elseif key == "mark_distressed_targets" then RF.Config.MARK_DISTRESSED_TARGETS = value
         end
       end
     end
   end

   file:close()
 end

RF.Config.LoadConfig() -- do right meow