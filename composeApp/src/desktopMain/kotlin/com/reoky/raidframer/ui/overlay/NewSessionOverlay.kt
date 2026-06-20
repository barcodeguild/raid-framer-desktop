package com.reoky.raidframer.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.helpers.RFColors
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.ui.LocalDragLock
import com.reoky.raidframer.ui.OverlayType
import com.reoky.raidframer.ui.WindowManager
import com.reoky.raidframer.ui.component.SESSION_TYPE_DONT_CARE
import com.reoky.raidframer.ui.component.SESSION_TYPES
import com.reoky.raidframer.ui.component.SessionTypeDropdown
import com.reoky.raidframer.ui.component.TitleBarComponent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.jetbrains.compose.resources.stringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.new_session_allow_pve
import raid_framer_desktop.composeapp.generated.resources.new_session_cancel
import raid_framer_desktop.composeapp.generated.resources.new_session_custom_name_placeholder
import raid_framer_desktop.composeapp.generated.resources.new_session_damage_mode_label
import raid_framer_desktop.composeapp.generated.resources.new_session_event_type_label
import raid_framer_desktop.composeapp.generated.resources.new_session_file_name_label
import raid_framer_desktop.composeapp.generated.resources.new_session_pvp_only
import raid_framer_desktop.composeapp.generated.resources.new_session_start_recording
import raid_framer_desktop.composeapp.generated.resources.new_session_title

