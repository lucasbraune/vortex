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
data class Broadcast(
    @SerialName("message")
    val value: Int
): MessagePayload

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

    private val values = mutableListOf<Int>()
    private lateinit var neighbors: List<String>

    init {
        registerHandler { message ->
            val source = message.source
            val messageId = message.messageId
            when (val payload = message.payload) {
                is Broadcast -> {
                    val value = payload.value
                    if (!values.contains(value)) {
                        values.add(value)
                        neighbors.filter { it != source }
                            .forEach { rpc(it, Broadcast(value)) {} }
                    }
                    send(BroadcastOk, destination = source, inReplyTo = messageId)
                }
                is Read -> {
                    send(ReadOk(values), destination = source, inReplyTo = messageId)
                }
                is Topology -> {
                    neighbors = payload.topology[nodeId]!!
                    send(TopologyOk, destination = source, inReplyTo = messageId)
                }
            }
        }
    }
}
