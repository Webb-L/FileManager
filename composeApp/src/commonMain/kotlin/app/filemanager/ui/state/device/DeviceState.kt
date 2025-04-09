package app.filemanager.ui.state.device

import androidx.compose.runtime.mutableStateListOf
import app.filemanager.data.device.DeviceJoinDeviceRole
import app.filemanager.data.main.DeviceCategory
import app.filemanager.data.main.DeviceConnectType
import app.filemanager.db.FileManagerDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 设备设置状态管理类
 * 
 * 负责管理设备相关的状态和数据操作，包括:
 * - 设备列表的维护
 * - 设备分类的更新
 * - 设备名称的更新
 * - 设备信息的刷新
 * 
 * @property database 文件管理数据库实例
 */
class DeviceSettingsState(private val database: FileManagerDatabase) {
    /**
     * 设备列表，包含设备及其角色信息
     * 使用mutableStateListOf以便Compose可以观察变化
     */
    val devices = mutableStateListOf<DeviceJoinDeviceRole>()

    private val _category: MutableStateFlow<DeviceCategory> = MutableStateFlow(DeviceCategory.SERVER)
    /**
     * 当前设备分类的状态流
     * 默认为SERVER类型
     */
    val category: StateFlow<DeviceCategory> = _category

    /**
     * 更新设备分类
     * @param value 新的设备分类值
     */
    fun updateCategory(value: DeviceCategory) {
        _category.value = value
    }

    private val _deviceName: MutableStateFlow<String> = MutableStateFlow("")
    /**
     * 设备名称的状态流
     * 用于设备搜索过滤
     */
    val deviceName: StateFlow<String> = _deviceName

    /**
     * 更新设备名称
     * @param value 新的设备名称
     */
    fun updateDeviceName(value: String) {
        _deviceName.value = value
    }

    /**
     * 刷新设备列表的回调函数
     * 根据当前设备名称和分类从数据库重新加载设备列表
     */
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

    /**
     * 更新设备的连接类型和分类
     * @param connectionType 新的连接类型
     * @param deviceId 要更新的设备ID
     * @param category 设备分类
     */
    fun updateDeviceCategory(connectionType: DeviceConnectType, deviceId: String, category: DeviceCategory) {
        database.deviceQueries.updateConnectionTypeByIdAndCategory(
            connectionType,
            deviceId,
            category
        )
        // TODO 更新设备
        onRefresh()
    }

    /**
     * 更新设备信息
     * @param index 设备在列表中的索引
     * @param device 包含更新后信息的设备对象
     */
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
