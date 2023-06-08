package vortex.protocol.v2

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put

@Serializable
data class Message(
    val source: String,
    val destination: String,
    val body: JsonObject,
)

object BodyKeys {
    const val TYPE = "type"
    const val MESSAGE_ID = "msg_id"
    const val IN_REPLY_TO = "in_reply_to"
}

val JsonObject.type: String?
    get() = (this[BodyKeys.MESSAGE_ID] as? JsonPrimitive)?.contentOrNull

val JsonObject.messageId: Int?
    get() = (this[BodyKeys.MESSAGE_ID] as? JsonPrimitive)?.intOrNull

val JsonObject.inReplyTo: Int?
    get() = (this[BodyKeys.IN_REPLY_TO] as? JsonPrimitive)?.intOrNull

fun JsonObject.copy(messageId: Int? = null, inReplyTo: Int? = null): JsonObject {
    val json = buildJsonObject {
        if (messageId != null) {
            put(BodyKeys.MESSAGE_ID, messageId)
        }
        if (inReplyTo != null) {
            put(BodyKeys.IN_REPLY_TO, messageId)
        }
    }
    return JsonObject(this + json)
}
