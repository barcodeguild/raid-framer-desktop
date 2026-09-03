package com.reoky.raidframer.core.pocket

import com.reoky.raidframer.core.database.PocketDao
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import java.nio.file.Path
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Shared lifecycle for the single Pocket editor session currently being edited.
 *
 * Entries are persisted immediately when created, so "draft" describes the active editor
 * session rather than a separate database state. Closing the editor ends that session while
 * leaving the saved Pocket entry available in the journal.
 */
object PocketDraftCoordinator {
  private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
  private lateinit var dao: PocketDao
  private lateinit var repository: PocketRepository

  private val _activeDraftId = MutableStateFlow<String?>(null)
  val activeDraftId: StateFlow<String?> = _activeDraftId.asStateFlow()

  private val _activeDraft = MutableStateFlow<PocketEntry?>(null)
  val activeDraft: StateFlow<PocketEntry?> = _activeDraft.asStateFlow()

  private val _entries = MutableStateFlow<List<PocketEntry>>(emptyList())
  val entries: StateFlow<List<PocketEntry>> = _entries.asStateFlow()

  fun init(pocketDao: PocketDao) {
    if (!::dao.isInitialized) {
      dao = pocketDao
      repository = PocketRepository(pocketDao)
      scope.launch { refreshEntries() }
    }
  }

  suspend fun createDraft(title: String = "", markdown: String = ""): PocketEntry {
    val entry = repository.createEntry(title = title, markdown = markdown)
    _activeDraftId.value = entry.metadata.id
    _activeDraft.value = entry
    refreshEntries()
    return entry
  }

  suspend fun openDraft(id: String): PocketEntry? {
    val entry = repository.readEntry(id)
    _activeDraftId.value = entry?.metadata?.id
    _activeDraft.value = entry
    return entry
  }

  fun updateDraft(title: String, markdown: String) {
    val id = _activeDraftId.value ?: return
    scope.launch {
      val knownPlayers = PlayerCacheInteractor.realPlayers.value.map { it.name }
      repository.updateEntryWithTags(id, title, markdown, knownPlayers)?.let {
        _activeDraft.value = it
        refreshEntries()
      }
    }
  }

  suspend fun addAttachment(
    source: Path,
    relativePath: String,
    mimeType: String,
    markdown: String,
  ): PocketAttachmentResult {
    val id = _activeDraftId.value
      ?: return PocketAttachmentResult.Rejected(PocketAttachmentRejection.ENTRY_NOT_FOUND)
    val result = repository.addAttachment(id, source, relativePath, mimeType, markdown)
    if (result is PocketAttachmentResult.Added) {
      _activeDraft.value = result.entry
      refreshEntries()
    }
    return result
  }

  suspend fun nextAttachmentName(id: String): String? = repository.nextAttachmentName(id)

  /**
   * Returns the id of the most recently updated entry when it was touched within [withinMillis]
   * (a "recent draft" that new screenshots should append to), or null to start a new entry.
   */
  suspend fun resolveRecentDraftId(withinMillis: Long = 30 * 60_000L): String? {
    val now = System.currentTimeMillis()
    return _entries.value
      .maxByOrNull { it.metadata.updatedAt }
      ?.takeIf { it.metadata.updatedAt >= now - withinMillis }
      ?.metadata?.id
  }

  suspend fun readEntryMarkdown(id: String): String? = repository.readEntry(id)?.markdown

  /**
   * Adds an attachment to a specific entry (not just the active editor session) and promotes
   * that entry to be the active draft so the editor reflects the new attachment.
   */
  suspend fun addAttachmentToEntry(
    entryId: String,
    source: Path,
    relativePath: String,
    mimeType: String,
    markdown: String,
  ): PocketAttachmentResult {
    val result = repository.addAttachment(entryId, source, relativePath, mimeType, markdown)
    if (result is PocketAttachmentResult.Added) {
      _activeDraftId.value = entryId
      _activeDraft.value = result.entry
      refreshEntries()
    }
    return result
  }

  suspend fun removeAttachment(attachmentId: String, markdown: String): PocketEntry? {
    val id = _activeDraftId.value ?: return null
    val entry = repository.removeAttachment(id, attachmentId, markdown) ?: return null
    _activeDraft.value = entry
    refreshEntries()
    return entry
  }


  suspend fun deleteDraft(id: String): Boolean {
    val deleted = repository.deleteEntry(id)
    if (deleted && _activeDraftId.value == id) {
      _activeDraftId.value = null
      _activeDraft.value = null
    }
    refreshEntries()
    return deleted
  }

  /** Ends the current editor session without deleting the already-persisted entry. */
  fun closeEditorSession() {
    _activeDraftId.value = null
    _activeDraft.value = null
  }

  /** Compatibility alias for callers that intentionally clear the active editor session. */
  fun clearActiveDraft() = closeEditorSession()

  suspend fun refreshEntries() {
    if (::repository.isInitialized) {
      _entries.value = repository.listEntries()
    }
  }

  suspend fun getEntriesByTag(normalizedTag: String, limit: Int = 12): List<PocketEntry> {
    if (!::repository.isInitialized) return emptyList()
    return repository.listEntriesByTag(normalizedTag, limit)
  }
}

fun initializePocketDraftCoordinator(dao: PocketDao) {
  PocketDraftCoordinator.init(dao)
}
