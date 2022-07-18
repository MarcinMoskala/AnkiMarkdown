//import ankimarkdown.fakes.FakeAnkiApi
//import kotlinx.coroutines.runBlocking
//import deckmarkdown.note.DefaultParser
//import org.junit.jupiter.api.BeforeEach
//import kotlin.test.Test
//import java.io.File
//import kotlin.test.assertEquals
//
//class PushFileTests {
//
//    private lateinit var apiFake: FakeAnkiApi
//    private lateinit var ankiMarkup: AnkiConnector
//
//    @BeforeEach
//    fun setup() {
//        apiFake = FakeAnkiApi()
//        ankiMarkup = AnkiConnector(
//            api = apiFake, parser = DefaultParser
//        )
//    }
//
//    @Test
//    fun `should not update when changes not needed`() = fakeFileTest { file ->
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
//
//        // when synced for the first time
//        ankiMarkup.pushFile(file)
//
//        // then added two noted
//        assertEquals(apiFake.addUsedCount, 2)
//        assertEquals(apiFake.removeUsedCount, 0)
//        assertEquals(apiFake.updateUsedCount, 0)
//
//        // when synced again without changes
//        ankiMarkup.pushFile(file)
//
//        // then no additional operations were made
//        assertEquals(apiFake.addUsedCount, 2)
//        assertEquals(apiFake.removeUsedCount, 0)
//        assertEquals(apiFake.updateUsedCount, 0)
//    }
//
//    @Test
//    fun `should update when changes needed`() = fakeFileTest { file ->
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
//
//        // when synced for the first time
//        ankiMarkup.pushFile(file)
//
//        // then added two noted
//        assertEquals(apiFake.addUsedCount, 2)
//        assertEquals(apiFake.removeUsedCount, 0)
//        assertEquals(apiFake.updateUsedCount, 0)
//
//        // when synced with changed file
//        file.writeText(
//            """
//@1
//qa: Some queXXXstion
//aq: Some answer
//
//@2
//A {{c1::B}} C {{c2::D}} E
//        """.trimIndent()
//        )
//        ankiMarkup.pushFile(file)
//
//        // then added two noted
//        assertEquals(apiFake.addUsedCount, 2)
//        assertEquals(apiFake.removeUsedCount, 0)
//        assertEquals(apiFake.updateUsedCount, 1)
//    }
//}
