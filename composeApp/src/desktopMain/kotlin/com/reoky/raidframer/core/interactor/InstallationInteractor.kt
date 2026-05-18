package com.reoky.raidframer.core.interactor

import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.helpers.sha256
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import raid_framer_desktop.composeapp.generated.resources.Res
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.isDirectory

/*
 * For post-installation setup tasks.
 * - Confirm that the ArcheRage game config has combat logging enabled, otherwise this app won't work.
 * - Can place the RaidFramer LUA addon into the game's AddOns folder automatically.
 * - Helps reduce the number of manual steps and the number of support DMs by just doing it.
 */
object InstallationInteractor : Interactor() {

  const val TAG = "InstallationInteractor"

  const val AR_CONFIG_FILE_NAME = "system.cfg"
  const val AR_ADDONS_DIRECTORY = "Addon"

  // list of resource Lua files to install to the addons directory from the app's resources
  val RF_ADDON_DIRECTORY = "RaidFramer"
  val RF_ADDON_META = listOf(
    "toc.g", "apitypes.lua", "windowcommon.lua", "window.lua", "config.lua",
    "buttoncommon.lua", "button.lua", "combobox.lua", "parsers.lua", "debug.lua",
    "json.lua", "combat.lua", "chat.lua", "raid.lua", "ipc.lua", "reload.lua",
    "raidframer.lua"
  )

