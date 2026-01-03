package com.reoky.raidframer.core.interactor

import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.model.ARBuffEvent
import com.reoky.raidframer.core.model.ARRaidFrame
import com.reoky.raidframer.core.model.BuffEndedEvent
import com.reoky.raidframer.core.model.BuffGainedEvent
import com.reoky.raidframer.core.model.CombatEvent
import com.reoky.raidframer.core.model.DebuffEndedEvent
import com.reoky.raidframer.core.model.DebuffGainedEvent
import com.reoky.raidframer.core.model.Faction
import com.reoky.raidframer.core.model.RaidMember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import kotlin.io.path.exists
import kotlin.io.path.readLines
import kotlin.io.path.writeText

object CompanionInteractor : Interactor() {

  private const val TAG = "IPCInteractor"
  private const val ADDON_RELATIVE_PATH = "Addon/RaidFramer"
  private const val IPC_IN_FILENAME = "ipc.rfin"   // App writes to this file (Lua reads)
  private const val IPC_OUT_FILENAME = "ipc.rfout" // App reads from this file (Lua writes)

  private val _scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

  private var _shouldNotifyCompanion: Boolean = false
  private var _didATestPing: Boolean = false

  // JSON configuration
  private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
  }

  @Serializable
  enum class MessageType {
    @SerialName("PLAYER_CAST") PLAYER_CAST,
    @SerialName("PLAYER_DAMAGE") PLAYER_DAMAGE,
    @SerialName("PLAYER_HEAL") PLAYER_HEAL,
    @SerialName("PLAYER_DEBUFF") PLAYER_DEBUFF,
    @SerialName("PLAYER_BUFF") PLAYER_BUFF,
    @SerialName("PLAYER_DEATH") PLAYER_DEATH,
    @SerialName("WORLD_EVENT") WORLD_EVENT,
    @SerialName("FRAMES_UPDATE") FRAMES_UPDATE,
    @SerialName("TARGET_UPDATE") TARGET_UPDATE, // when the user tab-targets to a new character
    @SerialName("SELF_UPDATE") SELF_UPDATE, // sets the character name of the player automatically at char switch
    @SerialName("SELF_FACTION") SELF_FACTION, // notifies the app of the players' faction (for enemy/friendly coloring & may change on character switch!)
    @SerialName("SOUND_ALERT") SOUND_ALERT,
    @SerialName("AOE_SPLAT") AOE_SPLAT,
    @SerialName("TEST_PING") TEST_PING,
    @SerialName("CONFIG_UPDATE") CONFIG_UPDATE
  }

  @Serializable
  data class IPCMessage(
    val version: Int = 1,
    val type: MessageType,
    val timestamp: Long = System.currentTimeMillis() / 1000,
    val payload: JsonElement? = null
  )

  override suspend fun interact() {
    val gameDirectory = RFConfig.state.value.defaultArcheRageDirectory

    // Only read on even seconds, the lua code only writes on odd seconds
    val currentSecond = System.currentTimeMillis() / 1000
    if (currentSecond % 2 != 0L) return

    // If config is not set, just return (poll again later)
    if (gameDirectory.isBlank()) return

    val addonDirectory = Paths.get(gameDirectory, ADDON_RELATIVE_PATH)

    // If addon directory doesn't exist yet (InstallationInteractor hasn't run?), return
    if (!addonDirectory.exists()) return

    val outFile = addonDirectory.resolve(IPC_OUT_FILENAME)

    // Check if there are messages to read
    if (outFile.exists()) {
      try {
        val lines = withContext(Dispatchers.IO) {
          val readLines = outFile.readLines()
          if (readLines.isNotEmpty()) {
            // Clear the file immediately after reading to consume messages
            // This mirrors the Lua logic which reads then truncates
            outFile.writeText("")
          }
          readLines
        }

        // Process each line
        lines.filter { it.isNotBlank() }.forEach { line ->
          processIncomingMessage(line)
        }

      } catch (e: Exception) {
        Log.error(TAG, "Error reading IPC out file: ${e.message}")
      }
    }

    // Send a test ping once to establish communication. (We don't know if app or lua code was started first, this syncs them)
    if (!_didATestPing) {
      sendMessage(MessageType.TEST_PING)
      _didATestPing = true
    }

    // If we need to notify the companion of config changes, do so now
    if (_shouldNotifyCompanion) {
      sendMessage(MessageType.CONFIG_UPDATE)
      _shouldNotifyCompanion = false
    }
  }

  /*
   * The main message handler man.. processes incoming messages from the companion addon switching on the command type.
   */
  private fun processIncomingMessage(rawJson: String) {
    try {
      val message = json.decodeFromString<IPCMessage>(rawJson)
      val payload = message.payload.toString().trim('"')

      when (message.type) {
        MessageType.TEST_PING -> {
          Log.info(TAG, "Received TEST_PING from Addon.")
        }
        MessageType.FRAMES_UPDATE -> {
          message.payload?.let { jsonElement ->
            handleFrameEvent(jsonElement, message.timestamp) { frames ->
              PlayerCacheInteractor.updatePlayersForRaidById(0, frames.take(50).map { RaidMember(
                name = it.playerName,
                health = 57000, // placeholder, we don't have health info here
                role = 0
              )})
              PlayerCacheInteractor.updatePlayersForRaidById(1, frames.slice(51 .. 100).map { RaidMember(
                name = it.playerName,
                health = 57000, // placeholder, we don't have health info here
                role = 0
              )})
            }
          }
        }
        MessageType.CONFIG_UPDATE -> {
          Log.info(TAG, "Received CONFIG_UPDATE request. This is not meant to go in this direction aaaa.")
        }
        MessageType.SELF_UPDATE -> {
          if (payload.isNotBlank()) RFConfig.update {
            Log.info(TAG, "Player character name updated to: $payload")
            it.copy(playerName = payload)
          }
        }
        MessageType.PLAYER_BUFF,
        MessageType.PLAYER_DEBUFF -> {
          message.payload?.let { jsonElement ->
            handleBuffEvent(jsonElement, message.timestamp) { event ->
              PlayerCacheInteractor.postEvent(event)
            }
          }
        }
        MessageType.SELF_FACTION -> {
          Faction.fromString(payload).let { faction ->
            RFConfig.update { it.copy(playerFaction = faction.value) }
            Log.info(TAG, "Player character faction updated to: $faction")
          }
        }
        else -> {
          Log.debug(TAG, "Received message: ${message.type}")
        }
      }
    } catch (e: Exception) {
      Log.error(TAG, "Failed to decode IPC message: $rawJson")
    }
  }

  /**
   * Sends a message to the Addon by appending it to rf_ipc.in
   */
  suspend fun sendMessage(type: MessageType, payload: JsonElement? = null) {
    val gameDirectory = RFConfig.state.value.defaultArcheRageDirectory
    if (gameDirectory.isBlank()) {
      Log.error(TAG, "Cannot send IPC message: Game directory not set.")
      return
    }

    val addonDirectory = Paths.get(gameDirectory, ADDON_RELATIVE_PATH)
    if (!addonDirectory.exists()) {
      Log.error(TAG, "Cannot send IPC message: Addon directory not found.")
      return
    }

    val inFile = addonDirectory.resolve(IPC_IN_FILENAME)
    val message = IPCMessage(type = type, payload = payload)

    try {
      val jsonString = json.encodeToString(message)
      withContext(Dispatchers.IO) {
        // Append the JSON line to the file
        Files.write(
          inFile,
          (jsonString + "\n").toByteArray(),
          StandardOpenOption.CREATE,
          StandardOpenOption.APPEND
        )
      }
      Log.debug(TAG, "Sent IPC Message: $jsonString")
    } catch (e: Exception) {
      Log.error(TAG, "Failed to write IPC message to $inFile: ${e.message}")
    }
  }

  /*
   * Builds a data structure to hold raid frames from the JSON payload and then calls the lamba to dispatch the event.
   */
  private fun handleFrameEvent(jsonElement: JsonElement, messageTimestamp: Long, dispatch: (List<ARRaidFrame>) -> Unit) {
    val json = Json { ignoreUnknownKeys = true }

    try {
      // If payload is a JSON string containing the real JSON, parse it first
      val actualElement = if (jsonElement is JsonPrimitive && jsonElement.isString) {
        json.parseToJsonElement(jsonElement.content)
      } else {
        jsonElement
      }

      // Normalize to a list of JsonElement (JsonArray implements List<JsonElement>)
      val items: List<JsonElement> = if (actualElement is kotlinx.serialization.json.JsonArray) {
        actualElement
      } else {
        listOf(actualElement)
      }

      val frames = mutableListOf<ARRaidFrame>()

      fun parseInt(e: JsonElement?, default: Int): Int {
        if (e == null) return default
        val s = (e as? JsonPrimitive)?.content ?: return default
        return s.toIntOrNull() ?: default
      }

      fun parseLong(e: JsonElement?, default: Long): Long {
        if (e == null) return default
        val s = (e as? JsonPrimitive)?.content ?: return default
        return s.toLongOrNull() ?: default
      }

      fun parseString(e: JsonElement?, default: String = ""): String {
        if (e == null) return default
        val s = (e as? JsonPrimitive)?.content ?: return default
        return s.trim().takeIf { it.isNotBlank() } ?: default
      }

      for (el in items) {
        try {
          val obj = when {
            el is kotlinx.serialization.json.JsonObject -> el
            el is JsonPrimitive && el.isString -> {
              val parsed = json.parseToJsonElement(el.content)
              if (parsed is kotlinx.serialization.json.JsonObject) parsed else continue
            }
            else -> continue
          }

          val slot = parseInt(obj["slot"], 0).coerceAtLeast(0)
          val playerName = parseString(obj["playerName"], "")
          val gearScore = parseInt(obj["gearScore"], 0).coerceAtLeast(0)
          val characterBuild = parseString(obj["characterBuild"], "")
          val lastZone = parseString(obj["lastZone"], "")
          val distance = parseInt(obj["distance"], -1)
          val lastUpdated = parseLong(obj["lastUpdated"], 0L).let { if (it != 0L) it else messageTimestamp }

          // Require at least a playerName or slot to consider this a valid frame
          if (playerName.isBlank() && slot == 0 && gearScore == 0) {
            // skip obviously invalid / empty entries
            continue
          }

          frames += ARRaidFrame(
            slot = slot,
            playerName = playerName,
            gearScore = gearScore,
            characterBuild = characterBuild,
            lastZone = lastZone,
            distance = distance,
            lastUpdated = lastUpdated
          )
        } catch (inner: Throwable) {
          Log.error(TAG, "Skipping malformed frame element: ${inner.message}")
          continue
        }
      }

      dispatch(frames)
    } catch (t: Throwable) {
      Log.error(TAG, "Failed to decode ARRaidFrames payload: ${t.message}")
    }
  }


  private fun handleBuffEvent(jsonElement: JsonElement, messageTimestamp: Long, dispatch: (CombatEvent) -> Unit) {
    val json = Json { ignoreUnknownKeys = true }
    try {
      // If the payload is a JSON string containing the actual object, parse that string first.
      val actualElement = if (jsonElement is JsonPrimitive && jsonElement.isString) {
        json.parseToJsonElement(jsonElement.content)
      } else {
        jsonElement
      }

      val event = json.decodeFromJsonElement(ARBuffEvent.serializer(), actualElement)

      val eventType = event.eventType?.trim()?.uppercase()
      val auraType = event.auraType?.trim()?.uppercase()
      val name = event.buffName?.takeIf { it.isNotBlank() }
      val target = event.target?.takeIf { it.isNotBlank() }
      val source = event.source?.takeIf { it.isNotBlank() }
      val ts = if (event.timestamp != 0L) event.timestamp * 1000L else messageTimestamp

      if (eventType == null) {
        println("ignore: missing eventType")
        return
      }
      if (name == null || target == null) {
        println("ignore: missing buffName or target")
        return
      }

      when (eventType) {
        "SPELL_AURA_APPLIED" -> {
          when (auraType) {
            "BUFF" -> dispatch(BuffGainedEvent(timestamp = ts, source = source, target = target, buff = name))
            "DEBUFF" -> dispatch(DebuffGainedEvent(timestamp = ts, source = source, target = target, debuff = name))
            else -> Log.error(TAG, "ignore: unknown auraType=$auraType")
          }
        }
        "SPELL_AURA_REMOVED" -> {
          when (auraType) {
            "BUFF" -> dispatch(BuffEndedEvent(timestamp = ts, source = source, target = target, buff = name))
            "DEBUFF" -> dispatch(DebuffEndedEvent(timestamp = ts, source = source, target = target, debuff = name))
            else -> Log.error(TAG, "ignore: unknown auraType=$auraType")
          }
        }
        else -> Log.error(TAG, "ignore: unhandled eventType=$eventType")
      }
    } catch (t: Throwable) {
      println("failed to decode ARBuffEvent: ${t.message}")
    }
  }
  /*
   * Sends a test ping message to the companion addon.
   */
  fun sendTestPing() {
    _scope.launch(Dispatchers.IO) {
      sendMessage(MessageType.TEST_PING)
    }
  }

  /*
   * Simply tells the companion addon that the config has updated,
   * so it can re-read any relevant settings.
   */
  fun notifyConfigUpdated() {
    _shouldNotifyCompanion = true
  }


}
