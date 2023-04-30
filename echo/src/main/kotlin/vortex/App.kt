package vortex

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import vortex.protocol.Init
import vortex.protocol.InitOk
import vortex.protocol.MessageBody
import vortex.protocol.MessagePayload
import vortex.protocol.Node

@Serializable
@SerialName("echo")
data class Echo(val echo: String): MessagePayload

@Serializable
@SerialName("echo_ok")
data class EchoOk(val echo: String): MessagePayload

val echoSerialModule = SerializersModule {
    polymorphic(MessagePayload::class) {
        subclass(Echo::class)
        subclass(EchoOk::class)
    }
}

class EchoNode : Node(echoSerialModule) {
    override fun handleMessage(source: String, body: MessageBody) {
        when (body.payload) {
            is Init -> {
                nodeId = body.payload.nodeId
                nodeIds = body.payload.nodeIds
                sendMessage(
                    destination = source,
                    payload = InitOk,
                    inReplyTo = body.messageId,
                )
            }
            is Echo -> sendMessage(
                destination = source,
                payload = EchoOk(body.payload.echo),
                inReplyTo = body.messageId,
            )
            else -> throw Exception("Unexpected message payload: ${body.payload}")
        }
    }
}

fun main() {
    EchoNode().serve()
}
