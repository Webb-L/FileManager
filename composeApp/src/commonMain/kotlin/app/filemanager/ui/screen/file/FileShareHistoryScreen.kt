package app.filemanager.ui.screen.file

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.filemanager.data.file.FileShareHistory
import app.filemanager.db.FileManagerDatabase
import app.filemanager.exception.EmptyDataException
import app.filemanager.extensions.formatFileSize
import app.filemanager.extensions.timestampToSyncDate
import app.filemanager.extensions.timestampToYMDHM
import app.filemanager.ui.components.GridList
import app.filemanager.ui.state.file.FileShareStatus
import app.filemanager.ui.state.main.MainState
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class FileShareHistoryScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val mainState = koinInject<MainState>()
        // 获取导航器实例
        val navigator = LocalNavigator.currentOrThrow

        // 注入数据库实例
        val database = koinInject<FileManagerDatabase>()

        // 历史记录列表状态
        var histories by remember { mutableStateOf(emptyList<FileShareHistory>()) }
        // 是否显示详情对话框
        var showDetailDialog by remember { mutableStateOf(false) }
        // 当前选择的历史记录
        var selectedHistory by remember { mutableStateOf<FileShareHistory?>(null) }
        // 是否只显示发送记录
        var showOutgoingOnly by remember { mutableStateOf(true) }
        // 是否只显示接收记录
        var showIncomingOnly by remember { mutableStateOf(false) }
        // 是否按状态筛选
        var statusFilter by remember { mutableStateOf<FileShareStatus?>(null) }

        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        val statusOptions = mapOf(
            null to "全部",
            FileShareStatus.WAITING to "等待中",
            FileShareStatus.SENDING to "传输中",
            FileShareStatus.COMPLETED to "已完成",
            FileShareStatus.REJECTED to "已拒绝",
            FileShareStatus.ERROR to "错误"
        )


        // 加载所有历史记录数据
        LaunchedEffect(showOutgoingOnly, showIncomingOnly, statusFilter) {
            histories = when {
                showOutgoingOnly && statusFilter != null -> {
                    database.shareHistoryQueries.selectOutgoingByStatus(
                        status = statusFilter!!,
                        FileShareHistory::mapper
                    ).executeAsList()
                }

                showIncomingOnly && statusFilter != null -> {
                    database.shareHistoryQueries.selectIncomingByStatus(
                        status = statusFilter!!,
                        FileShareHistory::mapper
                    ).executeAsList()
                }

                showOutgoingOnly ->
                    database.shareHistoryQueries.selectOutgoing(FileShareHistory::mapper).executeAsList()

                showIncomingOnly ->
                    database.shareHistoryQueries.selectIncoming(FileShareHistory::mapper).executeAsList()

                statusFilter != null -> {
                    database.shareHistoryQueries.selectByStatus(status = statusFilter!!, FileShareHistory::mapper)
                        .executeAsList()
                }

                else ->
                    database.shareHistoryQueries.selectAll(FileShareHistory::mapper).executeAsList()
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("分享历史") },
                    navigationIcon = {
                        IconButton({
                            mainState.updateScreen(null)
                            navigator.pop()
                        }) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, null)
                        }
                    },
                    actions = {
                        // 清空历史记录
                        IconButton(onClick = {
                            scope.launch(Dispatchers.Default) {
                                when (snackbarHostState.showSnackbar(
                                    "确认清空所有历史记录吗？",
                                    actionLabel = "清空"
                                )) {
                                    SnackbarResult.Dismissed -> {}
                                    SnackbarResult.ActionPerformed -> {
                                        database.shareHistoryQueries.deleteAll()
                                        histories = emptyList()
                                    }
                                }
                            }
                        }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "清空历史")
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SingleChoiceSegmentedButtonRow {
                        SegmentedButton(
                            selected = showOutgoingOnly,
                            onClick = {
                                showOutgoingOnly = true
                                showIncomingOnly = false
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        ) { Text("发送") }
                        SegmentedButton(
                            selected = showIncomingOnly,
                            onClick = {
                                showOutgoingOnly = false
                                showIncomingOnly = true
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        ) { Text("接收") }
                    }

                    // 状态筛选菜单
                    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
                        var showStatusMenu by remember { mutableStateOf(false) }
                        TextButton(
                            onClick = { showStatusMenu = true },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (statusFilter != null) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            )
                        ) {
                            Text(statusOptions[statusFilter] ?: "全部")
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = "状态筛选",
                            )
                        }
                        DropdownMenu(
                            expanded = showStatusMenu,
                            onDismissRequest = { showStatusMenu = false }
                        ) {
                            for (item in statusOptions) {
                                DropdownMenuItem(
                                    text = { Text(item.value) },
                                    onClick = {
                                        statusFilter = item.key
                                        showStatusMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
                // 列表展示所有历史记录
                GridList(
                    modifier = Modifier
                        .fillMaxSize(),
                    exception = if (histories.isEmpty()) EmptyDataException() else null
                ) {
                    items(histories) { history ->
                        HistoryListItem(
                            history = history,
                            onDelete = {
                                scope.launch(Dispatchers.Default) {
                                    when (snackbarHostState.showSnackbar(
                                        "确认删除此记录吗？",
                                        actionLabel = "删除",
                                    )) {
                                        SnackbarResult.Dismissed -> {}
                                        SnackbarResult.ActionPerformed -> {
                                            database.shareHistoryQueries.deleteById(history.id)
                                            // 重新加载历史记录
                                            histories = when {
                                                showOutgoingOnly && statusFilter != null -> {
                                                    database.shareHistoryQueries.selectOutgoingByStatus(
                                                        status = statusFilter!!,
                                                        FileShareHistory::mapper
                                                    ).executeAsList()
                                                }

                                                showIncomingOnly && statusFilter != null -> {
                                                    database.shareHistoryQueries.selectIncomingByStatus(
                                                        status = statusFilter!!,
                                                        FileShareHistory::mapper
                                                    ).executeAsList()
                                                }

                                                showOutgoingOnly ->
                                                    database.shareHistoryQueries.selectOutgoing(FileShareHistory::mapper)
                                                        .executeAsList()

                                                showIncomingOnly ->
                                                    database.shareHistoryQueries.selectIncoming(FileShareHistory::mapper)
                                                        .executeAsList()

                                                statusFilter != null -> {
                                                    database.shareHistoryQueries.selectByStatus(
                                                        status = statusFilter!!,
                                                        FileShareHistory::mapper
                                                    )
                                                        .executeAsList()
                                                }

                                                else ->
                                                    database.shareHistoryQueries.selectAll(FileShareHistory::mapper)
                                                        .executeAsList()
                                            }
                                        }
                                    }
                                }
                            }
                        ) {
                            // 点击某个历史记录，显示详情对话框
                            selectedHistory = history
                            showDetailDialog = true
                        }
                    }
                }
            }

            // 显示历史记录详情对话框
            if (showDetailDialog && selectedHistory != null) {
                HistoryDetailDialog(
                    history = selectedHistory!!,
                    onDismiss = { showDetailDialog = false }
                )
            }
        }
    }

    // 历史记录列表项
    @Composable
    private fun HistoryListItem(
        history: FileShareHistory,
        onDelete: () -> Unit,
        onClick: () -> Unit = {}
    ) {
        val statusColor = when (history.status) {
            FileShareStatus.WAITING -> MaterialTheme.colorScheme.tertiary
            FileShareStatus.SENDING -> MaterialTheme.colorScheme.secondary
            FileShareStatus.COMPLETED -> MaterialTheme.colorScheme.primary
            FileShareStatus.REJECTED -> MaterialTheme.colorScheme.error
            FileShareStatus.ERROR -> MaterialTheme.colorScheme.error
        }

        ListItem(
            modifier = Modifier.clickable(onClick = onClick),
            overlineContent = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Badge(
                        containerColor = if (history.isOutgoing) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                    ) {
                        Text(if (history.isOutgoing) "发送" else "接收")
                    }

                    Badge(
                        containerColor = statusColor
                    ) {
                        Text(
                            when (history.status) {
                                FileShareStatus.WAITING -> "等待中"
                                FileShareStatus.SENDING -> "传输中"
                                FileShareStatus.COMPLETED -> "已完成"
                                FileShareStatus.REJECTED -> "已拒绝"
                                FileShareStatus.ERROR -> "错误"
                            }
                        )
                    }
                }
            },
            headlineContent = {
                Text(
                    history.fileName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            supportingContent = {
                Row {
                    Text(
                        if (history.isDirectory) {
                            "${history.fileSize}项"
                        } else {
                            history.fileSize.formatFileSize()
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        history.timestamp.timestampToYMDHM(),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            leadingContent = {
                if (history.isDirectory) {
                    Icon(Icons.Default.Folder, null)
                } else {
                    Icon(Icons.Default.InsertDriveFile, null)
                }
            },
            trailingContent = {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(Icons.Default.Delete, "删除记录")
                }
            }
        )
    }

    @Composable
    fun HistoryDetailDialog(
        history: FileShareHistory,
        onDismiss: () -> Unit
    ) {
        // 计算状态颜色
        val statusColor = when (history.status) {
            FileShareStatus.WAITING -> MaterialTheme.colorScheme.tertiary
            FileShareStatus.SENDING -> MaterialTheme.colorScheme.secondary
            FileShareStatus.COMPLETED -> MaterialTheme.colorScheme.primary
            FileShareStatus.REJECTED -> MaterialTheme.colorScheme.error
            FileShareStatus.ERROR -> MaterialTheme.colorScheme.error
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        if (history.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("分享详情")
                }
            },
            text = {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SelectionContainer {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())
                        ) {
                            // 文件信息组
                            DetailSection(
                                title = "文件信息",
                                icon = Icons.Default.Description
                            ) {
                                EnhancedLabeledText("文件名", history.fileName)
                                EnhancedLabeledText("类型", if (history.isDirectory) "文件夹" else "文件")
                                EnhancedLabeledText(
                                    "大小",
                                    if (history.isDirectory) "${history.fileSize}项" else history.fileSize.formatFileSize()
                                )
                                EnhancedLabeledText("路径", history.filePath)

                                if (history.savePath != null && !history.isOutgoing) {
                                    EnhancedLabeledText("保存路径", history.savePath)
                                }
                            }

                            // 传输信息组
                            DetailSection(
                                title = "传输信息",
                                icon = Icons.Default.SwapHoriz
                            ) {
                                EnhancedLabeledText(
                                    if (history.isOutgoing) "接收设备" else "发送设备",
                                    if (history.isOutgoing) history.targetDeviceName else history.sourceDeviceName
                                )

                                EnhancedLabeledText("时间", history.timestamp.timestampToSyncDate())

                                // 状态信息带颜色标识
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "状态",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(80.dp)
                                    )

                                    Badge(
                                        containerColor = statusColor
                                    ) {
                                        Text(
                                            when (history.status) {
                                                FileShareStatus.WAITING -> "等待中"
                                                FileShareStatus.SENDING -> "传输中"
                                                FileShareStatus.COMPLETED -> "已完成"
                                                FileShareStatus.REJECTED -> "已拒绝"
                                                FileShareStatus.ERROR -> "错误"
                                            },
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }

                                if (history.status == FileShareStatus.ERROR && history.errorMessage != null) {
                                    Spacer(Modifier.height(8.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Error,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.padding(end = 8.dp)
                                            )
                                            Text(
                                                text = history.errorMessage,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = onDismiss,
                ) {
                    Text("关闭")
                }
                Button(
                    onClick = onDismiss,
                ) {
                    Text("跳转")
                }
            }
        )
    }

    @Composable
    private fun DetailSection(
        title: String,
        icon: ImageVector,
        content: @Composable () -> Unit
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 小节标题
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // 小节内容
            Surface(
                shape = RoundedCornerShape(8.dp),
                tonalElevation = 0.5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    content()
                }
            }
        }
    }

    @Composable
    private fun EnhancedLabeledText(label: String, value: String?) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(80.dp)
            )

            if (value != null) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Text(
                    text = "无",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
