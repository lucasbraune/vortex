package vortex.protocol.v3

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory
import kotlin.reflect.KClass

data class Message(
    val src: String,
    val dest: String,
    val body: MessageBody,
)

interface MessageBody {
    val msgId: Int?
        get() = null
    val inReplyTo: Int?
        get() = null
}

interface RequestBody: MessageBody {
    override val msgId: Int
}

interface ResponseBody: MessageBody {
    override val inReplyTo: Int
}

data class Init(
    override val msgId: Int,
    val nodeId: String,
    val nodeIds: List<String>,
): RequestBody

data class InitOk(
    override val inReplyTo: Int,
): ResponseBody

data class RpcError(
    override val inReplyTo: Int,
    val code: Int,
    val text: String? = null
): ResponseBody

class Protocol private constructor (
    private val gson: Gson
) {
    class Builder {
        private val bodyAdapterFactory: RuntimeTypeAdapterFactory<MessageBody> =
            RuntimeTypeAdapterFactory.of(MessageBody::class.java)

        init {
            registerMessageBody("init", Init::class)
            registerMessageBody("init_ok", InitOk::class)
            registerMessageBody("error", RpcError::class)
        }

        fun registerMessageBody(label: String, type: KClass<out MessageBody>) {
            bodyAdapterFactory.registerSubtype(type.java, label)
        }

        fun build(): Protocol {
            val gson = GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapterFactory(bodyAdapterFactory)
                .create()
            return Protocol(gson)
        }
    }

    fun receiveMessage(): Message? =
        readLine()?.let {
            gson.fromJson(it, Message::class.java)
        }

    fun sendMessage(message: Message) {
        println(gson.toJson(message))
    }
}

fun buildProtocol(
    init: Protocol.Builder.() -> Unit = {}
): Protocol = Protocol.Builder()
    .apply { init() }
    .build()

fun log(message: Any?) {
    System.err.println(message)
}
