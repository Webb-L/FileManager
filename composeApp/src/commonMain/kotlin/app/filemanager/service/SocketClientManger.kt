package app.filemanager.service

import app.filemanager.service.data.SocketDevice
import app.filemanager.service.handle.BookmarkHandle
import app.filemanager.service.handle.FileHandle
import app.filemanager.service.handle.PathHandle
import app.filemanager.service.response.BookmarkResponse
import app.filemanager.service.response.FileResponse
import app.filemanager.service.response.PathResponse
import app.filemanager.service.socket.SocketClient
import app.filemanager.service.socket.createSocketClient
import app.filemanager.ui.state.main.DeviceState
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SocketClientManger : KoinComponent, BaseSocketManager("client") {
    override val socket: SocketClient = createSocketClient()

    private val deviceState by inject<DeviceState>()

    internal val pathHandle by lazy { PathHandle(this) }
    internal val fileHandle by lazy { FileHandle(this) }
    internal val bookmarkHandle by lazy { BookmarkHandle(this) }

    private val pathResponse by lazy { PathResponse(this) }
    private val fileResponse by lazy { FileResponse(this) }
    private val bookmarkResponse by lazy { BookmarkResponse(this) }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun connect(host: String = "127.0.0.1", port: Int = 1204) {
        socket.connect(host, port) { message ->
            when (message.header.command) {
                "connect" -> {
                    val socketDevice = ProtoBuf.decodeFromByteArray<SocketDevice>(message.body)
                    socketDevice.socketManger = this@SocketClientManger
                    deviceState.socketDevices.add(socketDevice)
                }

                "cancelKey" -> cancelKeys.add((message.params["replyKey"] ?: "0").toLong())

                "replyRootPaths" -> pathResponse.replyRootPaths(message)
                "replyList" -> pathResponse.replyList(message)
                "replyTraversePath" -> pathResponse.replyTraversePath(message)
                "replyBookmark" -> bookmarkResponse.replyBookmark(message)
                "replyRename" -> fileResponse.replyRename(message)
                "replyCreateFolder" -> fileResponse.replyCreateFolder(message)
                "replyGetSizeInfo" -> fileResponse.replyGetSizeInfo(message)
                "replyDeleteFile" -> fileResponse.replyDeleteFile(message)
                "replyWriteBytes" -> fileResponse.replyWriteBytes(message)

                else -> {
                    println("【client】未能匹配上： header = ${message.header} params = ${message.params} it=${message.body.size}")
                }
            }
        }
    }
}