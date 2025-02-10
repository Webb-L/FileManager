package app.filemanager.ui.screen.file

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.file.toIcon
import app.filemanager.data.main.DeviceType.*
import app.filemanager.extensions.formatFileSize
import app.filemanager.extensions.randomString
import app.filemanager.extensions.timestampToSyncDate
import app.filemanager.service.data.ConnectType.*
import app.filemanager.service.rpc.SocketClientIPEnum
import app.filemanager.service.rpc.getAllIPAddresses
import app.filemanager.ui.components.FileIcon
import app.filemanager.ui.state.file.FileShareLikeCategory
import app.filemanager.ui.state.file.FileShareState
import app.filemanager.ui.state.file.FileShareStatus
import app.filemanager.ui.state.file.FileState
import app.filemanager.ui.state.main.DeviceState
import app.filemanager.ui.state.main.MainState
import app.filemanager.ui.theme.Typography
import app.filemanager.utils.WindowSizeClass
import app.filemanager.utils.calculateWindowSizeClass
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class FileShareScreen(private val _files: List<FileSimpleInfo>) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val scope = rememberCoroutineScope()

        val mainState = koinInject<MainState>()

        val files = mutableStateListOf<FileSimpleInfo>()
        files.addAll(_files)

        val checkedFiles = mutableStateListOf<FileSimpleInfo>()
        checkedFiles.addAll(_files)

        val deviceState = koinInject<DeviceState>()

        val fileState = koinInject<FileState>()
        val fileShareState = koinInject<FileShareState>()

        val snackbarHostState = remember { SnackbarHostState() }

        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val socketDevices = deviceState.socketDevices.sortedBy { it.client != null }

        var category by remember { mutableStateOf(FileShareLikeCategory.WAITING) }
        var isExpandFileList by remember { mutableStateOf(true) }
        var selectFileType by remember { mutableStateOf(0) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("分享文件") },
                    navigationIcon = {
                        IconButton({
                            mainState.updateScreen(null)
                            navigator.pop()
                        }) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, null)
                        }
                    },
                    actions = {
                        BadgedBox({
                            if (!drawerState.isClosed) return@BadgedBox
                            Badge { Text("${if (checkedFiles.size > 100) "99+" else checkedFiles.size}") }
                        }) {
                            IconButton({
                                scope.launch {
                                    if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                }
                            }) {
                                Icon(if (drawerState.isClosed) Icons.Default.Description else Icons.Default.Close, null)
                            }
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { paddingValues ->
            DismissibleNavigationDrawer(
                modifier = Modifier.padding(paddingValues).fillMaxSize(),
                drawerState = drawerState,
                drawerContent = {
                    DismissibleDrawerSheet {
                        LazyColumn {
                            item {
                                AppDrawerHeader(
                                    "文件列表(${files.size})",
                                    actions = {}
                                )
                            }
                            item {
                                SingleChoiceSegmentedButtonRow(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                ) {
                                    listOf("全选", "反选").forEachIndexed { index, label ->
                                        SegmentedButton(
                                            selected = selectFileType == index,
                                            onClick = {
                                                selectFileType = index
                                                if (index == 0) {
                                                    checkedFiles.clear()
                                                    checkedFiles.addAll(_files)
                                                }
                                                if (index == 1) {
                                                    for (file in files) {
                                                        if (checkedFiles.contains(file)) {
                                                            checkedFiles.remove(file)
                                                        } else {
                                                            checkedFiles.add(file)
                                                        }
                                                    }
                                                }
                                            },
                                            shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                                        ) { Text(label) }
                                    }
                                }
                            }
                            items(files) { file ->
                                ListItem(
                                    headlineContent = { Text(file.name) },
                                    supportingContent = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            file.protocol.toIcon()

                                            if (file.isDirectory) {
                                                Text(
                                                    "${file.size}项",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            } else {
                                                Text(
                                                    file.size.formatFileSize(),
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                file.createdDate.timestampToSyncDate(),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    },
                                    leadingContent = {
                                        Box(
                                            modifier = Modifier.padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!checkedFiles.contains(file)) {
                                                FileIcon(file)
                                            } else {
                                                Checkbox(checkedFiles.contains(file), onCheckedChange = null)
                                            }
                                        }
                                    },
                                    trailingContent = {
                                        IconButton({
                                            scope.launch {
                                                when (snackbarHostState.showSnackbar(
                                                    message = file.name,
                                                    actionLabel = "删除",
                                                    withDismissAction = true,
                                                    duration = SnackbarDuration.Short
                                                )) {
                                                    SnackbarResult.Dismissed -> {}
                                                    SnackbarResult.ActionPerformed -> {
                                                        files.remove(file)
                                                        checkedFiles.remove(file)
                                                    }
                                                }
                                            }
                                        }) {
                                            Icon(Icons.Default.Close, null)
                                        }
                                    },
                                    modifier = Modifier.clickable {
                                        if (checkedFiles.contains(file)) {
                                            checkedFiles.remove(file)
                                        } else {
                                            checkedFiles.add(file)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            ) {
                BoxWithConstraints {
                    val columnCount = when (calculateWindowSizeClass(maxWidth, maxHeight)) {
                        WindowSizeClass.Compact -> 1
                        WindowSizeClass.Medium -> 2
                        WindowSizeClass.Expanded -> 3
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columnCount)
                    ) {
                        item(span = { GridItemSpan(columnCount) }) {
                            Column {
                                AppDrawerHeader(title = "链接方式分享", actions = {
                                    Icon(
                                        if (isExpandFileList) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        null,
                                        Modifier.clip(RoundedCornerShape(25.dp))
                                            .clickable { isExpandFileList = !isExpandFileList }
                                    )
                                })
                                LinkShareFileCard()
                            }
                        }

                        item(span = { GridItemSpan(columnCount) }) {
                            SingleChoiceSegmentedButtonRow(
                                modifier = Modifier.padding(horizontal = 16.dp),
                            ) {
                                listOf(
                                    FileShareLikeCategory.WAITING to "等待",
                                    FileShareLikeCategory.RUNNING to "允许",
                                    FileShareLikeCategory.REJECTED to "拒绝",
                                ).forEachIndexed { index, (cat, label) ->
                                    SegmentedButton(
                                        selected = category == cat,
                                        onClick = { category = cat },
                                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 3),
                                    ) { Text(label) }
                                }
                            }
                        }

                        items(socketDevices) { device ->
                            ListItem(
                                overlineContent = { Text("是否允许访问") },
                                headlineContent = { Text(device.name) },
                                trailingContent = {
                                    Row {
                                        IconButton({}) {
                                            Icon(Icons.Default.Done, null)
                                        }
                                        IconButton({}) {
                                            Icon(Icons.Default.Close, null)
                                        }
                                    }
                                },
                            )
                        }

                        item(span = { GridItemSpan(columnCount) }) {
                            Column {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                Spacer(Modifier.height(16.dp))
                                AppDrawerHeader(title = "分享到其他设备", actions = {
                                    Icon(
                                        if (isExpandFileList) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        null,
                                        Modifier.clip(RoundedCornerShape(25.dp))
                                            .clickable { isExpandFileList = !isExpandFileList }
                                    )
                                })
                            }
                        }
                        items(socketDevices) { device ->
                            ListItem(
                                modifier = Modifier.clickable {
                                    fileShareState.sendFile[device.id] = FileShareStatus.WAITING
                                },
                                overlineContent = {
                                    when (device.connectType) {
                                        Connect -> Badge(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ) { Text("已链接") }

                                        Fail -> Badge { Text("连接失败") }
                                        UnConnect -> Badge { Text("未连接") }
                                        Loading -> Badge(
                                            containerColor = MaterialTheme.colorScheme.tertiary
                                        ) { Text("连接中") }

                                        New -> Badge { Text("新-未连接") }
                                        Rejected -> Badge { Text("拒绝连接") }
                                    }
                                },
                                headlineContent = {
                                    Text(device.name)
                                },
                                supportingContent = {
                                    when (fileShareState.sendFile[device.id]) {
                                        FileShareStatus.SENDING -> {
                                            Text("发送中...")
                                        }

                                        FileShareStatus.REJECTED -> {
                                            Text("对方拒绝了")
                                        }

                                        FileShareStatus.ERROR -> {
                                            Text("发送失败")
                                        }

                                        FileShareStatus.COMPLETED -> {
                                            Text("已完成")
                                        }

                                        FileShareStatus.WAITING -> {
                                            Text("等待对方确认")
                                        }

                                        null -> Text("点击发送文件")
                                    }
                                },
                                leadingContent = {
                                    when (device.type) {
                                        Android -> Icon(Icons.Default.PhoneAndroid, null)
                                        IOS -> Icon(Icons.Default.PhoneIphone, null)
                                        JVM -> Icon(Icons.Default.Devices, null)
                                        JS -> Icon(Icons.Default.Javascript, null)
                                    }
                                },
                            )
                        }
                    }
                }
            }

//            return@Scaffold
//            AlertDialog(
//                modifier = Modifier.padding(16.dp),
//                text = {
//                    Column {
//                        Box(Modifier.fillMaxSize().weight(1f).background(Color.Red)) {}
//                        Spacer(Modifier.height(8.dp))
//                        SelectionContainer {
//                            Text("http://127.0.0.1:8080/")
//                        }
//                    }
//                },
//                onDismissRequest = {
//                    // TODO 关闭弹窗
//                },
//                confirmButton = {},
//                dismissButton = {}
//            )
        }


    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun LinkShareFileCard() {
        val allIPAddresses = getAllIPAddresses(type = SocketClientIPEnum.IPV4_UP)
        var address by remember { mutableStateOf(allIPAddresses.first()) }

        var showAddress by remember { mutableStateOf(false) }

        var showSettings by remember { mutableStateOf(false) }
        var encryption by remember { mutableStateOf(false) }
        var passwordAccess by remember { mutableStateOf(false) }
        var password by remember { mutableStateOf("") }

        Card(Modifier.padding(start = 16.dp, end = 16.dp)) {
            Column {
                Row {
                    Box(Modifier.size(128.dp).background(Color.Red)) {}
                    Column(Modifier.weight(1f).padding(16.dp)) {
                        Text("服务已启动", style = Typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        SelectionContainer {
                            Text("${if (encryption) "https" else "http"}://${address}:8080/${if (passwordAccess) "?pwd=${password}" else ""}")
                        }
                    }
                }
                AnimatedVisibility(showAddress) {
                    FlowRow(
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(Alignment.Top)
                            .padding(start = 8.dp, end = 8.dp, top = 8.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        for (ipAddress in allIPAddresses) {
                            FilterChip(
                                selected = address == ipAddress,
                                onClick = { address = ipAddress },
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .align(Alignment.CenterVertically),
                                label = { Text(ipAddress) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.QrCode,
                                        contentDescription = null,
                                        modifier = Modifier.clickable { }
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.CopyAll,
                                        contentDescription = null,
                                        modifier = Modifier.clickable { }
                                    )
                                }
                            )
                        }
                    }
                }
                AnimatedVisibility(showSettings) {
                    FlowRow(
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(Alignment.Top)
                            .padding(start = 8.dp, end = 8.dp, top = 8.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        FilterChip(
                            selected = encryption,
                            onClick = { encryption = !encryption },
                            modifier = Modifier.padding(horizontal = 4.dp)
                                .align(Alignment.CenterVertically),
                            label = { Text("加密") },
                            leadingIcon = {
                                if (!encryption) return@FilterChip
                                Icon(
                                    Icons.Default.Done,
                                    contentDescription = null
                                )
                            }
                        )
                        FilterChip(
                            selected = passwordAccess,
                            onClick = {
                                passwordAccess = !passwordAccess
                                if (passwordAccess) {
                                    password = 6.randomString(includeSpecial = false)
                                }
                            },
                            modifier = Modifier.padding(horizontal = 4.dp)
                                .align(Alignment.CenterVertically),
                            label = { Text("密码访问") },
                            leadingIcon = {
                                if (!passwordAccess) return@FilterChip
                                Icon(
                                    Icons.Default.Done,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }
                Row(Modifier.padding(8.dp)) {
                    OutlinedButton({}) {
                        Text("启动")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button({}) {
                        Icon(Icons.Default.CopyAll, null)
                        Spacer(Modifier.width(4.dp))
                        Text("复制")
                    }
                    Spacer(Modifier.weight(1f))
                    FilledIconToggleButton(checked = showAddress, onCheckedChange = {
                        showAddress = it
                    }) {
                        Icon(Icons.Default.MoreVert, null)
                    }
                    FilledIconToggleButton(checked = showSettings, onCheckedChange = {
                        showSettings = it
                    }) {
                        Icon(Icons.Default.Settings, null)
                    }
                }
            }
        }
    }

    @Composable
    private fun SendFileDialog(onSendClick: () -> Unit, onCancelClick: () -> Unit) {
        AlertDialog(
            onDismissRequest = onCancelClick,
            title = {
                Text("发送文件")
            },
            text = {
                Text("要将100个文件发送到xxx设备吗？")
            },
            confirmButton = {
                TextButton(onClick = onSendClick) {
                    Text("发送")
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelClick) {
                    Text("取消")
                }
            }
        )
    }

    @Composable
    private fun AppDrawerHeader(title: String, actions: @Composable () -> Unit) {
        Row(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
            Text(title)
            Spacer(Modifier.weight(1f))
            actions()
        }
    }
}