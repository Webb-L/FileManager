package app.filemanager.ui.screen.file

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.file.toIcon
import app.filemanager.data.main.Device
import app.filemanager.data.main.DeviceType.*
import app.filemanager.extensions.formatFileSize
import app.filemanager.extensions.randomString
import app.filemanager.extensions.timestampToSyncDate
import app.filemanager.service.HttpShareFileServer
import app.filemanager.service.data.ConnectType.*
import app.filemanager.service.rpc.SocketClientIPEnum
import app.filemanager.service.rpc.getAllIPAddresses
import app.filemanager.ui.components.FileIcon
import app.filemanager.ui.components.IpsButton
import app.filemanager.ui.screen.device.DeviceSettingsScreen
import app.filemanager.ui.state.file.FileShareLikeCategory.*
import app.filemanager.ui.state.file.FileShareState
import app.filemanager.ui.state.file.FileShareStatus
import app.filemanager.ui.state.main.DeviceState
import app.filemanager.ui.state.main.MainState
import app.filemanager.ui.theme.Typography
import app.filemanager.utils.WindowSizeClass
import app.filemanager.utils.calculateWindowSizeClass
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.seiko.imageloader.component.fetcher.ByteArrayFetcher
import com.seiko.imageloader.model.ImageRequest
import com.seiko.imageloader.rememberImagePainter
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import qrcode.QRCode

