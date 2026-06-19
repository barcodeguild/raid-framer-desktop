package com.reoky.raidframer.core.interactor

import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.model.BuffEndedEvent
import com.reoky.raidframer.core.model.BuffGainedEvent
import com.reoky.raidframer.core.model.CastingEvent
import com.reoky.raidframer.core.model.CombatEvent
import com.reoky.raidframer.core.model.DamageEvent
import com.reoky.raidframer.core.model.DebuffGainedEvent
import com.reoky.raidframer.core.model.HealEvent
import com.reoky.raidframer.core.model.SuccessfulCastEvent
import com.reoky.raidframer.core.serialization.AppJson
import com.reoky.raidframer.core.serialization.CombatEventPayload
import com.reoky.raidframer.ui.export.ImageExportInteractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

object CombatLogInteractor : Interactor() {

  private const val TAG = "CombatLogInteractor"

  // Don't we love Microsoft for continually moving the user's documents directory? Now it's sometimes mounted inside
  // a OneDrive so we have to do all this extra logic just to locate an output folder
  private fun getDocumentsDirectory(): String? {
    if (System.getProperty("os.name").lowercase().contains("win")) {
      val userProfile = System.getenv("USERPROFILE")
      if (!userProfile.isNullOrBlank()) {
        val oneDriveDocs = Paths.get(userProfile, "OneDrive", "Documents")
        if (Files.exists(oneDriveDocs)) return oneDriveDocs.toString()
        val regularDocs = Paths.get(userProfile, "Documents")
        if (Files.exists(regularDocs)) return regularDocs.toString()
      }
    }
    val home = System.getProperty("user.home") ?: return null
    return Paths.get(home, "Documents").toString()
  }

  private val eventBuffer = mutableListOf<CombatEvent>()
  private val mutex = Mutex()
  private val _isRecording = MutableStateFlow(false)
  private val _hasEverRecorded = MutableStateFlow(false)
  private val _showHint = MutableStateFlow(true)

  val isRecording = _isRecording.asStateFlow()
  val showHint = _showHint.asStateFlow()

  private var writer: BufferedWriter? = null
  private var currentOutputFile: java.nio.file.Path? = null

  override suspend fun interact() {
    if (!_isRecording.value) return
    flushBuffer()
  }

  fun startRecording() {
    _isRecording.value = true
    _hasEverRecorded.value = true
    _showHint.value = false
    eventBuffer.clear()
    closeWriter()
    Log.info(TAG, "Combat log recording started")
  }

  fun stopRecording() {
    if (!_isRecording.value) return
    _isRecording.value = false
    scope.launch {
      flushBuffer()
      closeWriter()
      val config = RFConfig.state.value
      val sessionTitle = config.lastSessionTitle.ifBlank { "session" }
      val sessionStart = config.lastSessionStart
      val sessionEnd = System.currentTimeMillis()
      val durationMs = if (sessionStart > 0) sessionEnd - sessionStart else 0L
      val now = Date()
      val year = SimpleDateFormat("yyyy", Locale.US).format(now)
      val month = SimpleDateFormat("MM", Locale.US).format(now)
      val documentsDir = getDocumentsDirectory()
      val exportDir = if (documentsDir != null) Paths.get(documentsDir, "RFExports", year, month).toString() else ""
      RFConfig.update { it.copy(
        lastSessionDurationMs = durationMs,
        lastSessionExportDir = exportDir
      )}
      exportSummaryImage()
      Log.info(TAG, "Combat log recording stopped")
    }
  }

  fun recordEvent(event: CombatEvent) {
    if (!_isRecording.value) return
    scope.launch {
      mutex.withLock {
        eventBuffer.add(event)
      }
    }
  }

