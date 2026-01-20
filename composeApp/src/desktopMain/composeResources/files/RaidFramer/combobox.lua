local createdComboBoxes = {}

local function CreateEmptyWindow(name, parent)
    return UIParent:CreateWidget("window", name, parent, "")
end

local function UpdateButtonColors(comboBoxId, activeName)
    local comboBox = createdComboBoxes[comboBoxId]
    if comboBox then
        for _, option in ipairs(comboBox.options) do
            local color = (option:GetText() == activeName) and UIParent:GetFontColor("green") or UIParent:GetFontColor("btn_df")
            option:SetTextColor(color[1], color[2], color[3], color[4])
        end
    end
end

function CreateComboBox(parent, triggerWidth, triggerHeight, maxVisibleOptions, optionsData, optionHeight, triggerAnchor, triggerAnchorParent, triggerOffsetX, triggerOffsetY)
    local comboBoxId = #createdComboBoxes + 1
    local dropdownVisible = false
    local selectedOptionText = ""
    local scrollOffset = 0
    local options = {}
    local dropdownContainer = nil
    local scrollButtonsContainer = nil
    local scrollUpButton = nil
    local scrollDownButton = nil
    local scrollSliderButtons = {}
    local scrollButtonWidth = 20
    local scrollButtonHeight = 20
    local scrollButtonSpacing = 2
    local totalOptions = #optionsData
    local comboBox = {
        id = comboBoxId,
        options = {},
        selectedOptionText = selectedOptionText,
        dropdownVisible = dropdownVisible,
        scrollOffset = scrollOffset,
    }
    createdComboBoxes[comboBoxId] = comboBox

    local dropdownTrigger = parent:CreateChildWidget("button", "dropdownTrigger" .. comboBoxId, 0, true)
    dropdownTrigger:SetText("")
    local greenColor = UIParent:GetFontColor("green")
    dropdownTrigger:SetTextColor(greenColor[1], greenColor[2], greenColor[3], greenColor[4])
    dropdownTrigger:SetExtent(triggerWidth, triggerHeight)
    dropdownTrigger:AddAnchor(triggerAnchor, triggerAnchorParent, triggerOffsetX, triggerOffsetY)
    dropdownTrigger:Show(true)
    dropdownTrigger.drawable = dropdownTrigger:CreateColorDrawable(0, 0, 0, 0.5, "background")
    dropdownTrigger.drawable:AddAnchor("TOPLEFT", dropdownTrigger, 0, 0)
    dropdownTrigger.drawable:AddAnchor("BOTTOMRIGHT", dropdownTrigger, 0, 0)

 
    function dropdownTrigger:ResetDisplay()
        selectedOptionText = ""       
        self:SetText(selectedOptionText) 
        UpdateButtonColors(comboBoxId, selectedOptionText) 
    end

    dropdownContainer = CreateEmptyWindow("dropdownContainer" .. comboBoxId, dropdownTrigger)
    if totalOptions <= maxVisibleOptions then
        dropdownContainer:SetExtent(triggerWidth, totalOptions * optionHeight)
    else
        dropdownContainer:SetExtent(triggerWidth - scrollButtonWidth - scrollButtonSpacing, maxVisibleOptions * optionHeight)
    end
    dropdownContainer:AddAnchor("TOPLEFT", dropdownTrigger, "BOTTOMLEFT", 0, 0)
    dropdownContainer:Show(false)
    local menuBackground = dropdownContainer:CreateColorDrawable(0, 0, 0, 0.5, "background")
    menuBackground:AddAnchor("TOPLEFT", dropdownContainer, 0, 0)
    menuBackground:AddAnchor("BOTTOMRIGHT", dropdownContainer, 0, 0)
    dropdownContainer.clipChildren = true

    scrollButtonsContainer = CreateEmptyWindow("scrollButtonsContainer" .. comboBoxId, dropdownContainer)
    scrollButtonsContainer:SetExtent(scrollButtonWidth, maxVisibleOptions * optionHeight)
    scrollButtonsContainer:AddAnchor("TOPLEFT", dropdownContainer, "TOPRIGHT", scrollButtonSpacing, 0)
    scrollButtonsContainer:Show(false)
    local scrollBackground = scrollButtonsContainer:CreateColorDrawable(0, 0, 0, 0.4, "background")
    scrollBackground:AddAnchor("TOPLEFT", scrollButtonsContainer, 0, 0)
    scrollBackground:AddAnchor("BOTTOMRIGHT", scrollButtonsContainer, 0, 0)

    for i = 1, totalOptions do
        local option = dropdownContainer:CreateChildWidget("button", "option" .. i .. "_" .. comboBoxId, 0, true)
        option:SetText(optionsData[i].text)
        if totalOptions <= maxVisibleOptions then
            option:SetExtent(triggerWidth, optionHeight)
        else
            option:SetExtent(triggerWidth - scrollButtonWidth - scrollButtonSpacing, optionHeight)
        end
        option:AddAnchor("TOPLEFT", dropdownContainer, 0, (i - 1) * optionHeight)
        option:Show(false)
        option.drawable = option:CreateColorDrawable(0, 0, 0, 0.5, "background")
        option.drawable:AddAnchor("TOPLEFT", option, 0, 0)
        option.drawable:AddAnchor("BOTTOMRIGHT", option, 0, 0)
        
        option:SetHandler("OnEnter", function(self)
            self.drawable:SetColor(0.3, 0.3, 0.3, 0.7)
            
            local currentComboBox = createdComboBoxes[comboBoxId]
            if currentComboBox and currentComboBox.selectedOptionText == self:GetText() then
                local selectedColor = UIParent:GetFontColor("green")
                self:SetTextColor(selectedColor[1], selectedColor[2], selectedColor[3], selectedColor[4])
            else
                local hoverColor = UIParent:GetFontColor("btn_ov")
                self:SetTextColor(hoverColor[1], hoverColor[2], hoverColor[3], hoverColor[4])
            end
        end)
        
        option:SetHandler("OnLeave", function(self)
            self.drawable:SetColor(0, 0, 0, 0.5)
            
            local currentComboBox = createdComboBoxes[comboBoxId]
            if currentComboBox and currentComboBox.selectedOptionText == self:GetText() then
                local selectedColor = UIParent:GetFontColor("green")
                self:SetTextColor(selectedColor[1], selectedColor[2], selectedColor[3], selectedColor[4])
            else
                local defaultColor = UIParent:GetFontColor("btn_df")
                self:SetTextColor(defaultColor[1], defaultColor[2], defaultColor[3], defaultColor[4])
            end
        end)
        
        comboBox.options[i] = option
        if optionsData[i].handler then
            option:SetHandler("OnClick", function(self)
                optionsData[i].handler(self)
                comboBox.selectedOptionText = self:GetText()
                selectedOptionText = comboBox.selectedOptionText
                dropdownTrigger:SetText(selectedOptionText)
                UpdateButtonColors(comboBoxId, selectedOptionText)

                dropdownContainer:Show(false)
                scrollButtonsContainer:Show(false)
                comboBox.dropdownVisible = false
                dropdownVisible = false
            end)
        else
            option:SetHandler("OnClick", function(self)
                comboBox.selectedOptionText = self:GetText()
                selectedOptionText = comboBox.selectedOptionText
                dropdownTrigger:SetText(selectedOptionText)
                UpdateButtonColors(comboBoxId, selectedOptionText)

                dropdownContainer:Show(false)
                scrollButtonsContainer:Show(false)
                comboBox.dropdownVisible = false
                dropdownVisible = false
            end)
        end
    end

    local function UpdateSliderButtonsVisibility()
        local numSliderPositions = math.max(0, totalOptions - maxVisibleOptions + 1)
        if totalOptions > maxVisibleOptions then
            if scrollOffset < numSliderPositions then
                for i = 0, numSliderPositions - 1 do
                    scrollSliderButtons[i]:Show(i == scrollOffset)
                end
            else
                if numSliderPositions > 0 then
                    scrollSliderButtons[numSliderPositions - 1]:Show(true)
                end
            end
        else
            for _, button in pairs(scrollSliderButtons) do
                button:Show(false)
            end
        end
    end

    local function UpdateVisibleOptions()
        if dropdownVisible then
            scrollUpButton:Show(totalOptions > maxVisibleOptions)
            scrollDownButton:Show(totalOptions > maxVisibleOptions)
            scrollButtonsContainer:Show(totalOptions > maxVisibleOptions)
            UpdateSliderButtonsVisibility()
            for i = 1, totalOptions do
                if i > scrollOffset and i <= scrollOffset + maxVisibleOptions then
                    comboBox.options[i]:Show(true)
                    comboBox.options[i]:AddAnchor("TOPLEFT", dropdownContainer, 0, (i - scrollOffset - 1) * optionHeight)
                    comboBox.options[i].drawable:SetColor(0, 0, 0, 0.5)
                    if selectedOptionText == comboBox.options[i]:GetText() then
                        UpdateButtonColors(comboBoxId, comboBox.options[i]:GetText())
                    else
                        local defaultColor = UIParent:GetFontColor("btn_df")
                        comboBox.options[i]:SetTextColor(defaultColor[1], defaultColor[2], defaultColor[3], defaultColor[4])
                    end
                else
                    comboBox.options[i]:Show(false)
                end
            end
        else
            for i = 1, totalOptions do
                comboBox.options[i]:Show(false)
            end
            scrollUpButton:Show(false)
            scrollDownButton:Show(false)
            for _, button in pairs(scrollSliderButtons) do
                button:Show(false)
            end
            scrollOffset = 0
        end
    end

    function dropdownTrigger:OnClick()
        comboBox.dropdownVisible = not comboBox.dropdownVisible
        dropdownVisible = comboBox.dropdownVisible
        dropdownContainer:Show(dropdownVisible)
        scrollButtonsContainer:Show(dropdownVisible and totalOptions > maxVisibleOptions)
        UpdateVisibleOptions()
        UpdateSliderButtonsVisibility()
    end
    dropdownTrigger:SetHandler("OnClick", dropdownTrigger.OnClick)

    scrollUpButton = scrollButtonsContainer:CreateChildWidget("button", "scrollUpButton" .. comboBoxId, 0, true)
    scrollUpButton:SetText("")
    scrollUpButton:SetExtent(scrollButtonWidth, scrollButtonHeight)
    scrollUpButton:AddAnchor("TOPRIGHT", scrollButtonsContainer, "TOPRIGHT", 0, 0)
    scrollUpButton.iconOverlay = scrollUpButton:CreateIconDrawable("artwork")
    scrollUpButton.iconOverlay:SetExtent(scrollButtonWidth, scrollButtonHeight)
    scrollUpButton.iconOverlay:AddAnchor("CENTER", scrollUpButton, 0, 0)
    scrollUpButton.iconOverlay:AddTexture("Addon/RaidSnapshot/Icones/Up.dds")
    scrollUpButton.iconOverlay:SetVisible(true)
    scrollUpButton:Show(false)

    scrollDownButton = scrollButtonsContainer:CreateChildWidget("button", "scrollDownButton" .. comboBoxId, 0, true)
    scrollDownButton:SetText("")
    scrollDownButton:SetExtent(scrollButtonWidth, scrollButtonHeight)
    scrollDownButton:AddAnchor("BOTTOMRIGHT", scrollButtonsContainer, "BOTTOMRIGHT", 0, 0)
    scrollDownButton.iconOverlay = scrollDownButton:CreateIconDrawable("artwork")
    scrollDownButton.iconOverlay:SetExtent(scrollButtonWidth, scrollButtonHeight)
    scrollDownButton.iconOverlay:AddAnchor("CENTER", scrollDownButton, 0, 0)
    scrollDownButton.iconOverlay:AddTexture("Addon/RaidSnapshot/Icones/Down.dds")
    scrollDownButton.iconOverlay:SetVisible(true)
    scrollDownButton:Show(false)

    local numSliderPositions = math.max(0, totalOptions - maxVisibleOptions + 1)
    local availableHeight = scrollButtonsContainer:GetHeight() - scrollUpButton:GetHeight() - 5
    local actualSliderPositions = math.max(1, numSliderPositions)

    for i = 0, numSliderPositions - 1 do
        local sliderButton = scrollButtonsContainer:CreateChildWidget("button", "scrollSliderButton" .. i .. "_" .. comboBoxId, 0, true)
        sliderButton:SetText("")
        sliderButton:SetExtent(scrollButtonWidth, scrollButtonHeight)
        local yPosStart = scrollUpButton:GetHeight() - 7
        local yPosOffset = (availableHeight - (numSliderPositions * scrollButtonHeight)) / (actualSliderPositions - 1 + (actualSliderPositions == 1 and 1 or 0)) * i
        local yPosButton = yPosStart + yPosOffset + i * scrollButtonHeight
        sliderButton:AddAnchor("TOPLEFT", scrollButtonsContainer, "TOPLEFT", 0, yPosButton)
        sliderButton.iconOverlay = sliderButton:CreateIconDrawable("artwork")
        sliderButton.iconOverlay:SetExtent(scrollButtonWidth, scrollButtonHeight)
        sliderButton.iconOverlay:AddAnchor("CENTER", sliderButton, 0, 0)
        sliderButton.iconOverlay:AddTexture("Addon/RaidSnapshot/Icones/Scroll.dds")
        sliderButton:Show(false)
        scrollSliderButtons[i] = sliderButton
    end

    function scrollUpButton:OnClick()
        if comboBox.scrollOffset > 0 then
            comboBox.scrollOffset = math.max(0, comboBox.scrollOffset - 1)
            scrollOffset = comboBox.scrollOffset
            UpdateVisibleOptions()
            UpdateSliderButtonsVisibility()
        end
    end
    scrollUpButton:SetHandler("OnClick", scrollUpButton.OnClick)

    function scrollDownButton:OnClick()
        if comboBox.scrollOffset < totalOptions - maxVisibleOptions then
            comboBox.scrollOffset = math.min(totalOptions - maxVisibleOptions, comboBox.scrollOffset + 1)
            scrollOffset = comboBox.scrollOffset
            UpdateVisibleOptions()
            UpdateSliderButtonsVisibility()
        end
    end
    scrollDownButton:SetHandler("OnClick", scrollDownButton.OnClick)

    function dropdownContainer:OnWheelDown()
        if dropdownVisible and scrollOffset < totalOptions - maxVisibleOptions then
            comboBox.scrollOffset = math.min(totalOptions - maxVisibleOptions, comboBox.scrollOffset + 1)
            scrollOffset = comboBox.scrollOffset
            UpdateVisibleOptions()
            UpdateSliderButtonsVisibility()
        end
    end
    dropdownContainer:SetHandler("OnWheelDown", dropdownContainer.OnWheelDown)

    function dropdownContainer:OnWheelUp()
        if dropdownVisible and scrollOffset > 0 then
            comboBox.scrollOffset = math.max(0, comboBox.scrollOffset - 1)
            scrollOffset = comboBox.scrollOffset
            UpdateVisibleOptions()
            UpdateSliderButtonsVisibility()
        end
    end
    dropdownContainer:SetHandler("OnWheelUp", dropdownContainer.OnWheelUp)

    return dropdownTrigger
end