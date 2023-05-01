package vortex

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import vortex.protocol.MessagePayload
import vortex.protocol.Node

@Serializable
@SerialName("generate")
object Generate: MessagePayload

@Serializable
@SerialName("generate_ok")
data class GenerateOk(val id: Int): MessagePayload

val uniqueIdsSerialModule = SerializersModule {
    polymorphic(MessagePayload::class) {
        subclass(Generate::class)
        subclass(GenerateOk::class)
    }
}

class UniqueIdsNode : Node(uniqueIdsSerialModule) {
    private var generatedCount = 0

    init {
        registerHandler { message ->
            val source = message.source
            val messageId = message.messageId
            when (message.payload) {
                is Generate -> {
                    send(
                        destination = source,
                        payload = GenerateOk(id = generateId()),
                        inReplyTo = messageId,
                    )
                }
            }
        }
    }

    private fun generateId(): Int =
        (generatedCount++) * nodeIds.count() + nodeIds.indexOf(nodeId)
}
