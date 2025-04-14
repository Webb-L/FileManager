package app.filemanager.ui.components.buttons

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.filemanager.data.file.FileFilterType
import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.file.getFileFilterType
import app.filemanager.db.FileFilter

/**
 * 文件过滤器按钮组组件
 *
 * 用于显示一组文件过滤器按钮，包括文件夹和不同类型的文件过滤器，每个过滤器显示对应类型的文件数量
 *
 * @param fileAndFolder 要显示的文件和文件夹列表
 * @param filterFileTypes 可用于过滤的文件类型列表
 * @param filterFileExtensions 当前已选择的文件过滤类型列表
 * @param isHide 是否显示隐藏文件，默认为false
 * @param onCheckedFileFilterTypeChange 文件过滤类型选择状态变化时的回调
 * @param modifier 应用于组件的修饰符
 */
@Composable
fun FileFilterButtonGroup(
    fileAndFolder: List<FileSimpleInfo>,
    filterFileTypes: List<FileFilter>,
    filterFileExtensions: List<FileFilterType>,
    isHide: Boolean = false,
    onCheckedFileFilterTypeChange: (Boolean, FileFilterType) -> Unit,
    modifier: Modifier = Modifier
) {
    // 获取所有非目录文件，并根据是否显示隐藏文件进行过滤，然后按mime类型分组，计算每种类型的文件数量
    val extensions =
        fileAndFolder
            .filter { !it.isDirectory }
            .filter {
                if (!isHide) {
                    !it.isHidden
                } else {
                    true
                }
            }
            .groupBy { it.mineType }
            .mapValues { (_, value) -> value.size }

    // 获取文件夹数量，同样根据是否显示隐藏文件进行过滤
    val folderCount = fileAndFolder
        .filter { it.isDirectory }
        .count {
            if (!isHide) {
                !it.isHidden
            } else {
                true
            }
        }

    // 过滤出extensions列表中存在的文件类型
    val filterExtensions = filterFileTypes
        .filter { filterFileType -> filterFileType.extensions.any { it in extensions.keys } }

    // 使用LazyRow水平显示过滤器按钮
    LazyRow(modifier) {
        item {
            // 添加文件夹过滤器按钮，如果没有文件夹则不显示
            if (folderCount < 1) return@item

            val isSelected = filterFileExtensions.contains(FileFilterType.Folder)

            FilterChip(
                selected = isSelected,
                label = { Text("文件夹($folderCount)") },
                leadingIcon = { getFileFilterType(FileFilterType.Folder) },
                shape = RoundedCornerShape(25.dp),
                onClick = { onCheckedFileFilterTypeChange(isSelected, FileFilterType.Folder) },
            )
            Spacer(Modifier.width(8.dp))
        }

        // 为每种文件类型添加过滤器按钮
        itemsIndexed(filterExtensions) { index, fileFilter ->
            val isSelected = filterFileExtensions.contains(fileFilter.type)
            // 计算当前文件类型的文件总数
            val fileCount = fileFilter.extensions.intersect(extensions.keys).sumOf { key ->
                extensions.filterKeys { it == key }.values.sum()
            }

            FilterChip(
                selected = isSelected,
                label = { Text("${fileFilter.name}($fileCount)") },
                leadingIcon = { getFileFilterType(fileFilter.type) },
                shape = RoundedCornerShape(25.dp),
                onClick = { onCheckedFileFilterTypeChange(isSelected, fileFilter.type) },
            )
            Spacer(Modifier.width(8.dp))
        }

        item {
            // 添加通用文件过滤器按钮（没有特定mime类型的文件）
            val fileCount = extensions[""] ?: return@item

            val isSelected = filterFileExtensions.contains(FileFilterType.File)

            FilterChip(
                selected = isSelected,
                label = { Text("文件($fileCount)") },
                leadingIcon = { getFileFilterType(FileFilterType.File) },
                shape = RoundedCornerShape(25.dp),
                onClick = { onCheckedFileFilterTypeChange(isSelected, FileFilterType.File) },
            )
            Spacer(Modifier.width(8.dp))
        }
    }
}