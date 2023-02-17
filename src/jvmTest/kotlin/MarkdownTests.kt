package note.markdown

import deckmarkdown.Note
import deckmarkdown.note.BasicParser
import deckmarkdown.note.ClozeParser
import deckmarkdown.note.DefaultParser
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownTests {

    private val parser = DefaultParser

    @Test
    fun `Bold is understood on cloze`() {
        val note = Note.Cloze(1, "This is **text** {{c1::1}}")
        val apiNote = parser.noteToApiNote(note, "ABC", "DEF")
        assertEquals("This is <b>text</b> {{c1::1}}", apiNote.fields[ClozeParser.TEXT_FIELD])
        val processedNote: Note = parser.apiNoteToNote(apiNote)
        assertEquals(processedNote, note)
    }

    @Test
    fun `Italianic is understood on basic`() {
        val note = Note.Basic(1, "This is *front*", "And this *is* back")
        val apiNote = parser.noteToApiNote(note, "ABC", "DEF")
        assertEquals("This is <i>front</i>", apiNote.fields[BasicParser.FRONT_FIELD])
        assertEquals("And this <i>is</i> back", apiNote.fields[BasicParser.BACK_FIELD])
        val processedNote: Note = parser.apiNoteToNote(apiNote)
        assertEquals(processedNote, note)
    }
}
