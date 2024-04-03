package app.filemanager.ui.state.main

import app.filemanager.data.file.FileProtocol
import app.filemanager.data.main.Device
import app.filemanager.data.main.DiskBase
import app.filemanager.data.main.Local
import app.filemanager.data.main.Network
import app.filemanager.utils.PathUtils.getHomePath
import app.filemanager.utils.PathUtils.getRootPaths
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainState {
    private val _isExpandDrawer: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isExpandDrawer: StateFlow<Boolean> = _isExpandDrawer
    fun updateExpandDrawer(value: Boolean) {
        _isExpandDrawer.value = value
    }

    private val _isFavorite: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite
    fun updateFavorite(value: Boolean) {
        _isFavorite.value = value
    }

    private val _isEditPath: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isEditPath: StateFlow<Boolean> = _isEditPath
    fun updateEditPath(value: Boolean) {
        _isEditPath.value = value
    }

    private val _rootPath: MutableStateFlow<String> = MutableStateFlow(getRootPaths().first())
    val rootPath: StateFlow<String> = _rootPath
    fun updateRootPath(value: String) {
        _rootPath.value = value
    }

    private val _path: MutableStateFlow<String> = MutableStateFlow(getHomePath())
    val path: StateFlow<String> = _path
    fun updatePath(value: String) {
        _path.value = value
    }

    private val _isCreateFolder: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isCreateFolder: StateFlow<Boolean> = _isCreateFolder
    fun updateCreateFolder(value: Boolean) {
        _isCreateFolder.value = value
    }


    private val _deskType: MutableStateFlow<DiskBase> = MutableStateFlow(Local())
    val deskType: StateFlow<DiskBase> = _deskType
    fun updateDesk(protocol: FileProtocol, type: DiskBase) {
        _deskType.value = type
        when (protocol) {
            FileProtocol.Local -> {

            }

            FileProtocol.Device -> {
                val device = type as Device
            }

            FileProtocol.Network -> {
                val network = type as Network
            }
        }
    }
}
