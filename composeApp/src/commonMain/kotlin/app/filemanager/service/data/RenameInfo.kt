package app.filemanager.service.data

import kotlinx.serialization.Serializable

@Serializable
data class RenameInfo(
    val path: String,
    val oldName: String,
    val newName: String
) {
    /**
     * 校验 path、oldName、newName 是否任意为空
     */
    fun hasEmptyField(): Boolean {
        return path.isBlank() || oldName.isBlank() || newName.isBlank()
    }
}