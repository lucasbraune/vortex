package vortex.protocol.v3

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import java.io.InterruptedIOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

typealias HandleFunction = suspend (message: Message) -> Unit

interface Node {
    val nodeId: String
    val nodeIds: List<String>
    val client: Client
    fun registerHandler(handle: HandleFunction)
    fun serve(context: CoroutineContext = EmptyCoroutineContext): Job
}

interface Client {
    fun nextMsgId(): Int
    fun send(dest: String, body: MessageBody)
    suspend fun rpc(dest: String, body: RequestBody): ResponseBody
}

fun buildNode(protocol: Protocol): Node = NodeImpl(protocol)

@OptIn(ExperimentalCoroutinesApi::class)
private class NodeImpl(
    private val protocol: Protocol,
): Node {
    private val _nodeId = CompletableDeferred<String>()
    private val _nodeIds = CompletableDeferred<List<String>>()
    override val nodeId by lazy { _nodeId.getCompleted() }
    override val nodeIds by lazy { _nodeIds.getCompleted() }
    override val client = ClientImpl(protocol, lazy { nodeId })
    private val handlers = CopyOnWriteArrayList<HandleFunction>()

    init {
        registerHandler {
            when (val body = it.body) {
                is Init -> {
                    _nodeId.complete(body.nodeId)
                    _nodeIds.complete(body.nodeIds)
                    client.send(dest = it.src, InitOk(inReplyTo = body.msgId))
                }
                is ResponseBody -> client.handleResponse(it)
            }
        }
    }

    override fun registerHandler(handle: HandleFunction) {
        handlers.add(handle)
    }

    override fun serve(context: CoroutineContext): Job =
        CoroutineScope(context).launch {
            while(true) {
                val message = runInterruptible(Dispatchers.IO) {
                    try {
                        protocol.receiveMessage()
                    } catch (ex: InterruptedIOException) {
                        log(ex.stackTraceToString())
                        null
                    }
                } ?: return@launch
                launch {
                    handlers.forEach { it(message) }
                }
            }
        }
}

private class ClientImpl(
    private val protocol: Protocol,
    lazyNodeId: Lazy<String>,
): Client {
    private val nodeId: String by lazyNodeId
    private val msgId = AtomicInteger(1)
    private val deferredResponses = ConcurrentHashMap<Int, CompletableDeferred<ResponseBody>>()

    override fun nextMsgId(): Int = msgId.getAndIncrement()

    override fun send(dest: String, body: MessageBody) {
        protocol.sendMessage(Message(nodeId, dest, body))
    }

    override suspend fun rpc(dest: String, body: RequestBody): ResponseBody {
        val response = CompletableDeferred<ResponseBody>()
        deferredResponses[body.msgId] = response
        send(dest, body)
        return response.await()
    }

    fun handleResponse(message: Message) {
        val body = message.body as? ResponseBody ?: return
        val deferredResponse = deferredResponses[body.inReplyTo] ?: return
        deferredResponse.complete(body)
    }
}
