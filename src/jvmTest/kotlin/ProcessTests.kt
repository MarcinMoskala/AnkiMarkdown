package note

import deckmarkdown.note.DefaultParser
import kotlin.test.Test
import kotlin.test.assertEquals

class ProcessTests {

    val parser = DefaultParser

    @Test
    fun `Basically formatted text stays the same after reading and writing`() {
        val texts = listOf(
            """
                Lorem {{c1::ipsum}} est
            """,
            """
                This is text {{c1::1}}
            
                qa: My question
                aq: My answer
            
                q: Question 2
                a: Answer 2
            
                And this {{c1::text}} number is {{c2::2}}
                
                @10
                L: AAA
                * A
                * B
                Comment
                * C
            """
        ).map { it.trimIndent() }

        for (text in texts) {
            val notes = parser.parseNotes(text)
            val processed = parser.writeNotes(notes)
            assertEquals(text, processed)
        }
    }
}