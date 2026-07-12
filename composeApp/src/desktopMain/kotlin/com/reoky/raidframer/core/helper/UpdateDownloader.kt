package com.reoky.raidframer.core.helper

import com.reoky.raidframer.core.helpers.sha256
import com.reoky.raidframer.core.interactor.Log
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "UpdateDownloader"

sealed class DownloadStatus {
  data class Progress(val percent: Float, val bytesDownloaded: Long, val totalBytes: Long) : DownloadStatus()
  object Verifying : DownloadStatus()
  object Installing : DownloadStatus()
  data class Success(val msiFile: File) : DownloadStatus()
  data class Error(val message: String) : DownloadStatus()
  object Cancelled : DownloadStatus()
}

object UpdateDownloader {

  @Volatile
  private var cancelled = false

  fun cancel() {
    cancelled = true
  }

  /**
   * Downloads the MSI file from the given URL to a temp location, verifies the SHA-256 hash,
   * and launches the MSI installer. Calls [onStatus] throughout to report progress.
   * Returns the downloaded File on success.
   */
  suspend fun downloadAndInstall(
    updateInfo: UpdateInfo,
    onStatus: (DownloadStatus) -> Unit
  ): DownloadStatus {
    cancelled = false

    try {
      // Step 1: Download
      onStatus(DownloadStatus.Progress(0f, 0L, 0L))
      val msiFile = downloadMsi(updateInfo.msiUrl, updateInfo.version) { percent, downloaded, total ->
        if (cancelled) return@downloadMsi
        onStatus(DownloadStatus.Progress(percent, downloaded, total))
      }

      if (cancelled) return DownloadStatus.Cancelled

      if (msiFile == null) {
        val msg = "Download failed or was cancelled."
        Log.error(TAG, msg)
        return DownloadStatus.Error(msg)
      }

      // Step 2: Verify SHA-256
      if (updateInfo.msiSha256.isNotBlank()) {
        onStatus(DownloadStatus.Verifying)
        Log.info(TAG, "Verifying SHA-256 hash of downloaded MSI...")
        val actualHash = msiFile.sha256()
        if (!actualHash.equals(updateInfo.msiSha256, ignoreCase = true)) {
          val msg = "SHA-256 verification failed. Expected: ${updateInfo.msiSha256}, Got: $actualHash"
          Log.error(TAG, msg)
          msiFile.delete()
          return DownloadStatus.Error(msg)
        }
        Log.info(TAG, "SHA-256 verification passed.")
      } else {
        Log.info(TAG, "No SHA-256 digest available, skipping verification.")
      }

      if (cancelled) return DownloadStatus.Cancelled

      // Step 3: Install via msiexec
      onStatus(DownloadStatus.Installing)
      Log.info(TAG, "Launching MSI installer: ${msiFile.absolutePath}")
      val installSuccess = launchMsiInstaller(msiFile)
      if (!installSuccess) {
        Log.error(TAG, "MSI installer launch failed.")
        msiFile.delete()
        return DownloadStatus.Error("Failed to launch the MSI installer.")
      }

      return DownloadStatus.Success(msiFile)
    } catch (e: Exception) {
      Log.error(TAG, "Update failed: ${e.message}")
      return DownloadStatus.Error(e.message ?: "Unknown error during update.")
    }
  }

