package vortex

import vortex.protocol.Echo
import vortex.protocol.EchoOk
import vortex.protocol.Init
import vortex.protocol.InitOk
import vortex.protocol.MessageBody
import vortex.protocol.Node

fun main() {
    EchoNode().serve()
}

class EchoNode: Node() {
    override fun handleMessage(source: String, body: MessageBody) {
        when (body.payload) {
            is Init -> {
                nodeId = body.payload.nodeId
                nodeIds = body.payload.nodeIds
                sendMessage(
                    destination = source,
                    payload = InitOk,
                    inReplyTo = body.messageId,
                )
            }
            is Echo -> sendMessage(
                destination = source,
                payload = EchoOk(body.payload.echo),
                inReplyTo = body.messageId,
            )
            else -> throw Exception("Unexpected message payload: ${body.payload}")
        }
    }
}
