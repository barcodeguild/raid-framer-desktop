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
import com.reoky.raidframer.core.serialization.RaidFramePayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import kotlin.io.path.exists
import kotlin.io.path.writeText
import kotlin.math.abs

object CompanionInteractor : Interactor() {

  private const val TAG = "CompanionInteractor"
  private const val ADDON_RELATIVE_PATH = "Addon/RaidFramer"
  private const val IPC_IN_FILENAME = "ipc.rfin"
  private const val IPC_OUT_FILENAME = "ipc.rfout"

  private var shouldNotifyCompanion: Boolean = false
  private var didATestPing: Boolean = false
  private var lastProcessedByteOffset: Long = 0L  // Byte offset into the out file already consumed
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
              inFile.writeText("") // discard commands queued while the game was closed
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

  /**
   * Reads only the lines appended to [path] since the last call and advances the read offset.
   * Handles the Lua addon truncating the file (its 100MB reset) by resetting the offset.
   * A trailing line without a trailing newline (read mid-write) is dropped rather than fed to
   * the decoder, since it is incomplete; the writer completes it as a fresh line on its next
   * write, so nothing is permanently lost.
   */
  private fun readAppendedLines(path: java.nio.file.Path): List<String> {
    val channel = FileChannel.open(path, StandardOpenOption.READ)
    try {
      val size = channel.size()
      if (size < lastProcessedByteOffset) {
        lastProcessedByteOffset = 0L // file was truncated/reset
      }
      if (size <= lastProcessedByteOffset) return emptyList()

      val remaining = (size - lastProcessedByteOffset).toInt()
      val buffer = ByteBuffer.allocate(remaining)
      channel.position(lastProcessedByteOffset)
      while (buffer.hasRemaining()) {
        val n = channel.read(buffer)
        if (n <= 0) break
      }
      buffer.flip()

      val text = StandardCharsets.UTF_8.decode(buffer).toString()
      lastProcessedByteOffset = size

      var lines = text.split('\n')
      if (text.isNotEmpty() && !text.endsWith('\n') && lines.isNotEmpty()) {
        lines = lines.dropLast(1) // incomplete trailing line (mid-write)
      }
      return lines.filter { it.isNotBlank() }
    } finally {
      channel.close()
    }
  }

