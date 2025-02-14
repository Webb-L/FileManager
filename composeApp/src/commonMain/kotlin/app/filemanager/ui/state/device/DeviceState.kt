package app.filemanager.ui.state.device

import androidx.compose.runtime.mutableStateListOf
import app.filemanager.data.device.DeviceJoinDeviceRole
import app.filemanager.data.main.DeviceCategory
import app.filemanager.data.main.DeviceConnectType
import app.filemanager.db.FileManagerDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DeviceSettingsState(private val database: FileManagerDatabase) {
    val devices = mutableStateListOf<DeviceJoinDeviceRole>()

    private val _category: MutableStateFlow<DeviceCategory> = MutableStateFlow(DeviceCategory.SERVER)
    val category: StateFlow<DeviceCategory> = _category

    fun updateCategory(value: DeviceCategory) {
        _category.value = value
    }

    private val _deviceName: MutableStateFlow<String> = MutableStateFlow("")
    val deviceName: StateFlow<String> = _deviceName

    fun updatedDeviceName(value: String) {
        _deviceName.value = value
    }

    val onRefresh: () -> Unit = {
        devices.apply {
            clear()
            addAll(
                database.deviceQueries.queryByNameLikeAndCategory(
                    "%${_deviceName.value}%",
                    _category.value
                ).executeAsList().map {
                    DeviceJoinDeviceRole(
                        id = it.id,
                        name = it.name,
                        type = it.type,
                        connectionType = it.connectionType,
                        firstConnection = it.firstConnection,
                        lastConnection = it.lastConnection,
                        category = it.category,
                        roleId = it.roleId,
                        roleName = it.roleName ?: ""
                    )
                }
            )
        }
    }

    fun updateDeviceCategory(connectionType: DeviceConnectType, deviceId: String, category: DeviceCategory) {
        database.deviceQueries.updateConnectionTypeByIdAndCategory(
            connectionType,
            deviceId,
            category
        )
        // TODO 更新设备
        onRefresh()
    }

    fun updateDevice(index: Int, device: DeviceJoinDeviceRole) {
        database.deviceQueries.updateNameConnectTypeRoleIdByIdAndCategory(
            device.name,
            device.connectionType,
            device.roleId,
            device.id,
            device.category
        )

        database.deviceQueries.queryById(device.id, device.category).executeAsOneOrNull()?.let {
            devices[index] = DeviceJoinDeviceRole(
                id = it.id,
                name = it.name,
                type = it.type,
                connectionType = it.connectionType,
                firstConnection = it.firstConnection,
                lastConnection = it.lastConnection,
                category = it.category,
                roleId = it.roleId,
                roleName = database.deviceRoleQueries.selectNameById(it.roleId).executeAsOneOrNull() ?: ""
            )
        }
    }
}