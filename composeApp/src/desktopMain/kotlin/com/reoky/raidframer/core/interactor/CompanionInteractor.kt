package com.reoky.raidframer.core.interactor

import com.reoky.raidframer.AppState
import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.model.BuffEndedEvent
import com.reoky.raidframer.core.model.BuffGainedEvent
import com.reoky.raidframer.core.model.CastingEvent
import com.reoky.raidframer.core.model.DamageEvent
import com.reoky.raidframer.core.model.DebuffGainedEvent
import com.reoky.raidframer.core.model.HealEvent
import com.reoky.raidframer.core.model.SuccessfulCastEvent

import com.reoky.raidframer.core.serialization.AppJson
import com.reoky.raidframer.core.serialization.CombatEventPayload
import com.reoky.raidframer.core.serialization.IPCMessagePayload
import com.reoky.raidframer.core.serialization.PlayerInfoPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import kotlin.io.path.exists
import kotlin.io.path.readLines
import kotlin.io.path.writeText
import kotlin.math.abs

object CompanionInteractor : Interactor() {

  private const val TAG = "IPCInteractor"
  private const val ADDON_RELATIVE_PATH = "Addon/RaidFramer"
  private const val IPC_IN_FILENAME = "ipc.rfin"   // App writes to this file (Lua reads)
  private const val IPC_OUT_FILENAME = "ipc.rfout" // App reads from this file (Lua writes)

