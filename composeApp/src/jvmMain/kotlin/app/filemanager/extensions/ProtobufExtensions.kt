package app.filemanager.extensions

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf

@OptIn(ExperimentalSerializationApi::class)
suspend inline fun <reified T> ApplicationCall.respondProtobuf(value: T) {
    respondBytes(ContentType.Application.ProtoBuf) { ProtoBuf.encodeToByteArray(value) }
}