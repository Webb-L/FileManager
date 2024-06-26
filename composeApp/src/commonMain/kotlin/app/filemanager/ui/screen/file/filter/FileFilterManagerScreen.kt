package app.filemanager.ui.screen.file.filter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.filemanager.db.FileFilter
import app.filemanager.ui.components.GridList
import app.filemanager.ui.components.TextFieldDialog
import app.filemanager.ui.state.file.FileFilterState
import app.filemanager.utils.VerificationUtils
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject

class FileFilterManagerScreen(private val filterId: Long) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val fileFilterState = koinInject<FileFilterState>()
        var fileFilter by remember {
            mutableStateOf(fileFilterState.getFileFilter(filterId))
        }

        val snackbarHostState = remember { SnackbarHostState() }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("${fileFilter.name} - 过滤类型") },
                    navigationIcon = {
                        IconButton({
                            // TODO 这里应该是HomeNavigator文件处理
                            fileFilterState.syncFilterFileTypes()
                            navigator.pop()
                        }) {
                            Icon(Icons.Default.ArrowBack, null)
                        }
                    },
                    actions = {}
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                ExtendedFloatingActionButton({ fileFilterState.updateCreateDialog(true) }) {
                    Icon(Icons.Filled.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("新增")
                }
            }
        ) {
            val extensions = fileFilter.extensions
            GridList(
                modifier = Modifier.padding(it)
            ) {
                itemsIndexed(extensions) { index, type ->
                    ListItem(
                        headlineContent = { Text(type) },
                        trailingContent = {
                            Icon(
                                Icons.Outlined.Delete,
                                null,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(25.dp))
                                    .clickable {
                                        fileFilterState.updateFileFilter(
                                            extensions
                                                .toMutableList().apply {
                                                    removeAt(index)
                                                },
                                            fileFilter.id
                                        )
                                        fileFilter = fileFilterState.getFileFilter(filterId)
                                    })
                        },
                        modifier = Modifier
                            .clickable {}
                    )
                }
            }
        }


        DialogContent(fileFilter) {
            fileFilter = it
        }
    }


    @Composable
    fun DialogContent(fileFilter: FileFilter, onUpdateFilter: (FileFilter) -> Unit) {
        val fileFilterState = koinInject<FileFilterState>()
        val isCreateDialog by fileFilterState.isCreateDialog.collectAsState()

        if (isCreateDialog) {
            TextFieldDialog(
                "新增扩展名",
                label = "名称",
                verifyFun = { text -> VerificationUtils.filterExtensions(text, fileFilter.extensions) }
            ) {
                fileFilterState.updateCreateDialog(false)
                if (it.isEmpty()) return@TextFieldDialog
                val extension = if (it.indexOf(".") == 0) it else ".$it"
                fileFilterState.updateFileFilter(
                    fileFilter.extensions
                        .toMutableList().apply {
                            add(extension)
                        },
                    fileFilter.id
                )
                onUpdateFilter(fileFilterState.getFileFilter(filterId))
            }
        }
    }
}