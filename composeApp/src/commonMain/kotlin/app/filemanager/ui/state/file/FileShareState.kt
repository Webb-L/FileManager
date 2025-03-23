package app.filemanager.ui.state.file

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.main.Device
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FileShareState {
    val files = mutableStateListOf<FileSimpleInfo>()

    val checkedFiles = mutableStateListOf<FileSimpleInfo>()

    val sendFile = mutableStateMapOf<String, FileShareStatus>()

    // 是否展开抽屉的状态流
    private val _connectPassword: MutableStateFlow<String> = MutableStateFlow("")
    val connectPassword: StateFlow<String> = _connectPassword

    // 更新是否展开抽屉的方法
    fun updateConnectPassword(value: String) {
        _connectPassword.value = value
    }

    // 已授权通过链接访问文件的设备
    val authorizedLinkShareDevices = mutableStateMapOf<Device, Pair<Boolean, List<FileSimpleInfo>>>()

    // 等待授权通过链接访问文件的设备
    val pendingLinkShareDevices = mutableStateListOf<Device>()

    // 被拒绝通过链接访问文件的设备
    val rejectedLinkShareDevices = mutableStateListOf<Device>()

    val shareToDevices = mutableStateMapOf<String, Pair<Boolean, List<FileSimpleInfo>>>()
}


/**
 * 表示文件共享的状态。
 */
enum class FileShareStatus {
    /** 文件正在传输中 */
    SENDING,

    /** 文件共享被拒绝 */
    REJECTED,

    /** 文件共享发生错误 */
    ERROR,

    /** 文件共享已同意 */
    COMPLETED,

    /** 文件共享等待中 */
    WAITING
}

/**
 * 表示文件共享的状态类型。
 * 用于定义文件共享操作的当前状态。
 */
enum class FileShareLikeCategory {
    /**
     * 表示文件共享过程中的等待状态。
     * 此状态下操作可能尚未开始或正在排队中。
     */
    WAITING,

    /**
     * 表示文件分享操作正在进行的状态。
     */
    RUNNING,

    /**
     * 表示文件共享中被拒绝的状态。
     *
     * 此状态表明共享操作已经被明确拒绝，无法继续进行。
     */
    REJECTED
}