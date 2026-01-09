package com.reoky.raidframer.ui.component.graphs

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.unit.dp
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.core.model.CombatEvent
import com.reoky.raidframer.core.model.DamageEvent
import com.reoky.raidframer.core.model.DebuffAppliedEvent
import com.reoky.raidframer.core.model.HealEvent
import org.jetbrains.compose.resources.stringResource
import io.github.koalaplot.core.line.AreaBaseline
import io.github.koalaplot.core.line.AreaPlot2
import io.github.koalaplot.core.line.LinePlot2
import io.github.koalaplot.core.style.AreaStyle
import io.github.koalaplot.core.style.LineStyle
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.xygraph.DefaultPoint
import io.github.koalaplot.core.xygraph.TickPosition
import io.github.koalaplot.core.xygraph.XYGraph
import io.github.koalaplot.core.xygraph.rememberAxisStyle
import io.github.koalaplot.core.xygraph.rememberFloatLinearAxisModel
import kotlinx.coroutines.delay
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.general_instructions_dragging_overlays
import raid_framer_desktop.composeapp.generated.resources.general_metric_type_cc_short
import raid_framer_desktop.composeapp.generated.resources.general_metric_type_damage_short
import raid_framer_desktop.composeapp.generated.resources.general_metric_type_healing_short
import raid_framer_desktop.composeapp.generated.resources.graphs_no_recent_data
import raid_framer_desktop.composeapp.generated.resources.graphs_one_minute
import raid_framer_desktop.composeapp.generated.resources.graphs_player_metric_format
import kotlin.math.max

private const val BUCKET_COUNT = 60
private const val BUCKET_MILLIS = 1000L

enum class MiniGraphMode {
  DMG, HEALS, CC
}

