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

    override fun handleMessage(
        source: String,
        messageId: Int?,
        inReplyTo: Int?,
        payload: MessagePayload,
    ) {
        when (payload) {
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
}
