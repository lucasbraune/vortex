package vortex

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import vortex.protocol.v4.InitService
import vortex.protocol.v4.MessageBody
import vortex.protocol.v4.RequestBody
import vortex.protocol.v4.ResponseBody
import java.util.concurrent.atomic.AtomicInteger

@Serializable
@SerialName("generate")
class Generate(
    override val msgId: Int,
    override val inReplyTo: Int? = null,
) : RequestBody

@Serializable
@SerialName("generate_ok")
data class GenerateOk(
    val id: Int,
    override val inReplyTo: Int,
    override val msgId: Int? = null,
): ResponseBody

val UniqueIdsSerializers = SerializersModule {
    polymorphic(MessageBody::class) {
        subclass(Generate::class)
        subclass(GenerateOk::class)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class UniqueIdsService(
    initService: InitService,
) {
    private val nodeId: String by lazy {
        initService.nodeId.getCompleted()
    }
    private val nodeIds: List<String> by lazy {
        initService.nodeIds.getCompleted()
    }

    private var generatedCount = AtomicInteger()

    fun handle(body: Generate) = GenerateOk(generateId(), body.msgId)

    private fun generateId(): Int =
        generatedCount.getAndIncrement() * nodeIds.count() + nodeIds.indexOf(nodeId)
}
