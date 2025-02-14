package app.filemanager.data.device

import app.filemanager.data.main.DeviceCategory
import app.filemanager.data.main.DeviceConnectType
import app.filemanager.data.main.DeviceType

data class DeviceJoinDeviceRole(
    val id: String,
    val name: String,
    val type: DeviceType,
    val connectionType: DeviceConnectType,
    val firstConnection: Long,
    val lastConnection: Long,
    val category: DeviceCategory,
    val roleId: Long,
    val roleName: String,
)