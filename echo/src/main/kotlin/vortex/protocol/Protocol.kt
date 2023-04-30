package vortex.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.*

data class MessageBody(
    val messageId: Int?,
    val inReplyTo: Int?,
    val payload: MessagePayload,
)

interface MessagePayload

@Serializable
@SerialName("init")
data class Init(
    @SerialName("node_id")
    val nodeId: String,
    @SerialName("node_ids")
    val nodeIds: List<String>,
): MessagePayload

@Serializable
@SerialName("init_ok")
object InitOk: MessagePayload

val protocolSerialModule: SerializersModule = SerializersModule {
    polymorphic(MessagePayload::class) {
        subclass(Init::class)
        subclass(InitOk::class)
    }
}
