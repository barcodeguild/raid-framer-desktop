RF = RF or {} -- ensure global RF exists

ADDON:ImportObject(OBJECT_TYPE.BUTTON)
ADDON:ImportAPI(API_TYPE.CHAT.id)

local DEV_RELOAD_BUTTON = true  -- flip to false before release

local reloadButton

local function CreateReloadButton()
  if reloadButton or not DEV_RELOAD_BUTTON then return end
  X2Chat:DispatchChatMessage(CMF_SYSTEM,"|cFFE44D9D[Raid Framer]|r Dev Reload Button Created")

  reloadButton = UIParent:CreateWidget("button","RaidFramerReloadButton","UIParent","")

  reloadButton:SetText("Reload RaidFramer")
  reloadButton:SetStyle("text_default")
  reloadButton:AddAnchor("BOTTOM", "UIParent", 700, -150)
  reloadButton:Show(true)
  reloadButton:EnableDrag(true)

  reloadButton:SetHandler("OnDragStart", function(self)
    self:StartMoving()
  end)

  reloadButton:SetHandler("OnDragStop", function(self)
    self:StopMovingOrSizing()
  end)

  reloadButton:SetHandler("OnClick", function()
    if type(RF.Shutdown) == "function" then
      RF:Shutdown()
    end

    if type(RF.Init) == "function" then
      RF:Init()
    end

    RF:Log("Soft reloaded via dev button")
  end)
end

UIParent:SetEventHandler(UIEVENT_TYPE.ENTERED_WORLD, CreateReloadButton)
