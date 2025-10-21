package com.reoky.raidframer.ui.graphs

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*
import java.util.*
import lol.rfcloud.core.helpers.humanReadableAbbreviation
import kotlin.collections.get
import kotlin.compareTo
import kotlin.text.get
import kotlin.text.toFloat

data class CandlestickDataFrame(
  val xValues: List<String>,
  val opens: List<Double>,
  val highs: List<Double>,
  val lows: List<Double>,
  val closes: List<Double>
)

data class CandlestickColorScheme(
  val bullish: Color,
  val bearish: Color,
  val neutral: Color,
  val background: Color,
  val grid: Color,
  val axis: Color,
  val text: Color
)

data class GraphMeta(
  val title: String,
  val subtitle: String,
  val description: String,
  val xAxisLabel: String,
  val yAxisLabel: String,
  val colorScheme: List<Color>,
  val data: CandlestickDataFrame
)

enum class DamageType(val unicodeIcon: String) {
  DAMAGE("🔥"), HEAL("💖"), CC("🛡️");
}

fun chartTitleFor(name: String, damageType: DamageType, pvp: Boolean): String {
  return when (damageType) {
    DamageType.DAMAGE -> if (pvp) "$name's PvP Damage/Time" else "$name's PvE Damage/Time"
    DamageType.HEAL -> if (pvp) "$name's PvP Healing/Time" else "$name's PvE Healing/Time"
    DamageType.CC -> if (pvp) "$name's PvP CC/Time" else "$name's PvE CC/Time"
  }
}

