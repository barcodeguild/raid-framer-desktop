package com.reoky.raidframer.core.helpers

import com.reoky.raidframer.AppGlobals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.HttpURLConnection
import java.net.URI
import java.util.regex.Pattern

data class UpdateInfo(
  val version: String,
  val msiUrl: String,
  val msiSha256: String,
  val releaseUrl: String,
  val tagName: String,
  val releaseNotes: String
)

sealed class UpdateStatus {
  object Idle : UpdateStatus()
  object Checking : UpdateStatus()
  data class Available(val updateInfo: UpdateInfo) : UpdateStatus()
  object UpToDate : UpdateStatus()
  object Error : UpdateStatus()
}

object UpdateHelper {

  private const val GITHUB_API_URL = "https://api.github.com/repos/barcodeguild/raid-framer-desktop/releases/latest"
  private const val GITHUB_RELEASES_URL = "https://github.com/barcodeguild/raid-framer-desktop/releases/latest"

  private val _pendingUpdate = MutableStateFlow<UpdateInfo?>(null)
  val pendingUpdate: StateFlow<UpdateInfo?> = _pendingUpdate.asStateFlow()

  @Volatile
  var shouldScrollToUpdate = false

  fun checkForUpdates(onResult: (UpdateStatus) -> Unit) {
    try {
      val connection = URI(GITHUB_API_URL).toURL().openConnection() as HttpURLConnection
      connection.requestMethod = "GET"
      connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
      connection.setRequestProperty("User-Agent", "RaidFramer-Desktop")
      val responseCode = connection.responseCode
      if (responseCode == 200) {
        val response = connection.inputStream.bufferedReader().use { it.readText() }
        val tagName = extractTagName(response)
        val htmlUrl = extractHtmlUrl(response)
        val newVersion = parseVersionFromTag(tagName)
        val msiAsset = extractMsiAsset(response)
        val releaseNotes = extractReleaseBody(response)?.let { filterChangelog(stripImages(it)) } ?: ""

        if (newVersion != null && (AppGlobals.DEBUG_UPDATE_SAME_VERSION || isVersionGreater(newVersion, AppGlobals.APP_VERSION))) {
          if (msiAsset != null) {
            val updateInfo = UpdateInfo(
              version = newVersion,
              msiUrl = msiAsset.first,
              msiSha256 = msiAsset.second,
              releaseUrl = htmlUrl ?: GITHUB_RELEASES_URL,
              tagName = tagName ?: "",
              releaseNotes = releaseNotes
            )
            _pendingUpdate.value = updateInfo
            onResult(UpdateStatus.Available(updateInfo))
          } else {
            onResult(UpdateStatus.Error)
          }
        } else {
          _pendingUpdate.value = null
          onResult(UpdateStatus.UpToDate)
        }
      } else {
        onResult(UpdateStatus.Error)
      }
    } catch (_: Exception) {
      onResult(UpdateStatus.Error)
    }
  }

  fun clearPendingUpdate() {
    _pendingUpdate.value = null
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

  /**
   * Extracts the release body (patch notes) from the GitHub release JSON.
   * The body is a JSON-escaped string — unescapes \\r\\n, \\n, \\\" etc.
   */
  private fun extractReleaseBody(json: String): String? {
    // Use possessive quantifiers (*+) to prevent catastrophic backtracking
    val pattern = Pattern.compile("\"body\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*+)\"")
    val matcher = pattern.matcher(json)
    if (!matcher.find()) return null
    return matcher.group(1)
      ?.replace("\\r\\n", "\n")
      ?.replace("\\n", "\n")
      ?.replace("\\\"", "\"")
      ?.replace("\\\\", "\\")
      ?.trim()
  }

  /**
   * Strips Markdown image patterns ![alt](url) from text.
   */
  private fun stripImages(body: String): String {
    // Use possessive quantifiers to prevent backtracking
    val pattern = Pattern.compile("!\\[[^]]*]\\([^)]*+\\)")
    return pattern.matcher(body).replaceAll("").trim()
  }

  /**
   * Filters release notes to only include content between separator lines
   * (5+ consecutive dashes, asterisks, or underscores).
   * If fewer than 2 separators are found, returns the full body.
   */
  private fun filterChangelog(body: String): String {
    val separatorRegex = Regex("^-{5,}$|^\\*{5,}$|^_{5,}$", RegexOption.MULTILINE)
    val separators = separatorRegex.findAll(body).toList()
    if (separators.size >= 2) {
      val start = separators.first().range.last + 1
      val end = separators.last().range.first
      return body.substring(start, end).trim()
    }
    return body.trim()
  }

  /**
   * Extracts the MSI download URL and SHA-256 digest from the GitHub release JSON.
   * Returns Pair<downloadUrl, sha256Digest> or null if no MSI asset found.
   */
  private fun extractMsiAsset(json: String): Pair<String, String>? {
    val assetsBlockRegex = Pattern.compile("\"assets\"\\s*:\\s*\\[")
    val assetsMatcher = assetsBlockRegex.matcher(json)
    if (!assetsMatcher.find()) return null
    val assetsStart = assetsMatcher.end()

    var searchFrom = assetsStart
    while (searchFrom < json.length) {
      val namePattern = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+\\.msi)\"")
      val nameMatcher = namePattern.matcher(json)
      if (!nameMatcher.find(searchFrom)) break
      val msiName = nameMatcher.group(1) ?: break

      val urlPattern = Pattern.compile("\"browser_download_url\"\\s*:\\s*\"([^\"]+)\"")
      val urlMatcher = urlPattern.matcher(json)
      if (!urlMatcher.find(nameMatcher.start())) break
      val downloadUrl = urlMatcher.group(1) ?: break

      // Look for the digest in the same asset block (within ~2000 chars after name)
      val digestRegion = json.substring(nameMatcher.start(), minOf(nameMatcher.end() + 2000, json.length))
      val digestPattern = Pattern.compile("\"digest\"\\s*:\\s*\"sha256:([a-f0-9]+)\"")
      val digestMatcher = digestPattern.matcher(digestRegion)
      val sha256 = if (digestMatcher.find()) digestMatcher.group(1) ?: "" else ""

      if (msiName.endsWith(".msi", ignoreCase = true)) {
        return Pair(downloadUrl, sha256)
      }
      searchFrom = nameMatcher.end() + 1
    }
    return null
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
