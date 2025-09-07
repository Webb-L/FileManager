package app.filemanager.service.plugins

import app.filemanager.utils.CryptoProtoBuf
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.serialization.ExperimentalSerializationApi

val ProtobufRequest = createRouteScopedPlugin("ProtobufRequest") {
    on(CallSetup) { call ->
        val contentType = call.request.contentType()
        if (contentType != ContentType.Application.ProtoBuf) {
            call.respond(HttpStatusCode.UnsupportedMediaType, "Content-Type must be application/x-protobuf")
            return@on
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
suspend inline fun <reified T> ApplicationCall.receiveProtobuf(): T {
    val requestBody = receive<ByteArray>()
    return CryptoProtoBuf.decode(requestBody)
}

@OptIn(ExperimentalSerializationApi::class)
suspend inline fun <reified T> ApplicationCall.respondProtobuf(value: T) {
    val responseBytes = CryptoProtoBuf.encode(value)
    respondBytes(responseBytes, ContentType.Application.ProtoBuf)
}
