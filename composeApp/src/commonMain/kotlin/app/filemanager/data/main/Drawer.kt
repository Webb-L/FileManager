package app.filemanager.data.main

import app.filemanager.data.file.FileProtocol
import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.file.FileSizeInfo
import app.filemanager.data.file.PathInfo
import app.filemanager.service.data.*
import app.filemanager.service.rpc.HttpRouteClientManager
import app.filemanager.service.rpc.RpcShareClientManager
import app.filemanager.ui.state.file.FileState
import app.filemanager.ui.state.main.DeviceState
import app.filemanager.ui.state.main.Task
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

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
    val host: MutableMap<String, HttpRouteClientManager>,
    val type: DeviceType,
    val token: String
) : DiskBase(), KoinComponent {

    private val fileState: FileState by inject()
    private val deviceState: DeviceState by inject()

    private fun getConnect(): HttpRouteClientManager {
        return host.values.first()
    }

    private fun handleError() {
        println("sdfsfafsafasfsdfs")
        deviceState.devices.remove(this)
        deviceState.socketDevices.indexOfFirst { it.id == id }.takeIf { it != -1 }?.let { index ->
            deviceState.socketDevices[index] = deviceState.socketDevices[index].withCopy(
                connectType = ConnectType.Fail
            )
        }
        fileState.updateDesk(FileProtocol.Local, Local())
    }


    suspend fun getRootPaths(replyCallback: (Result<List<PathInfo>>) -> Unit) {
        try {
            replyCallback(getConnect().pathRouteClient.getRootPaths())
        } catch (e: Exception) {
            handleError()
        }
    }

    suspend fun getFileList(path: String, replyCallback: (Result<List<FileSimpleInfo>>) -> Unit) {
        try {
            replyCallback(getConnect().pathRouteClient.listPath(ListRequest(path)))
        } catch (e: Exception) {
            handleError()
        }
    }

    suspend fun getTraversePath(path: String, replyCallback: (Result<List<FileSimpleInfo>>) -> Unit) {
        try {
            val fileSimpleInfos = mutableListOf<FileSimpleInfo>()
            getConnect().pathRouteClient.traversePath(TraversePathRequest(path)).collect { result ->
                fileSimpleInfos.addAll(result.getOrDefault(listOf()))
            }
            replyCallback(Result.success(fileSimpleInfos))
        } catch (e: Exception) {
            handleError()
        }
    }

    suspend fun getBookmark(replyCallback: (Result<List<DrawerBookmark>>) -> Unit) {
        try {
            replyCallback(getConnect().bookmarkRouteClient.getBookmarks())
        } catch (e: Exception) {
            handleError()
        }
    }

    suspend fun renames(renameInfos: List<RenameInfo>, replyCallback: (Result<List<Result<Boolean>>>) -> Unit) {
        try {
            replyCallback(getConnect().fileRouteClient.renames(RenameRequest(renameInfos)))
        } catch (e: Exception) {
            handleError()
        }
    }

    suspend fun createFolders(paths: List<CreateInfo>, replyCallback: (Result<List<Result<Boolean>>>) -> Unit) {
        try {
            replyCallback(getConnect().fileRouteClient.createFolders(CreateFolderRequest(paths)))
        } catch (e: Exception) {
            handleError()
        }
    }

    suspend fun getFileSizeInfo(
        fileSimpleInfo: FileSimpleInfo,
        totalSpace: Long,
        freeSpace: Long,
        replyCallback: (Result<FileSizeInfo>) -> Unit
    ) {
        try {
            replyCallback(
                getConnect().fileRouteClient.getSizeInfo(
                    GetSizeInfoRequest(
                        totalSpace,
                        freeSpace,
                        fileSimpleInfo
                    )
                )
            )
        } catch (e: Exception) {
            handleError()
        }
    }

    suspend fun deletes(
        paths: List<String>,
        replyCallback: (Result<List<Result<Boolean>>>) -> Unit
    ) {
        try {
            replyCallback(getConnect().fileRouteClient.deletes(DeleteRequest(paths)))
        } catch (e: Exception) {
            handleError()
        }
    }

//    suspend fun writeBytes(
//        srcPath: String,
//        destPath: String,
//        replyCallback: (Result<Boolean>) -> Unit
//    ) {
//        try {
//            getConnect().fileRouteClient.writeBytes(id, srcPath, destPath, replyCallback)
//        } catch (e: Exception) {
//            handleError()
//        }
//    }

    suspend fun copyFile(
        task: Task,
        srcFileSimpleInfo: FileSimpleInfo,
        destFileSimpleInfo: FileSimpleInfo,
        replyCallback: (Result<Boolean>) -> Unit
    ) {
        try {
            getConnect().pathRouteClient.copyFile(task, srcFileSimpleInfo, destFileSimpleInfo, replyCallback)
        } catch (e: Exception) {
            handleError()
        }
    }

    suspend fun getFile(path: String, replyCallback: (Result<FileSimpleInfo>) -> Unit) {
        try {
            replyCallback(getConnect().fileRouteClient.getFileByPath(GetFileByPathRequest(path)))
        } catch (e: Exception) {
            handleError()
        }
    }
}

data class Share(
    val id: String,
    override val name: String,
    val rpcClientManager: RpcShareClientManager,
    val type: DeviceType,
    val token: String = ""
) : DiskBase(), KoinComponent {
    private val fileState: FileState by inject()
    private val deviceState: DeviceState by inject()

    private fun handleError() {
        deviceState.shares.remove(this)
        fileState.updateDesk(FileProtocol.Local, Local())
        println("Error in sharing")
    }

    suspend fun getFileList(path: String, replyCallback: (Result<List<FileSimpleInfo>>) -> Unit) {
        try {
            rpcClientManager.getList(path, id, replyCallback)
        } catch (e: Exception) {
            println("Error in sharing $e")
            handleError()
        }
    }

    suspend fun copyFile(
        srcFileSimpleInfo: FileSimpleInfo,
        destFileSimpleInfo: FileSimpleInfo,
        replyCallback: (Result<Boolean>) -> Unit
    ) {
        try {
            rpcClientManager.copyFile(srcFileSimpleInfo, destFileSimpleInfo, replyCallback)
        } catch (e: Exception) {
            handleError()
        }
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
