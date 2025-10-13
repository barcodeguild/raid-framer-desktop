package com.reoky.raidframer.core.mock

import kotlin.math.*
import kotlin.random.Random
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.reoky.raidframer.ui.graphs.CandlestickDataFrame

enum class Regime { Idle, Skirmish, Battle }

fun mockCandlestickDataFrame(
  points: Int = 60,
  intervalMinutes: Int = 1,
  seed: Long? = null,
  maxDpsCap: Double = 100_000.0
): CandlestickDataFrame {
  val rnd = seed?.let { Random(it) } ?: Random.Default

  data class RegimeParams(
    val mean: Double,       // steady-state average DPS this regime “pulls” to
    val vol: Double,        // per-minute natural volatility around the mean
    val spikeP: Double,     // probability of a burst spike on this minute
    val spikeScale: Double  // typical spike size (added to the mean before noise)
  )

  val params = mapOf(
    Regime.Idle     to RegimeParams(mean = 80.0,    vol = 60.0,   spikeP = 0.02, spikeScale = 600.0),
    Regime.Skirmish to RegimeParams(mean = 1_000.0, vol = 700.0,  spikeP = 0.05, spikeScale = 2_500.0),
    Regime.Battle   to RegimeParams(mean = 8_000.0, vol = 4_500.0, spikeP = 0.10, spikeScale = 8_000.0)
  )

  // --- Build a regime schedule (segments with random lengths) ----------------
  fun nextLen(r: Regime): Int = when (r) {
    Regime.Idle     -> rnd.nextInt(3, 10)       // 3–9 minutes idle/between fights
    Regime.Skirmish -> rnd.nextInt(2, 8)        // 2–7 minutes
    Regime.Battle   -> rnd.nextInt(2, 15)       // 2–14 minutes
  }

  val regimes = ArrayList<Regime>()
  var remain = points
  var cur = listOf(Regime.Idle, Regime.Skirmish, Regime.Battle).random(rnd)
  while (remain > 0) {
    val len = min(nextLen(cur), remain)
    repeat(len) { regimes += cur }
    remain -= len
    // rotate likely next state
    cur = when (cur) {
      Regime.Idle     -> if (rnd.nextDouble() < 0.6) Regime.Skirmish else Regime.Battle
      Regime.Skirmish -> if (rnd.nextDouble() < 0.6) Regime.Battle else Regime.Idle
      Regime.Battle   -> if (rnd.nextDouble() < 0.7) Regime.Skirmish else Regime.Idle
    }
  }

  // --- Time axis -------------------------------------------------------------
  val xValues = ArrayList<String>(points)
  var t = LocalDateTime.now().withSecond(0).withNano(0)
  val fmt = DateTimeFormatter.ofPattern("HH:mm") // minutes are what you show on X

  // --- Series containers -----------------------------------------------------
  val opens  = DoubleArray(points)
  val highs  = DoubleArray(points)
  val lows   = DoubleArray(points)
  val closes = DoubleArray(points)

  // --- Price/damage process --------------------------------------------------
  // Mean-reverting (AR(1)) toward the regime mean + occasional spikes.
  // k controls how quickly we move toward the current regime mean (ramps).
  val k = 0.28f

  // Start close near skirmish/battle small value so the first minutes look sane.
  var prevClose = 600.0

  repeat(points) { i ->
    val r = regimes[i]
    val p = params.getValue(r)

    // Spike (rare, regime dependent)
    val spike = if (rnd.nextDouble() < p.spikeP) {
      // log-normal-ish positive burst
      (exp(rnd.nextDouble(0.0, 1.1)) - 1.0) * p.spikeScale
    } else 0.0

    // Pull toward regime mean + noise + spike
    val target = p.mean + spike
    val drift = (target - prevClose) * k
    val noise = rnd.nextDouble(-p.vol, p.vol)
    val newClose = (prevClose + drift + noise).coerceIn(0.0, maxDpsCap)

    // Candle anatomy ----------------------------------------------------------
    // Open is last close + very small jitter (keeps continuity)
    val open = (prevClose + rnd.nextDouble(-p.vol * 0.15, p.vol * 0.15))
      .coerceIn(0.0, maxDpsCap)

    val close = newClose

    // Decide wick “style” (some with long tails, some with tiny or none)
    // weights: long both (15%), long top (15%), long bottom (15%), short both (40%), none (15%)
    val stylePick = rnd.nextDouble()
    val (upMul, dnMul) = when {
      stylePick < 0.15 -> 1.4 to 1.4
      stylePick < 0.30 -> 1.6 to 0.2
      stylePick < 0.45 -> 0.2 to 1.6
      stylePick < 0.85 -> 0.5 to 0.5
      else             -> 0.05 to 0.05
    }

    // Wick magnitudes scale with current regime volatility and candle body size
    val body = abs(close - open)
    val base = max(p.vol * 0.25, body * 0.35)

    val wickUp = rnd.nextDouble(base * 0.2, base) * upMul
    val wickDn = rnd.nextDouble(base * 0.2, base) * dnMul

    var high = max(open, close) + wickUp
    var low  = min(open, close) - wickDn

    // Clamp to [0, cap], and ensure the usual relations
    if (low < 0.0) low = 0.0
    if (high > maxDpsCap) high = maxDpsCap
    val finalHigh = max(high, max(open, close))
    val finalLow  = min(low,  min(open, close))

    // Commit
    opens[i]  = open
    highs[i]  = finalHigh
    lows[i]   = finalLow
    closes[i] = close
    xValues  += t.format(fmt)

    prevClose = close
    t = t.plusMinutes(intervalMinutes.toLong())
  }

  // Round to 1 decimal to keep files small/consistent
  fun Double.round1() = (this * 10.0).roundToInt() / 10.0
  return CandlestickDataFrame(
    xValues = xValues,
    opens   = opens.map(Double::round1),
    highs   = highs.map(Double::round1),
    lows    = lows.map(Double::round1),
    closes  = closes.map(Double::round1)
  )
}
