package app.filemanager.service.handle

import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.main.DeviceConnectType
import app.filemanager.service.data.SocketDevice
import app.filemanager.service.rpc.RpcShareClientManager
import app.filemanager.ui.state.file.FileState
import app.filemanager.ui.state.main.DeviceState
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ShareHandle(private val rpc: RpcShareClientManager) : KoinComponent {
    val fileState: FileState by inject()
    val deviceState: DeviceState by inject()

    /**
     * 从远程设备获取指定路径下的文件和文件夹列表。
     *
     * @param path 要获取列表的路径。
     * @param remoteId 远程设备的ID。
     */
    suspend fun getList(path: String, remoteId: String, replyCallback: (Result<List<FileSimpleInfo>>) -> Unit) {
        val fileSimpleInfos: MutableList<FileSimpleInfo> = mutableListOf()

        val result = rpc.shareService.list(rpc.token, path)
        if (!result.isSuccess) {
            replyCallback(Result.failure(result.deSerializable()))
            return
        }

        result.value?.forEach {
            it.value.forEach { fileSimpleInfo ->
                fileSimpleInfos.add(fileSimpleInfo.apply {
                    protocol = it.key.first
                    protocolId = it.key.second
                    this.path = path + this.path
                })
            }
        }
        replyCallback(Result.success(fileSimpleInfos))
    }

    suspend fun connect(manager: RpcShareClientManager, device: SocketDevice) {
        val share = rpc.shareService.connect(device)
        when (share.first) {
            DeviceConnectType.AUTO_CONNECT, DeviceConnectType.APPROVED -> {
                deviceState.shares.add(device.toShare(rpc))
            }
            DeviceConnectType.PERMANENTLY_BANNED, DeviceConnectType.REJECTED -> {}
            DeviceConnectType.WAITING -> {}
        }

    }
}