package com.reoky.raidframer.core.seedtable

import com.reoky.raidframer.core.database.PlayerCacheEntity
import com.reoky.raidframer.core.helpers.sha256
import com.reoky.raidframer.core.model.Faction
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

const val SEED_TABLE_MAGIC = 0x65434399
const val SEED_TABLE_VERSION = 1
const val SEED_TABLE_FILE_NAME = "seedtable.rfst"
const val SEED_TABLE_MAX_ROWS = 10000

data class SeedTableHeader(
  val magic: Int,
  val version: Int,
  val appVersionCode: Int,
  val createdTimestamp: Long,
  val rowCount: Int
)

data class SeedTableEntry(
  val nameHash: String,
  val gearScore: Int,
  val lastSeen: Long,
  val lastKnownSpec: String,
  val lastKnownFaction: String,
  val lastKnownGuild: String = ""
)

data class SeedTable(
  val header: SeedTableHeader,
  val entries: List<SeedTableEntry>
) {
  fun findEntryForHash(nameHash: String): SeedTableEntry? {
    return entries.firstOrNull { it.nameHash == nameHash }
  }
}

object SeedTableFormat {

  private const val HASH_BYTE_LENGTH = 32
  private const val MAX_STRING_LENGTH = 64

  fun write(file: File, entries: List<SeedTableEntry>, appVersionCode: Int) {
    FileOutputStream(file).use { fos ->
      val headerBuffer = ByteBuffer.allocate(4 + 4 + 4 + 8 + 4).order(ByteOrder.LITTLE_ENDIAN)
      headerBuffer.putInt(SEED_TABLE_MAGIC)
      headerBuffer.putInt(SEED_TABLE_VERSION)
      headerBuffer.putInt(appVersionCode)
      headerBuffer.putLong(System.currentTimeMillis())
      headerBuffer.putInt(entries.size)
      fos.write(headerBuffer.array())

      entries.forEach { entry ->
        val hashBytes = entry.nameHash.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        fos.write(hashBytes)

        val gearBuffer = ByteBuffer.allocate(4 + 8).order(ByteOrder.LITTLE_ENDIAN)
        gearBuffer.putInt(entry.gearScore)
        gearBuffer.putLong(entry.lastSeen)
        fos.write(gearBuffer.array())

        writePaddedString(fos, entry.lastKnownSpec, MAX_STRING_LENGTH)
        writePaddedString(fos, entry.lastKnownFaction, MAX_STRING_LENGTH)
        writePaddedString(fos, entry.lastKnownGuild, MAX_STRING_LENGTH)
      }
    }
  }

  private fun writePaddedString(fos: FileOutputStream, str: String, maxLength: Int) {
    val bytes = str.toByteArray().take(maxLength).toByteArray()
    val padded = ByteArray(maxLength)
    System.arraycopy(bytes, 0, padded, 0, bytes.size)
    fos.write(padded)
  }

  fun read(file: File): SeedTable? {
    if (!file.exists()) return null
    return FileInputStream(file).use { fis ->
      val headerBuffer = ByteArray(4 + 4 + 4 + 8 + 4)
      if (fis.read(headerBuffer) != headerBuffer.size) return null
      val bb = ByteBuffer.wrap(headerBuffer).order(ByteOrder.LITTLE_ENDIAN)
      val magic = bb.int
      if (magic != SEED_TABLE_MAGIC) return null
      val version = bb.int
      val appVersionCode = bb.int
      val createdTimestamp = bb.long
      val rowCount = bb.int

      val entries = mutableListOf<SeedTableEntry>()
      repeat(rowCount) {
        val hashBytes = ByteArray(HASH_BYTE_LENGTH)
        if (fis.read(hashBytes) != HASH_BYTE_LENGTH) return null
        val nameHash = hashBytes.fold("") { str, b -> str + "%02x".format(b) }

        val gearBuffer = ByteArray(4 + 8)
        if (fis.read(gearBuffer) != gearBuffer.size) return null
        val gb = ByteBuffer.wrap(gearBuffer).order(ByteOrder.LITTLE_ENDIAN)
        val gearScore = gb.int
        val lastSeen = gb.long

        val spec = readPaddedString(fis, MAX_STRING_LENGTH)
        val faction = readPaddedString(fis, MAX_STRING_LENGTH)
        val guild = readPaddedString(fis, MAX_STRING_LENGTH)

        entries.add(SeedTableEntry(nameHash, gearScore, lastSeen, spec, faction, guild))
      }

      SeedTable(
        SeedTableHeader(magic, version, appVersionCode, createdTimestamp, rowCount),
        entries
      )
    }
  }

  private fun readPaddedString(fis: FileInputStream, length: Int): String {
    val bytes = ByteArray(length)
    fis.read(bytes)
    return bytes.takeWhile { it != 0.toByte() }.toByteArray().toString(Charsets.UTF_8)
  }

  fun hashPlayerName(name: String): String {
    return sha256(name, name)
  }

  fun createEntriesFromCache(cacheEntities: List<PlayerCacheEntity>): List<SeedTableEntry> {
    return cacheEntities
      .filter {
        val faction = Faction.fromString(it.lastKnownFaction)
        faction != Faction.UNKNOWN && it.lastKnownGearScore > 0
      }
      .map {
        SeedTableEntry(
          nameHash = hashPlayerName(it.playerName),
          gearScore = it.lastKnownGearScore,
          lastSeen = it.lastSeen,
          lastKnownSpec = it.lastKnownSpec,
          lastKnownFaction = it.lastKnownFaction,
          lastKnownGuild = it.lastKnownGuild
        )
      }
      .take(SEED_TABLE_MAX_ROWS)
  }
}
