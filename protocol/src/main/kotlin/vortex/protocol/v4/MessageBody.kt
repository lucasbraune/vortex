package vortex.protocol.v4

import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.modules.SerializersModule
import kotlin.coroutines.CoroutineContext

interface MessageBody {
    val msgId: Int?
    val inReplyTo: Int?
}

interface RequestBody : MessageBody {
    override val msgId: Int
}

interface ResponseBody : MessageBody {
    override val inReplyTo: Int
}

@OptIn(ExperimentalSerializationApi::class)
fun messageBodyFormat(serializers: SerializersModule): Json = Json {
    namingStrategy = JsonNamingStrategy.SnakeCase
    serializersModule = serializers
}

fun Json.encodeBody(body: MessageBody): JsonObject = encodeToJsonElement(body).jsonObject
fun Json.decodeBody(json: JsonObject): MessageBody = decodeFromJsonElement<MessageBody>(json)

fun Server(
    format: Json,
    handlerContext: CoroutineContext = Dispatchers.Default,
    handle: suspend (src: String, dest: String, body: MessageBody) -> ResponseBody?
) = Server(handlerContext) { message ->
    handle(message.src, message.dest, format.decodeBody(message.body))?.let {
        send(Message(message.dest, message.src, format.encodeBody(it)))
    }
}
