package deckmarkdown

import deckmarkdown.api.AnkiApi
import deckmarkdown.api.ApiNote
import deckmarkdown.api.RepositoryApi
import deckmarkdown.note.DeckParser
import deckmarkdown.note.DefaultParser
import kotlinx.serialization.Serializable
import note.MarkdownParser
import kotlin.js.JsExport

class AnkiConnector(
    private val api: RepositoryApi = AnkiApi(),
    private val parser: DeckParser = DefaultParser,
) {
    private val headerService = HeaderService()

    suspend fun checkConnection(): Boolean = api.connected()

    suspend fun getDeckNames(): List<String> = api.getDecks()

    fun generateArticle(fileContent: String): FileResult? {
        val (markdown, headerConfig) = headerService.separateHeaderFromFile(fileContent)
        val articleFileName = headerConfig.articleFileName ?: return null
        return FileResult(
            name = articleFileName,
            content = markdown
                .let(parser::parseNotes)
                .let(parser::markdownWriteNotes)
        )
    }

    /*
    Used to generate Anki package in `.apkg` format based on the previously pushed notes.
    To generate a package with the current notes, use `pushFile` first.
     */
    suspend fun exportAnkiPackage(fileContent: String): Boolean {
        val (_, headerConfig) = headerService.separateHeaderFromFile(fileContent)
        val packageDestination = headerConfig.packageDestination ?: return false
        api.exportPackage(headerConfig.deckName, packageDestination, false)
        return true
    }

    suspend fun pushFile(fileContent: String): AnkiConnectorResult {
        val (markdown, headerConfig, originalHeader) = headerService.separateHeaderFromFile(fileContent)
        val result = pushDeck(headerConfig.deckName, markdown)
        return result.copy(
            markdown = originalHeader + result.markdown
        )
    }

    suspend fun pushDeck(deckName: String, markdown: String): AnkiConnectorResult {
        require(deckName.isNotBlank()) { "Deck name must not be blank" }
        require(markdown.isNotBlank()) { "File content must not be blank" }
        return storeOrUpdateNoteText(
            deckName = deckName,
            noteContent = markdown,
            comment = ""
        )
    }

    suspend fun createFile(deckName: String): AnkiConnectorResult {
        val noteContent = api.getNotesInDeck(deckName)
            .map(parser::apiNoteToNote)
            .let(parser::writeNotes)
        val header = headerService.headerToText(HeaderConfig(deckName = deckName))
        val mediaToUpdate = MarkdownParser.findImagesInMarkdown(noteContent)
        return AnkiConnectorResult(
            markdown = header + noteContent,
            mediaToUpdate = mediaToUpdate.toTypedArray()
        )
    }

    suspend fun pullFile(fileContent: String): AnkiConnectorResult {
        val (markdown, headerConfig, originalHeader) = headerService.separateHeaderFromFile(fileContent)
        val pullDeckResult = pullDeck(headerConfig.deckName, markdown)
        return pullDeckResult.copy(markdown = originalHeader + pullDeckResult.markdown)
    }

    suspend fun pullDeck(deckName: String, currentMarkdown: String? = null): AnkiConnectorResult {
        val currentAnkiNotes: List<Note> = api.getNotesInDeck(deckName)
            .map(parser::apiNoteToNote)
        val newNotes = if (currentMarkdown == null) {
            currentAnkiNotes
        } else {
            val currentAnkiNotesByIds: Map<Long?, Note> = currentAnkiNotes.associateBy { it.id }
            val currentFileNotes: List<Note> = parser.parseNotes(currentMarkdown)
            val currentFileNotesIds = currentFileNotes.map { it.id }.toSet()
            val updatedFileNotes = currentFileNotes.mapNotNull { if (it is Note.Text || it.id == null) it else currentAnkiNotesByIds[it.id] }
            val newFileNotes = currentAnkiNotes.filter { it.id !in currentFileNotesIds }
            updatedFileNotes + newFileNotes
        }

        val noteContent = parser.writeNotes(newNotes)
        val mediaToUpdate = MarkdownParser.findImagesInMarkdown(noteContent)
        return AnkiConnectorResult(
            markdown = noteContent,
            mediaToUpdate = mediaToUpdate.toTypedArray()
        )
    }

    private suspend fun storeOrUpdateNoteText(deckName: String, noteContent: String, comment: String): AnkiConnectorResult {
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
        if (removedCardIds.isNotEmpty()) {
            api.deleteNotes(removedCardIds.toSet())
        }

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

        val mediaToUpdate = MarkdownParser.findImagesInMarkdown(noteContent)

        return AnkiConnectorResult(
            markdown = parser.writeNotes(newCards),
            ankiModificationsCounts = AnkiConnectorResult.ModificationsCounts(
                addedCount = addedCount,
                updatedCount = updatedCount,
                removedCount = removedCount,
                unchangedCount = leftUnchanged,
            ),
            mediaToUpdate = mediaToUpdate.toTypedArray()
        )
    }

    suspend fun storeMediaFile(fileName: String, fileContentBase64: String): Boolean =
        api.storeMediaFile(fileName, fileContentBase64)

    suspend fun retrieveMediaFile(fileName: String): String =
        api.retrieveMediaFile(fileName)
}

object FileMustHaveHeader : Exception("File must have header")
object HeaderMustSpecifyName : Exception("Header must specify name")

@JsExport
@Serializable
class HeaderConfig(
    val deckName: String,
    val articleFileName: String? = null,
    val packageDestination: String? = null,
//    val generalComment: String? = null,
//    val resourcesFile:String? = null
)

@Suppress("ArrayInDataClass")
@JsExport
data class AnkiConnectorResult(
    val markdown: String,
    val ankiModificationsCounts: ModificationsCounts? = null,
    val mediaToUpdate: Array<String> = arrayOf(),
) {
    class ModificationsCounts(
        val addedCount: Int = 0,
        val updatedCount: Int = 0,
        val removedCount: Int = 0,
        val unchangedCount: Int = 0,
    )
}

@JsExport
data class FileResult(
    val name: String,
    val content: String,
)

//private fun String.dropMediaFolderPrefix(): String = this.replace("\"media/", "\"")
//
//private fun String.addMediaFolderPrefix(): String = this.replace("<img src=\"([\\w.]*)\"".toRegex()) {
//    "<img src=\"media/${it.groupValues[1]}\""
//}
