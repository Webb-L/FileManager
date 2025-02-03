package app.filemanager.data.main

import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.file.FileSizeInfo
import app.filemanager.data.file.PathInfo
import app.filemanager.service.rpc.RpcClientManager
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

/**
 * 设备连接类型的枚举类，用于表示设备的连接状态。
 * @param type 设备连接类型的字符串标识。
 */
@Serializable
enum class DeviceConnectType(type: String) {
    /**
     * 表示设备连接的自动连接类型。
     *
     * AUTO_CONNECT 枚举值用于指示设备可以自动连接，无需进一步用户交互。
     */
    AUTO_CONNECT("AUTO_CONNECT"),

    /**
     * 表示设备连接状态为永久禁止连接。
     *
     * 用于指代设备被明确禁止连接到当前系统的状态。
     * 一旦设置为此状态，设备将不会再请求或尝试建立连接。
     */
    PERMANENTLY_BANNED("PERMANENTLY_BANNED"),

    /**
     * 表示设备连接已被批准的状态。
     */
    APPROVED("APPROVED"),

    /**
     * 表示设备连接已被拒绝的状态。
     *
     * REJECTED 枚举值用于指代设备的连接请求已经被明确拒绝。
     * 设备在此状态下不能建立连接，可能需要用户执行额外操作以更改状态。
     */
    REJECTED("REJECTED"),

    /**
     * 表示设备连接处于等待状态。
     *
     * WAITING 枚举值用于指代设备当前正在等待连接的状态。
     */
    WAITING("WAITING")
}

/**
 * 表示设备类别的枚举类。
 *
 * 该枚举类用于区分设备是客户端类型还是服务器类型。
 */
enum class DeviceCategory(type: String) {
    /**
     * 代表设备类别中的客户端类型。
     * CLIENT 表明该设备是客户端设备。
     * 枚举常量用来区分不同的设备类型。
     */
    CLIENT("CLIENT"),
    /**
     * 表示设备类别中的服务器类型。
     * SERVER 是设备类别的枚举常量之一，代表服务器设备。
     */
    SERVER("SERVER")
}


data class Device(
    val id: String,
    override val name: String,
    val host: MutableMap<String, RpcClientManager>,
    val type: DeviceType
) : DiskBase() {
    private fun getConnect(): RpcClientManager? {
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
