package com.marcinmoskala.application

import deckmarkdown.Note
import deckmarkdown.note.DeckParser
import deckmarkdown.note.DefaultParser
import deckmarkdown.api.AnkiApi
import deckmarkdown.api.ApiNote
import deckmarkdown.api.RepositoryApi
import java.io.File

class AnkiConnectorJvm(
    private val api: RepositoryApi = AnkiApi(),
    private val parser: DeckParser = DefaultParser
) {
    suspend fun syncFolder(folderName: String) {
        val notesFile = File(folderName)
        require(notesFile.exists())
        require(notesFile.isDirectory)

        syncMedia("$folderName/media")

        notesFile.listFiles()
            .orEmpty()
            .filterNot { it.isDirectory }
            .forEach { pushFile(it) }
    }

    suspend fun pushFile(file: File) {
        require(file.exists())

        val name = file.name.replace(".md", "")
        val body = file.readText()
        val (text, comment) = separateComment(body)
        val notes = storeOrUpdateNoteText(
            deckName = name,
            noteContent = text.dropMediaFolderPrefix(),
            comment = comment.orEmpty()
        )
        writeToFile(notes, comment, file)
        File("docs/$name.md").writeText(
            parser.markdownWriteNotes(notes)
        )
        File("docs/$name.html").writeText(
            parser.htmlWriteNotes(notes)
        )
    }

    suspend fun pullFile(file: File) {
        val name = file.name.replace(".md", "")
        val body = file.readText()
        val (text, comment) = separateComment(body)
        val noteContent = text.dropMediaFolderPrefix()
        val currentAnkiNotes: List<Note> = api.getNotesInDeck(name).map { parser.apiNoteToNote(it) }
        val currentAnkiNotesByIds: Map<Long?, Note> = currentAnkiNotes.associateBy { it.id }
        val currentFileNotes: List<Note> = parser.parseNotes(noteContent)
        val currentFileNotesIds = currentFileNotes.map { it.id }.toSet()
        val updatedFileNotes = currentFileNotes.mapNotNull { if (it is Note.Text) it else currentAnkiNotesByIds[it.id] }
        val newFileNotes = currentAnkiNotes.filter { it.id !in currentFileNotesIds }
        val allFileNotes = updatedFileNotes + newFileNotes
        writeToFile(allFileNotes, comment, file)
    }

    suspend fun syncMedia(folderName: String) {
        val notesFile = File(folderName)

        if (!notesFile.exists() || !notesFile.isDirectory) return

        notesFile.listFiles()!!.forEach { file ->
            // TODO
//            api.storeMediaFile(file)
        }
    }

    suspend fun readNotesFromDeck(deckName: String): List<Note> =
        api.getNotesInDeck(deckName)
            .map(parser::apiNoteToNote)

    suspend fun writeNotesToFile(deckName: String, file: File, comment: String? = null) {
        val notes = readNotesFromDeck(deckName)
        writeToFile(notes, comment, file)
    }

    private fun writeToFile(notes: List<Note>, comment: String?, file: File) {
        val textAfter = parser.writeNotes(notes)
        val bodyAfter = comment?.let { "$it\n***\n\n" }
            .orEmpty() + textAfter.addMediaFolderPrefix()
        file.writeText(bodyAfter)
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

    suspend fun storeOrUpdateNoteText(deckName: String, noteContent: String, comment: String): List<Note> {
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
        return newCards
    }
}

private fun String.containsOnly(text: String): Boolean = this.replace("*", "").isEmpty()

/**
 * This is done because all media files needs to be located in "media", but later in Anki
 * they are all by default in the folder containing all media
 */
private fun String.dropMediaFolderPrefix(): String = this.replace("\"media/", "\"")

private fun String.addMediaFolderPrefix(): String = this.replace("<img src=\"([\\w.]*)\"".toRegex()) {
    "<img src=\"media/${it.groupValues[1]}\""
}
