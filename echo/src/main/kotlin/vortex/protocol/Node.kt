package vortex.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

abstract class Node {
    @Serializable
    private data class Message(
        @SerialName("src")
        val source: String,
        @SerialName("dest")
        val destination: String,
        val body: JsonObject,
    )

    private val format = Json { ignoreUnknownKeys = true }
    private var nextMessageId: Int = 0
    protected lateinit var nodeId: String
    protected lateinit var nodeIds: List<String>

    fun serve() {
        while (true) {
            val line = readLine() ?: return // End of input
            val message = format.decodeFromString<Message>(line)
            val body = messageBodyFromFlatJson(message.body, format)

            handleMessage(message.source, body)
        }
    }

    protected abstract fun handleMessage(source: String, body: MessageBody)

    /**
     * Sends a message and returns its ID. Throws if [nodeId] has not yet been set.
     */
    protected fun sendMessage(
        destination: String,
        payload: MessagePayload,
        inReplyTo: Int? = null,
    ): Int {
        val messageId = nextMessageId++
        val body = MessageBody(messageId, inReplyTo, payload)
        val message = Message(source = nodeId, destination = destination, body.toFlatJson(format))
        println(format.encodeToString(message))
        return messageId
    }

    private companion object {
        private const val IN_REPLY_TO = "in_reply_to"
        private const val MESSAGE_ID = "msg_id"

        fun MessageBody.toFlatJson(format: Json = Json.Default): JsonObject {
            val obj = format.encodeToJsonElement(payload) as JsonObject
            return obj.setReservedFields(messageId, inReplyTo)
        }

        fun messageBodyFromFlatJson(
            flatJson: JsonObject,
            format: Json = Json.Default
        ) = MessageBody(
            messageId = flatJson[MESSAGE_ID]?.toIntOrThrow(),
            inReplyTo = flatJson[IN_REPLY_TO]?.toIntOrThrow(),
            payload = format.decodeFromJsonElement(flatJson.unsetReservedFields()),
        )

        private fun JsonObject.unsetReservedFields(): JsonObject {
            val map = toMutableMap()
            map.remove(IN_REPLY_TO)
            map.remove(MESSAGE_ID)
            return JsonObject(map)
        }

        private fun JsonObject.setReservedFields(
            messageId: Int?,
            inReplyTo: Int?,
        ): JsonObject {
            val map = toMutableMap()
            if (messageId != null) {
                map[MESSAGE_ID] = JsonPrimitive(messageId)
            }
            if (inReplyTo != null) {
                map[IN_REPLY_TO] = JsonPrimitive(inReplyTo)
            }
            return JsonObject(map)
        }

        private fun JsonElement.toIntOrThrow(): Int = (this as JsonPrimitive).content.toInt()
    }
}
