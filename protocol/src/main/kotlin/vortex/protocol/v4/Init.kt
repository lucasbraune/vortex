package vortex.protocol.v4

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
@SerialName("init")
data class Init(
    val nodeId: String,
    val nodeIds: List<String>,
    override val inReplyTo: Int? = null,
    override val msgId: Int
): RequestBody

@Serializable
@SerialName("init_ok")
data class InitOk(
    override val msgId: Int? = null,
    override val inReplyTo: Int
): ResponseBody

val InitSerializers = SerializersModule {
    polymorphic(MessageBody::class) {
        subclass(Init::class)
        subclass(InitOk::class)
    }
}

class InitService {
    private val _nodeId = CompletableDeferred<String>()
    private val _nodeIds = CompletableDeferred<List<String>>()

    val nodeId: Deferred<String> = _nodeId
    val nodeIds: Deferred<List<String>> = _nodeIds

    fun handle(body: Init): InitOk {
        _nodeId.complete(body.nodeId)
        _nodeIds.complete(body.nodeIds)
        return InitOk(inReplyTo = body.msgId)
    }
}
