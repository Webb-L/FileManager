package app.filemanager.service.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf

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
    return ProtoBuf.decodeFromByteArray<T>(requestBody)
}

@OptIn(ExperimentalSerializationApi::class)
suspend inline fun <reified T> ApplicationCall.respondProtobuf(value: T) {
    val responseBytes = ProtoBuf.encodeToByteArray(value)
    respondBytes(responseBytes)
}