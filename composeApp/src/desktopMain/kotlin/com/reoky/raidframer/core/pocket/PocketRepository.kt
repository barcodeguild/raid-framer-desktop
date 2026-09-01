package com.reoky.raidframer.core.pocket

import com.reoky.raidframer.core.database.PocketAttachmentEntity
import com.reoky.raidframer.core.database.PocketDao
import com.reoky.raidframer.core.database.PocketEntryEntity
import com.reoky.raidframer.core.database.PocketTagEntity
import com.reoky.raidframer.core.helpers.getPocketJournalDirectory
import com.reoky.raidframer.core.helpers.writeTextAtomically
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

const val MAX_POCKET_ATTACHMENTS = 10

/** In-memory representation of an entry together with its local file metadata. */
data class PocketEntry(
  val metadata: PocketEntryEntity,
  val markdown: String,
  val tags: List<PocketTagEntity>,
  val attachments: List<PocketAttachmentEntity>,
)

sealed interface PocketAttachmentResult {
  data class Added(val entry: PocketEntry) : PocketAttachmentResult
  data class Rejected(val reason: PocketAttachmentRejection) : PocketAttachmentResult
}

enum class PocketAttachmentRejection {
  ENTRY_NOT_FOUND,
  ATTACHMENT_LIMIT_REACHED,
  INVALID_ATTACHMENT_PATH,
}

/** Coordinates Pocket database records and their journal-directory files. */
class PocketRepository(private val dao: PocketDao) {
  private val writeMutex = Mutex()

  suspend fun listEntries(limit: Int = 100, offset: Int = 0): List<PocketEntry> {
    return dao.getEntries(limit, offset).mapNotNull { readEntry(it.id) }
  }

  suspend fun createEntry(
    title: String = "",
    markdown: String = "",
    createdAt: Long = System.currentTimeMillis(),
  ): PocketEntry {
    val id = UUID.randomUUID().toString()
    val directory = getPocketJournalDirectory(createdAt, id)
      ?: error("Unable to resolve the Pocket journal directory")
    val markdownPath = directory.resolve("entry.md")
    val metadata = PocketEntryEntity(
      id = id,
      createdAt = createdAt,
      updatedAt = createdAt,
      title = title.trim(),
      markdownPath = markdownPath.toString(),
    )

    return writeMutex.withLock {
      Files.createDirectories(directory)
      writeTextAtomically(markdownPath, markdown)
      try {
        dao.insertEntry(metadata)
        readEntry(id) ?: error("Pocket entry was not available after creation")
      } catch (error: Throwable) {
        runCatching { deleteDirectory(directory) }
        throw error
      }
    }
  }

  suspend fun readEntry(id: String): PocketEntry? {
    val metadata = dao.getEntry(id) ?: return null
    val markdownPath = Path.of(metadata.markdownPath)
    val markdown = if (Files.exists(markdownPath)) {
      Files.readString(markdownPath)
    } else {
      ""
    }
    return PocketEntry(
      metadata = metadata,
      markdown = markdown,
      tags = dao.getTags(id),
      attachments = dao.getAttachments(id),
    )
  }

  suspend fun updateEntry(id: String, title: String, markdown: String): PocketEntry? {
    return writeMutex.withLock {
      val current = dao.getEntry(id) ?: return@withLock null
      writeTextAtomically(Path.of(current.markdownPath), markdown)
      dao.insertEntry(
        current.copy(
          title = title.trim(),
          updatedAt = System.currentTimeMillis(),
        )
      )
      readEntry(id)
    }
  }

  suspend fun deleteEntry(id: String): Boolean {
    return writeMutex.withLock {
      val metadata = dao.getEntry(id) ?: return@withLock false
      dao.deleteTags(id)
      dao.deleteAttachments(id)
      dao.deleteEntry(id)
      deleteDirectory(Path.of(metadata.markdownPath).parent)
      true
    }
  }

  suspend fun replaceTags(id: String, tags: Collection<String>) {
    val entities = tags
      .map { it.trim() }
      .filter { it.isNotEmpty() }
      .distinctBy { it.lowercase() }
      .map { PocketTagEntity(id, it, it.lowercase()) }
    writeMutex.withLock {
      dao.deleteTags(id)
      if (entities.isNotEmpty()) dao.insertTags(entities)
    }
  }

  suspend fun addAttachment(
    id: String,
    source: Path,
    relativePath: String,
    mimeType: String,
    markdown: String,
  ): PocketAttachmentResult {
    return writeMutex.withLock {
      val metadata = dao.getEntry(id)
        ?: return@withLock PocketAttachmentResult.Rejected(PocketAttachmentRejection.ENTRY_NOT_FOUND)
      val existing = dao.getAttachments(id)
      if (existing.size >= MAX_POCKET_ATTACHMENTS) {
        return@withLock PocketAttachmentResult.Rejected(PocketAttachmentRejection.ATTACHMENT_LIMIT_REACHED)
      }
      val directory = Path.of(metadata.markdownPath).parent
      val destination = directory.resolve(relativePath).normalize()
      if (destination.parent != directory.normalize()) {
        return@withLock PocketAttachmentResult.Rejected(PocketAttachmentRejection.INVALID_ATTACHMENT_PATH)
      }
      Files.copy(source, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
      writeTextAtomically(Path.of(metadata.markdownPath), markdown)
      dao.insertAttachment(
        PocketAttachmentEntity(
          id = UUID.randomUUID().toString(),
          entryId = id,
          relativePath = relativePath,
          mimeType = mimeType,
          createdAt = System.currentTimeMillis(),
        )
      )
      dao.insertEntry(metadata.copy(updatedAt = System.currentTimeMillis()))
      readEntry(id)?.let(PocketAttachmentResult::Added)
        ?: PocketAttachmentResult.Rejected(PocketAttachmentRejection.ENTRY_NOT_FOUND)
    }
  }

  private fun deleteDirectory(directory: Path?) {
    if (directory == null || !Files.exists(directory)) return
    Files.walk(directory).use { stream ->
      stream.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
    }
  }
}
