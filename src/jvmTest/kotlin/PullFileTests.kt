//import fakes.FakeAnkiApi
//import kotlinx.coroutines.runBlocking
//import deckmarkdown.note.ClozeParser
//import deckmarkdown.note.DefaultParser
//import org.junit.jupiter.api.BeforeEach
//import kotlin.test.Test
//import java.io.File
//import kotlin.test.assertEquals
//
//class PullFileTests {
//
//    private lateinit var apiFake: FakeAnkiApi
//    private lateinit var ankiMarkup: AnkiConnector
//
//    @Before
//    fun setup() {
//        apiFake = FakeAnkiApi()
//        ankiMarkup = AnkiConnector(
//            api = apiFake, parser = DefaultParser
//        )
//    }
//
//    @Test
//    fun `should update when changes made`() = fakeFileTest { file ->
//        // given
//        file.writeText(
//            """
//@1
//qa: Some question
//aq: Some answer
//
//@2
//A {{c1::B}} C {{c2::D}} E
//        """.trimIndent()
//        )
//        ankiMarkup.pushFile(file)
//
//        // when note changed
//        val aNote = apiFake.notes.first { it.noteId == 2L }
//        val changedNote = aNote.copy(fields = aNote.fields + (ClozeParser.TEXT_FIELD to "A {{c1::B}} C D E"))
//        apiFake.updateNoteFields(changedNote)
//
//        // when pulling notes
//        ankiMarkup.pullFile(file)
//
//        // then file has changed
//        assertEquals("""
//@1
//qa: Some question
//aq: Some answer
//
//@2
//A {{c1::B}} C D E
//        """.trimIndent(), file.readText())
//    }
//
//    @Test
//    fun `non-cards are ignored`() = fakeFileTest { file ->
//        // given
//        file.writeText(
//            """
//@1
//qa: Some question
//aq: Some answer
//
//AAA
//BBB
//CCC
//
//@2
//A {{c1::B}} C {{c2::D}} E
//        """.trimIndent()
//        )
//        ankiMarkup.pushFile(file)
//
//        // when pulling notes
//        ankiMarkup.pullFile(file)
//
//        // then file has changed
//        assertEquals("""
//@1
//qa: Some question
//aq: Some answer
//
//AAA
//BBB
//CCC
//
//@2
//A {{c1::B}} C {{c2::D}} E
//        """.trimIndent(), file.readText())
//    }
//}
