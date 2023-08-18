package vortex.protocol.v3

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.PrintStream

class ProtocolTest {
    private val stderr: PrintStream = System.err
    private val stdout: PrintStream = System.out
    private val stdin: InputStream = System.`in`

    @AfterEach
    fun restoreStreams() {
        System.setErr(stderr)
        System.setOut(stdout)
        System.setIn(stdin)
    }

    @Test
    fun `test log writes to STDERR`() {
        val err = ByteArrayOutputStream()
        System.setErr(PrintStream(err))
        val loggedMessage = "Debugging info"

        log(loggedMessage)

        val actual = err.toString()
        assertEquals(loggedMessage + "\n", actual)
    }

    @ParameterizedTest
    @MethodSource("serializedMessages")
    fun `test sendMessage writes JSON to STDOUT`(message: Message, json: String) {
        val out = ByteArrayOutputStream()
        System.setOut(PrintStream(out))

        protocol.sendMessage(message)

        val actual = out.toString()
        assertJsonEquals(json, actual)
    }

    @ParameterizedTest
    @MethodSource("serializedMessages")
    fun `test readMessage reads JSON from STDIN`(message: Message, json: String) {
        val input = json.byteInputStream()
        System.setIn(input)

        val actual = protocol.receiveMessage()

        assertEquals(message, actual)
    }

    data class Read(
        val key: Int,
        override val msgId: Int?,
    ) : MessageBody {
        override val inReplyTo: Int? = null
    }

    private companion object {
        val protocol = buildProtocol {
            registerMessageBody("read", Read::class)
        }

        @JvmStatic
        fun serializedMessages() = listOf(
            Arguments.of(
                Message(
                    src = "n1",
                    dest = "c1",
                    body = Init(msgId = 1, nodeId = "n3", nodeIds = listOf("n1", "n2", "n3"))
                ),
                """
                    {"src":"n1","dest":"c1","body":{"type":"init","msg_id":1,"node_id":"n3","node_ids":["n1","n2","n3"]}}
                """.trimIndent()
            ),
            Arguments.of(
                Message(
                    src = "n1",
                    dest = "c1",
                    body = InitOk(inReplyTo = 1),
                ),
                """
                    {"src":"n1","dest":"c1","body":{"type":"init_ok","in_reply_to":1}}
                """.trimIndent()
            ),
            Arguments.of(
                Message(
                    src = "n2",
                    dest = "c1",
                    body = Read(msgId = 123, key = 3)
                ),
                """
                    {"src":"n2","dest":"c1","body":{"type":"read","msg_id":123,"key":3}}
                """.trimIndent()
            ),
            Arguments.of(
                Message(
                    src = "n5",
                    dest = "c1",
                    body = RpcError(
                        inReplyTo = 5,
                        code = 11,
                        text = "Node n5 is waiting for quorum and cannot service requests yet"
                    )
                ),
                """
                    {"src":"n5","dest":"c1","body":{"type":"error","in_reply_to":5,"code":11,"text":"Node n5 is waiting for quorum and cannot service requests yet"}}
                """.trimIndent()
            )
        )
    }
}
