package app.filemanager.data.file

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