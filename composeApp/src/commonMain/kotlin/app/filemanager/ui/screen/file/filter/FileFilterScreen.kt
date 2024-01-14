package app.filemanager.ui.screen.file.filter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.filemanager.data.file.getFileFilterType
import app.filemanager.ui.components.GridList
import app.filemanager.ui.components.TextFieldDialog
import app.filemanager.ui.state.file.FileFilterState
import app.filemanager.utils.VerificationUtils
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class FileFilterScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val fileFilterState = koinInject<FileFilterState>()

        val scope = rememberCoroutineScope()
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
            val filterFileTypes = fileFilterState.filterFileTypes
            GridList(
                isEmpty = filterFileTypes.isEmpty(),
                modifier = Modifier.padding(it)
            ) {
                items(filterFileTypes) { fileFilter ->
                    val extensions = fileFilterState.getExtensions(fileFilter.type)
                    ListItem(
                        headlineContent = { Text(fileFilter.name) },
                        supportingContent = if (extensions.isNotEmpty()) {
                            {
                                Text(
                                    extensions.joinToString(", "),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        } else {
                            null
                        },
                        leadingContent = { getFileFilterType(fileFilter.type) },
                        trailingContent = {
                            Icon(
                                Icons.Outlined.Delete,
                                null,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(25.dp))
                                    .clickable {
                                        scope.launch {
                                            when (snackbarHostState.showSnackbar(
                                                message = fileFilter.name,
                                                actionLabel = "删除",
                                                withDismissAction = true,
                                                duration = SnackbarDuration.Short
                                            )) {
                                                SnackbarResult.Dismissed -> {}
                                                SnackbarResult.ActionPerformed -> {
                                                    fileFilterState.deleteFilter(fileFilter)
                                                }
                                            }
                                        }
                                    }
                            )
                        },
                        modifier = Modifier.clickable {
                            navigator.push(FileFilterManagerScreen(fileFilter.id))
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
                fileFilterState.createFilter(it)
            }
        }
    }
}