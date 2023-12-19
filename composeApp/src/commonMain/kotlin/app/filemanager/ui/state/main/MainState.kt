package app.filemanager.ui.state.main

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
}
