package app.filemanager.ui.screen.file.filter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.filemanager.data.file.FileExtensions
import app.filemanager.data.file.FileFilterType
import app.filemanager.db.FileManagerDatabase
import app.filemanager.ui.state.file.FileFilterState
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject

class FileFilterManagerScreen(private val filterType: String) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val fileFilterState = koinInject<FileFilterState>()
        val fileFilterType = FileFilterType.valueOf(filterType)
        // TODO 临时
        val filter = fileFilterState.filterFileTypes.filter { it.iconType == fileFilterType }

        val snackbarHostState = remember { SnackbarHostState() }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("${filter.last().name} - 过滤类型") },
                    navigationIcon = {
                        IconButton({ navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, null)
                        }
                    },
                    actions = {
                        IconButton({ }) {
                            Icon(Icons.Default.Search, null)
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                ExtendedFloatingActionButton({ }) {
                    Icon(Icons.Filled.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("新增")
                }
            }
        ) {
            LazyColumn(Modifier.padding(it)) {
                items(FileExtensions.getExtensions(fileFilterType)) { type ->
                    ListItem(
                        headlineContent = { Text(type) },
                        trailingContent = {
                            Icon(
                                Icons.Outlined.Delete,
                                null,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(25.dp))
                                    .clickable {

                                    })
                        },
                        modifier = Modifier.clickable {}
                    )
                }
            }
        }
    }
}