  private suspend fun downloadMsi(
    downloadUrl: String,
    version: String,
    onProgress: (percent: Float, bytesDownloaded: Long, totalBytes: Long) -> Unit
  ): File? {
    val tempDir = System.getProperty("java.io.tmpdir") ?: return null
    val msiFile = File(tempDir, "RaidFramer-$version.msi")

    try {
      val connection = URL(downloadUrl).openConnection() as HttpURLConnection
      connection.requestMethod = "GET"
      connection.setRequestProperty("User-Agent", "RaidFramer-Desktop")
      connection.connectTimeout = 30_000
      connection.readTimeout = 60_000
      connection.instanceFollowRedirects = true

      val responseCode = connection.responseCode
      if (responseCode != 200) {
        Log.error(TAG, "Download returned HTTP $responseCode")
        return null
      }

      val totalBytes = connection.contentLengthLong.toLong()
      connection.inputStream.use { input ->
        FileOutputStream(msiFile).use { output ->
          val buffer = ByteArray(8192)
          var bytesRead: Long = 0
          var lastProgressUpdate = 0L
          while (true) {
            if (cancelled) {
              msiFile.delete()
              return null
            }
            val read = input.read(buffer)
            if (read == -1) break
            output.write(buffer, 0, read)
            bytesRead += read

            // Throttle progress updates to every ~500ms to avoid UI thrashing
            val now = System.currentTimeMillis()
            if (now - lastProgressUpdate > 500 || bytesRead == totalBytes) {
              val percent = if (totalBytes > 0) (bytesRead.toFloat() / totalBytes * 100f) else 0f
              onProgress(percent.coerceIn(0f, 100f), bytesRead, totalBytes)
              lastProgressUpdate = now
            }
          }
        }
      }

      Log.info(TAG, "Download complete: ${msiFile.absolutePath} (${msiFile.length()} bytes)")
      return msiFile
    } catch (e: Exception) {
      Log.error(TAG, "Download error: ${e.message}")
      msiFile.delete()
      return null
    }
  }

  /**
   * Launches the MSI installer with passive mode (shows progress bar, no user interaction needed).
   * Before returning, spawns an independent watcher process that will wait for msiexec to finish
   * and then attempt to relaunch the app. This is necessary because the main app process will
   * exit immediately after this call so the MSI can replace files.
   */
  private fun launchMsiInstaller(msiFile: File): Boolean {
    return try {
      val msiexecPath = findMsiexec()
      val msiPath = msiFile.absolutePath

      val installLogFile = File(
        System.getProperty("java.io.tmpdir"),
        "RaidFramer-install-${System.currentTimeMillis()}.log"
      )

      // Build the msiexec command: passive mode shows progress but doesn't require clicks.
      // The verbose log provides a reliable installation-completion signal.
      val command = listOf(
        msiexecPath,
        "/i", msiPath,
        "/passive",
        "/norestart",
        "/l*v", installLogFile.absolutePath
      )

      Log.info(TAG, "Running: ${command.joinToString(" ")}")

      val processBuilder = ProcessBuilder(command)
        .redirectErrorStream(true)
      val process = processBuilder.start()

      // Spawn an independent watcher process that outlives the main app.
      // It finds the exe, waits for the file to stabilize, then relaunches the app.
      spawnRelaunchWatcher(installLogFile.absolutePath)

      true
    } catch (e: Exception) {
      Log.error(TAG, "Failed to launch msiexec: ${e.message}")
      false
    }
  }

