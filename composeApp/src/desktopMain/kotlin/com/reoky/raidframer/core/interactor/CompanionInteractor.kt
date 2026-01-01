package com.reoky.raidframer.core.interactor

import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.model.Faction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
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
          Log.debug(TAG, "Received FRAMES_UPDATE.")
          // TODO: Dispatch update to UI or State
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
