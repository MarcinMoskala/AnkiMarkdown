package deckmarkdown

import deckmarkdown.note.DeckParser
import deckmarkdown.note.DefaultParser
import deckmarkdown.api.AnkiApi
import deckmarkdown.api.ApiNote
import deckmarkdown.api.RepositoryApi
import kotlin.js.JsExport

class AnkiConnector(
    private val api: RepositoryApi = AnkiApi(),
    private val parser: DeckParser = DefaultParser
) {

    suspend fun pushDeck(deckName: String, markdown: String): AnkiConnectorResult {
        require(deckName.isNotBlank())
        require(markdown.isNotBlank())
        val (text, comment) = separateComment(markdown)
        return storeOrUpdateNoteText(
            deckName = deckName,
            noteContent = text.dropMediaFolderPrefix(),
            comment = comment.orEmpty()
        )
    }

    suspend fun pullDeckToExisting(deckName: String, currentMarkdown: String): AnkiConnectorResult {
        val (text, comment) = separateComment(currentMarkdown)
        val noteContent = text.dropMediaFolderPrefix()
        val currentAnkiNotes: List<Note> = api.getNotesInDeck(deckName).map { parser.apiNoteToNote(it) }
        val currentAnkiNotesByIds: Map<Long?, Note> = currentAnkiNotes.associateBy { it.id }
        val currentFileNotes: List<Note> = parser.parseNotes(noteContent)
        val currentFileNotesIds = currentFileNotes.map { it.id }.toSet()
        val updatedFileNotes = currentFileNotes.mapNotNull { if (it is Note.Text) it else currentAnkiNotesByIds[it.id] }
        val newFileNotes = currentAnkiNotes.filter { it.id !in currentFileNotesIds }
        val allFileNotes = updatedFileNotes + newFileNotes
        return AnkiConnectorResult(
            updatedMarkdown = notesToMarkdown(allFileNotes, comment)
        )
    }

    suspend fun pullDeck(deckName: String, comment: String? = null): AnkiConnectorResult {
        val notes = api.getNotesInDeck(deckName)
            .map(parser::apiNoteToNote)
        return AnkiConnectorResult(
            updatedMarkdown = notesToMarkdown(notes, comment)
        )
    }

    private fun notesToMarkdown(notes: List<Note>, comment: String?): String {
        val textAfter = parser.writeNotes(notes)
        val bodyAfter = comment?.let { "$it\n***\n\n" }
            .orEmpty() + textAfter.addMediaFolderPrefix()
        return bodyAfter
    }

    private data class TextAndComment(val text: String, val comment: String? = null)

    private fun separateComment(text: String): TextAndComment {
        val possibleIntroTextWithSeparator = text.substringBefore("\n\n")
        val lastLine = possibleIntroTextWithSeparator.substringAfterLast("\n").trimEnd()
        val isHeaderEnding = lastLine.containsOnly("*") && lastLine.length >= 3
        if (!isHeaderEnding) return TextAndComment(text)
        val introText = possibleIntroTextWithSeparator.substringBeforeLast("\n")
        return TextAndComment(text.substringAfter("\n\n"), introText)
    }

    suspend fun storeOrUpdateNoteText(deckName: String, noteContent: String, comment: String): AnkiConnectorResult {
        if (!api.connected()) {
            error("This function requires opened Anki with installed Anki Connect plugin. Details in ReadMe.md")
        }

        api.createDeck(deckName)
        val notes = parser.parseNotes(noteContent)

        if (!api.connected()) {
            error("This function requires opened Anki with installed Anki Connect plugin. Details in ReadMe.md")
        }

        val currentCards = api.getNotesInDeck(deckName)
        val currentIds = currentCards.map { it.noteId }

        val removedCardIds = currentIds - notes.mapNotNull { it.id }.toSet()
        api.deleteNotes(removedCardIds.toSet())

        val removedCount = removedCardIds.size
        var addedCount = 0
        var updatedCount = 0
        var leftUnchanged = 0
        val newCards = notes.map(fun(note: Note): Note {
            if (note is Note.Text) return note
            val apiNote: ApiNote = parser.noteToApiNote(note, deckName, comment)
            val newApiNote = if (apiNote.hasId && apiNote.noteId in currentIds) {
                val current: Note? = currentCards.find { it.noteId == apiNote.noteId }
                    ?.let { parser.apiNoteToNote(it) }
                if (note == current) {
                    leftUnchanged++
                    apiNote
                } else {
                    updatedCount++
                    api.updateNoteFields(apiNote)
                }
            } else {
                addedCount++
                api.addNote(apiNote)
            }
            return parser.apiNoteToNote(newApiNote)
        })

        println("In deck $deckName added $addedCount, updated $updatedCount, removed $removedCount, letf unchanged: $leftUnchanged")
        return AnkiConnectorResult(
            updatedMarkdown = notesToMarkdown(newCards, comment),
            addedCount = addedCount,
            updatedCount = updatedCount,
            removedCount = removedCount,
            unchangedCount = leftUnchanged,
        )
    }
}

@JsExport
class AnkiConnectorResult(
    val updatedMarkdown: String,
    val addedCount: Int = 0,
    val updatedCount: Int = 0,
    val removedCount: Int = 0,
    val unchangedCount: Int = 0,
)

private fun String.containsOnly(text: String): Boolean = this.replace("*", "").isEmpty()

/**
 * This is done because all media files needs to be located in "media", but later in Anki
 * they are all by default in the folder containing all media
 */
private fun String.dropMediaFolderPrefix(): String = this.replace("\"media/", "\"")

private fun String.addMediaFolderPrefix(): String = this.replace("<img src=\"([\\w.]*)\"".toRegex()) {
    "<img src=\"media/${it.groupValues[1]}\""
}