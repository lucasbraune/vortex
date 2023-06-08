package vertex.protocol.v2

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class NodeTest {
    private val outContent = ByteArrayOutputStream()
    private val inContent = "Lucas".byteInputStream()
    private val originalOut = System.out
    private val originalIn = System.`in`

    @BeforeEach
    fun setUpStreams() {
        System.setOut(PrintStream(outContent))
        System.setIn(inContent)
    }

    @AfterEach
    fun restoreStreams() {
        System.setOut(originalOut)
        System.setIn(originalIn)
    }
    
    @Test
    fun foo() = runBlocking {
        val originalOut = System.out
        val newOut = ByteArrayOutputStream()
        System.setOut(PrintStream(newOut))

        withContext(Dispatchers.IO) {
            val name = readLine()
            println("Hello, $name")
        }

        System.setOut(originalOut)
        withContext(Dispatchers.IO) {
            val name = readLine()
            println("Goodbye, $name")
            print(newOut.toString())
        }
    }
}
