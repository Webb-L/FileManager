package app.filemanager.data.file

import app.filemanager.data.main.DeviceType
import app.filemanager.ui.state.file.FileShareStatus
import kotlinx.datetime.Instant

/**
 * 表示文件分享历史记录的数据类
 * 包含文件信息、设备信息和传输状态
 */
data class FileShareHistory(
    // 历史记录基本信息
    val id: Long,
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val isDirectory: Boolean,
    val isOutgoing: Boolean,
    val timestamp: Long,
    val status: FileShareStatus,
    val errorMessage: String?,
    val savePath: String?,

    // 源设备信息
    val sourceDeviceId: String,
    val sourceDeviceName: String,
    val sourceDeviceType: DeviceType,

    // 目标设备信息
    val targetDeviceId: String,
    val targetDeviceName: String,
    val targetDeviceType: DeviceType
) {
    /**
     * 获取格式化的时间
     */
    val formattedTime: String
        get() = Instant.fromEpochMilliseconds(timestamp).toString()

    /**
     * 判断是否为成功的传输
     */
    val isSuccessful: Boolean
        get() = status == FileShareStatus.COMPLETED

    /**
     * 判断是否为正在进行中的传输
     */
    val isInProgress: Boolean
        get() = status == FileShareStatus.SENDING || status == FileShareStatus.WAITING

    /**
     * 获取传输方向的描述
     */
    val transferDirection: String
        get() = if (isOutgoing) "发送到" else "接收自"
        
    companion object {
        /**
         * SQLDelight查询结果映射器
         * 参数顺序和类型与查询结果列完全匹配
         */
        fun mapper(
            id: Long,
            fileName: String,
            filePath: String,
            fileSize: Long,
            isDirectory: Boolean,
            sourceDeviceId: String,
            targetDeviceId: String,
            isOutgoing: Boolean,
            timestamp: Long,
            status: FileShareStatus,
            errorMessage: String?,
            savePath: String?,
            sourceDeviceName: String,
            sourceDeviceType: DeviceType,
            targetDeviceName: String,
            targetDeviceType: DeviceType
        ): FileShareHistory {
            return FileShareHistory(
                id = id,
                fileName = fileName,
                filePath = filePath,
                fileSize = fileSize,
                isDirectory = isDirectory,
                sourceDeviceId = sourceDeviceId,
                targetDeviceId = targetDeviceId,
                isOutgoing = isOutgoing,
                timestamp = timestamp,
                status = status,
                errorMessage = errorMessage,
                savePath = savePath,
                sourceDeviceName = sourceDeviceName,
                sourceDeviceType = sourceDeviceType,
                targetDeviceName = targetDeviceName,
                targetDeviceType = targetDeviceType
            )
        }
    }
}