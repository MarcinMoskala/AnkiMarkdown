package deckmarkdown

import deckmarkdown.note.DeckParser
import deckmarkdown.note.DefaultParser
import deckmarkdown.api.AnkiApi
import deckmarkdown.api.ApiNote
import deckmarkdown.api.RepositoryApi
import kotlinx.serialization.Serializable
import net.mamoe.yamlkt.Yaml
import kotlin.js.JsExport

class AnkiConnector(
    private val api: RepositoryApi = AnkiApi(),
    private val parser: DeckParser = DefaultParser
) {
    suspend fun checkConnection(): Boolean = api.connected()

    suspend fun getDeckNames(): List<String> = api.getDecks()

    fun generateArticle(fileName: String, fileContent: String): FileResult? {
        val (markdown, headerConfig) = separateHeaderFromFile(fileContent)
        val articleFileName = headerConfig?.articleFileName ?: return null
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
    suspend fun exportAnkiPackage(fileName: String, fileContent: String): Boolean {
        val (markdown, headerConfig) = separateHeaderFromFile(fileContent)
        val packageDestination = headerConfig?.packageDestination ?: return false
        val deckName = chooseDeckName(headerConfig, fileName)
        api.exportPackage(deckName, packageDestination, false)
        return true
    }

    suspend fun pushFile(fileName: String, fileContent: String): AnkiConnectorResult {
        val (markdown, headerConfig, originalHeader) = separateHeaderFromFile(fileContent)
        val deckName = chooseDeckName(headerConfig, fileName)
        val result = pushDeck(deckName, markdown)
        return AnkiConnectorResult(
            markdown = originalHeader + result.markdown
        )
    }

    suspend fun pullFile(deckName: String): AnkiConnectorResult {
        val markdown = api.getNotesInDeck(deckName)
            .map(parser::apiNoteToNote)
            .let(parser::writeNotes)
        val header = headerToText(HeaderConfig(deckName = deckName))
        return AnkiConnectorResult(
            markdown = header + markdown
        )
    }

    suspend fun pullDeckToExistingFile(fileName: String, fileContent: String): AnkiConnectorResult {
        val (markdown, headerConfig) = separateHeaderFromFile(fileContent)
        val deckName = chooseDeckName(headerConfig, fileName)
        return pullDeckToExisting(deckName, markdown)
    }

    suspend fun pushDeck(deckName: String, markdown: String): AnkiConnectorResult {
        require(deckName.isNotBlank())
        require(markdown.isNotBlank())
        return storeOrUpdateNoteText(
            deckName = deckName,
            noteContent = markdown,
            comment = ""
        )
    }

    suspend fun pullDeck(deckName: String): AnkiConnectorResult =
        AnkiConnectorResult(
            markdown = api.getNotesInDeck(deckName)
                .map(parser::apiNoteToNote)
                .let(parser::writeNotes)
        )

    suspend fun pullDeckToExisting(deckName: String, currentMarkdown: String): AnkiConnectorResult {
        val currentAnkiNotes: List<Note> = api.getNotesInDeck(deckName)
            .map(parser::apiNoteToNote)
        val currentAnkiNotesByIds: Map<Long?, Note> = currentAnkiNotes.associateBy { it.id }
        val currentFileNotes: List<Note> = parser.parseNotes(currentMarkdown)
        val currentFileNotesIds = currentFileNotes.map { it.id }.toSet()
        val updatedFileNotes = currentFileNotes.mapNotNull { if (it is Note.Text || it.id == null) it else currentAnkiNotesByIds[it.id] }
        val newFileNotes = currentAnkiNotes.filter { it.id !in currentFileNotesIds }
        val allFileNotes = updatedFileNotes + newFileNotes
        return AnkiConnectorResult(
            markdown = parser.writeNotes(allFileNotes),
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

        println("In deck $deckName added $addedCount, updated $updatedCount, removed $removedCount, letf unchanged: $leftUnchanged")
        return AnkiConnectorResult(
            markdown = parser.writeNotes(newCards),
            ankiModificationsCounts = AnkiConnectorResult.ModificationsCounts(
                addedCount = addedCount,
                updatedCount = updatedCount,
                removedCount = removedCount,
                unchangedCount = leftUnchanged,
            )
        )
    }

    data class HeaderSeparationResult(
        val markdown: String,
        val headerConfig: HeaderConfig?,
        val originalHeader: String?,
    )

    private fun separateHeaderFromFile(fileContent: String): HeaderSeparationResult {
        val matchResult = Regex("""^(---([\w\W]*)\n---[\n]+)([\w\W]*)""")
            .find(fileContent.trim())
            ?: return HeaderSeparationResult(fileContent, null, null)
        val markdown = matchResult.groupValues[3]
        val originalHeader = matchResult.groupValues[1]
        val headerContent = matchResult.groupValues[2]
        val headerConfig = Yaml.decodeFromString(HeaderConfig.serializer(), headerContent)
        return HeaderSeparationResult(markdown, headerConfig, originalHeader)
    }

    private fun headerToText(headerConfig: HeaderConfig): String =
        Yaml.encodeToString(HeaderConfig.serializer(), headerConfig)
            .let { "---\n$it\n---\n\n" }

    private fun chooseDeckName(headerConfig: HeaderConfig?, fileName: String) =
        headerConfig?.deckName
            ?: fileName
                .replace("__", "::")
                .substringBefore(".")
}

@JsExport
@Serializable
class HeaderConfig(
    val deckName: String? = null,
    val articleFileName: String? = null,
    val packageDestination: String? = null,
//    val generalComment: String? = null,
//    val resourcesFile:String? = null
)

@JsExport
data class AnkiConnectorResult(
    val markdown: String,
    val ankiModificationsCounts: ModificationsCounts? = null
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
