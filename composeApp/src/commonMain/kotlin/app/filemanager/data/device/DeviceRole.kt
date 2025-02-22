package app.filemanager.data.device

data class DeviceRole(
    val id: Long,
    val name: String,
    val comment: String?,
    val sortOrder: Long,
    val permissionCount: Long = 0,
)
