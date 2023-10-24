package vortex

import kotlinx.serialization.modules.plus
import vortex.protocol.v4.Client
import vortex.protocol.v4.Init
import vortex.protocol.v4.InitSerializers
import vortex.protocol.v4.InitService
import vortex.protocol.v4.ResponseBody
import vortex.protocol.v4.Server
import vortex.protocol.v4.messageBodyFormat
import java.util.*

fun main() {
    val format = messageBodyFormat(InitSerializers + BroadcastSerializers)
    val initService = InitService()
    val client = Client(initService, format)
    val broadcastService = BroadcastService(initService, client)
    Server(format) { src, _, body ->
        when (body) {
            is Init -> initService.handle(body)
            is ResponseBody -> {
                client.handle(src, body)
                null
            }
            is Read -> broadcastService.handle(body)
            is Broadcast -> broadcastService.handle(body)
            is Topology -> broadcastService.handle(body)
            else -> throw InputMismatchException("Unexpected message body: $body")
        }
    }.serve()
}
