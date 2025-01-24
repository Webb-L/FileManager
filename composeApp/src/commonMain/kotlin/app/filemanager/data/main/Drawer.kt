package app.filemanager.data.main

import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.file.FileSizeInfo
import app.filemanager.data.file.PathInfo
import app.filemanager.service.SocketClientManger
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

@Serializable
enum class DeviceType(type: String) {
    Android("Android"),
    IOS("IOS"),
    JVM("JVM"),
    JS("JS")
}


data class Device(
    val id: String,
    override val name: String,
    val host: MutableMap<String, SocketClientManger>,
    val type: DeviceType
) : DiskBase() {
    private fun getConnect(): SocketClientManger? {
//        for (key in host.keys) {
//            if (key.isPrivateIPAddress()) {
//                return host[key]
//            }
//        }
        return host.values.first()
    }

    suspend fun getRootPaths(replyCallback: (List<PathInfo>) -> Unit) {
        getConnect()?.pathHandle?.getRootPaths(id, replyCallback)
    }

    suspend fun getFileList(path: String, replyCallback: (Result<List<FileSimpleInfo>>) -> Unit) {
        getConnect()?.pathHandle?.getList(path, id, replyCallback)
    }

    suspend fun getTraversePath(path: String, replyCallback: (Result<List<FileSimpleInfo>>) -> Unit) {
        getConnect()?.pathHandle?.getTraversePath(path, id, replyCallback)
    }

    suspend fun getBookmark(replyCallback: (Result<List<DrawerBookmark>>) -> Unit) {
        getConnect()?.bookmarkHandle?.getBookmark(id, replyCallback)
    }

    suspend fun rename(path: String, oldName: String, newName: String, replyCallback: (Result<Boolean>) -> Unit) {
        getConnect()?.fileHandle?.rename(id, path, oldName, newName, replyCallback)
    }

    suspend fun createFolder(path: String, name: String, replyCallback: (Result<Boolean>) -> Unit) {
        getConnect()?.fileHandle?.createFolder(id, path, name, replyCallback)
    }

    suspend fun getFileSizeInfo(
        fileSimpleInfo: FileSimpleInfo,
        totalSpace: Long,
        freeSpace: Long,
        replyCallback: (Result<FileSizeInfo>) -> Unit
    ) {
        getConnect()?.fileHandle?.getFileSizeInfo(id, fileSimpleInfo, totalSpace, freeSpace, replyCallback)
    }

    suspend fun deleteFile(
        paths: List<String>,
        replyCallback: (Result<List<Boolean>>) -> Unit
    ) {
        getConnect()?.fileHandle?.deleteFile(id, paths, replyCallback)
    }

    suspend fun writeBytes(
        srcPath: String,
        destPath: String,
        replyCallback: (Result<Boolean>) -> Unit
    ) {
        getConnect()?.fileHandle?.writeBytes(id, srcPath, destPath, replyCallback)
    }

    suspend fun copyFile(
        srcPath: String,
        destPath: String,
        replyCallback: (Result<Boolean>) -> Unit
    ) {
        getConnect()?.pathHandle?.copyFile(id, srcPath, destPath, replyCallback)
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
