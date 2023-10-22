package vortex.protocol.v4

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.io.PrintStream
import java.io.PrintWriter
import java.util.Scanner

@TestInstance(Lifecycle.PER_CLASS)
class ProtocolTest {
    private val stdout: PrintStream = System.out
    private val stdin: InputStream = System.`in`
    private lateinit var inputWriter: PrintWriter
    private lateinit var outputReader: Scanner

    @BeforeEach
    fun resetTestIO() {
        inputWriter = run<PrintWriter> {
            val source = PipedOutputStream()
            val sink = PipedInputStream(source)
            System.setIn(sink)
            PrintWriter(source, true)
        }
        outputReader = run<Scanner> {
            val source = PipedOutputStream()
            val sink = PipedInputStream(source)
            System.setOut(PrintStream(source, true))
            Scanner(sink)
        }
    }

    @AfterEach
    fun closeTestIO() {
        inputWriter.close()
        outputReader.close()
    }

    @AfterAll
    fun restoreStandardIO() {
        System.setOut(stdout)
        System.setIn(stdin)
    }

    @Test
    fun `send writes JSON to STDOUT`() {
        send(helloMessage)
        val actual = outputReader.nextLine()

        assertEquals(helloMessageJson, actual)
    }

    @Test
    fun `receiveMessage reads JSON from STDIN`() {
        inputWriter.println(helloMessageJson)
        val actual = receiveMessage()

        assertEquals(helloMessage, actual)
    }

    @Test
    fun `receiveMessage returns null on end of input`() {
        inputWriter.close()
        val actual = receiveMessage()

        assertNull(actual)
    }




    companion object {
        val helloMessage = Message("n1", "c1", buildJsonObject { put("hello", "world") })
        val helloMessageJson = """
            {"src":"n1","dest":"c1","body":{"hello":"world"}}
        """.trimIndent()
    }
}
