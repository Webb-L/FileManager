package app.filemanager.ui.state.device

import app.filemanager.db.FileManagerDatabase

class DeviceCertificateState(private val database: FileManagerDatabase) {
    // Map<连接的token, 角色id>
    private val permissions = mutableMapOf<String, Long>()

    // Map<设备ID, 连接token>
    private val deviceTokens = mutableMapOf<String, String>()

    /**
     * 设置设备权限
     * @param deviceId 设备ID
     * @param roleId 角色ID
     */
    fun setDevicePermission(deviceId: String, roleId: Long) {
        deviceTokens[deviceId]?.let { token ->
            permissions[token] = roleId
        }
    }

    /**
     * 设置设备token和权限
     * @param deviceId 设备ID
     * @param token 连接token
     * @param roleId 角色ID
     */
    fun setDeviceTokenAndPermission(deviceId: String, token: String, roleId: Long) {
        deviceTokens[deviceId] = token
        permissions[token] = roleId
    }

    /**
     * 移除设备权限
     * @param deviceId 设备ID
     */
    fun removeDevicePermission(deviceId: String) {
        deviceTokens[deviceId]?.let { token ->
            permissions.remove(token)
        }
    }

    /**
     * 设置设备token
     * @param deviceId 设备ID
     * @param token 连接token
     */
    fun setDeviceToken(deviceId: String, token: String) {
        deviceTokens[deviceId] = token
    }

    /**
     * 移除设备token
     * @param deviceId 设备ID
     */
    fun removeDeviceToken(deviceId: String) {
        deviceTokens[deviceId]?.let { token ->
            permissions.remove(token)
        }
        deviceTokens.remove(deviceId)
    }

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