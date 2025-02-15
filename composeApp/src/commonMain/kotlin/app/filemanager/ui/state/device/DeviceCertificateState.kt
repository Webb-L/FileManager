package app.filemanager.ui.state.device

import app.filemanager.db.FileManagerDatabase

class DeviceCertificateState(private val database: FileManagerDatabase) {
    val permissions = mutableMapOf<String, Long>()

    fun checkPermission(token: String, path: String, permission: String): Boolean {
        if (!permissions.contains(token)) {
            return true
        }

        val permissions = database.deviceRoleDevicePermissionQueries
            .queryByRoleId(permissions[token] ?: 0L)
            .executeAsList()

        if (permissions.isEmpty()) {
            return true
        }

        val firstOrNull = permissions.firstOrNull {
            val permissionStatus = mapOf(
                "read" to it.read,
                "write" to it.write,
                "remove" to it.remove,
                "rename" to it.rename,
            )[permission] ?: false

            (permissionStatus && (path == it.path || (path.contains(it.path ?: "") && it.useAll == true)))
        }

        return firstOrNull == null
    }
}