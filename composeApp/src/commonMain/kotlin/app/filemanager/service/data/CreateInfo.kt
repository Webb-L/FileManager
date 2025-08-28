package app.filemanager.service.data

import kotlinx.serialization.Serializable

@Serializable
data class CreateInfo(
    val path: String,
    val name: String,
)