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
        FileFilter(
            name = "可执行",
            iconType = FileFilterIcon.Executable,
        ),
        FileFilter(
            name = "压缩",
            iconType = FileFilterIcon.Compressed,
        ),
        FileFilter(
            name = "原始图像",
            iconType = FileFilterIcon.ImageRaw,
        ),
        FileFilter(
            name = "矢量图",
            iconType = FileFilterIcon.ImageVector,
        ),
        FileFilter(
            name = "游戏",
            iconType = FileFilterIcon.Game,
        ),
        FileFilter(
            name = "3D",
            iconType = FileFilterIcon.Image3D,
        ),
        FileFilter(
            name = "网页",
            iconType = FileFilterIcon.Web,
        ),
        FileFilter(
            name = "页面布局",
            iconType = FileFilterIcon.PageLayout,
        ),
        FileFilter(
            name = "CAD",
            iconType = FileFilterIcon.CAD,
        ),
        FileFilter(
            name = "数据库",
            iconType = FileFilterIcon.Database,
        ),
        FileFilter(
            name = "插件",
            iconType = FileFilterIcon.Plugin,
        ),
        FileFilter(
            name = "字体",
            iconType = FileFilterIcon.Font,
        ),
        FileFilter(
            name = "系统",
            iconType = FileFilterIcon.System,
        ),
        FileFilter(
            name = "设置",
            iconType = FileFilterIcon.Settings,
        ),
        FileFilter(
            name = "加密",
            iconType = FileFilterIcon.Encoded,
        ),
        FileFilter(
            name = "位置",
            iconType = FileFilterIcon.GIS,
        ),
        FileFilter(
            name = "磁盘映像",
            iconType = FileFilterIcon.Disk,
        ),
        FileFilter(
            name = "开发",
            iconType = FileFilterIcon.Developer,
        ),
        FileFilter(
            name = "备份",
            iconType = FileFilterIcon.Backup,
        ),
        FileFilter(
            name = "杂项",
            iconType = FileFilterIcon.Misc,
        ),
    )
}