@OptIn(ExperimentalKoalaPlotApi::class, ExperimentalComposeUiApi::class)
@Composable
fun PlayerMetricMiniLineGraphComponent(
  playerName: String,
  modifier: Modifier = Modifier
) {
  var selectedMode by remember { mutableStateOf(MiniGraphMode.DMG) }

  // buckets indexed 0..BUCKET_COUNT-1 representing times [baseSec .. baseSec + BUCKET_COUNT-1]
  var baseSec by remember { mutableStateOf((System.currentTimeMillis() / 1000L) - (BUCKET_COUNT - 1)) }
  var buckets by remember { mutableStateOf(LongArray(BUCKET_COUNT) { 0L }) }


  // update every second and switch event source based on selectedMode
  LaunchedEffect(playerName, selectedMode) {
    while (true) {
      val nowMillis = System.currentTimeMillis()
      val nowSec = nowMillis / 1000L
      val newBase = nowSec - (BUCKET_COUNT - 1)
      val newBuckets = LongArray(BUCKET_COUNT) { 0L }

      val card = PlayerCacheInteractor.getCard(playerName)
      val windowStartMillis = newBase * 1000L

      val events: List<CombatEvent>? = when (selectedMode) {
        MiniGraphMode.DMG -> card?.recentDamageEvents
        MiniGraphMode.HEALS -> card?.recentHealEvents
        MiniGraphMode.CC -> card?.recentDebuffAppliedEvents
      }

      events?.forEach { combatEvent ->
        when (selectedMode) {
          MiniGraphMode.DMG -> {
            val e = combatEvent as? DamageEvent ?: return@forEach
            if (e.timestamp >= windowStartMillis) {
              val sec = e.timestamp / 1000L
              val idx = (sec - newBase).toInt()
              if (idx in 0 until BUCKET_COUNT) {
                // replace property names with the actual ones on your DamageEvent
                val value: Long = e.damage.toLong()
                newBuckets[idx] += value
              }
            }
          }

          MiniGraphMode.HEALS -> {
            val e = combatEvent as? HealEvent ?: return@forEach
            if (e.timestamp >= windowStartMillis) {
              val sec = e.timestamp / 1000L
              val idx = (sec - newBase).toInt()
              if (idx in 0 until BUCKET_COUNT) {
                // replace property names with the actual ones on your HealEvent
                val value: Long = e.amount.toLong()
                newBuckets[idx] += value
              }
            }
          }

          MiniGraphMode.CC -> {
            val e = combatEvent as? DebuffAppliedEvent ?: return@forEach
            if (e.timestamp >= windowStartMillis) {
              val sec = e.timestamp / 1000L
              val idx = (sec - newBase).toInt()
              if (idx in 0 until BUCKET_COUNT) {
                // replace property names with the actual ones on your DebuffAppliedEvent
                val value: Long = 1L // count each application as 1
                newBuckets[idx] += value
              }
            }
          }
        }
      }

      baseSec = newBase
      buckets = newBuckets
      delay(1000L)
    }
  }

  val maxY = remember(buckets) {
    val maxVal = buckets.maxOrNull() ?: 0L
    max(maxVal.toFloat(), 10f) * 1.1f
  }

  val highlightColor = when (selectedMode) {
    MiniGraphMode.DMG -> Color.Red
    MiniGraphMode.HEALS -> Color.Green
    MiniGraphMode.CC -> Color.Cyan
  }

  Column(modifier = modifier.fillMaxWidth()) {
    // track hover for the whole graph area
    var isHovered by remember { mutableStateOf(false) }

    Box(
      modifier = Modifier
        .fillMaxHeight()
        .fillMaxWidth()
        .background(Color.Transparent)
        .padding(start = 0.dp, end = 6.dp, top = 2.dp, bottom = 4.dp)
        .pointerMoveFilter(
          onEnter = {
            isHovered = true
            false
          },
          onExit = {
            isHovered = false
            false
          }
        )
    ) {
      if (buckets.all { it == 0L }) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row {
              Text(
                stringResource(Res.string.graphs_no_recent_data),
                color = Color.LightGray,
                style = MaterialTheme.typography.caption
              )
            }
            Row {
              Text(
                stringResource(Res.string.general_instructions_dragging_overlays),
                color = Color.LightGray,
                style = MaterialTheme.typography.caption
              )
            }

          }
        }
      } else {
        val series = remember(buckets) {
          List(BUCKET_COUNT) { i -> DefaultPoint(i.toFloat(), buckets[i].toFloat()) }
        }

        val topVal = maxY
        val midVal = topVal / 2f

        XYGraph<Float, Float>(
          xAxisModel = rememberFloatLinearAxisModel(
            range = 0f..(BUCKET_COUNT - 1).toFloat(),
            minViewExtent = (BUCKET_COUNT - 1).toFloat() * 0.2F,
            maxViewExtent = (BUCKET_COUNT - 1).toFloat(),
            minimumMajorTickIncrement = (BUCKET_COUNT - 1).toFloat() * 0.25F,
            minimumMajorTickSpacing = 40.dp,
            minorTickCount = 0
          ),
          yAxisModel = rememberFloatLinearAxisModel(
            range = 0f..maxY,
            minViewExtent = (maxY - 0f) * 0.2F,
            maxViewExtent = (maxY - 0f),
            minimumMajorTickIncrement = (maxY - 0) * 0.245F,
            minimumMajorTickSpacing = 50.dp,
            minorTickCount = 0
          ),
          horizontalMajorGridLineStyle = LineStyle(SolidColor(Color.White), strokeWidth = 0.5.dp, alpha = 0.0f),
          verticalMajorGridLineStyle = LineStyle(SolidColor(Color.White), strokeWidth = 0.5.dp, alpha = 0.20f),
          horizontalMinorGridLineStyle = LineStyle(SolidColor(Color.White), strokeWidth = 0.5.dp, alpha = 0f),
          verticalMinorGridLineStyle = LineStyle(SolidColor(Color.White), strokeWidth = 0.5.dp, alpha = 0f),
          xAxisStyle = rememberAxisStyle (
            color = Color.White,
            tickPosition = TickPosition.None,
            lineWidth = 0.5.dp
          ),
          yAxisStyle = rememberAxisStyle(
            color = Color.White,
            majorTickSize = 5.dp,
            minorTickSize = 3.dp,
            tickPosition = TickPosition.Outside,
            lineWidth = 0.5.dp,
            labelRotation = 0
          ),
          xAxisLabels = { _: Float -> "" },
          yAxisLabels = @Composable { yVal: Float ->
            val label = if (yVal >= 1000f) "${(yVal / 1000f).toInt()}k" else yVal.toInt().toString()
            Text(label, style = MaterialTheme.typography.caption, color = Color.LightGray)
          }
        ) {


          AreaPlot2(
            data = series,
            areaStyle = AreaStyle(
              brush = Brush.verticalGradient(listOf(highlightColor.copy(alpha = 0.35f), Color.Transparent)),
              alpha = 1.0f
            ),
            areaBaseline = AreaBaseline.ConstantLine(0f)
          )

          LinePlot2(
            data = series,
            lineStyle = LineStyle(
              brush = Brush.linearGradient(listOf(highlightColor, highlightColor.copy(alpha = 0.85f))),
              strokeWidth = 1.2.dp,
              alpha = 0.95f
            )
          )
        }

        // Overall mini-graph title and subtitle (centered)
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // REPLACED: Hardcoded strings and logic
            val metricName = when(selectedMode) {
              MiniGraphMode.DMG -> stringResource(Res.string.general_metric_type_damage_short)
              MiniGraphMode.HEALS -> stringResource(Res.string.general_metric_type_healing_short)
              MiniGraphMode.CC -> stringResource(Res.string.general_metric_type_cc_short)
            }
            Text(
              stringResource(Res.string.graphs_player_metric_format, playerName, metricName),
              color = Color.White.copy(alpha = 0.2f),
              style = MaterialTheme.typography.h6
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              stringResource(Res.string.graphs_one_minute),
              color = Color.White.copy(alpha = 0.2f),
              style = MaterialTheme.typography.subtitle2
            )
          }
        }
      }

      // animate toggles alpha based on outer hover
      val buttonsAlpha by animateFloatAsState(
        targetValue = if (isHovered) 1f else 0f,
        animationSpec = tween(durationMillis = 250)
      )

      // tiny top-right mode toggles
      Row(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(6.dp)
          .alpha(buttonsAlpha),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        fun tinyToggle(label: String, mode: MiniGraphMode) : @Composable () -> Unit = {
          var hovered by remember { mutableStateOf(false) }
          val isSelected = mode == selectedMode
          val bgAlpha = when {
            isSelected -> 0.9f
            hovered -> 0.55f
            else -> 0.35f
          }
          Box(
            modifier = Modifier
              .height(18.dp)
              .width(42.dp)
              .background(if (isSelected) highlightColor.copy(alpha = 0.2f) else Color.White.copy(alpha = bgAlpha), shape = MaterialTheme.shapes.small)
              .pointerMoveFilter(
                onEnter = {
                  hovered = true; false
                },
                onExit = {
                  hovered = false; false
                }
              )
              .clickable { selectedMode = mode },
            contentAlignment = Alignment.Center
          ) {
            Text(
              label,
              color = Color.White,
              style = MaterialTheme.typography.caption
            )
          }
        }

        // Render the three toggles
        // REPLACED: Hardcoded strings
        tinyToggle(stringResource(Res.string.general_metric_type_damage_short), MiniGraphMode.DMG).invoke()
        tinyToggle(stringResource(Res.string.general_metric_type_healing_short), MiniGraphMode.HEALS).invoke()
        tinyToggle(stringResource(Res.string.general_metric_type_cc_short), MiniGraphMode.CC).invoke()

      }
    }
  }
}
