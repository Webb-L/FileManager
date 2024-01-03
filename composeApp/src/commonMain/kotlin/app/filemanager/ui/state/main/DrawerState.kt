package app.filemanager.ui.state.main

import androidx.compose.runtime.mutableStateListOf
import app.filemanager.data.main.DrawerBookmark
import app.filemanager.data.main.DrawerBookmarkType
import app.filemanager.data.main.DrawerDevice
import app.filemanager.data.main.DrawerNetwork
import app.filemanager.utils.PathUtils.getHomePath
import app.filemanager.utils.PathUtils.getPathSeparator
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
        val homePath = getHomePath()
        val separator = getPathSeparator()
        bookmarks.addAll(
            listOf(
                DrawerBookmark(name = "主目录", path = homePath, iconType = DrawerBookmarkType.Home),
                DrawerBookmark(
                    name = "图片",
                    path = "$homePath${separator}Pictures",
                    iconType = DrawerBookmarkType.Image
                ),
                DrawerBookmark(name = "音乐", path = "$homePath${separator}Music", iconType = DrawerBookmarkType.Audio),
                DrawerBookmark(
                    name = "视频",
                    path = "$homePath${separator}Videos",
                    iconType = DrawerBookmarkType.Video
                ),
                DrawerBookmark(
                    name = "文档",
                    path = "$homePath${separator}Documents",
                    iconType = DrawerBookmarkType.Document
                ),
                DrawerBookmark(
                    name = "下载",
                    path = "$homePath${separator}Downloads",
                    iconType = DrawerBookmarkType.Download
                ),
            )
        )
    }

    private val _isExpandDevice: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isExpandDevice: StateFlow<Boolean> = _isExpandDevice
    fun updateExpandDevice(value: Boolean) {
        _isExpandDevice.value = value
    }

    private val _isDeviceAdd: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isDeviceAdd: StateFlow<Boolean> = _isDeviceAdd
    fun updateDeviceAdd(value: Boolean) {
        _isDeviceAdd.value = value
    }

    val devices = mutableStateListOf<DrawerDevice>()

    private val _isExpandNetwork: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isExpandNetwork: StateFlow<Boolean> = _isExpandNetwork
    fun updateExpandNetwork(value: Boolean) {
        _isExpandNetwork.value = value
    }

    val networks = mutableStateListOf<DrawerNetwork>()
}