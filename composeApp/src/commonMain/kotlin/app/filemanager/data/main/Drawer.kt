package app.filemanager.data.main

import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.extensions.isPrivateIPAddress
import app.filemanager.service.WebSocketConnectService
import kotlinx.serialization.Serializable

enum class DrawerBookmarkType {
    Home,
    Image,
    Audio,
    Video,
    Document,
    Download,
    Custom
}

@Serializable
data class DrawerBookmark(
    val name: String,
    val path: String,
    val iconType: DrawerBookmarkType,
    val iconPath: String = ""
)


abstract class DiskBase {
    abstract val name: String
}

data class Local(
    override val name: String = "本地"
) : DiskBase()

enum class DeviceType(type: String) {
    Android("Android"),
    IOS("IOS"),
    JVM("JVM"),
    JS("JS")
}

data class Device(
    val id: String,
    override val name: String,
    val host: MutableMap<String, WebSocketConnectService>,
    val type: DeviceType
) : DiskBase() {
    fun getConnect(): WebSocketConnectService? {
        for (key in host.keys) {
            if (key.isPrivateIPAddress()) {
                return host[key]
            }
        }
        return host.values.first()
    }

    suspend fun getRootPaths(replyCallback: (List<String>) -> Unit) {
        getConnect()?.pathHandle?.getRootPaths(id, replyCallback)
    }

    suspend fun getFileList(path: String, replyCallback: (List<FileSimpleInfo>) -> Unit) {
        getConnect()?.pathHandle?.getList(path, id, replyCallback)
    }

    suspend fun getBookmark(replyCallback: (List<DrawerBookmark>) -> Unit) {
        getConnect()?.bookmarkHandle?.getBookmark(id, replyCallback)
    }

    suspend fun rename(path: String, oldName: String, newName: String) {
        getConnect()?.fileHandle?.rename(id, path, oldName, newName)
    }
}

enum class NetworkProtocol {
    FTP,
    SFTP,
    SMB,
    WebDav,
}

data class Network(
    override val name: String,
    val protocol: String,
    val host: String,
    val username: String,
    val password: String
) : DiskBase()
