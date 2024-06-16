package app.filemanager.service.request

import app.filemanager.data.file.FileProtocol
import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.extensions.getFileAndFolder
import app.filemanager.service.MAX_LENGTH
import app.filemanager.service.WebSocketConnectService
import app.filemanager.utils.PathUtils
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToHexString
import kotlinx.serialization.protobuf.ProtoBuf

class PathRequest(private val webSocketConnectService: WebSocketConnectService) {
    // 远程设备需要我本地文件
    // TODO 检查权限
    @OptIn(ExperimentalSerializationApi::class)
    fun sendList(id: String, replyKey: Long, directory: String) {
        val fileAndFolder = directory.getFileAndFolder()
        var index = 1
        val sendFileSimpleInfos = mutableMapOf<Pair<FileProtocol, String>, MutableList<FileSimpleInfo>>().apply {
            fileAndFolder.forEach { fileSimpleInfo ->
                val key = if (fileSimpleInfo.protocol == FileProtocol.Local)
                    Pair(FileProtocol.Device, fileSimpleInfo.protocolId)
                else
                    Pair(fileSimpleInfo.protocol, fileSimpleInfo.protocolId)

                if (!containsKey(key)) {
                    put(key, mutableListOf(fileSimpleInfo.apply {
                        this.path = this.path.replace(directory, "")
                        this.protocol = FileProtocol.Local
                        this.protocolId = ""
                    }))
                } else {
                    get(key)?.add(fileSimpleInfo.apply {
                        this.path = this.path.replace(directory, "")
                        this.protocol = FileProtocol.Local
                        this.protocolId = ""
                    })
                }
            }
        }

        val sendContent = ProtoBuf.encodeToHexString(sendFileSimpleInfos)

        println("send length ${sendContent.length}")
        val count = if (sendContent.length > MAX_LENGTH) sendContent.length / MAX_LENGTH else 1
        val chunked = sendContent.chunked(sendContent.length / count)

        MainScope().launch {
            chunked.forEach {
                webSocketConnectService.send(
                    "/reply_list",
                    "$id $replyKey",
                    "$index ${chunked.size}",
                    it
                )

                index++
            }
        }
    }

    // 远程设备需要我本地的书签
    // TODO 检查权限
    fun sendRootPaths(id: String, replyKey: Long) {
        MainScope().launch {
            val rootPaths = PathUtils.getRootPaths()
            webSocketConnectService.send(command = "/reply_root_paths", header = "$id $replyKey", value = rootPaths)
        }
    }
}