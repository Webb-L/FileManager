package app.filemanager.ui.state.main

import androidx.compose.runtime.mutableStateListOf
import app.filemanager.data.main.DrawerBookmark
import app.filemanager.utils.PathUtils.getBookmarks
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DrawerState() {
    private val _isExpandBookmark: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isExpandBookmark: StateFlow<Boolean> = _isExpandBookmark
    fun updateExpandBookmark(value: Boolean) {
        _isExpandBookmark.value = value
    }

    val bookmarks = mutableStateListOf<DrawerBookmark>()

    init {
        bookmarks.addAll(getBookmarks())
    }

    private val _isExpandDevice: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isExpandDevice: StateFlow<Boolean> = _isExpandDevice
    fun updateExpandDevice(value: Boolean) {
        _isExpandDevice.value = value
    }

    private val _isExpandNetwork: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isExpandNetwork: StateFlow<Boolean> = _isExpandNetwork
    fun updateExpandNetwork(value: Boolean) {
        _isExpandNetwork.value = value
    }

}