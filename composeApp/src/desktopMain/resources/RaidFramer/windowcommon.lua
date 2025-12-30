
WINDOW_WIDTH_300 = 1
WINDOW_WIDTH_350 = 2
WINDOW_WIDTH_430 = 3
WINDOW_WIDTH_450 = 4
WINDOW_WIDTH_510 = 5
WINDOW_WIDTH_600 = 6
WINDOW_WIDTH_680 = 7
WINDOW_WIDTH_800 = 8
WINDOW_WIDTH_900 = 9
WINDOW_WIDTH_1200 = 10

local windowWidths = {
    [WINDOW_WIDTH_300] = 300,
    [WINDOW_WIDTH_350] = 350,
    [WINDOW_WIDTH_430] = 430,
    [WINDOW_WIDTH_450] = 450,
    [WINDOW_WIDTH_510] = 510,
    [WINDOW_WIDTH_600] = 600,
    [WINDOW_WIDTH_680] = 680,
    [WINDOW_WIDTH_800] = 800,
    [WINDOW_WIDTH_900] = 900,
    [WINDOW_WIDTH_1200] = 1200,
}

local function GetWindowWidth(index)
    return windowWidths[index]
end

function SettingWindowSkin(window)
    if window.setSkin ~= true then
      do
        local bg = window:CreateDrawable("ui/common/default.dds", "main_bg", "background")
        bg:AddAnchor("TOPLEFT", window, -5, -5)
        bg:AddAnchor("BOTTOMRIGHT", window, 5, 5)
        local deco = window:CreateDrawable("ui/common/default.dds", "main_bg_deco", "background")
        deco:AddAnchor("TOP", window, 0, -5)
        local width, height = deco:GetExtent()
        function window:GetDecoOriginalExtent()
          return width, height
        end
        function window:SetDecoExtent(width, height)
          deco:SetExtent(width, height)
        end
        window.setSkin = true
      end
    end
    local width, height = window:GetDecoOriginalExtent()
    local windowWidth = window:GetWidth()
    if width > windowWidth then
      local ratio = windowWidth / width
      window:SetDecoExtent(width * ratio, height * ratio)
    else
      window:SetDecoExtent(width, height)
    end
end

function SetWindowUIAnimation(window)
    local time = 0.1
    window:SetAlphaAnimation(0.0, 1.0, time, time)
end

function CreateBaseWindow(id, parent, category, useTitleBar, showInHud)
    if category == nil then
        category = ""
    end

    local window = UIParent:CreateWidget("window", id, parent, category)
    window:Show(false)

    if useTitleBar then
        local titleBar = CreateTitleBar("titleBar", window, showInHud)
        window.titleBar = titleBar

        function window:SetTitle(text)
            titleBar:SetTitleText(text)
        end

        InitForStackInWindow(window)
    end

    local persmissionHandler = {
        ["system_message"] = function (v)
            AddMessageToSysMsgWindow(v)
        end
    }
    
    local function OnPermissionChanged(self, infos)
        for k, v in pairs(infos) do
            if (persmissionHandler[k]) ~= nil then
                persmissionHandler[k](v)
            end
        end
    end
    window:SetHandler("OnPermissionChanged", OnPermissionChanged)

    return window
end
