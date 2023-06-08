package vortex.protocol.v2

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.util.concurrent.atomic.AtomicInteger

typealias HandleFunction = suspend CoroutineScope.(message: Message) -> Unit

class Node {
    private val nextMessageId = AtomicInteger()
    private val pendingResponseChannel = Channel<PendingResponse>()
    private val handleFunctions = mutableMapOf<String, HandleFunction>()

    fun handle(type: String, handler: HandleFunction) {
        handleFunctions[type] = handler
    }

    fun send(destination: String, body: JsonObject) {
        val message = Message(
            source = id,
            destination = destination,
            body = body.copy(nextMessageId.getAndIncrement()),
        )
        send(message)
    }

    fun reply(message: Message, body: JsonObject) {
        val responseBody = body.copy(
            messageId = nextMessageId.getAndIncrement(),
            inReplyTo = message.body.messageId
        )
        val response = Message(
            source = id,
            destination = message.source,
            body = responseBody,
        )
        send(response)
    }

    suspend fun rpc(destination: String, body: JsonObject): JsonObject = coroutineScope {
        val requestId: Int = nextMessageId.getAndIncrement()
        val message = Message(
            source = id,
            destination = destination,
            body = body.copy(requestId),
        )
        val deferredResponse = expectResponse(requestId)
        send(message)
        deferredResponse.await().body
    }

    @Suppress("DeferredIsResult")
    private suspend fun expectResponse(requestId: Int): Deferred<Message> {
        val pendingResponse = PendingResponse(requestId)
        pendingResponseChannel.send(pendingResponse) // must be rendezvous channel
        return pendingResponse.response
    }

    fun run(): Unit = runBlocking {
        val messageChannel = receiveMessages()
        val responseHandler = ResponseHandler()
        val handleFunctionsCopy = handleFunctions.toMap() // defend against concurrent modification
        while (true) {
            select<Unit> {
                messageChannel.onReceive { message ->
                    responseHandler.handle(message)
                    val handleFunction = handleFunctionsCopy[message.body.type] ?: return@onReceive
                    launch(Dispatchers.Default) { handleFunction(message) }
                }
                pendingResponseChannel.onReceive { responseHandler.register(it) }
            }
        }
    }

    val id: String = "n1" // TODO

    val nodeIds: List<String> = listOf("n1") // TODO
}

private class PendingResponse(val requestId: Int){
    val response: CompletableDeferred<Message> = CompletableDeferred()
}

/** Not thread-safe. */
private class ResponseHandler {
    private val deferredResponses = mutableMapOf<Int, CompletableDeferred<Message>>()

    fun register(pendingResponse: PendingResponse) {
        deferredResponses[pendingResponse.requestId] = pendingResponse.response
    }

    fun handle(response: Message) {
        val requestId = response.body.inReplyTo ?: return
        deferredResponses[requestId]?.complete(response)
        deferredResponses.remove(requestId)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun CoroutineScope.receiveMessages(): ReceiveChannel<Message> =
    produce(Dispatchers.IO) {
        while (true) {
            val message = receiveMessage() ?: break
            channel.send(message)
        }
    }

/**
 * Returns null at the end of the input.
 */
fun receiveMessage(): Message? =
    readLine()?.let { Json.decodeFromString<Message>(it) }

fun send(message: Message) {
    println(Json.encodeToString(message))
}

fun log(message: Any?) {
    System.err.println(message)
}
