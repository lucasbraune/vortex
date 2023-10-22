package vortex.protocol.v4

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.io.PrintStream
import kotlin.coroutines.CoroutineContext

// Design goals:
// - Server receives arbitrary messages
// - Server is immutable
// - Client sends arbitrary messages

@Serializable
data class Message(
    val src: String,
    val dest: String,
    val body: JsonObject,
)

private val format = Json

fun send(message: Message): Unit = println(format.encodeToString(message))
fun receiveMessage(): Message? = readlnOrNull()?.let { format.decodeFromString<Message>(it) }
val LOG: PrintStream = System.err

typealias MessageHandler = suspend (message: Message) -> Unit

@OptIn(ExperimentalCoroutinesApi::class)
class Server(
    private val handlerContext: CoroutineContext = Dispatchers.Default,
    private val handler: MessageHandler,
) {
    private val deferredJob = CompletableDeferred<Job>()

    fun start(ioContext: CoroutineContext = Dispatchers.IO) {
        val job = CoroutineScope(ioContext).launch {
            deferredJob.await()
            runInterruptible {
                while (true) {
                    val message = receiveMessage() ?: break
                    launch(handlerContext) {
                        handler.invoke(message)
                    }
                }
            }
        }
        if (!deferredJob.complete(job)) {
            job.cancel()
            throw IllegalStateException("Server was running.")
        }
    }

    fun shutdownNow() {
        deferredJob.getCompleted().cancel()
    }

    suspend fun awaitTermination() {
        deferredJob.getCompleted().join()
    }
}
