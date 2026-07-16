package com.reoky.raidframer.core.interactor

import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.model.BuffEndedEvent
import com.reoky.raidframer.core.model.BuffGainedEvent
import com.reoky.raidframer.core.model.CastingEvent
import com.reoky.raidframer.core.model.DamageEvent
import com.reoky.raidframer.core.model.DebuffGainedEvent
import com.reoky.raidframer.core.model.HealEvent
import com.reoky.raidframer.core.model.SuccessfulCastEvent
import com.reoky.raidframer.core.model.normalize

import com.reoky.raidframer.core.serialization.AppJson
import com.reoky.raidframer.core.serialization.CombatEventPayload
import com.reoky.raidframer.core.serialization.IPCMessagePayload
import com.reoky.raidframer.core.serialization.PlayerInfoPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
              val combatEvent = CastingEvent(
                timestamp = event.timestamp,
                cid = event.cid,
                source = event.source,
                target = event.target,
                spell = event.spellName,
                spellId = event.spellId
              ).normalize<CastingEvent>()
              Log.info(TAG, "At ${event.timestamp} ${combatEvent.source} began casting ${event.spellName} (id:${event.spellId}) on ${combatEvent.target}.")
              PlayerCacheInteractor.postEvent(combatEvent)
            }

            is CombatEventPayload.SpellCastSuccessPayload -> {
              val combatEvent = SuccessfulCastEvent(
                timestamp = event.timestamp,
                cid = event.cid,
                source = event.source,
                target = event.target,
                spell = event.spellName,
                spellId = event.spellId
              ).normalize<SuccessfulCastEvent>()
              PlayerCacheInteractor.postEvent(combatEvent)
            }

            is CombatEventPayload.DamagePayload -> {
              val combatEvent = DamageEvent(
                timestamp = event.timestamp,
                cid = event.cid,
                source = event.source,
                target = event.target,
                damage = abs(event.amount),
                spell = event.spell,
                critical = event.f13,
                spellId = 0
              ).normalize<DamageEvent>()
              Log.info(TAG, "At ${event.timestamp} ${combatEvent.source} (${event.cid}) damaged ${combatEvent.target} for ${abs(event.amount)} using ${event.spell}.")
              PlayerCacheInteractor.postEvent(combatEvent)
            }

            is CombatEventPayload.HealPayload -> {
              val combatEvent = HealEvent(
                timestamp = event.timestamp,
                cid = event.cid,
                source = event.source,
                target = event.target,
                amount = abs(event.amount),
                spell = event.spell,
                critical = event.f10,
                spellId = 0
              ).normalize<HealEvent>()
              Log.info(TAG, "At ${event.timestamp} ${combatEvent.source} healed ${combatEvent.target} for ${event.amount} using ${event.spell}.")
              PlayerCacheInteractor.postEvent(combatEvent)
            }

            is CombatEventPayload.BuffGainedPayload -> {
              val combatEvent = BuffGainedEvent(
                timestamp = event.timestamp,
                cid = event.cid,
                source = event.source,
                target = event.target,
                buff = event.buffName,
                buffId = event.buffId,
              ).normalize<BuffGainedEvent>()
              if (event.buffType == "DEBUFF") {
                PlayerCacheInteractor.postEvent(
                  DebuffGainedEvent(
                    timestamp = event.timestamp,
                    cid = event.cid,
                    source = combatEvent.source,
                    target = combatEvent.target,
                    debuff = event.buffName,
                    debuffId = event.buffId,
                  )
                )
                Log.info(TAG, "At ${event.timestamp} ${combatEvent.source} applied debuff (${combatEvent.buff}:${combatEvent.buffId}) to ${combatEvent.target}.")
              } else {
                PlayerCacheInteractor.postEvent(combatEvent)
                Log.info(TAG, "At ${event.timestamp} ${combatEvent.source} applied buff (${combatEvent.buff}:${combatEvent.buffId}) to ${combatEvent.target}.")
              }
            }
            is CombatEventPayload.BuffEndedPayload -> {
              val combatEvent = BuffEndedEvent(
                timestamp = event.timestamp,
                cid = event.cid,
                source = event.source,
                target = event.target,
                buff = event.buffName,
                buffId = event.buffId
              ).normalize<BuffEndedEvent>()
              PlayerCacheInteractor.postEvent(combatEvent)
            }
            is CombatEventPayload.MeleeDamagePayload -> {
              val combatEvent = DamageEvent(
                timestamp = event.timestamp,
                cid = event.cid,
                source = event.source,
                target = event.target,
                damage = abs(event.amount),
                spell = "Basic Melee",
                critical = event.f10,
                spellId = 0
              ).normalize<DamageEvent>()
              Log.info(TAG, "At ${event.timestamp} ${combatEvent.source} melee damaged ${combatEvent.target} for ${abs(event.amount)}.")
              PlayerCacheInteractor.postEvent(combatEvent)
            }
            is CombatEventPayload.MeleeMissedPayload -> {
              val combatEvent = DamageEvent(
                timestamp = event.timestamp,
                cid = event.cid,
                source = event.source,
                target = event.target,
                damage = abs(event.amount),
                spell = "Melee Missed (Smol Scratch)",
                critical = false,
                spellId = 0
              ).normalize<DamageEvent>()
              Log.info(TAG, "At ${event.timestamp} ${combatEvent.target} avoided ${combatEvent.source}'s melee attack (miss).")
              PlayerCacheInteractor.postEvent(combatEvent)
            }
            is CombatEventPayload.SpellMissedPayload -> {
              val combatEvent = DamageEvent(
                timestamp = event.timestamp,
                cid = event.cid,
                source = event.source,
                target = event.target,
                damage = abs(event.amount),
                spell = "Spell Missed (hehe)",
                critical = false,
                spellId = 0
              ).normalize<DamageEvent>()
              Log.info(TAG, "At ${event.timestamp} ${combatEvent.target} avoided ${combatEvent.source}'s ${event.spell} spell (miss for ${event.amount} dmg) ${event.result}.")
              PlayerCacheInteractor.postEvent(combatEvent)
            }
            is CombatEventPayload.EnergizePayload -> {
              Log.info(TAG, "At ${event.timestamp} ${event.target} energized after a duel healing ${event.amount} health.")
            }
            is CombatEventPayload.EnvironmentalDamagePayload -> {
              Log.info(TAG, "At ${event.timestamp} ${event.target} took ${abs(event.amount)} ${event.damageType} damage.")
            }
            is CombatEventPayload.ConditionDamagePayload -> {
              val combatEvent = DamageEvent(
                timestamp = event.timestamp,
                cid = event.cid,
                source = event.source,
                target = event.target,
                damage = abs(event.amount),
                spell = event.spell,
                critical = event.f13,
                spellId = 0
              ).normalize<DamageEvent>()
              Log.info(TAG, "At ${event.timestamp} ${combatEvent.target} suffered ${abs(event.amount)} damage to their ${event.pool} because of ${combatEvent.source}'s ${event.spell} spell.")
              PlayerCacheInteractor.postEvent(combatEvent)
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
   * Sends a shutdown command to the Lua addon, waits for it to release file locks,
   * then cleanly removes the addon folder.
   */
  suspend fun uninstall() {
    val gameDirectory = RFConfig.state.value.defaultArcheRageDirectory
    if (gameDirectory.isBlank()) {
      Log.error(TAG, "Cannot uninstall Lua addon: Game directory not set.")
      return
    }

    // Tell the Lua addon to shut down and release its file handles
    try {
      sendMessage(IPCMessagePayload.Shutdown())
      Log.info(TAG, "Sent SHUTDOWN message to Lua addon. Waiting for file locks to release...")
    } catch (e: Exception) {
      Log.error(TAG, "Failed to send SHUTDOWN message: ${e.message}")
    }

    // Give the Lua addon time to process the SHUTDOWN message and close file handles
    delay(2000L)

    stop()
    InstallationInteractor.stop()

    val addonDirectory = Paths.get(gameDirectory, ADDON_RELATIVE_PATH)
    if (addonDirectory.exists()) {
      // Try recursive delete first (works if all handles are released)
      try {
        addonDirectory.toFile().deleteRecursively()
        Log.info(TAG, "Successfully uninstalled Lua addon and removed folder.")
        return
      } catch (e: Exception) {
        Log.info(TAG, "Recursive delete had locked files, will retry with individual file removal.")
      }

      // Fallback: delete individual files with retries for locked ones
      val maxRetries = 5
      for (attempt in 1..maxRetries) {
        val remainingFiles = addonDirectory.toFile().walkBottomUp().toList()
        if (remainingFiles.isEmpty()) break

        var allDeleted = true
        for (file in remainingFiles) {
          if (file.exists()) {
            val deleted = file.delete()
            if (!deleted) {
              allDeleted = false
              Log.debug(TAG, "Could not delete ${file.name} on attempt $attempt (may be locked)")
            }
          }
        }

        if (allDeleted) {
          Log.info(TAG, "Successfully uninstalled Lua addon and removed folder.")
          return
        }

        if (attempt < maxRetries) {
          delay(1000L) // wait before retrying
        }
      }

      // Final check
      if (addonDirectory.exists()) {
        val leftoverFiles = addonDirectory.toFile().walkTopDown().filter { it.isFile }.map { it.name }.toList()
        if (leftoverFiles.isNotEmpty()) {
          Log.warn(TAG, "Some files could not be removed (may still be locked by game): ${leftoverFiles.joinToString()}")
        } else {
          addonDirectory.toFile().delete()
          Log.info(TAG, "Successfully uninstalled Lua addon and removed folder.")
        }
      }
    }
  }

}
