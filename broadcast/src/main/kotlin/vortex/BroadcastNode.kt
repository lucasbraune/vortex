package vortex

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import vortex.protocol.v4.Client
import vortex.protocol.v4.InitService
import vortex.protocol.v4.LOG
import vortex.protocol.v4.MessageBody
import vortex.protocol.v4.RequestBody
import vortex.protocol.v4.ResponseBody
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration.Companion.milliseconds

@Serializable
@SerialName("broadcast")
data class Broadcast(
    @SerialName("message")
    val value: Int,
    override val msgId: Int,
    override val inReplyTo: Int? = null,
): RequestBody

@Serializable
@SerialName("broadcast_ok")
data class BroadcastOk(
    override val inReplyTo: Int,
    override val msgId: Int? = null,
) : ResponseBody

@Serializable
@SerialName("read")
data class Read(
    override val msgId: Int,
    override val inReplyTo: Int? = null,
) : RequestBody

@Serializable
@SerialName("read_ok")
data class ReadOk(
    val messages: List<Int>,
    override val inReplyTo: Int,
    override val msgId: Int? = null,
): ResponseBody

@Serializable
@SerialName("topology")
data class Topology(
    val topology: Map<String, List<String>>,
    override val msgId: Int,
    override val inReplyTo: Int? = null,
): RequestBody

@Serializable
@SerialName("topology_ok")
data class TopologyOk(
    override val inReplyTo: Int,
    override val msgId: Int? = null,
) : ResponseBody

val BroadcastSerializers = SerializersModule {
    polymorphic(MessageBody::class) {
        subclass(Broadcast::class)
        subclass(BroadcastOk::class)
        subclass(Read::class)
        subclass(ReadOk::class)
        subclass(Topology::class)
        subclass(TopologyOk::class)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class BroadcastService(
    private val initService: InitService,
    private val client: Client,
) {
    private val values: MutableList<Int> = CopyOnWriteArrayList()
    private val nodeId: String by lazy { initService.nodeId.getCompleted() }
    private val _neighborIds = CompletableDeferred<List<String>>()
    private val neighborIds: List<String> by lazy { _neighborIds.getCompleted() }

    suspend fun handle(body: Broadcast): BroadcastOk {
        if (!values.contains(body.value)) {
            values.add(body.value)
            coroutineScope {
                neighborIds.map {
                    launch {
                        retry {
                            withTimeout(300.milliseconds) {
                                client.rpc(it, Broadcast(body.value, client.nextMsgId))
                            }
                        }
                    }
                }.joinAll()
            }
        }
        return BroadcastOk(inReplyTo = body.msgId)
    }

    fun handle(body: Topology): TopologyOk {
        body.topology[nodeId]
            ?.filter { it != nodeId }
            ?.let { _neighborIds.complete(it) }
            ?: LOG.println("Topology missing node $nodeId: ${body.topology}")
        return TopologyOk(inReplyTo = body.msgId)
    }

    fun handle(body: Read): ReadOk {
        return ReadOk(
            messages = values.toList(),
            inReplyTo = body.msgId,
        )
    }

    companion object {
        suspend fun <T> retry(
            times: Int = Int.MAX_VALUE,
            initialDelay: Long = 100, // 0.1 second
            maxDelay: Long = 1000,    // 1 second
            factor: Double = 2.0,
            block: suspend () -> T): T
        {
            var currentDelay = initialDelay
            repeat(times - 1) {
                try {
                    return block()
                } catch (e: Exception) {
                    LOG.println("Retrying operation after ${it + 1} attempts. Cause: ${e.message}")
                }
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
            }
            return block() // last attempt
        }
    }
}
