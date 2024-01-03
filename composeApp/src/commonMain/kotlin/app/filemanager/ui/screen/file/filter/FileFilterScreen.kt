package app.filemanager.ui.screen.file.filter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.filemanager.data.file.FileExtensions
import app.filemanager.data.file.getFileFilterType
import app.filemanager.ui.components.TextFieldDialog
import app.filemanager.ui.state.file.FileFilterState
import app.filemanager.utils.VerificationUtils
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject

class FileFilterScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val fileFilterState = koinInject<FileFilterState>()

        val snackbarHostState = remember { SnackbarHostState() }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("过滤类型") },
                    navigationIcon = {
                        IconButton({ navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, null)
                        }
                    },
                    actions = {
                        IconButton({ }) {
                            Icon(Icons.Default.Sort, null)
                        }
                    }
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
            LazyColumn(Modifier.padding(it)) {
                items(fileFilterState.filterFileTypes) { fileFilter ->
                    ListItem(
                        headlineContent = { Text(fileFilter.name) },
                        supportingContent = {
                            Text(
                                FileExtensions.getExtensions(fileFilter.iconType).joinToString(", "),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        leadingContent = { getFileFilterType(fileFilter.iconType) },
                        trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                        modifier = Modifier.clickable {
                            navigator.push(FileFilterManagerScreen(fileFilter.iconType.toString()))
                        }
                    )
                }
            }
        }


        DialogContent()
    }

    @Composable
    fun DialogContent() {
        val fileFilterState = koinInject<FileFilterState>()
        val isCreateDialog by fileFilterState.isCreateDialog.collectAsState()

        if (isCreateDialog) {
            TextFieldDialog(
                "新增类型",
                label = "名称",
                verifyFun = { text -> VerificationUtils.filterType(text, fileFilterState.filterFileTypes) }
            ) {
                fileFilterState.updateCreateDialog(false)
                if (it.isEmpty()) return@TextFieldDialog
            }
        }
    }
}