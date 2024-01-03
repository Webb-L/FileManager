package app.filemanager.ui.state.file

import androidx.compose.runtime.mutableStateListOf
import app.filemanager.data.FileInfo
import app.filemanager.data.file.FileExtensions
import app.filemanager.data.file.FileFilter
import app.filemanager.data.file.FileFilterSort
import app.filemanager.data.file.FileFilterType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FileFilterState {
    private val _updateKey: MutableStateFlow<Int> = MutableStateFlow(0)
    val updateKey: StateFlow<Int> = _updateKey
    fun updateFilerKey() {
        _updateKey.value++
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
        _updateKey.value++
    }

    private val _sortType: MutableStateFlow<FileFilterSort> = MutableStateFlow(FileFilterSort.NameAsc)
    val sortType: StateFlow<FileFilterSort> = _sortType
    fun updateSortType(value: FileFilterSort) {
        _sortType.value = value
        _updateKey.value++
    }


    // 过滤的文件类型
    val filterFileExtensions = mutableStateListOf<FileFilterType>()

    // 是否显示隐藏文件
    private val _isHideFile: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isHideFile: StateFlow<Boolean> = _isHideFile
    fun updateHideFile(value: Boolean) {
        _isHideFile.value = value
        _updateKey.value++
    }

    val filterFileTypes = listOf(
        FileFilter(
            name = "图片",
            iconType = FileFilterType.Image,
        ),
        FileFilter(
            name = "音乐",
            iconType = FileFilterType.Audio,
        ),
        FileFilter(
            name = "视频",
            iconType = FileFilterType.Video,
        ),
        FileFilter(
            name = "文档",
            iconType = FileFilterType.Text,
        ),
        FileFilter(
            name = "可执行",
            iconType = FileFilterType.Executable,
        ),
        FileFilter(
            name = "压缩",
            iconType = FileFilterType.Compressed,
        ),
        FileFilter(
            name = "原始图像",
            iconType = FileFilterType.ImageRaw,
        ),
        FileFilter(
            name = "矢量图",
            iconType = FileFilterType.ImageVector,
        ),
        FileFilter(
            name = "游戏",
            iconType = FileFilterType.Game,
        ),
        FileFilter(
            name = "3D",
            iconType = FileFilterType.Image3D,
        ),
        FileFilter(
            name = "网页",
            iconType = FileFilterType.Web,
        ),
        FileFilter(
            name = "页面布局",
            iconType = FileFilterType.PageLayout,
        ),
        FileFilter(
            name = "CAD",
            iconType = FileFilterType.CAD,
        ),
        FileFilter(
            name = "数据库",
            iconType = FileFilterType.Database,
        ),
        FileFilter(
            name = "插件",
            iconType = FileFilterType.Plugin,
        ),
        FileFilter(
            name = "字体",
            iconType = FileFilterType.Font,
        ),
        FileFilter(
            name = "系统",
            iconType = FileFilterType.System,
        ),
        FileFilter(
            name = "设置",
            iconType = FileFilterType.Settings,
        ),
        FileFilter(
            name = "加密",
            iconType = FileFilterType.Encoded,
        ),
        FileFilter(
            name = "位置",
            iconType = FileFilterType.GIS,
        ),
        FileFilter(
            name = "磁盘映像",
            iconType = FileFilterType.Disk,
        ),
        FileFilter(
            name = "开发",
            iconType = FileFilterType.Developer,
        ),
        FileFilter(
            name = "备份",
            iconType = FileFilterType.Backup,
        ),
        FileFilter(
            name = "杂项",
            iconType = FileFilterType.Misc,
        ),
    )

    fun filter(fileInfos: List<FileInfo>, updateKey: Int): List<FileInfo> {
        var files = fileInfos
        if (_isHideFile.value) {
            files = files.filter { !it.isHidden }
        }

        for (type in filterFileExtensions) {
            files =
                files.filter { FileExtensions.getExtensions(type).contains(it.mineType) }
        }

        val searchText = searchText.value
        if (searchText.isNotEmpty()) {
            files = files.filter { it.name.contains(searchText) }
        }

        return when (_sortType.value) {
            FileFilterSort.NameAsc -> files
                .sortedWith(compareByDescending<FileInfo> { it.isDirectory }.thenBy { it.name })

            FileFilterSort.NameDesc -> files
                .sortedWith(compareByDescending<FileInfo> { it.isDirectory }.thenByDescending { it.name })

            FileFilterSort.SizeAsc -> files
                .sortedWith(compareByDescending<FileInfo> { it.isDirectory }.thenBy { it.size })

            FileFilterSort.SizeDesc -> files
                .sortedWith(compareByDescending<FileInfo> { it.isDirectory }.thenByDescending { it.size })

            FileFilterSort.TypeAsc -> files
                .sortedWith(compareBy<FileInfo> { it.isDirectory }.thenBy { it.name })

            FileFilterSort.TypeDesc -> files
                .sortedWith(compareByDescending<FileInfo> { it.isDirectory }.thenBy { it.name })

            FileFilterSort.CreatedDateAsc -> files
                .sortedWith(compareByDescending<FileInfo> { it.isDirectory }.thenBy { it.createdDate })

            FileFilterSort.CreatedDateDesc -> files
                .sortedWith(compareByDescending<FileInfo> { it.isDirectory }.thenByDescending { it.createdDate })

            FileFilterSort.UpdatedDateAsc -> files
                .sortedWith(compareByDescending<FileInfo> { it.isDirectory }.thenBy { it.updatedDate })

            FileFilterSort.UpdatedDateDesc -> files
                .sortedWith(compareByDescending<FileInfo> { it.isDirectory }.thenByDescending { it.updatedDate })
        }
    }

    private val _isCreateDialog: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isCreateDialog: StateFlow<Boolean> = _isCreateDialog
    fun updateCreateDialog(value: Boolean) {
        _isCreateDialog.value = value
    }
}