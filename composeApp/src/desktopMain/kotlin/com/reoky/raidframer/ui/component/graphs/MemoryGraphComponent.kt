package com.reoky.raidframer.ui.component.graphs

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reoky.raidframer.core.interactor.Log
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.WinNT.HANDLE
import com.sun.jna.win32.StdCallLibrary
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
import kotlin.math.max

private const val SAMPLE_COUNT = 60
private const val SAMPLE_INTERVAL_MS = 2000L
private const val BYTES_PER_MB = 1024L * 1024L
private const val TAG = "MemoryGraph"

private data class MemorySample(val heapMB: Int, val processMB: Int?)

private interface ProcessMemoryApi : StdCallLibrary {
  fun GetProcessMemoryInfo(
    process: HANDLE?,
    counters: Memory,
    size: Int
  ): Boolean
}

private const val PROCESS_MEMORY_COUNTERS_SIZE = 72L
private const val PROCESS_MEMORY_WORKING_SET_OFFSET = 16L

private val processMemoryApi: ProcessMemoryApi? by lazy {
  runCatching { Native.load("psapi", ProcessMemoryApi::class.java) }
    .onFailure { error -> Log.error(TAG, "Failed to load psapi.dll: ${error.javaClass.name}: ${error.message}") }
    .getOrNull()
}

/**
 * Returns the JVM heap usage in MB (totalMemory - freeMemory).
 */
private fun getHeapUsedMB(): Int {
  return runCatching {
    val runtime = Runtime.getRuntime()
    ((runtime.totalMemory() - runtime.freeMemory()) / BYTES_PER_MB)
      .coerceIn(0L, Int.MAX_VALUE.toLong())
      .toInt()
  }.onFailure { error ->
    Log.error(TAG, "Heap memory query failed: ${error.javaClass.name}: ${error.message}")
  }.getOrDefault(0)
}

/**
 * Returns the Windows process working set in MB. A null result means that the
 * native query was unavailable or failed; callers must not treat it as zero.
 */
private fun getProcessMemoryMB(): Int? {
  if (!System.getProperty("os.name", "").contains("Windows", ignoreCase = true)) {
    Log.warn(TAG, "Process memory query skipped: unsupported OS '${System.getProperty("os.name")}'")
    return null
  }

  return runCatching {
    val processHandle: HANDLE = Kernel32.INSTANCE.GetCurrentProcess()

    val api = processMemoryApi ?: return@runCatching null
    // Use a raw fixed-layout buffer instead of Structure. This avoids JNA
    // field reflection, which can fail in packaged Kotlin/JVM builds.
    val counters = Memory(PROCESS_MEMORY_COUNTERS_SIZE)
    counters.setInt(0, PROCESS_MEMORY_COUNTERS_SIZE.toInt())
    val success = api.GetProcessMemoryInfo(
      processHandle,
      counters,
      PROCESS_MEMORY_COUNTERS_SIZE.toInt()
    )
    val lastError = Kernel32.INSTANCE.GetLastError()
    if (!success) {
      Log.warn(TAG, "GetProcessMemoryInfo failed: success=false lastError=$lastError")
      return@runCatching null
    }

    val workingSetBytes = counters.getLong(PROCESS_MEMORY_WORKING_SET_OFFSET)
    if (workingSetBytes <= 0L) {
      Log.warn(TAG, "GetProcessMemoryInfo returned invalid working set: $workingSetBytes bytes")
      return@runCatching null
    }

    val memoryMB = (workingSetBytes / BYTES_PER_MB)
      .coerceIn(0L, Int.MAX_VALUE.toLong())
      .toInt()
    Log.debug(TAG, "Process memory query succeeded: workingSetBytes=$workingSetBytes processMB=$memoryMB")
    memoryMB
  }.onFailure { error ->
    Log.error(TAG, "Process memory query threw ${error.javaClass.name}: ${error.message}")
  }.getOrNull()
}

/**
 * A compact memory usage graph that samples JVM heap every 2 seconds
 * and displays the last 60 samples (2 minutes) as an area chart.
 * Current and peak values are overlaid as faint centered text.
 *
 * @param modifier The modifier to apply to the outer container.
 * @param sampleCount The number of data points to keep (default 60 = 2 minutes at 2s intervals).
 */
