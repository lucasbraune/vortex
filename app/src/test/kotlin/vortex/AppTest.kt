/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package vortex

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.test.Test

class AppTest {
    @Test
    fun `deserializes init body`() {
        val json = """
            {
              "type":     "init",
              "msg_id":   1,
              "node_id":  "n3",
              "node_ids": ["n1", "n2", "n3"]
            }
        """.trimIndent()

        val message = Json.decodeFromString<MessageBody>(json)

        val expected = Init(
            messageId = 1,
            nodeId = "n3",
            nodeIds = listOf("n1", "n2", "n3")
        )
        assertEquals(expected, message)
    }

    @Test
    fun `deserializes echo message`() {
        val json = """
            {
              "src": "c1",
              "dest": "n1",
              "body": {
                "type": "echo",
                "msg_id": 1,
                "echo": "Please echo 35"
              }
            }
        """.trimIndent()

        val message = Json.decodeFromString<Message>(json)

        val expected = Message(
            source = "c1",
            destination = "n1",
            body = Echo(messageId = 1, echo = "Please echo 35")
        )
        assertEquals(expected, message)
    }

    @Test
    fun `serializes init_ok body`() {
        val body = InitOk(inReplyTo = 1)

        val json = Json.encodeToString<MessageBody>(body)

        val expected = """
            {
              "type": "init_ok",
              "in_reply_to": 1
            }
        """.trimIndent()
        assertEquals(Json.parseToJsonElement(expected), Json.parseToJsonElement(json))
    }

    @Test
    fun `serializes echo_ok message`() {
        val message = Message(
            source = "n1",
            destination = "c1",
            body = EchoOk(
                messageId = 1,
                inReplyTo = 1,
                echo = "Please echo 35",
            )
        )

        val json = Json.encodeToString(message)

        val expected = """
            {
              "src": "n1",
              "dest": "c1",
              "body": {
                "type": "echo_ok",
                "msg_id": 1,
                "in_reply_to": 1,
                "echo": "Please echo 35"
              }
            }
        """.trimIndent()
        assertEquals(Json.parseToJsonElement(expected), Json.parseToJsonElement(json))
    }
}
