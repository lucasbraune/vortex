package vortex

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import vortex.protocol.Init
import vortex.protocol.InitOk
import vortex.protocol.MessagePayload
import vortex.protocol.Node

@Serializable
@SerialName("generate")
object Generate: MessagePayload

@Serializable
@SerialName("generate_ok")
data class GenerateOk(val id: Int): MessagePayload

val echoSerialModule = SerializersModule {
    polymorphic(MessagePayload::class) {
        subclass(Generate::class)
        subclass(GenerateOk::class)
    }
}

class UniqueIdsNode : Node(echoSerialModule) {
    override fun handleMessage(
        source: String,
        messageId: Int?,
        inReplyTo: Int?,
        payload: MessagePayload,
    ) {
        when (payload) {
            is Init -> {
                nodeId = payload.nodeId
                nodeIds = payload.nodeIds
                sendMessage(
                    destination = source,
                    payload = InitOk,
                    inReplyTo = messageId,
                )
            }
            is Generate -> {
                sendMessage(
                    destination = source,
                    payload = GenerateOk(id = 42), // TODO
                    inReplyTo = messageId,
                )
            }
            else -> throw Exception("Unexpected message payload: $payload")
        }
    }
}
