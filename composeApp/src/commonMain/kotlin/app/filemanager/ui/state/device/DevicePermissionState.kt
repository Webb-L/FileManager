package app.filemanager.ui.state.device

import androidx.compose.runtime.mutableStateListOf
import app.filemanager.db.DevicePermission
import app.filemanager.db.FileManagerDatabase

class DevicePermissionState(private val database: FileManagerDatabase) {
    val permissions = mutableStateListOf<DevicePermission>()

    init {
        permissions.addAll(database.devicePermissionQueries.select().executeAsList())
    }

    fun delete(permission: DevicePermission, index: Int) {
        database.devicePermissionQueries.deleteById(permission.id)
        permissions.removeAt(index)
    }

    fun update(permission: DevicePermission, index: Int) {
        database.devicePermissionQueries.updateById(
            path = permission.path,
            useAll = permission.useAll,
            read = permission.read,
            write = permission.write,
            remove = permission.remove,
            rename = permission.rename,
            sortOrder = permission.sortOrder,
            comment = permission.comment,
            id = permission.id
        )
        permissions[index] = permission
    }

    fun create(path: String, comment: String?) {
        database.devicePermissionQueries.insert(path, comment)
        database.devicePermissionQueries.lastInsertRowId().executeAsOneOrNull()?.let {id->
            database.devicePermissionQueries.selectById(id).executeAsOneOrNull()?.let {permission->
                permissions.add(permission)
            }
        }
    }
}