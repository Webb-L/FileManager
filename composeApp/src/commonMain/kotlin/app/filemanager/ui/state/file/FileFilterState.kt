package app.filemanager.ui.state.file

import androidx.compose.runtime.mutableStateListOf
import app.filemanager.data.file.FileFilterSort
import app.filemanager.data.file.FileFilterType
import app.filemanager.data.file.FileInfo
import app.filemanager.db.FileFilter
import app.filemanager.db.FileManagerDatabase
import app.filemanager.utils.NaturalOrderComparator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FileFilterState : KoinComponent {
    private val database by inject<FileManagerDatabase>()

    val filterFileTypes = mutableStateListOf<FileFilter>()

    init {
        syncFilterFileTypes()
    }

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

    fun filter(fileInfos: List<FileInfo>, updateKey: Int): List<FileInfo> {
        var files = fileInfos
        if (_isHideFile.value) {
            files = files.filter { !it.isHidden }
        }

        for (type in filterFileExtensions) {
            files = when (type) {
                FileFilterType.Folder -> files.filter { it.isDirectory }

                FileFilterType.File -> files.filter { !it.isDirectory && getFilterFileByFileExtension(it.mineType) == null }

                else -> files.filter { getExtensions(type).contains(it.mineType) }
            }
        }

        val searchText = searchText.value
        if (searchText.isNotEmpty()) {
            files = files.filter { it.name.contains(searchText) }
        }

        return when (_sortType.value) {
            FileFilterSort.NameAsc -> files
                .sortedWith(compareByDescending<FileInfo> { it.isDirectory }.then(NaturalOrderComparator()))

            FileFilterSort.NameDesc -> files
                .sortedWith(compareByDescending<FileInfo> { it.isDirectory }.thenDescending(NaturalOrderComparator()))

            FileFilterSort.SizeAsc -> files
                .sortedWith(compareByDescending<FileInfo> { it.isDirectory }.thenBy { it.size })

            FileFilterSort.SizeDesc -> files
                .sortedWith(compareByDescending<FileInfo> { it.isDirectory }.thenByDescending { it.size })

            FileFilterSort.TypeAsc -> files
                .sortedWith(compareBy<FileInfo> { it.isDirectory }.then(NaturalOrderComparator()))

            FileFilterSort.TypeDesc -> files
                .sortedWith(compareByDescending<FileInfo> { it.isDirectory }.then(NaturalOrderComparator()))

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

    fun getExtensions(type: FileFilterType): List<String> {
        val filter = filterFileTypes.filter { it.type == type }
        if (filter.isNotEmpty()) {
            return filter.last().extensions
        }
        return emptyList()
    }

    fun getFilterFileByFileExtension(type: String): FileFilter? {
        val filter = filterFileTypes.filter { it.extensions.contains(type) }
        if (filter.isEmpty()) {
            return null
        }
        return filter.first()
    }

    fun getFileFilter(filterId: Long) = database.fileFilterQueries.queryById(filterId).executeAsOne()
    fun updateFileFilter(extensions: List<String>, id: Long) {
        database.fileFilterQueries.updateExtensionsById(extensions, id)
    }

    fun deleteFilter(fileFilter: FileFilter) {
        database.fileFilterQueries.deleteById(fileFilter.id)
        syncFilterFileTypes()
    }

    fun createFilter(name: String) {
        database.fileFilterQueries.insert(
            name = name,
            type = FileFilterType.Custom,
            extensions = listOf(),
            icon = null,
            sort = 0
        )
        syncFilterFileTypes()
    }

    fun syncFilterFileTypes() {
        filterFileTypes.clear()
        filterFileTypes.addAll(database.fileFilterQueries.queryAllByLimit(0, 100).executeAsList())
    }
}