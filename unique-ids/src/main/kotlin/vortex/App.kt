package vortex

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.modules.plus
import vortex.protocol.v4.Init
import vortex.protocol.v4.InitSerializers
import vortex.protocol.v4.InitService
import vortex.protocol.v4.Message
import vortex.protocol.v4.Server
import vortex.protocol.v4.decodeBody
import vortex.protocol.v4.encodeBody
import vortex.protocol.v4.messageBodyFormat
import vortex.protocol.v4.send
import java.util.*

fun main() = runBlocking {
    val format = messageBodyFormat(InitSerializers + UniqueIdsSerializers)
    val initService = InitService()
    val uniqueIdsService = UniqueIdsService(initService)
    Server {
        val responseBody = when (val body = format.decodeBody(it.body)) {
            is Init -> initService.handle(body)
            is Generate -> uniqueIdsService.handle(body)
            else -> throw InputMismatchException("Unexpected message body: ${it.body}")
        }
        send(Message(it.dest, it.src, format.encodeBody(responseBody)))
    }.run {
        start()
        awaitTermination()
    }
}
