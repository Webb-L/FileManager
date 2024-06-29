package app.filemanager.data.file

import kotlinx.serialization.Serializable

@Serializable
data class PathInfo(
    val path: String,
    val totalSpace: Long,
    val freeSpace: Long,
)