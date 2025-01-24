package app.filemanager.data.file

import app.filemanager.utils.PathUtils
import kotlinx.serialization.Serializable

@Serializable
data class FileSimpleInfo(
    val name: String,
    val description: String = "",
    val isDirectory: Boolean,
    val isHidden: Boolean,
    var path: String,
    val mineType: String,
    val size: Long,
    val createdDate: Long,
    val updatedDate: Long,
    var protocol: FileProtocol = FileProtocol.Local,
    var protocolId: String = "",
) {
    companion object {
        fun nullFileSimpleInfo() = FileSimpleInfo(
            name = "",
            description = "",
            isDirectory = false,
            isHidden = false,
            path = "",
            mineType = "",
            size = 0,
            createdDate = 0,
            updatedDate = 0
        )

        fun pathFileSimpleInfo(path: String) = FileSimpleInfo(
            name = path,
            description = "",
            isDirectory = true,
            isHidden = false,
            path = path,
            mineType = "",
            size = 0,
            createdDate = 0,
            updatedDate = 0
        )
    }

    fun toFileInfo(
        permissions: Int,
        user: String,
        userGroup: String,
    ): FileInfo = FileInfo(
        name = name,
        description = description,
        isDirectory = isDirectory,
        isHidden = isHidden,
        path = path,
        mineType = mineType,
        size = size,
        permissions = permissions,
        user = user,
        userGroup = userGroup,
        createdDate = createdDate,
        updatedDate = updatedDate,
        protocol = protocol,
        protocolId = protocolId,
    )

    suspend fun getSizeInfo(totalSpace: Long, freeSpace: Long): FileSizeInfo {
        var fileCount = 0L
        var folderCount = 0L
        var fileSize = -1L
        if (isDirectory) {
            PathUtils.traverse(path) {
                if (it.isSuccess) {
                    fileSize += it.getOrNull()?.sumOf { fileSimpleInfo ->
                        if (fileSimpleInfo.isDirectory) {
                            folderCount++
                            0
                        } else {
                            fileCount++
                            fileSimpleInfo.size
                        }
                    } ?: 0
                }
            }
        } else {
            fileSize = size
        }
        return FileSizeInfo(
            fileSize = fileSize,
            fileCount = fileCount,
            folderCount = folderCount,
            totalSpace = totalSpace,
            freeSpace = freeSpace
        )
    }

    fun withCopy(
        name: String? = null,
        description: String? = null,
        isDirectory: Boolean? = null,
        isHidden: Boolean? = null,
        path: String? = null,
        mineType: String? = null,
        size: Long? = null,
        createdDate: Long? = null,
        updatedDate: Long? = null,
        protocol: FileProtocol? = null,
        protocolId: String? = null,
    ): FileSimpleInfo {
        return FileSimpleInfo(
            name = name ?: this.name,
            description = description ?: this.description,
            isDirectory = isDirectory ?: this.isDirectory,
            isHidden = isHidden ?: this.isHidden,
            path = path ?: this.path,
            mineType = mineType ?: this.mineType,
            size = size ?: this.size,
            createdDate = createdDate ?: this.createdDate,
            updatedDate = updatedDate ?: this.updatedDate,
            protocol = protocol ?: this.protocol,
            protocolId = protocolId ?: this.protocolId,
        )
    }
}

@Serializable
data class FileInfo(
    val name: String,
    val description: String = "",
    val isDirectory: Boolean,
    val isHidden: Boolean,
    val path: String,
    val mineType: String,
    val size: Long,
    val permissions: Int,
    val user: String,
    val userGroup: String,
    val createdDate: Long,
    val updatedDate: Long,
    var protocol: FileProtocol = FileProtocol.Local,
    var protocolId: String = "",
) {
    companion object {
        fun nullFileInfo() = FileInfo(
            name = "",
            description = "",
            isDirectory = false,
            isHidden = false,
            path = "",
            mineType = "",
            size = 0,
            permissions = 0,
            user = "",
            userGroup = "",
            createdDate = 0,
            updatedDate = 0
        )

        fun pathFileInfo(path: String) = FileInfo(
            name = path,
            description = "",
            isDirectory = true,
            isHidden = false,
            path = path,
            mineType = "",
            size = 0,
            permissions = 0,
            user = "",
            userGroup = "",
            createdDate = 0,
            updatedDate = 0
        )
    }
}

@Serializable
data class FileSizeInfo(
    val fileSize: Long = 0,
    val fileCount: Long = 0,
    val folderCount: Long = 0,
    val totalSpace: Long = 0,
    val freeSpace: Long = 0,
)