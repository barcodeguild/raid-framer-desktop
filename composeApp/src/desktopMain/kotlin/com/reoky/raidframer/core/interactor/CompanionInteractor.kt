package com.reoky.raidframer.core.interactor

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import kotlin.io.path.exists
import kotlin.io.path.readLines
import kotlin.io.path.writeText
import kotlin.math.abs

object CompanionInteractor : Interactor() {

  private const val TAG = "CompanionInteractor"
  private const val ADDON_RELATIVE_PATH = "Addon/RaidFramer"
  private const val IPC_IN_FILENAME = "ipc.rfin"
  private const val IPC_OUT_FILENAME = "ipc.rfout"

  private var shouldNotifyCompanion: Boolean = false
  private var didATestPing: Boolean = false
  private var lastProcessedLineCount: Int = 0  // Track lines processed
  private var ipcFilesInitialized: Boolean = false

  private fun initializeIpcFilesIfNeeded() {
    if (ipcFilesInitialized) return
    try {
      val gameDirectory = RFConfig.state.value.defaultArcheRageDirectory
      Log.info(TAG, "(init) Game directory from config: '$gameDirectory'")
      if (gameDirectory.isNotBlank()) {
        val addonDirectory = Paths.get(gameDirectory, ADDON_RELATIVE_PATH)
        if (addonDirectory.exists()) {
          val inFile = addonDirectory.resolve(IPC_IN_FILENAME)
          val outFile = addonDirectory.resolve(IPC_OUT_FILENAME)
          try {
            if (inFile.exists()) {
              inFile.writeText("") // truncate/blank the file; don't delete
            }
            if (outFile.exists()) {
              outFile.writeText("")
            }
            Log.info(TAG, "Cleared IPC files on CompanionInteractor initialization.")
          } catch (e: Exception) {
            Log.error(TAG, "Failed to clear IPC files: ${e.message}")
          }
        }
      }
    } catch (e: Exception) {
      Log.error(TAG, "Error during CompanionInteractor initialization clearing IPC files: ${e.message}")
    } finally {
      ipcFilesInitialized = true
    }
  }

