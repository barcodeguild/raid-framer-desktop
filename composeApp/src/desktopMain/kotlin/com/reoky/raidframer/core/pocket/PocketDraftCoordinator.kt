package com.reoky.raidframer.core.pocket

import com.reoky.raidframer.core.database.PocketDao
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

  fun init(pocketDao: PocketDao) {
    if (!::dao.isInitialized) {
      dao = pocketDao
      repository = PocketRepository(pocketDao)
    }
  }

  suspend fun createDraft(title: String = "", markdown: String = ""): PocketEntry {
    val entry = repository.createEntry(title = title, markdown = markdown)
    _activeDraftId.value = entry.metadata.id
    _activeDraft.value = entry
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
      repository.updateEntry(id, title, markdown)?.let { _activeDraft.value = it }
    }
  }

  suspend fun deleteDraft(id: String): Boolean {
    val deleted = repository.deleteEntry(id)
    if (deleted && _activeDraftId.value == id) {
      _activeDraftId.value = null
      _activeDraft.value = null
    }
    return deleted
  }

  /** Ends the current editor session without deleting the already-persisted entry. */
  fun closeEditorSession() {
    _activeDraftId.value = null
    _activeDraft.value = null
  }

  /** Compatibility alias for callers that intentionally clear the active editor session. */
  fun clearActiveDraft() = closeEditorSession()
}

fun initializePocketDraftCoordinator(dao: PocketDao) {
  PocketDraftCoordinator.init(dao)
}
