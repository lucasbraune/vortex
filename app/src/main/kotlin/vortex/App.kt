@file:OptIn(ExperimentalSerializationApi::class)

package vortex

import kotlinx.serialization.ExperimentalSerializationApi
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

fun main() {
    while (true) {
        val line = readLine() ?: return
        val message: Message = Json.decodeFromString(line)
        println(Json.encodeToString(message))
    }
}
