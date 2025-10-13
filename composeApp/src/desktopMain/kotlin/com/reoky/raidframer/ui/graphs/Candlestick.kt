package com.reoky.raidframer.ui.graphs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lol.rfcloud.core.helpers.humanReadableAbbreviation
import java.util.*
import kotlin.math.*

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
  title: String = chartTitleFor("Reoky", DamageType.CC, true),
  subtitle: String = "Black Dragon PvP",
  icon: DamageType = DamageType.CC,
  xAxisLabel: String = "Time (Minutes)",
  yAxisLabel: String = "Damage Per Second",
  data: CandlestickDataFrame,
  colorScheme: CandlestickColorScheme,
  modifier: Modifier = Modifier.fillMaxWidth().height(300.dp)
) {
  val textMeasurer = rememberTextMeasurer()

  val n = data.xValues.size
  val rawMax = data.highs.maxOrNull() ?: 1.0
  val rawMin = data.lows.minOrNull() ?: 0.0
  val span = (rawMax - rawMin).coerceAtLeast(1e-9)

  // Visual padding: give headroom + a small "floor" under zero when all data >= 0
  val topPadPct = 0.10      // 10% headroom
  val bottomPadPct = if (rawMin >= 0.0) 0.05 else 0.10  // at least 5% below zero for looks (oh eek!)
  val yMinDomain = if (rawMin >= 0.0) -rawMax * bottomPadPct else rawMin - span * bottomPadPct
  val yMaxDomain = rawMax + span * topPadPct
  val yDomainSpan = (yMaxDomain - yMinDomain).coerceAtLeast(1e-6)

  var scale by remember { mutableStateOf(1f) }
  var scrollX by remember { mutableStateOf(0f) }
  var scrollY by remember { mutableStateOf(0f) }

  fun clampX(visible: Int): Float {
    val maxOffset = (n - visible).coerceAtLeast(0)
    return scrollX.coerceIn(0f, maxOffset.toFloat())
  }

  Box(
    modifier = modifier
      .pointerInput(Unit) {
        detectTransformGestures { _, pan, zoom, _ ->
          scale = (scale * zoom).coerceIn(1f, 10f)
          val visible = (n / scale).roundToInt().coerceAtLeast(1)
          scrollX = (scrollX - pan.x / scale).coerceIn(0f, (n - visible).coerceAtLeast(0).toFloat())
          // vertical scroll in "value units" (we clamp later after we know chartHeight)
          scrollY += (pan.y * 0.003f)       // small constant keeps it comfortable
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

      /* ~~~~ Constants & layout ~~~~ */
      val titleStyle = TextStyle(color = colorScheme.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
      val subtitleStyle = TextStyle(color = colorScheme.text, fontSize = 12.sp)
      val axisStyle = TextStyle(color = colorScheme.text, fontSize = 12.sp)

      val titleLayout = textMeasurer.measure(title, titleStyle)
      val subtitleLayout = textMeasurer.measure(subtitle, subtitleStyle)
      val titleBlockH = 6f + titleLayout.size.height + 4f + subtitleLayout.size.height + 6f

      val tickLen = 6f
      val tickStyle = TextStyle(color = colorScheme.text, fontSize = 12.sp)
      val xCaption = textMeasurer.measure(xAxisLabel, axisStyle)
      val yCaption = textMeasurer.measure(yAxisLabel, axisStyle)

      // tick label height (for bottom spacing)
      val tickLblH = textMeasurer.measure("00:00", tickStyle).size.height.toFloat()

      /* ~~~~ Paddingness ~~~~ */
      val leftCapPad = 8f + yCaption.size.height + 8f         // rotated caption reserve
      val rightPad = 8f
      val bottomPad = max(36f, tickLen + 2f + tickLblH + 8f + xCaption.size.height + 4f)
      val topPad = titleBlockH

      // y window with zoom + vertical scroll clamped to domain
      val visRange = yDomainSpan / scale
      val yMinVisUnclamped = yMinDomain + scrollY * visRange  // scrollY is unitless, small

      val maxYMin = max(yMinDomain, yMaxDomain - visRange)
      val yMinVis = yMinVisUnclamped.coerceIn(yMinDomain, maxYMin)
      val yMaxVis = (yMinVis + visRange).coerceAtMost(yMaxDomain)

      val yTicks = 4
      val yTickVals = (0..yTicks).map { i -> yMinVis + i * (yMaxVis - yMinVis) / yTicks }
      val yTickLabels = yTickVals.map { v -> v.roundToLong().humanReadableAbbreviation() }
      val yTickMaxW = yTickLabels.maxOf { lbl -> textMeasurer.measure(lbl, tickStyle).size.width }.toFloat()

      // gaps to keep everything readable
      val gapCaptionToLabels = 10f
      val gapLabelsToAxis = 8f

      // reserve: [8 px margin] + [rotated caption width] + gap + [label max width] + gap + [tick length]
      val leftReserved = 8f + yCaption.size.height + gapCaptionToLabels + yTickMaxW + gapLabelsToAxis + tickLen

      val chartLeft = leftReserved
      val chartRight = size.width - 8f
      val chartTop = topPad
      val chartBottom = size.height - bottomPad
      val chartW = (chartRight - chartLeft).coerceAtLeast(1f)
      val chartH = (chartBottom - chartTop).coerceAtLeast(1f)

      // visible X window
      val visibleBars = (n / scale).roundToInt().coerceAtLeast(1)
      scrollX = clampX(visibleBars)
      val first = kotlin.math.floor(scrollX).toInt().coerceAtLeast(0)
      val last = kotlin.math.ceil(scrollX + visibleBars).toInt().coerceAtMost(n)

      val spacing = chartW / visibleBars
      val bodyW = (spacing * 0.65f).coerceAtMost(spacing - 1f)
      val halfW = bodyW / 2f

      // graphing helpers for mapping data -> screen coords
      fun mapX(i: Float): Float = chartLeft + (i - scrollX) * spacing + spacing / 2f
      fun mapY(v: Double): Float {
        val t = ((v - yMinVis) / (yMaxVis - yMinVis)).toFloat()
        return (chartBottom - t * chartH)
          .coerceIn(chartTop + 0.5f, chartBottom - 1.5f)
      }

      // damage type icon
      val iconLayout = textMeasurer.measure(icon.unicodeIcon, titleStyle)
      val iconGap = 4f
      val iconX = chartLeft + 8f - iconLayout.size.width - iconGap
      val iconY = 6f + (titleLayout.size.height - iconLayout.size.height) / 2f

      drawText(
        textMeasurer = textMeasurer,
        text = icon.unicodeIcon,
        topLeft = Offset(iconX, iconY),
        style = titleStyle
      )

      /* ~~~~ Title & captions ~~~~ */
      drawText(
        textMeasurer = textMeasurer,
        text = title,
        topLeft = Offset(chartLeft + 8f, 6f),
        style = titleStyle
      )
      drawText(
        textMeasurer = textMeasurer,
        text = subtitle,
        topLeft = Offset(chartLeft + 8f, 6f + titleLayout.size.height + 4f),
        style = subtitleStyle
      )

      // X caption (centered)
      val xCapX = chartLeft + (chartW - xCaption.size.width) / 2f
      val xCapY = chartBottom + tickLen + 2f + tickLblH + 8f

      // old method signaturee
      //drawText(textMeasurer, xAxisLabel, axisStyle, Offset(xCapX, xCapY))
      drawText(
        textMeasurer = textMeasurer,
        text = xAxisLabel,
        topLeft = Offset(xCapX, xCapY),
        style = axisStyle
      )

      // Rotated Y caption
      val textW = yCaption.size.width.toFloat()
      val textH = yCaption.size.height.toFloat()

      // center of chart vertically
      val centerY = chartTop + chartH / 2f

      // place the rotated text's center inside the reserved left area:
      //  left margin (8f) + half of the rotated text horizontal span (which equals text height)
      val pivotX = 8f + textH / 2f
      val pivot = Offset(pivotX, centerY)

      // derive top-left for drawText (pre-rotation coordinates)
      val topLeft = Offset(pivotX - textW / 2f, centerY - textH / 2f)

      withTransform({
        rotate(degrees = -90f, pivot = pivot)
      }) {
        drawText(
          textMeasurer = textMeasurer,
          text = yAxisLabel,
          style = axisStyle,
          topLeft = topLeft,
        )
      }

      /* ~~~~ Grid Axes & Ticks ~~~~ */
      for (i in 0..yTicks) {
        val v = yMinVis + i * (yMaxVis - yMinVis) / yTicks
        val y = mapY(v)
        drawLine(colorScheme.grid, Offset(chartLeft, y), Offset(chartRight, y), 1f)

        // tick + label using Long.humanReadableAbbreviation()
        drawLine(colorScheme.axis, Offset(chartLeft - tickLen, y), Offset(chartLeft, y), 2f)

        val lbl = yTickLabels[i]
        val layout = textMeasurer.measure(lbl, tickStyle)
        drawText(
          textMeasurer = textMeasurer,
          text = lbl,
          style = tickStyle,
          topLeft = Offset(
            chartLeft - tickLen - gapLabelsToAxis - layout.size.width,
            y - layout.size.height / 2f
          )
        )
      }

      // axes
      drawLine(colorScheme.axis, Offset(chartLeft, chartTop), Offset(chartLeft, chartBottom), 2f)
      drawLine(colorScheme.axis, Offset(chartLeft, chartBottom), Offset(chartRight, chartBottom), 2f)

      // X ticks (simple, steady spacing)
      val desiredXTicks = 4
      val totalMinutes = (last - first).coerceAtLeast(1)
      val rawStep = ceil(totalMinutes / desiredXTicks.toDouble()).toInt().coerceAtLeast(1)
      val step = listOf(1, 2, 5, 10, 15, 30, 60, 120, 300, 600).firstOrNull { it >= rawStep } ?: rawStep

      var iTick = first - (first % step) + step
      while (iTick < last) {
        val x = mapX(iTick.toFloat())
        drawLine(colorScheme.axis, Offset(x, chartBottom), Offset(x, chartBottom + tickLen), 2f)

        val minutes = iTick
        val label = if (minutes < 60) "%d:%02d".format(Locale.US, minutes, 0)
        else "%d:%02d".format(Locale.US, minutes / 60, minutes % 60)

        val layout = textMeasurer.measure(label, tickStyle)
        drawText(
          textMeasurer = textMeasurer,
          text = label,
          topLeft = Offset(x - layout.size.width / 2f, chartBottom + tickLen + 2f),
          style = tickStyle
        )
        iTick += step
      }

      // Draw a moving average line (9-period SMA smoothed with EMA, purple color)
      val maWindow = 9
      if (n > 0) {
        var sum = 0.0
        val sma = DoubleArray(n)
        for (i in 0 until n) {
          sum += data.closes[i]
          if (i >= maWindow) sum -= data.closes[i - maWindow]
          val w = minOf(maWindow, i + 1)
          sma[i] = sum / w
        }
        val alpha = 0.22
        var sm = sma[0]
        val ma = DoubleArray(n) { idx -> if (idx == 0) sm else { sm += alpha * (sma[idx] - sm); sm } }

        var prev: Offset? = null
        for (i in first until last) {
          val x = mapX(i.toFloat())
          val y = mapY(ma[i])
          if (x in (chartLeft - 4f)..(chartRight + 4f)) {
            val cur = Offset(x, y)
            prev?.let { drawLine(Color(0xFF751287), it, cur, 1.5f) }
            prev = cur
          } else prev = null
        }
      }

      // ---------- Candles (clamped to plot area so they never cross the axes) ----------
      for (i in first until last) {
        val x = mapX(i.toFloat())
        if (x < chartLeft - bodyW || x > chartRight + bodyW) continue

        val o = mapY(data.opens[i])
        val c = mapY(data.closes[i])
        val h = mapY(data.highs[i])
        val l = mapY(data.lows[i])

        val color = when {
          data.closes[i] > data.opens[i] -> colorScheme.bullish
          data.closes[i] < data.opens[i] -> colorScheme.bearish
          else -> colorScheme.neutral
        }

        drawLine(color, Offset(x, h), Offset(x, l), 2f)       // wick
        val top = min(o, c)
        val height = max(1f, abs(o - c))
        drawRect(
          color = color,
          topLeft = Offset(x - halfW, top),
          size = androidx.compose.ui.geometry.Size(bodyW, height)
        )
      }
    }
  }
}
