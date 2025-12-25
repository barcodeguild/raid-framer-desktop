package com.reoky.raidframer.core.interactor

import com.reoky.raidframer.core.config.RFConfig
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

  const val AR_CONFIG_FILE_NAME = "System.cfg"
  const val AR_ADDONS_DIRECTORY = "Addon"

  // main event loop oh eekm
  override suspend fun interact() {
    super.interact()

    // Check that there's an ArcheRage directory set in config
    val gameDirectory = RFConfig.state.value.defaultArcheRageDirectory
    if (gameDirectory.isBlank()) return Log.error(TAG, "No ArcheRage directory set in config; cannot perform installation checks.")

    // enumerate the directory
    val gameDirectoryPath  = Paths.get(gameDirectory)
    if (Files.isDirectory(gameDirectoryPath) && Files.isReadable(gameDirectoryPath)) return Log.error(TAG, "Configured ArcheRage directory does not exist or is not a directory: $gameDirectory")

    // find System.cfg
    val arConfigFile = Paths.get(gameDirectory, AR_CONFIG_FILE_NAME).toFile()
    if (!arConfigFile.exists() || arConfigFile.length() == 0L) return Log.error(TAG, "ArcheRage config file not found at expected location: ${arConfigFile.absolutePath}")

    // ok now we can do stuff with it
    Log.info(TAG, "InstallationInteractor started successfully")

  }
}
