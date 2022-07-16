import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

inline fun <reified T : Throwable> assertThrows(expectedMessage: String? = null, operation: () -> Unit) {
    val exception = try {
        operation()
        null
    } catch (t: Throwable) {
        t
    }

    assertNotNull(exception) { "No error thrown. Expecting ${T::class.qualifiedName}" }
    assert(exception is T) { "Caught error is $exception when it should be of type ${T::class.qualifiedName}" }
    if (expectedMessage != null) {
        assertEquals(
            expectedMessage,
            exception.message,
            "The message is <${exception.message}> when it should be <$expectedMessage>"
        )
    }
}

private val FAKE_FILE_NAME = "some::deck"

fun fakeFileTest(body: suspend (File) -> Unit) = runBlocking {
    val file = File("$FAKE_FILE_NAME.md")
    @Suppress("BlockingMethodInNonBlockingContext") file.createNewFile()
    try {
        body(file)
    } finally {
        file.delete()
    }
}