  /**
   * Spawns an independent PowerShell script that dynamically finds the installed exe,
   * polls until the file's date modified stabilizes (>5 seconds old), then relaunches.
   * The script survives exitProcess(0) because it's a detached OS process.
   */
  private fun spawnRelaunchWatcher(installLogPath: String) {
    try {
      val d = "$" // Kotlin raw strings still interpolate $, so build the PS vars separately
      val script = """
        |param([string]${d}LogPath)
        |
        |# Wait for Windows Installer to report successful completion in its verbose log.
        |${d}deadline = [DateTime]::Now.AddMinutes(10)
        |${d}installComplete = ${d}false
        |while ([DateTime]::Now -lt ${d}deadline) {
        |  if (Test-Path -LiteralPath ${d}LogPath) {
        |    ${d}log = Get-Content -LiteralPath ${d}LogPath -Raw -ErrorAction SilentlyContinue
        |    if (${d}log -match "Installation completed successfully") {
        |      ${d}installComplete = ${d}true
        |      break
        |    }
        |    if (${d}log -match "Installation failed" -or ${d}log -match "Return value 3") {
        |      Write-Host "The MSI installation failed."
        |      exit 1
        |    }
        |  }
        |  Start-Sleep -Milliseconds 500
        |}
        |
        |if (-not ${d}installComplete) {
        |  Write-Host "Timed out waiting for MSI installation to complete."
        |  exit 1
        |}
        |
        |${d}exePath = ${d}null
        |
        |# Try Start Menu shortcut (most reliable for Compose Desktop MSI)
        |${d}lnkPaths = @(
        |  "${d}env:APPDATA\Microsoft\Windows\Start Menu\Programs\Raid Framer\Raid Framer.lnk",
        |  "${d}env:ProgramData\Microsoft\Windows\Start Menu\Programs\Raid Framer\Raid Framer.lnk"
        |)
        |foreach (${d}lnk in ${d}lnkPaths) {
        |  if (Test-Path ${d}lnk) {
        |    ${d}shell = New-Object -ComObject WScript.Shell
        |    ${d}target = ${d}shell.CreateShortcut(${d}lnk).TargetPath
        |    if (${d}target -and (Test-Path ${d}target)) {
        |      ${d}exePath = ${d}target
        |      break
        |    }
        |  }
        |}
        |
        |# Fallback: search common install locations
        |if (-not ${d}exePath) {
        |  ${d}candidates = @(
        |    "${d}env:ProgramFiles\Raid Framer\Raid Framer.exe",
        |    "${d}{env:ProgramFiles(x86)}\Raid Framer\Raid Framer.exe",
        |    "${d}env:LOCALAPPDATA\Programs\Raid Framer\Raid Framer.exe"
        |  )
        |  foreach (${d}c in ${d}candidates) {
        |    if (Test-Path ${d}c) { ${d}exePath = ${d}c; break }
        |  }
        |}
        |
        |if (-not ${d}exePath) {
        |  Write-Host "Could not locate Raid Framer exe."
        |  exit 1
        |}
        |
        |Write-Host "Found exe: ${d}exePath"
        |
        |# Wait until the installed executable exists and can be opened.
        |# The MSI log is the completion signal; this check handles final file replacement
        |# and makes sure the executable is available before relaunching.
        |${d}fileDeadline = [DateTime]::Now.AddMinutes(2)
        |${d}fileReady = ${d}false
        |while ([DateTime]::Now -lt ${d}fileDeadline) {
        |  if (Test-Path -LiteralPath ${d}exePath -PathType Leaf) {
        |    try {
        |      ${d}stream = [System.IO.File]::Open(
        |        ${d}exePath,
        |        [System.IO.FileMode]::Open,
        |        [System.IO.FileAccess]::Read,
        |        [System.IO.FileShare]::ReadWrite -bor [System.IO.FileShare]::Delete
        |      )
        |      ${d}stream.Dispose()
        |      ${d}fileReady = ${d}true
        |      break
        |    } catch {
        |      # The installer may still be replacing or releasing the executable.
        |    }
        |  }
        |  Start-Sleep -Milliseconds 250
        |}
        |
        |if (-not ${d}fileReady) {
        |  Write-Host "Timed out waiting for the installed executable to become available."
        |  exit 1
        |}
        |
        |Write-Host "Launching ${d}exePath"
        |Start-Process -FilePath ${d}exePath
        |
        |Remove-Item -LiteralPath ${d}LogPath -Force -ErrorAction SilentlyContinue
        |
        |# Self-delete the script
        |Remove-Item -Path ${d}PSCommandPath -Force -ErrorAction SilentlyContinue
      """.trimMargin()

      val psFile = File(System.getProperty("java.io.tmpdir"), "RaidFramerRelaunch.ps1")
      psFile.writeText(script)

      ProcessBuilder(
        "powershell",
        "-NoProfile",
        "-ExecutionPolicy", "Bypass",
        "-File", psFile.absolutePath,
        "-LogPath", installLogPath
      ).start()

      Log.info(TAG, "Spawned relaunch watcher: ${psFile.absolutePath}")
    } catch (e: Exception) {
      Log.info(TAG, "Could not spawn relaunch watcher (non-critical): ${e.message}")
    }
  }

  private fun findMsiexec(): String {
    val systemRoot = System.getenv("SYSTEMROOT") ?: "C:\\Windows"
    val msiexec = "$systemRoot\\System32\\msiexec.exe"
    return if (File(msiexec).exists()) msiexec else "msiexec.exe"
  }
}
