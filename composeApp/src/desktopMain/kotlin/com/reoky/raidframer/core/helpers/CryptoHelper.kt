package com.reoky.raidframer.core.helpers

import java.io.File
import java.security.MessageDigest

private const val APP_PEPPER = "Pre-ComputedTablesLessUsefulWithAGoodSaltAndPeppering!~Reeeeeeeeoky!~"

/**
 * Simple Salt and Peppered SHA-256 hashing function used for comparing player names. Thank me later friends! <3 - Reoky
 * This is currently an offline APP so security is not a huge concern, but still good to have.
 */
fun sha256(input: String, salt: String): String {
  val combined = input + salt + APP_PEPPER
  val bytes = combined.toByteArray()
  val md = MessageDigest.getInstance("SHA-256")
  val digest = md.digest(bytes)
  return digest.fold("") { str, it -> str + "%02x".format(it) }
}

/**
 * This helper function allows us to validate that the addon files are not corrupt or tampered with. The idea is
 * that we can compute the SHA-256 hash of the file and compare it to a known good value, and replace individual
 * components automatically if they don't match. (so the user doesn't have to deal with passing rar archives around)
 */
fun File.sha256(): String {
  val md = MessageDigest.getInstance("SHA-256")
  inputStream().use { fis ->
    val buffer = ByteArray(1024)
    var bytesRead: Int
    while (fis.read(buffer).also { bytesRead = it } != -1) {
      md.update(buffer, 0, bytesRead)
    }
  }
  val digest = md.digest()
  return digest.fold("") { str, it -> str + "%02x".format(it) }
}

/**
 * Validates in-memory byte arrays (from Compose Resources).
 */
fun ByteArray.sha256(): String {
  val md = MessageDigest.getInstance("SHA-256")
  val digest = md.digest(this)
  return digest.fold("") { str, it -> str + "%02x".format(it) }
}