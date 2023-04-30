package vortex.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class MessageBody(
    val messageId: Int?,
    val inReplyTo: Int?,
    val payload: MessagePayload,
)

@Serializable
sealed interface MessagePayload

@Serializable
@SerialName("init")
data class Init(
    @SerialName("node_id")
    val nodeId: String,
    @SerialName("node_ids")
    val nodeIds: List<String>,
): MessagePayload

@Serializable
@SerialName("init_ok")
object InitOk: MessagePayload

@Serializable
@SerialName("echo")
data class Echo(val echo: String): MessagePayload

@Serializable
@SerialName("echo_ok")
data class EchoOk(val echo: String): MessagePayload
