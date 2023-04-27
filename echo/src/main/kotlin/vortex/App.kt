package vortex

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class Message(
    @SerialName("src")
    val source: String,
    @SerialName("dest")
    val destination: String,
    val body: MessageBody,
)

@Serializable
sealed interface MessageBody {
    val messageId: Int?
    val inReplyTo: Int?
}

@Serializable
@SerialName("init")
data class Init(
    @SerialName("node_id")
    val nodeId: String,
    @SerialName("node_ids")
    val nodeIds: List<String>,
    @SerialName("msg_id")
    override val messageId: Int? = null,
    @SerialName("in_reply_to")
    override val inReplyTo: Int? = null,
): MessageBody

@Serializable
@SerialName("init_ok")
data class InitOk(
    @SerialName("msg_id")
    override val messageId: Int? = null,
    @SerialName("in_reply_to")
    override val inReplyTo: Int? = null,
): MessageBody

@Serializable
@SerialName("echo")
data class Echo(
    val echo: String,
    @SerialName("msg_id")
    override val messageId: Int? = null,
    @SerialName("in_reply_to")
    override val inReplyTo: Int? = null,
): MessageBody

@Serializable
@SerialName("echo_ok")
data class EchoOk(
    val echo: String,
    @SerialName("msg_id")
    override val messageId: Int? = null,
    @SerialName("in_reply_to")
    override val inReplyTo: Int? = null,
): MessageBody

fun main() {
    val json = Json { ignoreUnknownKeys = true }
    var nextMessageId = 1
    while (true) {
        val line = readLine() ?: return // End of input
        val request: Message = json.decodeFromString(line)

        val responseBody = when (request.body) {
            is Init -> InitOk(
                messageId = nextMessageId++,
                inReplyTo = request.body.messageId,
            )
            is Echo -> EchoOk(
                echo = request.body.echo,
                messageId = nextMessageId++,
                inReplyTo = request.body.messageId,
            )
            else -> throw Exception("Unexpected message body: ${request.body}")
        }
        val response = Message(
            source = request.destination,
            destination = request.source,
            body = responseBody
        )

        println(json.encodeToString(response))
    }
}
