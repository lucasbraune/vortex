package vortex

import kotlinx.serialization.modules.plus
import vortex.protocol.v4.Init
import vortex.protocol.v4.InitSerializers
import vortex.protocol.v4.InitService
import vortex.protocol.v4.Server
import vortex.protocol.v4.messageBodyFormat
import java.util.*

fun main() {
    val format = messageBodyFormat(InitSerializers + UniqueIdsSerializers)
    val initService = InitService()
    val uniqueIdsService = UniqueIdsService(initService)
    Server(format) { _, _, body ->
        when (body) {
            is Init -> initService.handle(body)
            is Generate -> uniqueIdsService.handle(body)
            else -> throw InputMismatchException("Unexpected message body: $body")
        }
    }.serve()
}
