package app.filemanager.ui.state.device

import androidx.compose.runtime.mutableStateListOf
import app.filemanager.data.device.DeviceRole
import app.filemanager.db.FileManagerDatabase

class DeviceRoleState(private val database: FileManagerDatabase) {
    val roles = mutableStateListOf<DeviceRole>()

    init {
        val map = database.deviceRoleQueries.selectAll().executeAsList().map {
            DeviceRole(
                id = it.id,
                name = it.name,
                comment = it.comment,
                sortOrder = it.sortOrder,
                permissionCount = it.roleCount
            )
        }
        roles.addAll(map)
    }

    fun delete(deviceRole: DeviceRole, index: Int) {
        database.devicePermissionQueries.deleteById(deviceRole.id)
        roles.removeAt(index)
    }
}