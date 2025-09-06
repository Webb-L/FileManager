package app.filemanager.service.data

import kotlinx.serialization.Serializable

/**
 * 重命名文件或目录的信息数据类
 * 
 * @property path 文件或目录的路径
 * @property oldName 原始名称
 * @property newName 新的名称
 */
@Serializable
data class RenameInfo(
    val path: String,
    val oldName: String,
    val newName: String
) {
    /**
     * 校验 path、oldName、newName 是否任意为空
     * 
     * @return 如果任意字段为空或空白字符串则返回 true，否则返回 false
     */
    fun hasEmptyField(): Boolean {
        return path.isBlank() || oldName.isBlank() || newName.isBlank()
    }

    /**
     * 拼接原始完整路径
     * 
     * @return 原始文件的完整路径
     */
    fun joinOldPath(): String = path + oldName

    /**
     * 拼接新的完整路径
     * 
     * @return 重命名后文件的完整路径
     */
    fun joinNewPath(): String = path + newName
}