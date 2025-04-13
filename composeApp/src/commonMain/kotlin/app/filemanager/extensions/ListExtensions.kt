package app.filemanager.extensions

import app.filemanager.data.file.FileFilterSort
import app.filemanager.data.file.FileFilterType
import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.db.FileFilter
import app.filemanager.utils.NaturalOrderComparator

/**
 * 根据文件扩展名获取对应的文件过滤器
 * @param type 文件扩展名
 * @return 匹配的文件过滤器，未找到返回null
 */
fun List<FileFilter>.getFilterByExtension(type: String): FileFilter? {
    return firstOrNull { it.extensions.contains(type) }
}

/**
 * 获取指定文件类型的所有扩展名
 * @param type 文件类型
 * @return 该类型对应的所有扩展名列表
 */
fun List<FileFilter>.getExtensions(type: FileFilterType): List<String> {
    return lastOrNull { it.type == type }?.extensions ?: emptyList()
}

/**
 * 对文件列表进行过滤和排序的扩展函数
 *
 * @param isHidden 是否显示隐藏文件
 * @param filterFileExtensions 文件类型过滤条件列表
 * @param searchText 搜索文本，为空时不进行搜索过滤
 * @param sortType 排序类型
 * @param filterFileTypes 文件类型过滤器配置
 * @return 过滤和排序后的文件列表
 */

fun List<FileSimpleInfo>.filter(
    isHidden: Boolean = false,
    filterFileExtensions: List<FileFilterType> = emptyList(),
    searchText: String = "",
    sortType: FileFilterSort = FileFilterSort.NameAsc,
    filterFileTypes: List<FileFilter> = emptyList(),
): List<FileSimpleInfo> {
    var files = this

    // 过滤隐藏文件
    if (!isHidden) {
        files = files.filter { !it.isHidden }
    }

    // 根据文件类型进行过滤
    for (type in filterFileExtensions) {
        files = when (type) {
            FileFilterType.Folder -> files.filter { it.isDirectory } // 只显示文件夹

            FileFilterType.File -> files.filter {
                !it.isDirectory && filterFileTypes.getFilterByExtension(it.mineType) == null
            } // 只显示未分类的普通文件

            else -> files.filter {
                filterFileTypes.getExtensions(type).contains(it.mineType)
            } // 显示指定类型的文件
        }
    }

    // 根据搜索文本过滤文件名
    if (searchText.isNotEmpty()) {
        files = files.filter { it.name.contains(searchText, ignoreCase = true) }
    }

    // 根据排序类型进行排序
    return when (sortType) {
        // 文件名升序：文件夹优先，然后按自然顺序排序
        FileFilterSort.NameAsc -> files.sortedWith(
            compareByDescending<FileSimpleInfo> { it.isDirectory }
                .then(NaturalOrderComparator())
        )

        // 文件名降序：文件夹优先，然后按自然顺序逆序
        FileFilterSort.NameDesc -> files.sortedWith(
            compareByDescending<FileSimpleInfo> { it.isDirectory }
                .thenDescending(NaturalOrderComparator())
        )

        // 文件大小升序：文件夹优先，然后按大小升序
        FileFilterSort.SizeAsc -> files.sortedWith(
            compareByDescending<FileSimpleInfo> { it.isDirectory }
                .thenBy { it.size }
        )

        // 文件大小降序：文件夹优先，然后按大小降序
        FileFilterSort.SizeDesc -> files.sortedWith(
            compareByDescending<FileSimpleInfo> { it.isDirectory }
                .thenByDescending { it.size }
        )

        // 文件类型升序：文件夹优先，然后按自然顺序排序
        FileFilterSort.TypeAsc -> files.sortedWith(
            compareBy<FileSimpleInfo> { it.isDirectory }
                .then(NaturalOrderComparator())
        )

        // 文件类型降序：文件夹优先，然后按自然顺序排序
        FileFilterSort.TypeDesc -> files.sortedWith(
            compareByDescending<FileSimpleInfo> { it.isDirectory }
                .then(NaturalOrderComparator())
        )

        // 创建时间升序：文件夹优先，然后按创建时间升序
        FileFilterSort.CreatedDateAsc -> files.sortedWith(
            compareByDescending<FileSimpleInfo> { it.isDirectory }
                .thenBy { it.createdDate }
        )

        // 创建时间降序：文件夹优先，然后按创建时间降序
        FileFilterSort.CreatedDateDesc -> files.sortedWith(
            compareByDescending<FileSimpleInfo> { it.isDirectory }
                .thenByDescending { it.createdDate }
        )

        // 更新时间升序：文件夹优先，然后按更新时间升序
        FileFilterSort.UpdatedDateAsc -> files.sortedWith(
            compareByDescending<FileSimpleInfo> { it.isDirectory }
                .thenBy { it.updatedDate }
        )

        // 更新时间降序：文件夹优先，然后按更新时间降序
        FileFilterSort.UpdatedDateDesc -> files.sortedWith(
            compareByDescending<FileSimpleInfo> { it.isDirectory }
                .thenByDescending { it.updatedDate }
        )
    }
}
