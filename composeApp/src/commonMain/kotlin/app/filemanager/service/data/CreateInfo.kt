package app.filemanager.service.data

import kotlinx.serialization.Serializable

/**
 * 创建文件或目录的信息数据类
 * 
 * @property path 文件或目录的路径
 * @property name 文件或目录的名称
 */
@Serializable
data class CreateInfo(
    val path: String,
    val name: String,
) {
    /**
     * 将路径和名称拼接成完整路径
     * 
     * @return 完整的文件路径
     */
    fun join(): String = path + name
}