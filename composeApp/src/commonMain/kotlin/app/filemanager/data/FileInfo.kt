package app.filemanager.data

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
    }
}