@Composable
fun NewSessionOverlay(wm: WindowManager? = null) {
  val config by RFConfig.state.collectAsState()
  val scrollState = rememberScrollState()
  val dragLock = LocalDragLock.current

  var selectedEventType by remember { mutableStateOf(SESSION_TYPES.first()) }
  var isCustomEvent by remember { mutableStateOf(false) }
  var customEventName by remember { mutableStateOf("") }
  var allowPvEDamage by remember { mutableStateOf(false) }
  var sessionFileName by remember { mutableStateOf("") }
  var customError by remember { mutableStateOf<String?>(null) }
  var isDropdownExpanded by remember { mutableStateOf(false) }

  LaunchedEffect(isDropdownExpanded) {
    dragLock.value = isDropdownExpanded
  }

  LaunchedEffect(config.lastSessionTitle) {
    if (config.lastSessionTitle.isNotBlank()) {
      sessionFileName = config.lastSessionTitle
    }
  }

  fun updateFileName() {
    val baseName = if (selectedEventType == SESSION_TYPE_DONT_CARE) {
      "dont_care"
    } else if (isCustomEvent) {
      customEventName.lowercase().replace(Regex("[^a-z0-9]"), "_")
    } else {
      selectedEventType.lowercase().replace(Regex("[^a-z0-9]"), "_")
    }
    val modeSuffix = if (allowPvEDamage) "pve" else "pvp"
    val timeSuffix = SimpleDateFormat("HHmm'Z'", Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(Date())
    val dateSuffix = SimpleDateFormat("MMddyy", Locale.US).format(Date())
    sessionFileName = "${baseName}_${modeSuffix}_${timeSuffix}_${dateSuffix}.rf"
  }

  LaunchedEffect(selectedEventType, isCustomEvent, customEventName, allowPvEDamage) {
    updateFileName()
  }

  fun onStartRecording() {
    if (isCustomEvent && !customEventName.matches(Regex("^[a-zA-Z0-9]+$"))) {
      customError = "Only a-zA-Z0-9 characters allowed"
      return
    }
    customError = null

    val isDontCare = selectedEventType == SESSION_TYPE_DONT_CARE
    val effectiveAllowPvE = if (isDontCare) (0..1).random() == 1 else allowPvEDamage
    val displayName = if (isCustomEvent) customEventName else selectedEventType
    RFConfig.update {
      it.copy(
        lastSessionTitle = sessionFileName,
        lastSessionStart = System.currentTimeMillis(),
        lastSessionType = displayName,
        allowPVEDamage = effectiveAllowPvE
      )
    }
    PlayerCacheInteractor.startNewSession(displayName, effectiveAllowPvE)
    wm?.closeWindow(OverlayType.NEW_SESSION)
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF121212))
      .verticalScroll(scrollState)
  ) {
    Column {
      TitleBarComponent(
        title = stringResource(Res.string.new_session_title),
        onClose = { wm?.closeWindow(OverlayType.NEW_SESSION) }
      )

      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(12.dp)
      ) {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(10.dp),
          color = RFColors.CardBackground,
          elevation = 2.dp
        ) {
          Column(
            modifier = Modifier
              .border(1.dp, RFColors.CardBorder, RoundedCornerShape(10.dp))
              .padding(12.dp)
          ) {
            Text(
              text = stringResource(Res.string.new_session_event_type_label),
              color = RFColors.AccentRed,
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            SessionTypeDropdown(
              selectedType = if (isCustomEvent) SESSION_TYPES.last() else selectedEventType,
              onTypeSelected = { type ->
                isCustomEvent = type == SESSION_TYPES.last()
                selectedEventType = type
                if (!isCustomEvent) customEventName = ""
              },
              onExpandedChange = { expanded -> isDropdownExpanded = expanded },
              modifier = Modifier.fillMaxWidth()
            )

            if (isCustomEvent) {
              Spacer(modifier = Modifier.height(8.dp))

              TextField(
                value = customEventName,
                onValueChange = {
                  customEventName = it.filter { c -> c.isLetterOrDigit() }
                  customError = null
                },
                modifier = Modifier
                  .fillMaxWidth()
                  .onFocusChanged { dragLock.value = it.isFocused },
                placeholder = {
                  Text(stringResource(Res.string.new_session_custom_name_placeholder), color = RFColors.TextTertiary)
                },
                singleLine = true,
                colors = TextFieldDefaults.textFieldColors(
                  textColor = RFColors.TextPrimary,
                  backgroundColor = Color(0xFF1E1E1E),
                  focusedIndicatorColor = RFColors.AccentRed,
                  unfocusedIndicatorColor = RFColors.CardBorder,
                  placeholderColor = RFColors.TextTertiary,
                  cursorColor = RFColors.AccentRed
                ),
                enabled = true,
                textStyle = TextStyle(fontSize = 13.sp)
              )

              customError?.let { error ->
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = error,
                  color = RFColors.AccentRed,
                  fontSize = 11.sp
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(10.dp),
          color = RFColors.CardBackground,
          elevation = 2.dp
        ) {
          Column(
            modifier = Modifier
              .border(1.dp, RFColors.CardBorder, RoundedCornerShape(10.dp))
              .padding(12.dp)
          ) {
            Text(
              text = stringResource(Res.string.new_session_damage_mode_label),
              color = RFColors.AccentRed,
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            FlowRow(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(16.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                  .clickable { allowPvEDamage = false }
              ) {
                RadioButton(
                  selected = !allowPvEDamage,
                  onClick = { allowPvEDamage = false },
                  colors = RadioButtonDefaults.colors(
                    selectedColor = RFColors.AccentRed,
                    unselectedColor = RFColors.TextTertiary
                  )
                )
                Text(
                  text = stringResource(Res.string.new_session_pvp_only),
                  color = RFColors.TextPrimary,
                  fontSize = 13.sp
                )
              }

              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                  .clickable { allowPvEDamage = true }
              ) {
                RadioButton(
                  selected = allowPvEDamage,
                  onClick = { allowPvEDamage = true },
                  colors = RadioButtonDefaults.colors(
                    selectedColor = RFColors.AccentRed,
                    unselectedColor = RFColors.TextTertiary
                  )
                )
                Text(
                  text = stringResource(Res.string.new_session_allow_pve),
                  color = RFColors.TextPrimary,
                  fontSize = 13.sp
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(10.dp),
          color = RFColors.CardBackground,
          elevation = 2.dp
        ) {
          Column(
            modifier = Modifier
              .border(1.dp, RFColors.CardBorder, RoundedCornerShape(10.dp))
              .padding(12.dp)
          ) {
            Text(
              text = stringResource(Res.string.new_session_file_name_label),
              color = RFColors.AccentRed,
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            TextField(
              value = sessionFileName,
              onValueChange = {},
              readOnly = true,
              modifier = Modifier.fillMaxWidth(),
              colors = TextFieldDefaults.textFieldColors(
                textColor = RFColors.TextPrimary,
                backgroundColor = Color(0xFF1E1E1E),
                focusedIndicatorColor = RFColors.CardBorder,
                unfocusedIndicatorColor = RFColors.CardBorder,
                cursorColor = RFColors.AccentRed
              ),
              textStyle = TextStyle(fontSize = 13.sp)
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = { onStartRecording() },
            colors = ButtonDefaults.buttonColors(RFColors.AccentRed),
            modifier = Modifier.weight(1f)
          ) {
            Text(
              text = stringResource(Res.string.new_session_start_recording),
              color = Color.White,
              fontWeight = FontWeight.SemiBold,
              fontSize = 13.sp
            )
          }

          Button(
            onClick = { wm?.closeWindow(OverlayType.NEW_SESSION) },
            colors = ButtonDefaults.buttonColors(RFColors.CardBorder),
            modifier = Modifier.weight(1f)
          ) {
            Text(
              text = stringResource(Res.string.new_session_cancel),
              color = RFColors.TextPrimary,
              fontWeight = FontWeight.SemiBold,
              fontSize = 13.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))
      }
    }
  }
}
