package app.filemanager.ui.screen.file

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.filemanager.extensions.getAllFilesInDirectory
import app.filemanager.ui.components.FileCard
import app.filemanager.ui.state.file.FileState
import app.filemanager.utils.FileUtils
import app.filemanager.utils.WindowSizeClass
import app.filemanager.utils.calculateWindowSizeClass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileScreen(path: String, fileState: FileState, updatePath: (String) -> Unit) {
    FileFilter()
    BoxWithConstraints {
        val columnCount = when (calculateWindowSizeClass(maxWidth, maxHeight)) {
            WindowSizeClass.Compact -> 1
            WindowSizeClass.Medium -> 2
            WindowSizeClass.Expanded -> 3
        }
        LazyVerticalGrid(columns = GridCells.Fixed(columnCount)) {
            items(
                path.getAllFilesInDirectory()
                    .filter { !it.isHidden }
                    .sortedBy { it.isDirectory }
                    .sortedBy { it.name }
            ) {
                FileCard(
                    file = it,
                    onClick = {
                        if (it.isDirectory) {
                            updatePath(it.path)
                        } else {
                            FileUtils.openFile(it.path)
                        }
                    },
                    onRemove = {}
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileFilter() {
    Row(Modifier) {
        Spacer(Modifier.width(4.dp))
        IconButton({}) {
            Icon(Icons.Default.GridView, null, tint = MaterialTheme.colorScheme.primary)
        }
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            FilterChip(selected = false,
                label = {
                    Text("图片")
                },
                leadingIcon = {
                    Icon(Icons.Default.Image, null)
                },
                shape = RoundedCornerShape(25.dp),
                onClick = {

                })
            Spacer(Modifier.width(8.dp))
            FilterChip(selected = false,
                label = {
                    Text("音乐")
                },
                leadingIcon = {
                    Icon(Icons.Default.Headphones, null)
                },
                shape = RoundedCornerShape(25.dp),
                onClick = {

                })
            Spacer(Modifier.width(8.dp))
            FilterChip(selected = false,
                label = {
                    Text("视频")
                },
                leadingIcon = {
                    Icon(Icons.Default.Videocam, null)
                },
                shape = RoundedCornerShape(25.dp),
                onClick = {

                })
            Spacer(Modifier.width(8.dp))
            FilterChip(selected = false,
                label = {
                    Text("文档")
                },
                leadingIcon = {
                    Icon(Icons.Default.Description, null)
                },
                shape = RoundedCornerShape(25.dp),
                onClick = {

                })
        }
        Spacer(modifier = Modifier.weight(1f))
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            Spacer(Modifier.width(8.dp))
            FilterChip(selected = false,
                label = {
                    Text("隐藏文件")
                },
                leadingIcon = {
                    Icon(Icons.Default.Tune, null)
                },
                shape = RoundedCornerShape(25.dp),
                onClick = {

                })
        }
        Spacer(Modifier.width(12.dp))
    }
}