@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
fun MemoryGraphComponent(
  modifier: Modifier = Modifier,
  sampleCount: Int = SAMPLE_COUNT,
  title: String = "JVM heap",
  processMemory: Boolean = false
) {
  val safeSampleCount = sampleCount.coerceIn(2, 600)
  var samples by remember(safeSampleCount) { mutableStateOf(IntArray(safeSampleCount) { 0 }) }
  var currentSample by remember(safeSampleCount, processMemory) {
    mutableStateOf(MemorySample(0, null))
  }

  LaunchedEffect(safeSampleCount, processMemory) {
    Log.info(TAG, "Starting ${if (processMemory) "process" else "heap"} memory sampler: sampleCount=$safeSampleCount intervalMs=$SAMPLE_INTERVAL_MS")
    try {
      while (true) {
        val sample = MemorySample(getHeapUsedMB(), if (processMemory) getProcessMemoryMB() else null)
        val value = if (processMemory) sample.processMB else sample.heapMB
        if (value != null) {
          val newSamples = IntArray(safeSampleCount)
          System.arraycopy(samples, 1, newSamples, 0, safeSampleCount - 1)
          newSamples[safeSampleCount - 1] = value
          samples = newSamples
        } else {
          Log.warn(TAG, "Skipping unavailable process-memory sample; retaining previous chart data")
        }
        currentSample = sample
        Log.debug(TAG, "Recorded memory sample: heapMB=${sample.heapMB} processMB=${sample.processMB ?: "unavailable"}")
        delay(SAMPLE_INTERVAL_MS)
      }
    } finally {
      Log.info(TAG, "Stopping ${if (processMemory) "process" else "heap"} memory sampler")
    }
  }

  val currentMB = if (processMemory) currentSample.processMB ?: samples.lastOrNull() ?: 0 else currentSample.heapMB
  val peakMB = samples.maxOrNull() ?: 0
  val chartSampleCount = samples.size

  val memColor = when {
    currentMB < 1024 -> Color(0xFF4CAF50) // green
    currentMB < 2048 -> Color(0xFFFFC107) // yellow
    currentMB < 3072 -> Color(0xFFFF9800) // orange
    else -> Color(0xFFF44336)              // red
  }

  val series = remember(samples) {
    List(chartSampleCount) { i -> DefaultPoint(i.toFloat(), samples[i].toFloat()) }
  }

  val maxY = remember(peakMB, currentMB) {
    val ceiling = max(peakMB, currentMB) * 1.15f
    max(ceiling, 100f) // minimum 100MB range
  }

  // The chart with centered overlay text
  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(108.dp)
  ) {
    XYGraph<Float, Float>(
      xAxisModel = rememberFloatLinearAxisModel(
        range = 0f..(chartSampleCount - 1).toFloat(),
        minViewExtent = (chartSampleCount - 1).toFloat() * 0.3f,
        maxViewExtent = (chartSampleCount - 1).toFloat(),
        minimumMajorTickIncrement = (chartSampleCount - 1).toFloat() * 0.25f,
        minimumMajorTickSpacing = 40.dp,
        minorTickCount = 0
      ),
      yAxisModel = rememberFloatLinearAxisModel(
        range = 0f..maxY,
        minViewExtent = (maxY - 0f) * 0.3f,
        maxViewExtent = maxY,
        minimumMajorTickIncrement = maxY * 0.25f,
        minimumMajorTickSpacing = 30.dp,
        minorTickCount = 0
      ),
      horizontalMajorGridLineStyle = LineStyle(SolidColor(Color.White), strokeWidth = 0.5.dp, alpha = 0.0f),
      verticalMajorGridLineStyle = LineStyle(SolidColor(Color.White), strokeWidth = 0.5.dp, alpha = 0.10f),
      horizontalMinorGridLineStyle = LineStyle(SolidColor(Color.White), strokeWidth = 0.5.dp, alpha = 0f),
      verticalMinorGridLineStyle = LineStyle(SolidColor(Color.White), strokeWidth = 0.5.dp, alpha = 0f),
      xAxisStyle = rememberAxisStyle(color = Color.White, tickPosition = TickPosition.None, lineWidth = 0.5.dp),
      yAxisStyle = rememberAxisStyle(
        color = Color.White, majorTickSize = 3.dp, minorTickSize = 0.dp,
        tickPosition = TickPosition.Outside, lineWidth = 0.5.dp, labelRotation = 0
      ),
      xAxisLabels = { _: Float -> "" },
      yAxisLabels = @Composable { yVal: Float ->
        val l = if (yVal >= 1000f) "${(yVal / 1000f).toInt()}k" else yVal.toInt().toString()
        Text(l, style = MaterialTheme.typography.caption, color = Color.LightGray)
      }
    ) {
      AreaPlot2(
        data = series,
        areaStyle = AreaStyle(
          brush = Brush.verticalGradient(listOf(memColor.copy(alpha = 0.4f), Color.Transparent)),
          alpha = 1.0f
        ),
        areaBaseline = AreaBaseline.ConstantLine(0f)
      )

      LinePlot2(
        data = series,
        lineStyle = LineStyle(
          brush = Brush.linearGradient(listOf(memColor, memColor.copy(alpha = 0.85f))),
          strokeWidth = 1.2.dp,
          alpha = 0.95f
        )
      )
    }

    // Centered overlay text — faint, matching Mini Overlay style
    Column(
      modifier = Modifier.align(Alignment.Center),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = title,
        color = Color.White.copy(alpha = 0.45f),
        fontSize = 11.sp,
        fontWeight = FontWeight.Light
      )
      Text(
        text = "${currentMB}MB",
        color = Color.White.copy(alpha = 0.50f),
        fontSize = 18.sp,
        fontWeight = FontWeight.Light
      )
      if (peakMB > 0) {
        Text(
          text = "peak ${peakMB}MB",
          color = Color.White.copy(alpha = 0.30f),
          fontSize = 10.sp,
          fontWeight = FontWeight.Light
        )
      }
    }
  }
}