class FileShareScreen(private val _files: List<FileSimpleInfo>) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val scope = rememberCoroutineScope()

        val mainState = koinInject<MainState>()
        val deviceState = koinInject<DeviceState>()

        val fileShareState = koinInject<FileShareState>()
        fileShareState.files.apply {
            clear()
            addAll(_files)
        }
        fileShareState.checkedFiles.apply {
            clear()
            addAll(_files)
        }

        val files = fileShareState.files
        val checkedFiles = fileShareState.checkedFiles

        val snackbarHostState = remember { SnackbarHostState() }

        val socketDevices = deviceState.socketDevices.sortedBy { it.client != null }

        /* 链接方式分享 */
        var isExpandLinkShare by remember { mutableStateOf(true) }

        val sheetState = rememberModalBottomSheetState()
        var showBottomSheet by remember { mutableStateOf(false) }
        // 全部、反选
        var selectFileType by remember { mutableStateOf(0) }
        // 是否允许访问隐藏文件和文件夹
        var isHideFile by remember { mutableStateOf(true) }

        // 等待、允许、拒绝
        var category by remember { mutableStateOf(WAITING) }
        val tooltipState = rememberTooltipState(isPersistent = true)

        // 服务卡片
        val password by fileShareState.connectPassword.collectAsState()
        var openQrCodeDialog by remember { mutableStateOf<Pair<String, ImageRequest>?>(null) }
        val httpShareFileServer = HttpShareFileServer.getInstance(fileShareState)
        var curLinkDevice by remember { mutableStateOf<Device?>(null) }


        /* 分享到其他设备 */
        val loadingDevices by deviceState.loadingDevices.collectAsState()

        val infiniteTransition = rememberInfiniteTransition()
        val rotation by infiniteTransition.animateFloat(
            initialValue = 360f,
            targetValue = if (loadingDevices) 0f else 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 1500,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            )
        )

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("分享") },
                    navigationIcon = {
                        IconButton({
                            if (httpShareFileServer.isRunning()) {
                                scope.launch {
                                    when (snackbarHostState.showSnackbar(
                                        message = "链接方式分享文件服务仍在运行，是否继续？",
                                        actionLabel = "确认",
                                    )) {
                                        SnackbarResult.Dismissed -> {}
                                        SnackbarResult.ActionPerformed -> {
                                            mainState.updateScreen(null)
                                            navigator.pop()
                                        }
                                    }
                                }
                                return@IconButton
                            }
                            mainState.updateScreen(null)
                            navigator.pop()
                        }) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, null)
                        }
                    },
                    actions = {
                        BadgedBox({
                            if (showBottomSheet) return@BadgedBox
                            Badge { Text("${if (files.size > 100) "99+" else files.size}") }
                        }) {
                            IconButton({
                                showBottomSheet = !showBottomSheet
                            }) {
                                Icon(if (!showBottomSheet) Icons.Default.Description else Icons.Default.Close, null)
                            }
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { paddingValues ->
            BoxWithConstraints(Modifier.fillMaxWidth().padding(paddingValues)) {
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
                                    if (isExpandLinkShare) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    null,
                                    Modifier.clip(RoundedCornerShape(25.dp))
                                        .clickable { isExpandLinkShare = !isExpandLinkShare }
                                )
                            })
                            AnimatedVisibility(isExpandLinkShare) {
                                LinkShareFileCard(
                                    fileShareState = fileShareState,
                                    httpShareFileServer = httpShareFileServer,
                                    onStartServer = { showBottomSheet = true },
                                    onClickOpenQRCode = { openQrCodeDialog = it }
                                )
                            }
                        }
                    }

                    item(span = { GridItemSpan(columnCount) }) {
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier.padding(16.dp),
                        ) {
                            listOf(
                                WAITING to "等待",
                                RUNNING to "允许",
                                REJECTED to "拒绝",
                            ).forEachIndexed { index, (cat, label) ->
                                SegmentedButton(
                                    selected = category == cat,
                                    onClick = { category = cat },
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = 3),
                                ) {
                                    val deviceCount = when (cat) {
                                        WAITING -> fileShareState.pendingLinkShareDevices.size
                                        RUNNING -> fileShareState.authorizedLinkShareDevices.size
                                        REJECTED -> fileShareState.rejectedLinkShareDevices.size
                                    }
                                    Text("$label($deviceCount)")
                                }
                            }
                        }
                    }

                    val deviceStatusList = when (category) {
                        WAITING -> fileShareState.pendingLinkShareDevices
                        RUNNING -> fileShareState.authorizedLinkShareDevices.keys
                        REJECTED -> fileShareState.rejectedLinkShareDevices
                    }.toList()
                    items(deviceStatusList) { device ->
                        val isSelected = device == curLinkDevice
                        val primaryContainerColor = if (isSelected) {
                            colorScheme.primaryContainer
                        } else {
                            colorScheme.surface
                        }
                        val surfaceColor = if (isSelected) {
                            colorScheme.onPrimaryContainer
                        } else {
                            colorScheme.onSurface
                        }
                        ListItem(
                            overlineContent = {
                                if (category == WAITING) {
                                    Text("是否允许访问")
                                }
                            },
                            headlineContent = { Text(device.name) },
                            supportingContent = { Text(device.id) },
                            trailingContent = {
                                Row {
                                    if (category == WAITING) {
                                        IconButton({
                                            fileShareState.pendingLinkShareDevices.remove(device)
                                            fileShareState.authorizedLinkShareDevices[device] =
                                                Pair(isHideFile, fileShareState.checkedFiles.toList())
                                        }) {
                                            Icon(
                                                Icons.Default.Done,
                                                null,
                                                tint = surfaceColor
                                            )
                                        }
                                    }
                                    IconButton({
                                        when (category) {
                                            WAITING -> {
                                                fileShareState.authorizedLinkShareDevices.remove(device)
                                                fileShareState.pendingLinkShareDevices.remove(device)
                                                if (fileShareState.rejectedLinkShareDevices.contains(device)) {
                                                    return@IconButton
                                                }
                                                fileShareState.rejectedLinkShareDevices.add(device)
                                            }

                                            RUNNING -> {
                                                fileShareState.authorizedLinkShareDevices.remove(device)
                                                if (password.isNotEmpty()) {
                                                    fileShareState.rejectedLinkShareDevices.add(device)
                                                }
                                            }

                                            REJECTED -> fileShareState.rejectedLinkShareDevices.remove(device)
                                        }
                                    }) {
                                        Icon(
                                            Icons.Default.Close,
                                            null,
                                            tint = surfaceColor
                                        )
                                    }
                                }
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = primaryContainerColor,
                                headlineColor = surfaceColor,
                                supportingColor = surfaceColor,
                                overlineColor = surfaceColor
                            ),
                            modifier = Modifier.clickable(onClick = {
                                if (category != RUNNING) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("已允许的设备才可修改共享文件")
                                    }
                                    return@clickable
                                }
                                curLinkDevice = device

                                val deviceFileShareInfo =
                                    fileShareState.authorizedLinkShareDevices[device] ?: return@clickable
                                isHideFile = deviceFileShareInfo.first
                                fileShareState.checkedFiles.apply {
                                    clear()
                                    addAll(deviceFileShareInfo.second)
                                }
                            })
                        )
                    }

                    item(span = { GridItemSpan(columnCount) }) {
                        Column {
                            if (deviceStatusList.isEmpty()) {
                                Spacer(Modifier.height(16.dp))
                            }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            Spacer(Modifier.height(16.dp))
                            AppDrawerHeader(title = "分享到其他设备", actions = {
                                Row {
                                    Icon(
                                        Icons.Default.Add,
                                        null,
                                        Modifier.clip(RoundedCornerShape(25.dp)).clickable {
                                            deviceState.updateDeviceAdd(true)
                                        }
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Icon(
                                        Icons.Default.Sync,
                                        null,
                                        Modifier
                                            .clip(RoundedCornerShape(25.dp))
                                            .graphicsLayer { rotationZ = rotation }
                                            .alpha(if (loadingDevices) 0.5f else 1f)
                                            .clickable {
                                                if (loadingDevices) return@clickable
                                                scope.launch {
                                                    deviceState.scanner(getAllIPAddresses(type = SocketClientIPEnum.IPV4_UP))
                                                }
                                            }
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    IpsButton()
                                }
                            })
                        }
                    }
                    items(socketDevices) { device ->
                        ListItem(
                            modifier = Modifier.clickable {
                                fileShareState.sendFile[device.id] = FileShareStatus.WAITING
                                deviceState.share(device)
                                fileShareState.shareToDevices[device.id] =
                                    Pair(
                                        isHideFile,
                                        fileShareState.checkedFiles.toList()
                                    )
                            },
                            overlineContent = {
                                when (device.connectType) {
                                    Connect -> Badge(
                                        containerColor = colorScheme.primary
                                    ) { Text("已链接") }

                                    Fail -> Badge { Text("连接失败") }
                                    UnConnect -> Badge { Text("未连接") }
                                    Loading -> Badge(
                                        containerColor = colorScheme.tertiary
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

            if (showBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = {
                        showBottomSheet = false
                    },
                    sheetState = sheetState
                ) {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Row(Modifier.padding(bottom = 8.dp)) {
                            Text("文件列表(${files.size})")
                            Spacer(Modifier.weight(1f))
                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                tooltip = { Text("可以通过选择设备，修改已选择的文件") },
                                state = tooltipState
                            ) {
                                Icon(Icons.Default.Info, null)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SingleChoiceSegmentedButtonRow {
                                listOf("全选", "反选").forEachIndexed { index, label ->
                                    SegmentedButton(
                                        selected = selectFileType == index,
                                        onClick = {
                                            selectFileType = index
                                            if (index == 0) {
                                                fileShareState.checkedFiles.apply {
                                                    clear()
                                                    addAll(files)
                                                }
                                            }
                                            if (index == 1) {
                                                for (file in files) {
                                                    if (checkedFiles.contains(file)) {
                                                        fileShareState.checkedFiles.remove(file)
                                                    } else {
                                                        fileShareState.checkedFiles.add(file)
                                                    }
                                                }
                                            }


                                            if (curLinkDevice != null) {
                                                fileShareState.authorizedLinkShareDevices[curLinkDevice!!] =
                                                    Pair(
                                                        isHideFile,
                                                        fileShareState.checkedFiles.toList()
                                                    )
                                            }
                                        },
                                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                                    ) { Text(label) }
                                }
                            }

                            FilterChip(
                                selected = isHideFile,
                                label = { Text("隐藏文件") },
                                shape = RoundedCornerShape(25.dp),
                                onClick = {
                                    isHideFile = !isHideFile
                                    if (curLinkDevice != null) {
                                        fileShareState.authorizedLinkShareDevices[curLinkDevice!!] = Pair(
                                            isHideFile,
                                            fileShareState.checkedFiles.toList()
                                        )
                                    }
                                })
                        }
                    }

                    LazyColumn {
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
                                                    fileShareState.files.remove(file)
                                                    fileShareState.checkedFiles.remove(file)
                                                    if (curLinkDevice != null) {
                                                        fileShareState.authorizedLinkShareDevices[curLinkDevice!!] =
                                                            Pair(
                                                                isHideFile,
                                                                fileShareState.checkedFiles.toList()
                                                            )
                                                    }
                                                }
                                            }
                                        }
                                    }) {
                                        Icon(Icons.Default.Close, null)
                                    }
                                },
                                modifier = Modifier.clickable {
                                    if (checkedFiles.contains(file)) {
                                        fileShareState.checkedFiles.remove(file)
                                    } else {
                                        fileShareState.checkedFiles.add(file)
                                    }
                                    if (curLinkDevice != null) {
                                        fileShareState.authorizedLinkShareDevices[curLinkDevice!!] = Pair(
                                            isHideFile,
                                            fileShareState.checkedFiles.toList()
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }

            if (openQrCodeDialog != null) {
                AlertDialog(
                    modifier = Modifier.padding(16.dp),
                    text = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Image(rememberImagePainter(openQrCodeDialog!!.second), null)
                            Spacer(Modifier.height(8.dp))
                            SelectionContainer { Text(openQrCodeDialog!!.first) }
                        }
                    },
                    onDismissRequest = { openQrCodeDialog = null },
                    confirmButton = {},
                    dismissButton = {}
                )
            }
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun LinkShareFileCard(
        fileShareState: FileShareState,
        httpShareFileServer: HttpShareFileServer,
        onStartServer: () -> Unit,
        onClickOpenQRCode: (Pair<String, ImageRequest>) -> Unit
    ) {
        val allIPAddresses = getAllIPAddresses(type = SocketClientIPEnum.IPV4_UP)
        var address by remember { mutableStateOf(allIPAddresses.first()) }

        var showAddress by remember { mutableStateOf(false) }

        var showSettings by remember { mutableStateOf(false) }
        var encryption by remember { mutableStateOf(false) }
        var passwordAccess by remember { mutableStateOf(false) }
        val password by fileShareState.connectPassword.collectAsState()

        val url by derivedStateOf {
            "${if (encryption) "https" else "http"}://${address}:8080/${if (passwordAccess) "?pwd=${password}" else ""}"
        }

        var isRunning by remember { mutableStateOf(httpShareFileServer.isRunning()) }

        val qrCodeColor = colorScheme.primary.toArgb()
        val qrCodeBackground = colorScheme.background.toArgb()

        var imageRequest by remember { mutableStateOf<ImageRequest?>(null) }

        val scope = rememberCoroutineScope()

        // 添加一个状态来跟踪是否正在关闭中
        var isClosing by remember { mutableStateOf(false) }

        LaunchedEffect(url) {
            imageRequest = ImageRequest(
                data = QRCode.ofSquares()
                    .withColor(qrCodeColor)
                    .withBackgroundColor(qrCodeBackground)
                    .withSize(10)
                    .build(url).renderToBytes()
            ) {
                components {
                    add(ByteArrayFetcher.Factory())
                }
            }
        }

        Card(Modifier.padding(start = 16.dp, end = 16.dp)) {
            Column {
                Row {
                    Box(Modifier.size(128.dp).clickable {
                        onClickOpenQRCode(Pair(url, imageRequest!!))
                    }, contentAlignment = Alignment.Center) {
                        if (imageRequest == null) {
                            CircularProgressIndicator()
                            return@Box
                        }
                        Image(rememberImagePainter(imageRequest!!), null, Modifier.size(128.dp))
                    }
                    Column(Modifier.weight(1f).padding(16.dp)) {
                        Text(
                            if (isRunning) "服务已启动" else "服务未启动",
                            style = Typography.titleLarge
                        )
                        Spacer(Modifier.height(8.dp))
                        SelectionContainer {
                            Text("${if (encryption) "https" else "http"}://${address}:12040/${if (passwordAccess) "?pwd=${password}" else ""}")
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
//                        FilterChip(
//                            selected = encryption,
//                            onClick = { encryption = !encryption },
//                            modifier = Modifier.padding(horizontal = 4.dp)
//                                .align(Alignment.CenterVertically),
//                            label = { Text("加密") },
//                            leadingIcon = {
//                                if (!encryption) return@FilterChip
//                                Icon(
//                                    Icons.Default.Done,
//                                    contentDescription = null
//                                )
//                            }
//                        )
                        FilterChip(
                            selected = passwordAccess,
                            onClick = {
                                passwordAccess = !passwordAccess
                                if (passwordAccess) {
                                    fileShareState.updateConnectPassword(6.randomString(includeSpecial = false))
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
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                if (isRunning) {
                                    isClosing = true // 设置关闭中状态
                                    httpShareFileServer.stop()
                                    isClosing = false // 关闭完成
                                    fileShareState.authorizedLinkShareDevices.clear()
                                    fileShareState.pendingLinkShareDevices.clear()
                                    fileShareState.rejectedLinkShareDevices.clear()
                                } else {
                                    httpShareFileServer.start()
                                    onStartServer()
                                }
                                isRunning = httpShareFileServer.isRunning()
                            }
                        },
                        // 当正在关闭时禁用按钮
                        enabled = !isClosing
                    ) {
                        when {
                            isClosing -> Text("关闭中...")
                            isRunning -> Text("关闭")
                            else -> Text("启动")
                        }
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