package vortex.protocol

data class MessageBody(
    val messageId: Int?,
    val inReplyTo: Int?,
    val payload: MessagePayload,
)

sealed interface MessagePayload
