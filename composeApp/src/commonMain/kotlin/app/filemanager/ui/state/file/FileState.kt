package app.filemanager.ui.state.file

import androidx.compose.runtime.mutableStateListOf
import app.filemanager.data.file.FileFilter
import app.filemanager.data.file.FileFilterIcon
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FileState() {
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

    // 过滤的文件类型
    val filterFileExtensions = mutableStateListOf<FileFilterIcon>()

    // 是否显示隐藏文件
    private val _isHideFile: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isHideFile: StateFlow<Boolean> = _isHideFile
    fun updateHideFile(value: Boolean) {
        _isHideFile.value = value
    }

    val filterFileTypes = listOf(
        FileFilter(
            name = "图片",
            iconType = FileFilterIcon.Image,
        ),
        FileFilter(
            name = "音乐",
            iconType = FileFilterIcon.Audio,
        ),
        FileFilter(
            name = "视频",
            iconType = FileFilterIcon.Video,
        ),
        FileFilter(
            name = "文档",
            iconType = FileFilterIcon.Text,
        ),
    )
}