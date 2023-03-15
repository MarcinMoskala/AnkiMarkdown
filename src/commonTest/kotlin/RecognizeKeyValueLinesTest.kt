package ankimarkdown

import deckmarkdown.recognizeKeyValueLines
import kotlin.test.Test
import kotlin.test.assertEquals

class RecognizeKeyValueLinesTest {
    @Test
    fun simpleTest() = testRecognizeKeyValueLinesResult(
        input = """
            a: This is a
            b: This is b
        """.trimIndent(),
        expectedOutput = mapOf(
            "a" to "This is a",
            "b" to "This is b",
        )
    )

    @Test
    fun complexKeyTest() = testRecognizeKeyValueLinesResult(
        input = """
            a-b_c: This is a
            dEF: This is b
        """.trimIndent(),
        expectedOutput = mapOf(
            "a-b_c" to "This is a",
            "dEF" to "This is b",
        )
    )

    @Test
    fun multilineValueTest() = testRecognizeKeyValueLinesResult(
        input = """
            a-b_c: This is
            line first
            and this is
            the next one
            dEF: This is b
            {}{}
            ```
            val a = 10
            ```
        """.trimIndent(),
        expectedOutput = mapOf(
            "a-b_c" to "This is\nline first\nand this is\nthe next one",
            "dEF" to "This is b\n{}{}\n```\nval a = 10\n```",
        )
    )

    @Test
    fun fakeKeysTest() = testRecognizeKeyValueLinesResult(
        input = """
            a: A
            b :B
            c d:e
            f:g
            :h
        """.trimIndent(),
        expectedOutput = mapOf(
            "a" to "A\nb :B\nc d:e\nf:g\n:h",
        )
    )

    @Test
    fun noStartingKeyGivesNull() = testRecognizeKeyValueLinesResult(
        input = """
            Ala
            a: ma
            kota
            """.trimIndent(),
        expectedOutput = null
    )

    private fun testRecognizeKeyValueLinesResult(input: String, expectedOutput: Map<String, String>?) {
        assertEquals(expectedOutput, input.recognizeKeyValueLines())
    }
}