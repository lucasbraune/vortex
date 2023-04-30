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
import kotlinx.serialization.modules.*

/**
 * @param serialModule Module where subclasses of [MessagePayload] are
 * registered for serialization; used in addition to [protocolSerialModule]
 */
abstract class Node(serialModule: SerializersModule) {
    @Serializable
    private data class Message(
        @SerialName("src")
        val source: String,
        @SerialName("dest")
        val destination: String,
        val body: JsonObject,
    )

    private var nextMessageId: Int = 0

    protected lateinit var nodeId: String

    protected lateinit var nodeIds: List<String>

    private val format = Json {
        ignoreUnknownKeys = true
        this.serializersModule = serialModule + protocolSerialModule
    }

    fun serve() {
        while (true) {
            val line = readLine() ?: return // End of input
            val message = format.decodeFromString<Message>(line)
            val messageId = message.body[MESSAGE_ID]?.toIntOrThrow()
            val inReplyTo = message.body[IN_REPLY_TO]?.toIntOrThrow()
            val payload = format.decodeFromJsonElement<MessagePayload>(message.body)

            handleMessage(message.source, messageId, inReplyTo, payload)
        }
    }

    protected abstract fun handleMessage(
        source: String,
        messageId: Int?,
        inReplyTo: Int?,
        payload: MessagePayload,
    )

    /**
     * Sends a message and returns its ID. Throws if [nodeId] has not yet been set.
     */
    protected fun sendMessage(
        destination: String,
        payload: MessagePayload,
        inReplyTo: Int? = null,
    ): Int {
        val messageId = nextMessageId++
        val payloadJson = format.encodeToJsonElement(payload) as JsonObject
        val body = payloadJson.withReservedFields(messageId = messageId, inReplyTo = inReplyTo)
        val message = Message(source = nodeId, destination = destination, body)

        println(format.encodeToString(message))
        return messageId
    }

    private companion object {
        private const val IN_REPLY_TO = "in_reply_to"
        private const val MESSAGE_ID = "msg_id"

        private fun JsonObject.withReservedFields(
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
