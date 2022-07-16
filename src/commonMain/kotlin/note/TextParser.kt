package deckmarkdown.note

import deckmarkdown.Note
import deckmarkdown.Note.*
import deckmarkdown.api.ApiNote

/*
 Text note represents pure text without that is not translated to any real note in Anki.

 Parser must be last because otherwise will consume everything
 See recognize - always returns true so everything is accepted as a text
 */
object TextParser : FullNoteProcessor<Text> {
    override fun handlesNote(note: Note): Boolean = note is Text

    /*
     * Everything that hasn't been accepted by any other kind of notes should be treated as a text
     */
    override fun recognize(text: String): Boolean = true

    override fun parse(id: Long?, noteText: String): Text = Text(noteText)

    override fun render(note: Text): String = note.text

    /*
     *  Nothing that comes from API can be text
     */
    override fun recognizeApiNote(apiNote: ApiNote): Boolean = false

    override fun cardToAnkiNote(note: Text, deckName: String, comment: String): ApiNote =
        error("Should never be used as Text cannot be sent to API")

    override fun ankiNoteToCard(apiNote: ApiNote): Text =
        error("Should never be used as Text cannot come from API")

    override fun toHtml(note: Text): String = note.text

    override fun toMarkdown(note: Text): String = note.text
}

