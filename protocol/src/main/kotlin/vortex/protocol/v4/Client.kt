package vortex.protocol.v4

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.serialization.json.Json

@OptIn(ExperimentalCoroutinesApi::class)
class Client(
    initService: InitService,
    private val format: Json,
) {
    private val nodeId: String by lazy { initService.nodeId.getCompleted() }

    fun send(dest: String, body: MessageBody) {
        send(Message(nodeId, dest, format.encodeBody(body)))
    }

    suspend fun rpc(dest: String, request: RequestBody): ResponseBody = TODO()

    fun handle(response: ResponseBody): Unit = TODO()
}
