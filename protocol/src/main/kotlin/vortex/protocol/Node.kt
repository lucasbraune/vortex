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
    private data class WireMessage(
        @SerialName("src")
        val source: String,
        @SerialName("dest")
        val destination: String,
        val body: JsonObject,
    )

    data class Message(
        val source: String,
        val messageId: Int?,
        val inReplyTo: Int?,
        val payload: MessagePayload,
    )

    private class ResponseHandler {
        private val handlers: MutableMap<Pair<String, Int>, (MessagePayload) -> Unit> =
            mutableMapOf()

        fun register(
            requestDestination: String,
            requestMessageId: Int,
            handle: (message: MessagePayload) -> Unit,
        ) {
            handlers[Pair(requestDestination, requestMessageId)] = handle
        }

        fun handle(response: Message) {
            if (response.inReplyTo != null) {
                handlers.remove(Pair(response.source, response.inReplyTo))
                    ?.invoke((response.payload))
            }
        }
    }

    private var nextMessageId: Int = 0

    protected lateinit var nodeId: String

    protected lateinit var nodeIds: List<String>

    private val format = Json {
        ignoreUnknownKeys = true
        this.serializersModule = serialModule + protocolSerialModule
    }

    private val handlers: MutableList<(message: Message) -> Unit> =
        mutableListOf()

    private val responseHandler: ResponseHandler = ResponseHandler()

    init {
        registerHandler { message ->
            val source = message.source
            val messageId = message.messageId
            when (val payload = message.payload) {
                is Init -> {
                    this.nodeId = payload.nodeId
                    this.nodeIds = payload.nodeIds
                    sendMessage(
                        destination = source,
                        payload = InitOk,
                        inReplyTo = messageId,
                    )
                }
            }
        }
        registerHandler(responseHandler::handle)
    }

    fun serve() {
        while (true) {
            val line = readLine() ?: return // End of input
            val wireMessage = format.decodeFromString<WireMessage>(line)
            val messageId = wireMessage.body[MESSAGE_ID]?.toIntOrThrow()
            val inReplyTo = wireMessage.body[IN_REPLY_TO]?.toIntOrThrow()
            val payload = format.decodeFromJsonElement<MessagePayload>(wireMessage.body)
            val message = Message(wireMessage.source, messageId, inReplyTo, payload)

            handlers.forEach { it(message) }
        }
    }

    protected fun registerHandler(handler: (message: Message) -> Unit) {
        handlers.add(handler)
    }

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
        val message = WireMessage(source = nodeId, destination = destination, body)

        println(format.encodeToString(message))
        return messageId
    }

    protected fun rpc(
        destination: String,
        request: MessagePayload,
        handler: (response: MessagePayload) -> Unit
    ) {
        val messageId = sendMessage(destination, request)
        responseHandler.register(destination, messageId, handler)
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
