package com.reoky.raidframer.core.helper

import com.reoky.raidframer.AppGlobals
import java.net.HttpURLConnection
import java.net.URL

sealed class UpdateStatus {
  object Idle : UpdateStatus()
  object Checking : UpdateStatus()
  data class Available(val newVersion: String, val releaseUrl: String) : UpdateStatus()
  object UpToDate : UpdateStatus()
  object Error : UpdateStatus()
}

object UpdateHelper {

  private const val GITHUB_API_URL = "https://api.github.com/repos/barcodeguild/raid-framer-desktop/releases/latest"
  private const val GITHUB_RELEASES_URL = "https://github.com/barcodeguild/raid-framer-desktop/releases/latest"

  fun checkForUpdates(onResult: (UpdateStatus) -> Unit) {
    try {
      val connection = URL(GITHUB_API_URL).openConnection() as HttpURLConnection
      connection.requestMethod = "GET"
      connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
      connection.setRequestProperty("User-Agent", "RaidFramer-Desktop")
      val responseCode = connection.responseCode
      if (responseCode == 200) {
        val response = connection.inputStream.bufferedReader().use { it.readText() }
        val tagName = extractTagName(response)
        val htmlUrl = extractHtmlUrl(response)
        val newVersion = parseVersionFromTag(tagName)
        if (newVersion != null && isVersionGreater(newVersion, AppGlobals.APP_VERSION)) {
          onResult(UpdateStatus.Available(newVersion, htmlUrl ?: GITHUB_RELEASES_URL))
        } else {
          onResult(UpdateStatus.UpToDate)
        }
      } else {
        onResult(UpdateStatus.Error)
      }
    } catch (_: Exception) {
      onResult(UpdateStatus.Error)
    }
  }

  private fun parseVersionFromTag(tag: String?): String? {
    if (tag == null) return null
    val withoutV = tag.trimStart('v')
    val match = Regex("RF(\\d+)").find(withoutV)
    if (match != null) {
      val num = match.groupValues[1]
      return when (num.length) {
        2 -> "2.0.${num}"
        3 -> "${num[0]}.${num[1]}.${num[2]}"
        4 -> "${num[0]}.${num[1]}${num[2]}.${num[3]}"
        else -> null
      }
    }
    val semver = Regex("(\\d+)\\.(\\d+)\\.(\\d+)").find(withoutV)
    return semver?.groupValues?.get(0)
  }

  private fun extractTagName(json: String): String? {
    val regex = "\"tag_name\"\\s*:\\s*\"([^\"]+)\"".toRegex()
    return regex.find(json)?.groupValues?.get(1)
  }

  private fun extractHtmlUrl(json: String): String? {
    val regex = "\"html_url\"\\s*:\\s*\"([^\"]+)\"".toRegex()
    return regex.find(json)?.groupValues?.get(1)
  }

  private fun isVersionGreater(newVersion: String, currentVersion: String): Boolean {
    val newParts = newVersion.split('.').map { it.toIntOrNull() ?: 0 }
    val currentParts = currentVersion.split('.').map { it.toIntOrNull() ?: 0 }
    val max = maxOf(newParts.size, currentParts.size)
    for (i in 0 until max) {
      val newPart = newParts.getOrElse(i) { 0 }
      val currentPart = currentParts.getOrElse(i) { 0 }
      if (newPart > currentPart) return true
      if (newPart < currentPart) return false
    }
    return false
  }
}
