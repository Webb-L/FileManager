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

    private val _isSearchText: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isSearchText: StateFlow<Boolean> = _isSearchText
    fun updateSearch(value: Boolean) {
        _isSearchText.value = value
    }

    private val _searchText: MutableStateFlow<String> = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText
    fun updateSearchText(value: String) {
        _searchText.value = value
    }
}
