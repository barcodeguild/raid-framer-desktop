package com.reoky.raidframer.core.interactor

import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.helpers.sha256
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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


  // list of resource lua files to install to the addons directory from the app's resources
  val RF_ADDON_DIRECTORY = "RaidFramer"
  val RF_ADDON_META = listOf(
    "toc.g",
    "apitypes.lua",
    "windowcommon.lua",
    "window.lua",
    "buttoncommon.lua",
    "button.lua",
    "combobox.lua",
    "debug.lua",
    "json.lua",
    "raid.lua",
    "ipc.lua",
    "reload.lua",
    "raidframer.lua"
  )

  // main event loop oh eekm
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
    Log.info(TAG, "InstallationInteractor started successfully with path: $gameDirectory")

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
    val knownGoodHashes = computeResourceFileHashes()
    val installedHashes = computeInstalledAddonHashes(arAddonsPath.resolve(RF_ADDON_DIRECTORY).toString())
    for ((file, hash) in installedHashes) {
      val isValid = knownGoodHashes[file]?.let { it == hash } ?: false
      val isMissing = knownGoodHashes[file] == null
      if (isMissing) {
        Log.debug(TAG, "Addon file: $file (missing) -> ????")
      } else {
        Log.debug(TAG, "Addon file: $file ${if (!isValid) "(outdated)" else "(valid)"} -> $hash")
      }
    }

    // Now copy over any missing or outdated files
    for ((file, knownHash) in knownGoodHashes) {
      val installedHash = installedHashes[file]
      val needsInstall = installedHash == null || installedHash != knownHash
      if (needsInstall) {
        // copy from resources to addon directory
        val resourcePath = "/$RF_ADDON_DIRECTORY/$file"
        val inputStream = this::class.java.getResourceAsStream(resourcePath)
        if (inputStream != null) {
          val outputFilePath = rfAddonPath.resolve(file)
          try {
            withContext(Dispatchers.IO) {
              outputFilePath.toFile().outputStream().use { output ->
                inputStream.copyTo(output)
              }
            }
            Log.info(TAG, "Installed addon file: $outputFilePath")
          } catch (e: Exception) {
            Log.error(TAG, "Failed to install addon file: $outputFilePath")
          }
        } else {
          Log.error(TAG, "Resource file not found for installation: $resourcePath")
        }
      }
    }

  }

  /*
   * Function that uses File.sha256() extension to compute map of file paths to SHA256 hashes from the resource files.
   */
  fun computeResourceFileHashes(): Map<String, String> {
    val result = mutableMapOf<String, String>()
    for (fileName in RF_ADDON_META) {
      val resourcePath = "/$RF_ADDON_DIRECTORY/$fileName"
      val inputStream = this::class.java.getResourceAsStream(resourcePath)
      if (inputStream != null) {
        val tempFile = kotlin.io.path.createTempFile()
        tempFile.toFile().outputStream().use { output ->
          inputStream.copyTo(output)
        }
        val hash = tempFile.toFile().sha256()
        result[fileName] = hash
        tempFile.toFile().delete()
      } else {
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
      } else {
        Log.error(TAG, "Installed addon file not found for hashing: $filePath")
      }
    }
    return result
  }
}
