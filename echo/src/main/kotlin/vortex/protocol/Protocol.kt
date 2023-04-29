package vortex.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

abstract class Node {

    @Serializable
    private data class Message(
        val src: String,
        val dest: String,
        val body: JsonObject,
    )

    private val json = Json { ignoreUnknownKeys = true }
    private var nextMessageId: Int = 0
    protected lateinit var nodeId: String
    protected lateinit var nodeIds: List<String>

    fun serve() {
        while (true) {
            val line = readLine() ?: return // End of input
            val message: Message = json.decodeFromString(line)
            val messageId: Int? = message.body[MESSAGE_ID]?.toIntOrThrow()
            val inReplyTo: Int? = message.body[IN_REPLY_TO]?.toIntOrThrow()
            handleMessage(message.src, messageId, inReplyTo, message.body.unsetReservedFields())
        }
    }

    protected abstract fun handleMessage(
        source: String,
        messageId: Int?,
        inReplyTo: Int?,
        payload: JsonObject,
    )

    /**
     * Sends a message and returns its ID. Throws if [nodeId] has not yet been set.
     */
    protected fun sendMessage(
        destination: String,
        inReplyTo: Int?,
        payload: JsonObject,
    ): Int {
        val messageId = nextMessageId++
        val body = payload.setReservedFields(messageId, inReplyTo)
        val message = Message(src = nodeId, dest = destination, body = body)
        println(json.encodeToString(message))
        return messageId
    }

    private companion object {
        const val IN_REPLY_TO = "inReplyTo"
        const val MESSAGE_ID = "msg_id"

        fun JsonObject.unsetReservedFields(): JsonObject {
            val map = toMutableMap()
            map.remove(IN_REPLY_TO)
            map.remove(MESSAGE_ID)
            return JsonObject(map)
        }

        fun JsonObject.setReservedFields(
            messageId: Int,
            inReplyTo: Int?,
        ): JsonObject {
            val map = toMutableMap()
            map[MESSAGE_ID] = JsonPrimitive(messageId)
            if (inReplyTo != null) {
                map[IN_REPLY_TO] = JsonPrimitive(inReplyTo)
            }
            return JsonObject(map)
        }

        fun JsonElement.toIntOrThrow(): Int = (this as JsonPrimitive).content.toInt()
    }
}
