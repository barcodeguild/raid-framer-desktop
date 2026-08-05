package com.reoky.raidframer.ui.component

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.reoky.raidframer.core.helpers.colorToSliderValue
import com.reoky.raidframer.core.helpers.sliderValueToColor
import com.reoky.raidframer.ui.LocalDragLock
import kotlin.math.abs

@Composable
fun ColorSliderComponent(
  color: Int,
  onColorChange: (Int) -> Unit,
  modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
  val dragLock = LocalDragLock.current
  val interactionSource = remember { MutableInteractionSource() }
  var isDragging by remember { mutableFloatStateOf(0f) }
  var sliderValue by remember { mutableFloatStateOf(colorToSliderValue(color)) }

  LaunchedEffect(interactionSource) {
    interactionSource.interactions.collect { interaction ->
      when (interaction) {
        is DragInteraction.Start -> {
          dragLock.value = true
          isDragging = 1f
        }
        is DragInteraction.Stop, is DragInteraction.Cancel -> {
          dragLock.value = false
          isDragging = 0f
        }
      }
    }
  }

  LaunchedEffect(color, isDragging) {
    if (isDragging == 0f) {
      val nextValue = colorToSliderValue(color)
      if (abs(nextValue - sliderValue) > 0.0001f) sliderValue = nextValue
    }
  }

  Slider(
    value = sliderValue,
    onValueChange = { value ->
      sliderValue = value
      onColorChange(sliderValueToColor(value))
    },
    interactionSource = interactionSource,
    modifier = modifier,
    colors = SliderDefaults.colors(
      thumbColor = Color(0xFFDC143C),
      activeTrackColor = Color(0xFFDC143C),
      inactiveTrackColor = Color(0xFF2A2A2A)
    )
  )
}

@Composable
fun DragLockedSlider(
  value: Float,
  onValueChange: (Float) -> Unit,
  modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
  valueRange: ClosedFloatingPointRange<Float> = 0f..1f
) {
  val dragLock = LocalDragLock.current
  val interactionSource = remember { MutableInteractionSource() }

  LaunchedEffect(interactionSource) {
    interactionSource.interactions.collect { interaction ->
      when (interaction) {
        is DragInteraction.Start -> dragLock.value = true
        is DragInteraction.Stop, is DragInteraction.Cancel -> dragLock.value = false
      }
    }
  }

  Slider(
    value = value,
    onValueChange = onValueChange,
    valueRange = valueRange,
    interactionSource = interactionSource,
    modifier = modifier,
    colors = SliderDefaults.colors(
      thumbColor = Color(0xFFDC143C),
      activeTrackColor = Color(0xFFDC143C),
      inactiveTrackColor = Color(0xFF2A2A2A)
    )
  )
}