  private suspend fun flushBuffer() {
    val events = mutex.withLock {
      val copy = eventBuffer.toList()
      eventBuffer.clear()
      copy
    }
    if (events.isEmpty()) return

    val config = RFConfig.state.value
    val sessionTitle = config.lastSessionTitle.ifBlank { "session" }

    val now = Date()
    val year = SimpleDateFormat("yyyy", Locale.US).format(now)
    val month = SimpleDateFormat("MM", Locale.US).format(now)

    val documentsDir = getDocumentsDirectory() ?: return
    val exportDir = Paths.get(documentsDir, "RFExports", year, month)

    withContext(Dispatchers.IO) {
      try {
        exportDir.createDirectories()

        val outputFile = exportDir.resolve(sessionTitle)

        if (currentOutputFile != outputFile) {
          closeWriter()
          currentOutputFile = outputFile
          writer = Files.newBufferedWriter(
            outputFile,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND
          )
        }

        writer?.let { w ->
          for (event in events) {
            val payload = event.toCombatEventPayload() ?: continue
            val json = AppJson.encodeToString(CombatEventPayload.serializer(), payload)
            w.write(json)
            w.newLine()
          }
          w.flush()
        }
      } catch (e: Exception) {
        Log.error(TAG, "Failed to flush combat log: ${e.message}")
      }
    }
  }

  private fun closeWriter() {
    try {
      writer?.flush()
      writer?.close()
    } catch (_: Exception) {}
    writer = null
    currentOutputFile = null
  }

  private suspend fun exportSummaryImage() {
    try {
      val data = ImageExportInteractor.captureSnapshot()
      ImageExportInteractor.exportToPng(data)
    } catch (e: Exception) {
      Log.error(TAG, "Failed to export summary image: ${e.message}")
    }
  }

  private fun CombatEvent.toCombatEventPayload(): CombatEventPayload? {
    return when (this) {
      is DamageEvent -> CombatEventPayload.DamagePayload(
        timestamp = timestamp,
        cid = cid,
        source = source,
        target = target,
        unknownInt = 0,
        spell = spell,
        damageType = "",
        amount = damage,
        pool = "HEALTH",
        result = if (critical) "CRITICAL" else "HIT",
        f11 = 0,
        f12 = 0,
        f13 = critical,
        f14 = "",
        f15 = false,
      )
      is HealEvent -> CombatEventPayload.HealPayload(
        timestamp = timestamp,
        cid = cid,
        source = source,
        target = target,
        unknownInt = 0,
        spell = spell,
        damageType = "",
        amount = amount,
        result = if (critical) "CRITICAL" else "HIT",
        f10 = critical,
        f11 = 0,
      )
      is CastingEvent -> CombatEventPayload.SpellCastStartPayload(
        timestamp = timestamp,
        cid = cid,
        source = source,
        target = target,
        spellId = spellId,
        spellName = spell,
        damageType = "",
      )
      is SuccessfulCastEvent -> CombatEventPayload.SpellCastSuccessPayload(
        timestamp = timestamp,
        cid = cid,
        source = source,
        target = target,
        spellId = spellId,
        spellName = spell,
        damageType = "",
      )
      is BuffGainedEvent -> CombatEventPayload.BuffGainedPayload(
        timestamp = timestamp,
        cid = cid,
        source = source,
        target = target,
        buffId = buffId,
        buffName = buff,
        damageType = "",
        buffType = "BUFF",
        isActive = true,
      )
      is BuffEndedEvent -> CombatEventPayload.BuffEndedPayload(
        timestamp = timestamp,
        cid = cid,
        source = source,
        target = target,
        buffId = buffId,
        buffName = buff,
        damageType = "",
        buffType = "BUFF",
        isActive = false,
      )
      is DebuffGainedEvent -> CombatEventPayload.BuffGainedPayload(
        timestamp = timestamp,
        cid = cid,
        source = source,
        target = target,
        buffId = debuffId,
        buffName = debuff,
        damageType = "",
        buffType = "DEBUFF",
        isActive = true,
      )
      else -> null
    }
  }
}