  override suspend fun interact() {
    val gameDirectory = RFConfig.state.value.defaultArcheRageDirectory
    if (gameDirectory.isBlank()) return

    initializeIpcFilesIfNeeded()

    // Keep the Lua companion awake only while this app is actively running.
    sendMessage(IPCMessagePayload.Keepalive())

    val addonDirectory = Paths.get(gameDirectory, ADDON_RELATIVE_PATH)
    if (!addonDirectory.exists()) return

    val outFile = addonDirectory.resolve(IPC_OUT_FILENAME)

    if (outFile.exists()) {
      try {
        // Incremental tail-read: only read the bytes appended since the last pass, so we never
        // re-read (and re-allocate) the whole backlog of the out file on every tick.
        val newLines = withContext(Dispatchers.IO) {
          readAppendedLines(outFile)
        }
        newLines.forEach { line -> handleInboundIPCMessage(line) }
      } catch (e: Exception) {
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
      // Performance: when companion is disabled, silently drop all inbound messages.
      // IPC file is still read (lines consumed) in interact() to prevent unbounded growth.
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
              Log.info(TAG, "Metadata for NPC ${payload.name} (portal? ${payload.isPortal} : ${payload.portalOwner}) nickName ${payload.nickName} with cid ${payload.cid} expedition ${payload.expeditionName} family ${payload.familyName} faction ${payload.faction} received.")
            }
            // Fixed: Added 'Mate' branch to make 'when' exhaustive
            is PlayerInfoPayload.Mate -> {
              Log.info(TAG, "Metadata for companion pet ${payload.name} owned by ${payload.ownerName} with cid ${payload.cid} type ${payload.mateNpcName} received.")
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
              Log.info(TAG, "Metadata for slave ${payload.name} owned by ${payload.ownerName} with cid ${payload.cid} expedition ${payload.expeditionName} family ${payload.familyName} faction ${payload.faction} received.")

              //println("Metadata for vehicle summon ${payload.name} owned by ${payload.ownerName} received.")
              // we could do something with this in the future.. god do we love farm carts..
            }
          }
        }
        is IPCMessagePayload.FramesUpdate -> { // Was "BatchUpdate"
          // The Lua addon now sends only occupied slots, each carrying its 1-based `slot`.
          // Rebuild the full 100-slot positional array so the existing raid chunking (slot
          // 1-50 -> raid 0, slot 51-100 -> raid 1, parties of 5) keeps working unchanged.
          // Missing slots become empty frames. `accumulateCoherence` filters blank names.
          val framesBySlot = message.payload.associateBy { it.slot }
          val full = (1..100).map { framesBySlot[it] ?: RaidFramePayload(slot = it) }
          PlayerCacheInteractor.accumulateCoherence(full)
          val chunks = full.chunked(50).take(2)
          for ((index, chunk) in chunks.withIndex()) {
            PlayerCacheInteractor.updatePlayersForRaidById(index, chunk)
          }
          val playersWithBuffs = full.count { it.playerName.isNotBlank() && it.buffs.isNotEmpty() }
          val totalPlayers = full.count { it.playerName.isNotBlank() }
          Log.info(TAG, "FRAMES_UPDATE: $totalPlayers players, $playersWithBuffs with buffs")
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
              Log.info(TAG, "At ${combatEvent.timestamp} ${combatEvent.source} began casting ${combatEvent.spell} (id:${combatEvent.spellId}) on ${combatEvent.target} (${combatEvent.cid}).")
              ProtectiveWingsAttributorInteractor.onCast(combatEvent)
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
              Log.info(TAG, "At ${combatEvent.timestamp} ${combatEvent.source} damaged ${combatEvent.target} (${combatEvent.cid}) for ${combatEvent.damage} using ${combatEvent.spell}.")
              AreaEffectAttributorInteractor.onDamageEvent(combatEvent)
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
              Log.info(TAG, "At ${combatEvent.timestamp} ${combatEvent.source} healed ${combatEvent.target} (${combatEvent.cid}) for ${combatEvent.amount} using ${combatEvent.spell}.")
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
                val debuffEvent = DebuffGainedEvent(
                  timestamp = event.timestamp,
                  cid = event.cid,
                  source = combatEvent.source,
                  target = combatEvent.target,
                  debuff = event.buffName,
                  debuffId = event.buffId,
                )
                if (AreaEffectAttributorInteractor.isAreaEffectAura(debuffEvent)) {
                  AreaEffectAttributorInteractor.onAuraEvent(debuffEvent)
                } else {
                  PlayerCacheInteractor.postEvent(debuffEvent)
                }
                Log.info(TAG, "At ${combatEvent.timestamp} ${combatEvent.source} applied debuff (${combatEvent.buff}:${combatEvent.buffId}) to ${combatEvent.target} with CID ${combatEvent.cid}.")
              } else {
                if (ProtectiveWingsAttributorInteractor.isProtectiveWingsBuff(combatEvent)) {
                  ProtectiveWingsAttributorInteractor.onAura(combatEvent)
                } else if (AreaEffectAttributorInteractor.isAreaEffectAura(combatEvent)) {
                  AreaEffectAttributorInteractor.onAuraEvent(combatEvent)
                } else {
                  PlayerCacheInteractor.postEvent(combatEvent)
                }
                Log.info(TAG, "At ${combatEvent.timestamp} ${combatEvent.source} applied buff (${combatEvent.buff}:${combatEvent.buffId}) to ${combatEvent.target} with CID ${combatEvent.cid}.")
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
              Log.info(TAG, "At ${combatEvent.timestamp} ${combatEvent.target}'s (${combatEvent.cid}) (${combatEvent.buff}:${combatEvent.buffId}) de/buff applied by ${combatEvent.source} was removed.")
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
              Log.info(TAG, "At ${combatEvent.timestamp} ${combatEvent.source} melee damaged ${combatEvent.target} for ${combatEvent.damage} damage.")
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
              Log.info(TAG, "At ${combatEvent.timestamp} ${combatEvent.target} avoided ${combatEvent.source}'s melee attack (miss).")
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
              Log.info(TAG, "At ${combatEvent.timestamp} ${combatEvent.target} suffered ${combatEvent.damage} damage to their ${event.pool} because of ${combatEvent.source}'s ${combatEvent.spell} spell.")
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
      // Truncated JSON from the addon is expected during normal operation (race between
      // Lua write and Kotlin read). Log at WARN level with a truncated snippet for debugging.
      val preview = rawJson.take(120)
      Log.warn(TAG, "Skipping incomplete IPC message ($preview…): ${e.message}")
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
