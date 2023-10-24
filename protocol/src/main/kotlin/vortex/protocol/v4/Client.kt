package vortex.protocol.v4

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class Client(
    initService: InitService,
    private val format: Json,
) {
    private val nodeId: String by lazy { initService.nodeId.getCompleted() }
    private val _nextMsgId = AtomicInteger()
    val nextMsgId: Int get() = _nextMsgId.getAndIncrement()
    private val responsesByMsgId =
        ConcurrentHashMap<Int, CompletableDeferred<ResponseBody>>()

    fun send(dest: String, body: MessageBody) {
        send(Message(nodeId, dest, format.encodeBody(body)))
    }

    suspend fun rpc(dest: String, body: RequestBody): ResponseBody {
        val response = CompletableDeferred<ResponseBody>()
        responsesByMsgId[body.msgId] = response
        send(dest, body)
        try {
            return response.await()
        } catch (ex: Throwable) {
            responsesByMsgId.remove(body.msgId)
            throw ex
        }
    }

    fun handle(src: String, responseBody: ResponseBody) {
        responsesByMsgId.remove(responseBody.inReplyTo)
            ?.complete(responseBody)
            ?: LOG.println("Unexpected response from $src: $responseBody")
    }
}
