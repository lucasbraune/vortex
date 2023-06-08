package vortex

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import vortex.protocol.v1.MessagePayload
import vortex.protocol.v1.Node

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
    init {
        registerHandler { message ->
            val source = message.source
            val messageId = message.messageId
            when (val payload = message.payload) {
                is Echo -> {
                    send(
                        destination = source,
                        payload = EchoOk(payload.echo),
                        inReplyTo = messageId,
                    )
                }
            }
        }
    }
}
