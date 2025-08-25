package app.filemanager.service.data

import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.main.DeviceConnectType
import kotlinx.serialization.Serializable

@Serializable
data class RenameRequest(
    val renameInfos: List<RenameInfo>
)

@Serializable
data class CreateFolderRequest(
    val names: List<String>
)

@Serializable
data class GetSizeInfoRequest(
    val totalSpace: Long,
    val freeSpace: Long,
    val fileSimpleInfo: FileSimpleInfo
)

@Serializable
data class DeleteRequest(
    val names: List<String>
)

@Serializable
data class WriteBytesRequest(
    val fileSize: Long,
    val blockIndex: Long,
    val blockLength: Long,
    val path: String,
    val byteArray: ByteArray
)

@Serializable
data class ReadBytesRequest(
    val path: String,
    val blockIndex: Long,
    val blockLength: Long
)

@Serializable
data class ReadFileChunksRequest(
    val path: String,
    val chunkSize: Long
)

@Serializable
data class GetFileByPathRequest(
    val path: String
)

@Serializable
data class GetFileByPathAndNameRequest(
    val path: String,
    val name: String
)

@Serializable
data class CreateFileRequest(
    val paths: List<String>
)

@Serializable
data class DeviceConnectRequest(
    val device: SocketDevice
)

@Serializable
data class DeviceConnectResponse(
    val connectType: DeviceConnectType,
    val token: String
)