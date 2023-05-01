package vortex

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import vortex.protocol.MessagePayload
import vortex.protocol.Node

@Serializable
@SerialName("broadcast")
data class Broadcast(val message: Int): MessagePayload

@Serializable
@SerialName("broadcast_ok")
object BroadcastOk: MessagePayload

@Serializable
@SerialName("read")
object Read: MessagePayload

@Serializable
@SerialName("read_ok")
data class ReadOk(val messages: List<Int>): MessagePayload

@Serializable
@SerialName("topology")
data class Topology(val topology: Map<String, List<String>>): MessagePayload

@Serializable
@SerialName("topology_ok")
object TopologyOk: MessagePayload

val broadcastSerialModule = SerializersModule {
    polymorphic(MessagePayload::class) {
        subclass(Broadcast::class)
        subclass(BroadcastOk::class)
        subclass(Read::class)
        subclass(ReadOk::class)
        subclass(Topology::class)
        subclass(TopologyOk::class)
    }
}

class BroadcastNode : Node(broadcastSerialModule) {

    private val messages = mutableListOf<Int>()

    init {
        registerHandler { message ->
            val source = message.source
            val messageId = message.messageId
            when (val payload = message.payload) {
                is Broadcast -> {
                    messages.add(payload.message)
                    sendMessage(
                        destination = source,
                        payload = BroadcastOk,
                        inReplyTo = messageId,
                    )
                }
                is Read -> {
                    sendMessage(
                        destination = source,
                        payload = ReadOk(messages = messages),
                        inReplyTo = messageId,
                    )
                }
                is Topology -> {
                    sendMessage(
                        destination = source,
                        payload = TopologyOk,
                        inReplyTo = messageId,
                    )
                }
            }
        }
    }
}
