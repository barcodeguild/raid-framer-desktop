package com.reoky.raidframer.core.helpers

import java.security.MessageDigest

private const val APP_PEPPER = "Pre-ComputedTablesLessUsefulWithAGoodSaltAndPeppering!~Reeeeeeeeoky!~"

/*
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
