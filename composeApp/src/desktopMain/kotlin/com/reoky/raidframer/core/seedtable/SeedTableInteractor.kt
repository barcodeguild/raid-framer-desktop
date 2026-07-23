package com.reoky.raidframer.core.seedtable

import com.reoky.raidframer.AppGlobals
import com.reoky.raidframer.core.database.RFDao
import com.reoky.raidframer.core.interactor.Interactor
import com.reoky.raidframer.core.interactor.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

object SeedTableInteractor : Interactor() {

  const val TAG = "SeedTableInteractor"
  private const val THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000

  private val _seedTable = MutableStateFlow<SeedTable?>(null)
  val seedTable: StateFlow<SeedTable?> = _seedTable.asStateFlow()

  private val _status = MutableStateFlow<SeedTableStatus>(SeedTableStatus.None)
  val status: StateFlow<SeedTableStatus> = _status.asStateFlow()

  private var loaded = false

  private fun getAppDirectory(): String {
    return "${System.getProperty("user.home")}/.RaidFramer"
  }

  private fun getSeedTableFile(): File {
    return File("${getAppDirectory()}/$SEED_TABLE_FILE_NAME")
  }

  override suspend fun interact() {
    // Seed table only needs to be loaded once at startup
    if (!loaded) {
      loadSeedTable()
      loaded = true
    }
  }

  fun loadSeedTable() {
    val file = getSeedTableFile()
    if (!file.exists()) {
      _seedTable.value = null
      _status.value = SeedTableStatus.None
      return
    }
    val table = SeedTableFormat.read(file)
    if (table == null) {
      _seedTable.value = null
      _status.value = SeedTableStatus.None
      Log.warn(TAG, "Failed to read seed table file, it may be corrupt")
      return
    }
    _seedTable.value = table
    val ageMs = System.currentTimeMillis() - table.header.createdTimestamp
    _status.value = if (ageMs > THIRTY_DAYS_MS) {
      SeedTableStatus.Applied(ageMs, isStale = true, playerCount = table.entries.size)
    } else {
      SeedTableStatus.Applied(ageMs, isStale = false, playerCount = table.entries.size)
    }
    Log.info(TAG, "Loaded seed table with ${table.entries.size} entries, age=${ageMs / 1000}s")
  }

  fun exportSeedTable(file: File) {
    scope.launch {
      try {
        val cacheEntities = RFDao.playerCacheDao.getRecentPlayerCacheMetadata()
        val entries = SeedTableFormat.createEntriesFromCache(cacheEntities)
        if (entries.isEmpty()) {
          Log.warn(TAG, "No eligible players to export (all have UNKNOWN faction or 0 gear score)")
          return@launch
        }
        val versionCode = AppGlobals.APP_VERSION.replace(".", "").toInt()
        SeedTableFormat.write(file, entries, versionCode)
        Log.info(TAG, "Exported seed table with ${entries.size} entries to ${file.absolutePath}")
      } catch (e: Exception) {
        Log.error(TAG, "Failed to export seed table: ${e.message}")
      }
    }
  }

  fun importSeedTable(file: File) {
    scope.launch {
      try {
        val table = SeedTableFormat.read(file)
        if (table == null) {
          Log.warn(TAG, "Failed to read seed table from ${file.absolutePath}")
          return@launch
        }
        val destFile = getSeedTableFile()
        file.copyTo(destFile, overwrite = true)
        Log.info(TAG, "Imported seed table with ${table.entries.size} entries")
        loadSeedTable()
      } catch (e: Exception) {
        Log.error(TAG, "Failed to import seed table: ${e.message}")
      }
    }
  }

  fun removeSeedTable() {
    scope.launch {
      try {
        val file = getSeedTableFile()
        if (file.exists()) {
          file.delete()
        }
        _seedTable.value = null
        _status.value = SeedTableStatus.None
        Log.info(TAG, "Removed seed table")
      } catch (e: Exception) {
        Log.error(TAG, "Failed to remove seed table: ${e.message}")
      }
    }
  }

  fun lookupPlayer(name: String): SeedTableEntry? {
    val table = _seedTable.value ?: return null
    val hash = SeedTableFormat.hashPlayerName(name)
    return table.findEntryForHash(hash)
  }

  fun shouldUpdateFromSeedTable(localLastSeen: Long, seedEntry: SeedTableEntry): Boolean {
    return seedEntry.lastSeen > localLastSeen
  }

  fun showExportFileChooser(onFileSelected: (File) -> Unit) {
    scope.launch {
      try {
        val chooser = JFileChooser()
        chooser.dialogTitle = "Export Seed Table"
        chooser.selectedFile = File("seedtable.rfst")
        chooser.setFileFilter(FileNameExtensionFilter("Raid Framer Seed Table (*.rfst)", "rfst"))
        val result = chooser.showSaveDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
          var file = chooser.selectedFile
          if (!file.name.endsWith(".rfst", ignoreCase = true)) {
            file = File(file.parentFile, "${file.nameWithoutExtension}.rfst")
          }
          onFileSelected(file)
        }
      } catch (e: Exception) {
        Log.error(TAG, "Failed to show export file chooser: ${e.message}")
      }
    }
  }

  fun showImportFileChooser(onFileSelected: (File) -> Unit) {
    scope.launch {
      try {
        val chooser = JFileChooser()
        chooser.dialogTitle = "Import Seed Table"
        chooser.setFileFilter(FileNameExtensionFilter("Raid Framer Seed Table (*.rfst)", "rfst"))
        val result = chooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
          onFileSelected(chooser.selectedFile)
        }
      } catch (e: Exception) {
        Log.error(TAG, "Failed to show import file chooser: ${e.message}")
      }
    }
  }
}

sealed class SeedTableStatus {
  object None : SeedTableStatus()
  data class Applied(val ageMs: Long, val isStale: Boolean, val playerCount: Int) : SeedTableStatus()
}