  override suspend fun interact() {
    val gameDirectory = RFConfig.state.value.defaultArcheRageDirectory
    if (gameDirectory.isBlank()) return

    initializeIpcFilesIfNeeded()

    val addonDirectory = Paths.get(gameDirectory, ADDON_RELATIVE_PATH)
    if (!addonDirectory.exists()) return

    val outFile = addonDirectory.resolve(IPC_OUT_FILENAME)

    if (outFile.exists()) {
      try {
        val allLines = withContext(Dispatchers.IO) {
          outFile.readLines()
        }

        val totalLines = allLines.size

        // If file was truncated (Lua's 100MB reset), reset our counter
        if (totalLines < lastProcessedLineCount) {
          lastProcessedLineCount = 0
        }

        // Only process new lines
        if (totalLines > lastProcessedLineCount) {
          allLines
            .drop(lastProcessedLineCount)
            .filter { it.isNotBlank() }
            .forEach { line -> handleInboundIPCMessage(line) }

          lastProcessedLineCount = totalLines
        }} catch (e: Exception) {
        Log.error(TAG, "Error reading IPC out file: ${e.message}")
      }
    }

    // Send a test ping once to establish communication. (We don't know if app or Lua code was started first, this syncs them)
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
  private suspend fun handleInboundIPCMessage(rawJson: String) {
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
          val target = message.payload
          PlayerCacheInteractor.switchActiveTarget(target)
        }
        is IPCMessagePayload.PlayerInfo -> {
          when (val payload = message.payload) {
            is PlayerInfoPayload.Character -> {
              val playerName = payload.name
              if (playerName.isBlank() || playerName.contains(" ")) return
              PlayerCacheInteractor.stronglyAssertIsPlayer(payload.cid, payload.name, payload.classMap)
            }
            is PlayerInfoPayload.Npc -> {
              //println("Metadata for NPC ${payload.name} ${payload.isPortal} received.")
            }
            // Fixed: Added 'Mate' branch to make 'when' exhaustive
            is PlayerInfoPayload.Mate -> {
              println("Metadata for companion pet ${payload.name} owned by ${payload.ownerName} with cid ${payload.cid} type ${payload.mateNpcName} received.")
              val petName = payload.name
              if (petName.isBlank()) return
              PlayerCacheInteractor.createOrUpdatePetCard(
                cid = payload.cid,
                petName = petName,
                owner = payload.ownerName,
                petType = payload.mateNpcName
              )
            }
            is PlayerInfoPayload.Slave -> {
              //println("Metadata for vehicle summon ${payload.name} owned by ${payload.ownerName} received.")
              // we could do something with this in the future.. god do we love farm carts..
            }
          }
        }
        is IPCMessagePayload.FramesUpdate -> { // Was "BatchUpdate"
          val chunks = message.payload.chunked(50).take(2)
          for ((index, chunk) in chunks.withIndex()) {
            PlayerCacheInteractor.updatePlayersForRaidById(index, chunk)
          }
        }
        is IPCMessagePayload.CombatEvent -> {
          when (val event = message.payload) {
            is CombatEventPayload.SpellCastStartPayload -> {
              Log.info(TAG, "At ${event.timestamp} ${event.source} began casting ${event.spellName} (id:${event.spellId}) on ${event.target}.")
              PlayerCacheInteractor.postEvent(
                CastingEvent(
                  timestamp = event.timestamp,
                  cid = event.cid,
                  source = event.source.ifBlank { "Unknown Target" },
                  target = event.source.ifBlank { "Unknown Target" },
                  spell = event.spellName,
                  spellId = event.spellId
                )
              )
            }

            is CombatEventPayload.SpellCastSuccessPayload -> {
              Log.info(TAG, "At ${event.timestamp} ${event.source} successfully cast ${event.spellName} (id:${event.spellId}) on ${event.target}.")
              PlayerCacheInteractor.postEvent(
                SuccessfulCastEvent(
                  timestamp = event.timestamp,
                  cid = event.cid,
                  source = event.source.ifBlank { "Unknown Target" },
                  target = event.source.ifBlank { "Unknown Target" },
                  spell = event.spellName,
                  spellId = event.spellId
                )
              )
            }

            is CombatEventPayload.DamagePayload -> {
              Log.info(TAG, "At ${event.timestamp} ${event.source} (${event.cid}) damaged ${event.target} for ${abs(event.amount)} using ${event.spell}.")
              PlayerCacheInteractor.postEvent(
                DamageEvent(
                  timestamp = event.timestamp,
                  cid = event.cid,
                  source = event.source.ifBlank { "Unknown Target" },
                  target = event.source.ifBlank { "Unknown Target" },
                  damage = abs(event.amount),
                  spell = event.spell,
                  critical = event.f13,
                  spellId = 0
                )
              )
            }

            is CombatEventPayload.HealPayload -> {
              Log.info(TAG, "At ${event.timestamp} ${event.source} healed ${event.target} for ${event.amount} using ${event.spell}.")
              PlayerCacheInteractor.postEvent(
                HealEvent(
                  timestamp = event.timestamp,
                  cid = event.cid,
                  source = event.source.ifBlank { "Unknown Target" },
                  target = event.source.ifBlank { "Unknown Target" },
                  amount = abs(event.amount),
                  spell = event.spell,
                  critical = event.f10,
                  spellId = 0
                )
              )
            }

            is CombatEventPayload.BuffGainedPayload -> {
              Log.info(TAG, "At ${event.timestamp} ${event.source} applied ${event.buffName} (id:${event.buffId}) (type:${event.buffType}) to ${event.target}")
              if (event.buffType == "DEBUFF") {
                PlayerCacheInteractor.postEvent(
                  DebuffGainedEvent(
                    timestamp = event.timestamp,
                    cid = event.cid,
                    source = event.source.ifBlank { "Unknown Target" },
                    target = event.source.ifBlank { "Unknown Target" },
                    debuff = event.buffName,
                    debuffId = event.buffId,
                  )
                )
              } else {
                PlayerCacheInteractor.postEvent(
                  BuffGainedEvent(
                    timestamp = event.timestamp,
                    cid = event.cid,
                    source = event.source.ifBlank { "Unknown Target" },
                    target = event.source.ifBlank { "Unknown Target" },
                    buff = event.buffName,
                    buffId = event.buffId,
                  )
                )
              }
            }
            is CombatEventPayload.BuffEndedPayload -> {
              Log.info(TAG, "At ${event.timestamp} ${event.target}'s ${event.buffName} (id:${event.buffId}) (type:${event.buffType}) caused by ${event.source} ended.")
              PlayerCacheInteractor.postEvent(
                BuffEndedEvent(
                  timestamp = event.timestamp,
                  cid = event.cid,
                  source = event.source.ifBlank { "Unknown Target" },
                  target = event.source.ifBlank { "Unknown Target" },
                  buff = event.buffName,
                  buffId = event.buffId
                )
              )
            }
            is CombatEventPayload.MeleeDamagePayload -> {
              Log.info(TAG, "At ${event.timestamp} ${event.source} melee damaged ${event.target} for ${abs(event.amount)}.")
              PlayerCacheInteractor.postEvent(
                DamageEvent(
                  timestamp = event.timestamp,
                  cid = event.cid,
                  source = event.source.ifBlank { "Unknown Target" },
                  target = event.source.ifBlank { "Unknown Target" },
                  damage = abs(event.amount),
                  spell = "Basic Melee",
                  critical = event.f10,
                  spellId = 0
                )
              )
            }
            is CombatEventPayload.MeleeMissedPayload -> {
              Log.info(TAG, "At ${event.timestamp} ${event.target} avoided ${event.source}'s melee attack (miss).")
              PlayerCacheInteractor.postEvent(
                DamageEvent(
                  timestamp = event.timestamp,
                  cid = event.cid,
                  source = event.source.ifBlank { "Unknown Target" },
                  target = event.source.ifBlank { "Unknown Target" },
                  damage = abs(event.amount),
                  spell = "Melee Missed (Smol Scratch)",
                  critical = false,
                  spellId = 0
                )
              )
            }
            is CombatEventPayload.SpellMissedPayload -> {
              Log.info(TAG, "At ${event.timestamp} ${event.target} avoided ${event.source}'s ${event.spell} spell (miss for ${event.amount} dmg) ${event.result}.")
              PlayerCacheInteractor.postEvent(
                DamageEvent(
                  timestamp = event.timestamp,
                  cid = event.cid,
                  source = event.source.ifBlank { "Unknown Target" },
                  target = event.source.ifBlank { "Unknown Target" },
                  damage = abs(event.amount),
                  spell = "Spell Missed (hehe)",
                  critical = false,
                  spellId = 0
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
              Log.info(TAG, "At ${event.timestamp} ${event.target} suffered ${abs(event.amount)} damage to their ${event.pool} because of ${event.source}'s ${event.spell} spell.")
              PlayerCacheInteractor.postEvent(
                DamageEvent(
                  timestamp = event.timestamp,
                  cid = event.cid,
                  source = event.source.ifBlank { "Unknown Target" },
                  target = event.source.ifBlank { "Unknown Target" },
                  damage = abs(event.amount),
                  spell = event.spell,
                  critical = event.f13,
                  spellId = 0
                )
              )
            }
          }
        }
        is IPCMessagePayload.PlayerDeath -> {
          val playerName = message.payload
          if (playerName.isBlank() || playerName.contains(" ")) return
          Log.info(TAG, "Player death event received for $playerName at ${message.timestamp}.")
          DeathAccumulatorInteractor.queueDeath(playerName, message.timestamp)
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


  /**
   * Stops the interact loop and cleanly removes the Lua addon and folder.
   */
  fun uninstall() {
    stop()
    InstallationInteractor.stop()

    val gameDirectory = RFConfig.state.value.defaultArcheRageDirectory
    if (gameDirectory.isBlank()) {
      Log.error(TAG, "Cannot uninstall Lua addon: Game directory not set.")
      return
    }

    val addonDirectory = Paths.get(gameDirectory, ADDON_RELATIVE_PATH)
    if (addonDirectory.exists()) {
      try {
        addonDirectory.toFile().deleteRecursively()
        Log.info(TAG, "Successfully uninstalled Lua addon and removed folder.")
      } catch (e: Exception) {
        Log.error(TAG, "Failed to uninstall Lua addon: ${e.message}")
      }
    }
  }

}
