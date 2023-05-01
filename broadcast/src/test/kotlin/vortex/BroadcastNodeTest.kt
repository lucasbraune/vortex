/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package vortex

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import vortex.protocol.MessagePayload

class BroadcastNodeTest {
    @ParameterizedTest
    @MethodSource("serializedPayloads")
    fun `payload deserializes correctly`(payload: MessagePayload, serializedPayload: String) {
        val actual = json.decodeFromString<MessagePayload>(serializedPayload)

        assertEquals(payload, actual)
    }

    @ParameterizedTest
    @MethodSource("serializedPayloads")
    fun `payload serializes correctly`(payload: MessagePayload, serializedPayload: String) {
        val actual = json.encodeToJsonElement(payload)

        assertEquals(Json.parseToJsonElement(serializedPayload), actual)
    }

    companion object {
        private val json = Json { serializersModule = broadcastSerialModule }

        @JvmStatic
        fun serializedPayloads() = listOf<Arguments>(
            Arguments.of(
                ReadOk(messages = listOf(1, 8, 72, 25)),
                """
                    {
                      "type": "read_ok",
                      "messages": [1, 8, 72, 25]
                    }
                """.trimIndent()
            ),
            Arguments.of(
                Topology(topology = mapOf(
                    "n1" to listOf("n2", "n3"),
                    "n2" to listOf("n1"),
                    "n3" to listOf("n1"),
                )),
                """
                    {
                      "type": "topology",
                      "topology": {
                        "n1": ["n2", "n3"],
                        "n2": ["n1"],
                        "n3": ["n1"]
                      }
                    }
                """.trimIndent(),
            ),
        )
    }
}