  private var shouldNotifyCompanion: Boolean = false
  private var didATestPing: Boolean = false


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
          handleInboundIPCMessage(line)
        }

      } catch (e: Exception) {
        Log.error(TAG, "Error reading IPC out file: ${e.message}")
      }
    }

    // Send a test ping once to establish communication. (We don't know if app or lua code was started first, this syncs them)
    if (!didATestPing) {
      sendMessage(IPCMessagePayload.TestPing())
      didATestPing = true
    }

    // If we need to notify the companion of config changes, do so now
    if (shouldNotifyCompanion) {
      sendMessage(IPCMessagePayload.ConfigUpdate())
      shouldNotifyCompanion = false
    }
  }

  /*
   * Handles an inbound IPC message from the companion addon. The IPCMessagePayload sealed class
   * automatically deserializes the JSON into the correct subclass based on the "type" field. This is why
   * we can use a 'when' statement here to switch on the actual message type. (It took a while to get this right!)
   */
  private fun handleInboundIPCMessage(rawJson: String) {
    try {
      when (val message = AppJson.decodeFromString<IPCMessagePayload>(rawJson)) {
        is IPCMessagePayload.SelfUpdate -> {
          val playerName = message.payload
          if (playerName.isBlank() || playerName.contains(" ")) return
          RFConfig.update { it.copy(playerName = playerName)}
        }
        is IPCMessagePayload.SelfFaction -> {
          val factionName = message.payload
          if (factionName.isBlank() || factionName.contains(" ")) return
          RFConfig.update { it.copy(playerFaction = factionName)}
        }
        is IPCMessagePayload.TargetUpdate -> {
          val targetName = message.payload
          if (targetName.isBlank() || targetName.contains(" ")) return
          AppState.selectTarget(targetName)
          Log.info(TAG, "User tabbed over $targetName..")
        }
        is IPCMessagePayload.PlayerInfo -> {
          when (val payload = message.payload) {
            is PlayerInfoPayload.Character -> {
              val playerName = payload.name
              println("Metadata for player ${payload.name} received.")
              if (playerName.isBlank() || playerName.contains(" ")) return
              PlayerCacheInteractor.stronglyAssertIsPlayer(payload.name, payload.classMap)
            }
            is PlayerInfoPayload.Npc -> {
              println("Metadata for NPC ${payload.name} received.")
            }
            // Fixed: Added 'Mate' branch to make 'when' exhaustive
            is PlayerInfoPayload.Mate -> {
              println("Metadata for companion pet ${payload.name} owned by ${payload.ownerName} received.")
            }
            is PlayerInfoPayload.Slave -> {
              println("Metadata for vehicle summon ${payload.name} owned by ${payload.ownerName} received.")
            }
          }
        }
        is IPCMessagePayload.FramesUpdate -> { // Was "BatchUpdate"
          Log.info(TAG, "Player info updated with count: ${message.payload.size}")
          message.payload.chunked(50).take(2).forEachIndexed { index, chunk ->
            PlayerCacheInteractor.updatePlayersForRaidById(index, chunk)
          }
        }
        is IPCMessagePayload.CombatEvent -> {
          when (val event = message.payload) {

            is CombatEventPayload.SpellCastStartPayload -> {
              //Log.info(TAG, "At ${event.timestamp} ${event.source} began casting ${event.spellName} (id:${event.spellId}) on ${event.target}.")
              PlayerCacheInteractor.postEvent(
                CastingEvent(
                  timestamp = event.timestamp,
                  caster = event.source,
                  spell = event.spellName
                )
              )
            }

            is CombatEventPayload.SpellCastSuccessPayload -> {
              //Log.info(TAG, "At ${event.timestamp} ${event.source} successfully cast ${event.spellName} (id:${event.spellId}) on ${event.target}.")
              PlayerCacheInteractor.postEvent(
                SuccessfulCastEvent(
                  timestamp = event.timestamp,
                  caster = event.source,
                  spell = event.spellName,
                )
              )
            }

            is CombatEventPayload.DamagePayload -> {
              //Log.info(TAG, "At ${event.timestamp} ${event.source} damaged ${event.target} for ${abs(event.amount)} using ${event.spell}.")
              PlayerCacheInteractor.postEvent(
                DamageEvent(
                  timestamp = event.timestamp,
                  caster = event.source,
                  target = event.target,
                  damage = abs(event.amount),
                  spell = event.spell,
                  critical = event.f13
                )
              )
            }

            is CombatEventPayload.HealPayload -> {
              //Log.info(TAG, "At ${event.timestamp} ${event.source} healed ${event.target} for ${event.amount} using ${event.spell}.")
              PlayerCacheInteractor.postEvent(
                HealEvent(
                  timestamp = event.timestamp,
                  caster = event.source,
                  target = event.target,
                  amount = abs(event.amount),
                  spell = event.spell,
                  critical = event.f10
                )
              )
            }

            is CombatEventPayload.BuffGainedPayload -> {
              //Log.info(TAG, "At ${event.timestamp} ${event.source} applied ${event.buffName} (id:${event.buffId}) (type:${event.buffType}) to ${event.target}")
              if (event.buffType == "DEBUFF") {
                PlayerCacheInteractor.postEvent(
                  DebuffGainedEvent(
                    timestamp = event.timestamp,
                    source = event.source,
                    target = event.target,
                    debuff = event.buffName
                  )
                )
              } else {
                PlayerCacheInteractor.postEvent(
                  BuffGainedEvent(
                    timestamp = event.timestamp,
                    source = event.source,
                    target = event.target,
                    buff = event.buffName
                  )
                )
              }
            }

            is CombatEventPayload.BuffEndedPayload -> {
              //Log.info(TAG, "At ${event.timestamp} ${event.target}'s ${event.buffName} (id:${event.buffId}) (type:${event.buffType}) caused by ${event.source} ended.")
              PlayerCacheInteractor.postEvent(
                BuffEndedEvent(
                  timestamp = event.timestamp,
                  source = event.source,
                  target = event.target,
                  buff = event.buffName
                )
              )
            }
            is CombatEventPayload.EnergizePayload -> {
              Log.info(TAG, "At ${event.timestamp} ${event.target} energized after a duel healing ${event.amount} health.")
            }
            is CombatEventPayload.EnvironmentalDamagePayload -> {
              Log.info(TAG, "At ${event.timestamp} ${event.target} took ${abs(event.amount)} ${event.damageType} damage.")
            }
            is CombatEventPayload.ConditionDamagePayload -> {
              //Log.info(TAG, "At ${event.timestamp} ${event.target} suffered ${abs(event.amount)} damage to their ${event.pool} because of ${event.source}'s ${event.spell} spell.")
              PlayerCacheInteractor.postEvent(
                DamageEvent(
                  timestamp = event.timestamp,
                  caster = event.source,
                  target = event.target,
                  damage = abs(event.amount),
                  spell = event.spell,
                  critical = event.f13
                )
              )
            }
          }
        }
        is IPCMessagePayload.PlayerDeath -> {
          val playerName = message.payload
          if (playerName.isBlank() || playerName.contains(" ")) return
          Log.info(TAG, "Player death event received for $playerName")
          PlayerCacheInteractor.postPlayerDeath(playerName, message.timestamp)
        }
        else -> {}
      }
    } catch (e: Exception) {
      Log.error(TAG, "Could not decode JSON IPC message: ${e.message}")
    }
  }

  /**
   * Sends a message to the Addon by appending it to rf_ipc.in
   */
  suspend fun sendMessage(message: IPCMessagePayload) {
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

    try {
      // AppJson automatically handles the "type" field based on @SerialName
      val jsonString = AppJson.encodeToString(message)

      withContext(Dispatchers.IO) {
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
   * Simply tells the companion addon that the config has updated,
   * so it can re-read any relevant settings.
   */
  fun notifyConfigUpdated() {
    shouldNotifyCompanion = true
  }

}
