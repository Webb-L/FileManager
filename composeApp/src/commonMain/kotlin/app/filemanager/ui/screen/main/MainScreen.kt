package app.filemanager.ui.screen.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.filemanager.ui.components.AppDrawer
import app.filemanager.ui.screen.file.FileScreen
import app.filemanager.ui.state.file.FileState
import app.filemanager.ui.state.main.MainState
import app.filemanager.utils.WindowSizeClass
import io.ktor.http.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(mainState: MainState, screenType: WindowSizeClass) {
    val path by mainState.path.collectAsState()
    val isSearchText by mainState.isSearchText.collectAsState()
    val searchText by mainState.searchText.collectAsState()

    val fileState = FileState()

    Row {
        val expandDrawer by mainState.isExpandDrawer.collectAsState()
        if (listOf(WindowSizeClass.Medium, WindowSizeClass.Expanded).contains(screenType) && expandDrawer) {
            AppDrawer()
        }

        Column {
            val paths = Url(path).pathSegments
            val listState = rememberLazyListState(initialFirstVisibleItemIndex = paths.size - 1)
            TopAppBar(
                title = {
                    LazyRow(state = listState) {
                        item {
                            FilterChip(selected = false,
                                label = { Text("根目录") },
                                border = null,
                                shape = RoundedCornerShape(25.dp),
                                onClick = { mainState.updatePath("/") })
                        }
                        itemsIndexed(paths.filter { it.isNotEmpty() }) { index, text ->
                            FilterChip(selected = false,
                                label = { Text(text) },
                                border = null,
                                shape = RoundedCornerShape(25.dp),
                                onClick = { mainState.updatePath(paths.subList(0, index + 2).joinToString("/")) })
                        }
                    }
                },
                navigationIcon = {
                    IconButton({
                        mainState.updateExpandDrawer(!expandDrawer)
                    }) {
                        Icon(if (expandDrawer) Icons.Default.Close else Icons.Default.Menu, null)
                    }
                },
                actions = {
                    IconButton({ mainState.updateSearch(!isSearchText) }) {
                        Icon(Icons.Default.Search, null)
                    }
                    IconButton({}) {
                        Icon(Icons.Default.Sort, null)
                    }
                }
            )
            if (isSearchText) {
                Row(modifier = Modifier.fillMaxWidth().padding(end = 16.dp), horizontalArrangement = Arrangement.End) {
                    TextField(searchText, label = { Text("搜索") }, onValueChange = mainState::updateSearchText)
                }
            }
            FileScreen(path, fileState) {
                mainState.updatePath(it)
            }
        }
    }
}