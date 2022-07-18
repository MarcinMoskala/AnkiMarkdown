package ankimarkdown.fakes

import deckmarkdown.api.ApiNote
import deckmarkdown.api.ApiNoteModel
import deckmarkdown.api.RepositoryApi

class FakeAnkiApi : RepositoryApi {
    var notes = listOf<ApiNote>()
        private set
    var addUsedCount = 0
        private set
    var updateUsedCount = 0
        private set
    var removeUsedCount = 0
        private set
    var nextId = 0L

    fun clean() {
        this.notes = emptyList()
        addUsedCount = 0
        updateUsedCount = 0
        removeUsedCount = 0
        nextId = 0L
    }

    fun hasNotes(vararg notes: ApiNote) {
        this.notes = notes.toList()
    }

    override suspend fun connected(): Boolean = true

    override suspend fun addNote(apiNote: ApiNote): ApiNote {
        addUsedCount++
        val apiNote =
            if (apiNote.hasId) apiNote
            else apiNote.copy(noteId = nextId++)
        notes = notes + apiNote
        return apiNote
    }

    override suspend fun updateNoteFields(apiNote: ApiNote): ApiNote {
        updateUsedCount++
        notes = notes.filter { it.noteId != apiNote.noteId } + apiNote
        return apiNote
    }

    override suspend fun getNotesInDeck(deckName: String): List<ApiNote> =
        notes.filter { it.deckName == deckName }

    override suspend fun deleteNotes(ids: Set<Long>) {
        removeUsedCount += ids.size
        notes = notes.filter { it.noteId !in ids }
    }

    override suspend fun createDeck(name: String) {
    }

    override suspend fun removeDeck(name: String) {
    }

    override suspend fun getDecks(): List<String> {
        TODO()
    }

    override suspend fun getModelsNames(): List<String> {
        TODO()
    }

    override suspend fun addModel(model: ApiNoteModel) {
        TODO()
    }

//    override suspend fun storeMediaFile(file: File) {
//        TODO()
//    }
}