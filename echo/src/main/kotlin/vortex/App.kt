package vortex

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import vortex.protocol.v4.Init
import vortex.protocol.v4.InitSerializers
import vortex.protocol.v4.InitService
import vortex.protocol.v4.MessageBody
import vortex.protocol.v4.RequestBody
import vortex.protocol.v4.ResponseBody
import vortex.protocol.v4.Server
import vortex.protocol.v4.messageBodyFormat
import java.util.*

@Serializable
@SerialName("echo")
data class Echo(
    val echo: String,
    override val inReplyTo: Int? = null,
    override val msgId: Int
): RequestBody

@Serializable
@SerialName("echo_ok")
data class EchoOk(
    val echo: String,
    override val msgId: Int? = null,
    override val inReplyTo: Int
): ResponseBody

private val EchoSerializers = SerializersModule {
    polymorphic(MessageBody::class) {
        subclass(Echo::class)
        subclass(EchoOk::class)
    }
}

fun main() {
    val format = messageBodyFormat(InitSerializers + EchoSerializers)
    val initService = InitService()
    Server(format) { _, _, body ->
        when (body) {
            is Init -> initService.handle(body)
            is Echo -> EchoOk(echo = body.echo, inReplyTo = body.msgId)
            else -> throw InputMismatchException("Unexpected message body: $body")
        }
    }.serve()
}
