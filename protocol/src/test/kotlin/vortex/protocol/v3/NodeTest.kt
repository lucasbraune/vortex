package vortex.protocol.v3

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.assertThrows
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.io.PrintStream
import java.io.PrintWriter
import java.util.*
import kotlin.test.assertEquals

@TestInstance(Lifecycle.PER_CLASS)
class NodeTest {
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
    fun `reading uninitialised node IDs throws exception`() = runTest {
        val node = buildNode(protocol)

        assertThrows<IllegalStateException> { node.nodeId }
        assertThrows<IllegalStateException> { node.nodeIds }
    }

    @Test
    fun `node responds to init request`() = runTest {
        val node = buildNode(protocol)

        val job = node.serve(Dispatchers.Default)
        inputWriter.println("""
            {"src":"c1","dest":"n3","body":{"type":"init","msg_id":123,"node_id":"n3","node_ids":["n1","n2","n3"]}}
        """.trimIndent())
        val response = outputReader.nextLine()
        job.cancelAndJoin()

        val initOkJson = """
            {"src":"n3","dest":"c1","body":{"type":"init_ok","in_reply_to":123}}
        """.trimIndent()
        assertJsonEquals(initOkJson, response)
    }

    @Test
    fun `node exposes IDs from init request`() = runTest {
        val node = buildNode(protocol)

        val job = node.serve(Dispatchers.Default)
        inputWriter.println("""
            {"src":"c1","dest":"n3","body":{"type":"init","msg_id":123,"node_id":"n3","node_ids":["n1","n2","n3"]}}
        """.trimIndent())
        outputReader.nextLine()
        val nodeId = node.nodeId
        val nodeIds = node.nodeIds
        job.cancelAndJoin()

        assertEquals("n3", nodeId)
        assertEquals(listOf("n1", "n2", "n3"), nodeIds)
    }

    @Test
    fun `node RPC returns a response`() = runTest {
        val node = buildNode(protocol)

        val job = node.serve(Dispatchers.Default)
        inputWriter.println("""
            {"src":"c1","dest":"n3","body":{"type":"init","msg_id":1,"node_id":"n3","node_ids":["n1","n2","n3"]}}
        """.trimIndent())
        // Wait for the node's response (presumably init_ok)
        outputReader.nextLine()
        val requestId = node.client.nextMsgId()
        val responseBody = async(Dispatchers.Default) {
            node.client.rpc("c1", Read(requestId, 3))
        }
        // Wait for the node's request (presumably read)
        outputReader.nextLine()
        inputWriter.println("""
            {"src":"c1","dest":"n3","body":{"type":"read_ok","msg_id":56,"in_reply_to":$requestId,"value":4}}
        """.trimIndent())
        val actual = responseBody.await()
        job.cancelAndJoin()

        val expected = ReadOk(msgId = 56, inReplyTo = requestId, value = 4)
        assertEquals(expected, actual)
    }

    @Test
    fun `node RPC throws timeout exception`() = runBlocking {
        val node = buildNode(protocol)

        val job = node.serve(Dispatchers.Default)
        inputWriter.println("""
            {"src":"c1","dest":"n3","body":{"type":"init","msg_id":1,"node_id":"n3","node_ids":["n1","n2","n3"]}}
        """.trimIndent())
        // Wait for the node's response (presumably init_ok)
        outputReader.nextLine()
        val requestId = node.client.nextMsgId()
        val response = async(Dispatchers.Default) {
            withTimeout(100) {
                node.client.rpc("c1", Read(requestId, 3))
            }
        }
        // Wait for the node's request (presumably read)
        outputReader.nextLine()
        delay(200)
        inputWriter.println("""
            {"src":"c1","dest":"n3","body":{"type":"read_ok","msg_id":56,"in_reply_to":$requestId,"value":4}}
        """.trimIndent())

        assertThrows<TimeoutCancellationException> {
            response.await()
        }
        job.cancelAndJoin()
    }

    data class Read(
        override val msgId: Int,
        val key: Int
    ): RequestBody

    data class ReadOk(
        override val msgId: Int?,
        override val inReplyTo: Int,
        val value: Int
    ): ResponseBody

    private companion object {
        val protocol = buildProtocol {
            registerMessageBody("read", Read::class)
            registerMessageBody("read_ok", ReadOk::class)
        }
    }
}