  // main event loop oh eek
  override suspend fun interact() {
    super.interact()

    // Check that there's an ArcheRage directory set in config
    val gameDirectory = RFConfig.state.value.defaultArcheRageDirectory
    if (gameDirectory.isBlank()) return Log.error(TAG, "No ArcheRage directory set in config; cannot perform installation checks.")

    // enumerate the directory
    val gameDirectoryPath  = Paths.get(gameDirectory)
    if (!Files.isDirectory(gameDirectoryPath)) return Log.error(TAG, "Configured ArcheRage directory does not exist or is not a directory: $gameDirectory")

    // find System.cfg
    val arConfigFile = Paths.get(gameDirectory, AR_CONFIG_FILE_NAME).toFile()
    if (!arConfigFile.exists() || arConfigFile.length() == 0L) return Log.error(TAG, "ArcheRage config file not found at expected location: ${arConfigFile.absolutePath}")

    // ok now we can do stuff with it
    //Log.info(TAG, "InstallationInteractor started successfully with path: $gameDirectory")

    // Make a folder for addons if it doesn't exist (just in case the person has an older install of the game or something)
    // will do nothing if it already exists
    val arAddonsPath = Paths.get(gameDirectory, AR_ADDONS_DIRECTORY)
    if (!arAddonsPath.isDirectory()) {
      try {
        withContext(Dispatchers.IO) {
          Files.createDirectory(arAddonsPath)
        }
        Log.info(TAG, "Created Addon directory at: $arAddonsPath")
      } catch (e: Exception) {
        Log.error(TAG, "Failed to create Addon directory at: $arAddonsPath.")
      }
    }

    // Make another folder for RaidFramer inside the addons directory if it doesn't exist
    val rfAddonPath = arAddonsPath.resolve(RF_ADDON_DIRECTORY)
    if (!rfAddonPath.isDirectory()) {
      try {
        withContext(Dispatchers.IO) {
          Files.createDirectory(rfAddonPath)
        }
        Log.info(TAG, "Created RaidFramer Addon directory at: $rfAddonPath")
      } catch (e: Exception) {
        Log.error(TAG, "Failed to create RaidFramer addon directory at: $rfAddonPath.")
      }
    }

    // How to install something.. Well friends.. You check the hashes of the resource files vs the installed files..
    // and if she don't match you copy ~
    // Note: computeResourceFileHashes is now a suspend function oh eek
    val knownGoodHashes = computeResourceFileHashes()
    val installedHashes = computeInstalledAddonHashes(arAddonsPath.resolve(RF_ADDON_DIRECTORY).toString())

    for ((file, hash) in installedHashes) {
      val isValid = knownGoodHashes[file]?.let { it == hash } ?: false
      if (isValid) continue // skip logging valid files
      val isMissing = knownGoodHashes[file] == null
      if (isMissing) {
        Log.debug(TAG, "Hash for Addon file: $file (missing) -> ????")
      } else {
        Log.debug(TAG, "Addon file: $file ${if (!isValid) "(outdated)" else "(valid)"} -> $hash")
      }
    }

    // Install missing/outdated files
    for ((file, knownHash) in knownGoodHashes) {
      val installedHash = installedHashes[file]
      if (installedHash == null || installedHash != knownHash) {
        val resourcePath = "files/$RF_ADDON_DIRECTORY/$file"
        try {
          val fileBytes = Res.readBytes(resourcePath) // Read bytes from JAR/MSI via Compose Resources
          val outputFilePath = rfAddonPath.resolve(file)
          withContext(Dispatchers.IO) {
            outputFilePath.toFile().writeBytes(fileBytes)
          }
          Log.info(TAG, "Installed addon file: $outputFilePath")
        } catch (e: Exception) {
          Log.error(TAG, "Failed to install addon file ($resourcePath): ${e.message}")
        }
      }
    }

    // pick the relevant settings from the config to write to settings.conf
    // this is one-way write only from app to lua addon
    val config = RFConfig.state.value
    val relevantSettings = mapOf(
      "companion_enabled" to config.companionEnabled.toString(),
      "show_raid_status" to config.companionShowRaidStatus.toString(),
      "show_charmed_in_chat" to config.companionShowCharmedInChat.toString(),
      "show_silenced_in_chat" to config.companionShowSilencedInChat.toString(),
      "show_distressed_in_chat" to config.companionShowDistressedInChat.toString(),
      "play_charm_sound" to config.companionPlayCharmSound.toString(),
      "mark_hvt_healers" to config.companionMarkHVTHealers.toString(),
      "mark_hvt_dps" to config.companionMarkHVTDPS.toString(),
      "mark_hvt_cc" to config.companionMarkHVTCrowdControl.toString(),
      "mark_sac_dancers" to config.companionMarkSacDancers.toString(),
      "mark_charmed_targets" to config.companionMarkCharmedTargets.toString(),
      "mark_silenced_targets" to config.companionMarkSilencedTargets.toString(),
      "mark_distressed_targets" to config.companionMarkDistressedTargets.toString()
    )

    // write config in ini format to settings.conf in the addon directory
    val settingsFilePath = rfAddonPath.resolve("settings.conf")
    val beforeHash = if (settingsFilePath.toFile().exists()) settingsFilePath.toFile().sha256() else "missing"
    val settingsContent = "# RaidFramer Addon Settings Managed by RaidFramer App : Not for manual editing"
    try {
      withContext(Dispatchers.IO) {
        settingsFilePath.toFile().writeText(settingsContent)
        for ((key, value) in relevantSettings) {
          settingsFilePath.toFile().appendText("\n${key}=${value}")
        }
        val afterHash = settingsFilePath.toFile().sha256()
        if (beforeHash != afterHash) {
          Log.info(TAG, "Notifying companion addon of updated config settings.")
          CompanionInteractor.notifyConfigUpdated()
        }
      }
    } catch (e: Exception) {
      Log.error(TAG, "Failed to write settings.conf to addon directory: $settingsFilePath")
    }

  }

  /*
   * Function that uses File.sha256() extension to compute map of file paths to SHA256 hashes from the resource files.
   */
  @OptIn(ExperimentalResourceApi::class)
  suspend fun computeResourceFileHashes(): Map<String, String> {
    val result = mutableMapOf<String, String>()
    for (fileName in RF_ADDON_META) {
      val resourcePath = "files/$RF_ADDON_DIRECTORY/$fileName"
      try {
        val bytes = Res.readBytes(resourcePath) // into memory
        val hash = bytes.sha256() // get the hash from in-memory bytes
        result[fileName] = hash
      } catch (e: Exception) {
        Log.error(TAG, "Resource file not found for hashing: $resourcePath")
      }
    }
    return result
  }

  /*
   * Function that uses File.sha256() extension to compute map of file paths to SHA256 hashes from the installed addon files.
   * We assume the addonDirectory is valid and exists.
   */
  fun computeInstalledAddonHashes(addonDirectory: String): Map<String, String> {
    val result = mutableMapOf<String, String>()
    for (fileName in RF_ADDON_META) {
      val filePath = Paths.get(addonDirectory, fileName)
      val file = filePath.toFile()
      if (file.exists() && file.isFile) {
        val hash = file.sha256()
        result[fileName] = hash
      }
    }
    return result
  }
}
