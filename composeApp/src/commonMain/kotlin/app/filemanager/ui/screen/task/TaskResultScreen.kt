package app.filemanager.ui.screen.task

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CodeOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.filemanager.ui.components.ErrorEmptyData
import app.filemanager.ui.components.HighlightedText
import app.filemanager.ui.state.main.MainState
import app.filemanager.ui.state.main.Task
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject

class TaskResultScreen(
    private val task: Task
) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val mainState = koinInject<MainState>()

        // 搜索框用于输入目录
        var searchText by remember { mutableStateOf("") }
        // 搜索框用于显示错误
        var searchErrorText by remember { mutableStateOf("") }
        // 搜索框启用正则
        var useRegex by remember { mutableStateOf(false) }

        // 预设错误类型示例，可根据项目实际情况进行调整
        val errorTypes = task.result.values.toSet()

        // 多选 Chips 用到的状态
        var selectedErrorTypes by remember { mutableStateOf(setOf<String>()) }

        // 按照输入和多选过滤
        val filteredResults = task.result.filter { (dir, errorType) ->
            try {
                val matchesSearch = if (useRegex) {
                    searchText.toRegex(RegexOption.IGNORE_CASE).containsMatchIn(dir)
                } else {
                    dir.contains(searchText, ignoreCase = true)
                }
                // 判断目录名是否匹配搜索框中的文本
                matchesSearch &&
                        // 如果没有选中任何错误类型，则不过滤。如果有选中，则只显示被选中的类型
                        (selectedErrorTypes.isEmpty() || errorType in selectedErrorTypes)
            } catch (e: Exception) {
                searchErrorText = "正则表达式错误"
                false
            }
        }

        // 用于滚动的状态
        val scrollState = rememberScrollState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("任务结果") },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                navigator.pop()
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 8.dp)
            ) {
                // 过滤功能区域
                TextField(
                    value = searchText,
                    isError = searchText.isNotEmpty() && searchErrorText.isNotEmpty(),
                    singleLine = true,
                    onValueChange = { newText -> searchText = newText },
                    label = { Text("搜索目录") },
                    leadingIcon = {
                        IconButton(onClick = { useRegex = !useRegex }) {
                            Icon(
                                if (useRegex) Icons.Default.Code else Icons.Default.CodeOff,
                                contentDescription = "启用正则",
                            )
                        }
                    },
                    trailingIcon = {
                        IconButton(onClick = { searchText = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "清除")
                        }
                    },
                    supportingText = { Text(searchErrorText) },
                    modifier = Modifier.fillMaxWidth()
                )
                // 多选 Chips：用于选择错误类型
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    errorTypes.forEach { type ->
                        FilterChip(
                            leadingIcon = { Icon(Icons.Default.Error, null) },
                            selected = type in selectedErrorTypes,
                            onClick = {
                                selectedErrorTypes = if (type in selectedErrorTypes) {
                                    selectedErrorTypes - type
                                } else {
                                    selectedErrorTypes + type
                                }
                            },
                            shape = RoundedCornerShape(25.dp),
                            label = { Text(type) }
                        )
                    }
                }

                // 根据过滤后结果展示表格
                if (filteredResults.isEmpty()) {
                    ErrorEmptyData()
                    return@Column
                }

                // 表头
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "目录",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "错误信息",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                // 遍历过滤后的 Map 条目
                filteredResults.forEach { (dir, error) ->
                    SelectionContainer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Row {
                            HighlightedText(
                                text = dir,
                                keyword = searchText,
                                useRegex = useRegex,
                                modifier = Modifier.weight(1f),
                            )
                            Text(text = error)
                        }
                    }
                }
            }
        }
    }
}