@Composable
fun CandlestickChart(
  title: String = chartTitleFor("Reoky", DamageType.DAMAGE, true),
  subtitle: String = "Black Dragon PvP",
  icon: DamageType = DamageType.DAMAGE,
  xAxisLabel: String = "Time (Minutes)",
  yAxisLabel: String = "Damage Per Second",
  data: CandlestickDataFrame,
  colorScheme: CandlestickColorScheme,
  currentValue: Double? = null,
  modifier: Modifier = Modifier.fillMaxWidth().height(300.dp),
) {
  val textMeasurer = rememberTextMeasurer()
  var scale by remember { mutableStateOf(1f) }
  var scrollX by remember { mutableStateOf(0f) }
  var scrollY by remember { mutableStateOf(0f) }

  val n = data.xValues.size
  val minInitialBars = 10
  val visibleFromScale = ceil(n / scale).toInt().coerceAtLeast(1)
  val visibleBars = max(visibleFromScale, minInitialBars)

  fun clampX(visible: Int): Float {
    val maxOffset = (n - visible).coerceAtLeast(0)
    return scrollX.coerceIn(0f, maxOffset.toFloat())
  }

  val lastCloseValue by remember { derivedStateOf { if (data.closes.isNotEmpty()) data.closes.last() else 0.0 } }

  val initialTarget = (currentValue ?: lastCloseValue).toFloat()
  val displayedLiveAnim = remember { androidx.compose.animation.core.Animatable(initialTarget) }
  val targetLiveValue = currentValue ?: lastCloseValue

  LaunchedEffect(targetLiveValue) {
    val target = (targetLiveValue ?: lastCloseValue).toFloat()
    if (kotlin.math.abs(displayedLiveAnim.value - target) > 5000f) {
      displayedLiveAnim.snapTo(target)
    } else {
      displayedLiveAnim.animateTo(target, animationSpec = tween(durationMillis = 0))
    }
  }
  val displayedLiveValue = displayedLiveAnim.value.toDouble()

  val infiniteTransition = rememberInfiniteTransition()
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.5f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(animation = tween(durationMillis = 500), repeatMode = RepeatMode.Reverse)
  )

  val rawMax = data.highs.maxOrNull() ?: 1.0
  val rawMin = data.lows.minOrNull() ?: 0.0
  val span = (rawMax - rawMin).coerceAtLeast(1e-9)

  val topPadPct = 0.10
  val bottomPadPct = if (rawMin >= 0.0) 0.05 else 0.10
  val yMinDomain = if (rawMin >= 0.0) -rawMax * bottomPadPct else rawMin - span * bottomPadPct
  val yMaxDomain = rawMax + span * topPadPct
  val yDomainSpan = (yMaxDomain - yMinDomain).coerceAtLeast(1e-6)

  // precomputed ma with last value replaced by live animated value
  // we use this at the eek
  val renderMA by remember(displayedLiveValue, data) {
    derivedStateOf {
      val n = data.closes.size
      if (n == 0) DoubleArray(0)
      else {
        // build closes array where last element is the live displayed value
        val closes = DoubleArray(n) { idx -> if (idx == n - 1) displayedLiveValue else data.closes[idx] }

        // compute SMA window then EMA-like smoothing (keeps same algorithm as before)
        val maWindow = 9
        var sum = 0.0
        val sma = DoubleArray(n)
        for (i in 0 until n) {
          sum += closes[i]
          if (i >= maWindow) sum -= closes[i - maWindow]
          val w = minOf(maWindow, i + 1)
          sma[i] = sum / w
        }
        val alpha = 0.22
        var sm = sma[0]
        val ma = DoubleArray(n) { idx ->
          if (idx == 0) sm else {
            sm += alpha * (sma[idx] - sm)
            sm
          }
        }
        ma
      }
    }
  }

  Box(
    modifier = modifier
      .pointerInput(Unit) {
        detectTransformGestures { _, pan, zoom, _ ->
          scale = (scale * zoom).coerceIn(1f, 10f)
          val visible = (n / scale).roundToInt().coerceAtLeast(1)
          scrollX = (scrollX - pan.x / scale).coerceIn(0f, (n - visible).coerceAtLeast(0).toFloat())
          scrollY += (pan.y * 0.003f)
        }
      }
      .pointerInput(Unit) {
        awaitPointerEventScope {
          while (true) {
            val e = awaitPointerEvent()
            e.changes.firstOrNull()?.scrollDelta?.y?.let { d ->
              if (d != 0f) scale = (scale * (1f - d * 0.1f)).coerceIn(1f, 10f)
            }
          }
        }
      }
  ) {
    Canvas(Modifier.fillMaxSize()) {
      val titleStyle = TextStyle(color = colorScheme.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
      val subtitleStyle = TextStyle(color = colorScheme.text, fontSize = 12.sp)
      val axisStyle = TextStyle(color = colorScheme.text, fontSize = 12.sp)
      val tickStyle = TextStyle(color = colorScheme.text, fontSize = 12.sp)

      val titleLayout = textMeasurer.measure(title, titleStyle)
      val subtitleLayout = textMeasurer.measure(subtitle, subtitleStyle)
      val titleBlockH = 6f + titleLayout.size.height + 4f + subtitleLayout.size.height + 6f

      val tickLen = 6f
      val xCaption = textMeasurer.measure(xAxisLabel, axisStyle)
      val yCaption = textMeasurer.measure(yAxisLabel, axisStyle)

      val tickLblH = textMeasurer.measure("00:00", tickStyle).size.height.toFloat()

      val leftCapPad = 8f + yCaption.size.height + 8f
      val rightPad = 8f
      val bottomPad = max(36f, tickLen + 2f + tickLblH + 8f + xCaption.size.height + 4f)
      val topPad = titleBlockH

      val visRange = yDomainSpan / scale
      val yMinVisUnclamped = yMinDomain + scrollY * visRange
      val maxYMin = max(yMinDomain, yMaxDomain - visRange)
      val yMinVis = yMinVisUnclamped.coerceIn(yMinDomain, maxYMin)
      val yMaxVis = (yMinVis + visRange).coerceAtMost(yMaxDomain)

      val yTicks = 4
      val yTickVals = (0..yTicks).map { i -> yMinVis + i * (yMaxVis - yMinVis) / yTicks }
      val yTickLabels = yTickVals.map { v -> v.roundToLong().humanReadableAbbreviation() }
      val yTickMaxW = yTickLabels.maxOf { lbl -> textMeasurer.measure(lbl, tickStyle).size.width }.toFloat()

      val gapCaptionToLabels = 10f
      val gapLabelsToAxis = 8f
      val leftReserved = 8f + yCaption.size.height + gapCaptionToLabels + yTickMaxW + gapLabelsToAxis + tickLen

      val chartLeft = leftReserved
      val chartRight = size.width - 8f
      val chartTop = topPad
      val chartBottom = size.height - bottomPad
      val chartW = (chartRight - chartLeft).coerceAtLeast(1f)
      val chartH = (chartBottom - chartTop).coerceAtLeast(1f)

      val spacing = chartW / visibleBars
      val bodyW = max(1f, min(spacing * 0.65f, spacing - 1f))
      val halfW = bodyW / 2f

      scrollX = clampX(visibleFromScale)

      val first = floor(scrollX).toInt().coerceAtLeast(0)
      val last = (first + visibleFromScale).coerceAtMost(n)

      fun mapX(i: Float): Float = chartLeft + (i - scrollX) * spacing + spacing / 2f
      fun mapY(v: Double): Float {
        val t = ((v - yMinVis) / (yMaxVis - yMinVis)).toFloat()
        return (chartBottom - t * chartH)
          .coerceIn(chartTop + 0.5f, chartBottom - 1.5f)
      }

      val iconLayout = textMeasurer.measure(icon.unicodeIcon, titleStyle)
      val iconGap = 4f
      val iconX = chartLeft + 8f - iconLayout.size.width - iconGap
      val iconY = 6f + (titleLayout.size.height - iconLayout.size.height) / 2f

      drawText(textMeasurer = textMeasurer, text = icon.unicodeIcon, topLeft = Offset(iconX, iconY), style = titleStyle)
      drawText(textMeasurer = textMeasurer, text = title, topLeft = Offset(chartLeft + 8f, 6f), style = titleStyle)
      drawText(textMeasurer = textMeasurer, text = subtitle, topLeft = Offset(chartLeft + 8f, 6f + titleLayout.size.height + 4f), style = subtitleStyle)

      val xCapX = chartLeft + (chartW - xCaption.size.width) / 2f
      val xCapY = chartBottom + tickLen + 2f + tickLblH + 8f
      drawText(textMeasurer = textMeasurer, text = xAxisLabel, topLeft = Offset(xCapX, xCapY), style = axisStyle)

      val textW = yCaption.size.width.toFloat()
      val textH = yCaption.size.height.toFloat()
      val centerY = chartTop + chartH / 2f
      val pivotX = 8f + textH / 2f
      val pivot = Offset(pivotX, centerY)
      val topLeft = Offset(pivotX - textW / 2f, centerY - textH / 2f)
      withTransform({ rotate(degrees = -90f, pivot = pivot) }) {
        drawText(textMeasurer = textMeasurer, text = yAxisLabel, style = axisStyle, topLeft = topLeft)
      }

      drawRect(
        color = colorScheme.background,
        topLeft = Offset(chartLeft, chartTop),
        size = androidx.compose.ui.geometry.Size(chartW, chartH)
      )

      for (i in 0..yTicks) {
        val v = yMinVis + i * (yMaxVis - yMinVis) / yTicks
        val y = mapY(v)
        drawLine(colorScheme.grid, Offset(chartLeft, y), Offset(chartRight, y), 1f)
        drawLine(colorScheme.axis, Offset(chartLeft - tickLen, y), Offset(chartLeft, y), 2f)
        val lbl = yTickLabels[i]
        val layout = textMeasurer.measure(lbl, tickStyle)
        drawText(textMeasurer = textMeasurer, text = lbl, style = tickStyle, topLeft = Offset(chartLeft - tickLen - gapLabelsToAxis - layout.size.width, y - layout.size.height / 2f))
      }

      drawLine(colorScheme.axis, Offset(chartLeft, chartTop), Offset(chartLeft, chartBottom), 2f)
      drawLine(colorScheme.axis, Offset(chartLeft, chartBottom), Offset(chartRight, chartBottom), 2f)

      val desiredXTicks = 4
      val totalMinutes = (last - first).coerceAtLeast(1)
      val rawStep = ceil(totalMinutes / desiredXTicks.toDouble()).toInt().coerceAtLeast(1)
      val step = listOf(1, 2, 5, 10, 15, 30, 60, 120, 300, 600).firstOrNull { it >= rawStep } ?: rawStep

      var iTick = first - (first % step) + step
      while (iTick < last) {
        val x = mapX(iTick.toFloat())
        drawLine(colorScheme.axis, Offset(x, chartBottom), Offset(x, chartBottom + tickLen), 2f)
        val minutes = iTick
        val label = if (minutes < 60) "%d:%02d".format(Locale.US, minutes, 0) else "%d:%02d".format(Locale.US, minutes / 60, minutes % 60)
        val layout = textMeasurer.measure(label, tickStyle)
        drawText(textMeasurer = textMeasurer, text = label, topLeft = Offset(x - layout.size.width / 2f, chartBottom + tickLen + 2f), style = tickStyle)
        iTick += step
      }



      val lastDataIndex = (n - 1).coerceAtLeast(0)
      clipRect(left = chartLeft, top = chartTop, right = chartRight, bottom = chartBottom) {

        // eek
        // Use the precomputed renderMA instead of recomputing from raw data inside the Canvas draw loop
        if (renderMA.isNotEmpty() && n > 0) {
          var prev: Offset? = null
          for (i in first until last) {
            val x = mapX(i.toFloat())
            val y = mapY(renderMA[i])
            if (x in (chartLeft - 4f)..(chartRight + 4f)) {
              val cur = Offset(x, y)
              prev?.let { drawLine(Color(0xFF751287), it, cur, 1.5f) }
              prev = cur
            } else prev = null
          }
        } else {
          // previously empty branch behavior preserved (no MA drawn)
        }

        // ---------- Candles with animated last-candle adjustment ----------
        for (i in first until last) {
          val x = mapX(i.toFloat())
          if (x < chartLeft - bodyW || x > chartRight + bodyW) continue

          // Base mapped positions for this candle
          var o = mapY(data.opens[i])
          var c = mapY(data.closes[i])
          var h = mapY(data.highs[i])
          var l = mapY(data.lows[i])

          val isLast = (i == lastDataIndex && n > 0)
          if (isLast) {
            // Anchor the open; make the close/high/low reflect the animated live value plus historical extremes.
            val openY = mapY(data.opens[i])
            val liveCloseY = mapY(displayedLiveValue)
            val histHighY = mapY(data.highs[i])
            val histLowY = mapY(data.lows[i])

            // pixel Y: smaller = higher price. high should be min Y; low should be max Y.
            h = minOf(openY, liveCloseY, histHighY)
            l = maxOf(openY, liveCloseY, histLowY)
            o = openY
            c = liveCloseY
          }

          // Choose color using the effective close (animated for last candle)
          val closeForColor = if (isLast) displayedLiveValue else data.closes[i]
          val openForColor = data.opens[i]
          val color = when {
            closeForColor > openForColor -> colorScheme.bullish
            closeForColor < openForColor -> colorScheme.bearish
            else -> colorScheme.neutral
          }

          // Wick
          drawLine(color, Offset(x, h), Offset(x, l), 2f)
          // Body
          val top = min(o, c)
          val height = max(1f, abs(o - c))
          drawRect(color = color, topLeft = Offset(x - halfW, top), size = androidx.compose.ui.geometry.Size(bodyW, height))
        }

      }

      // ---------- Price line + blue dot (driven by animated displayed live value) ----------
      val liveVal = displayedLiveValue
      liveVal.let { v ->
        val centerX = if (n > 0) mapX(lastDataIndex.toFloat()) else (chartRight - 8f)
        val yPrice = mapY(v)

        drawLine(
          color = Color(0xFF1E88E5),
          start = Offset(chartLeft, yPrice),
          end = Offset(chartRight, yPrice),
          strokeWidth = 1.5f,
          pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
        )

        drawCircle(
          color = Color(0xFF1E88E5).copy(alpha = pulseAlpha),
          radius = 6f,
          center = Offset(centerX, yPrice)
        )
      }

    } // Canvas
  } // Box
}