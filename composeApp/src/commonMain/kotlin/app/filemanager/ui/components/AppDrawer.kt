package app.filemanager.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import app.filemanager.service.rpc.RpcClientManager.Companion.PORT
import app.filemanager.service.rpc.SocketClientIPEnum
import app.filemanager.service.rpc.getAllIPAddresses
import app.filemanager.ui.components.drawer.*
import app.filemanager.ui.screen.device.DeviceScreen
import app.filemanager.ui.screen.file.FileShareSettingsScreen
import app.filemanager.ui.screen.main.NotificationScreen
import app.filemanager.ui.state.main.DeviceState
import app.filemanager.ui.state.main.DrawerState
import app.filemanager.ui.state.main.MainState
import app.filemanager.ui.state.main.TaskState
import app.filemanager.utils.WindowSizeClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawer() {
    val mainState = koinInject<MainState>()
    val drawerState = koinInject<DrawerState>()
    val taskState = koinInject<TaskState>()
    val deviceState = koinInject<DeviceState>()
    val isDeviceAdd by deviceState.isDeviceAdd.collectAsState()

    val isExpandNetwork by drawerState.isExpandNetwork.collectAsState()

    val scope = rememberCoroutineScope()

    ModalDrawerSheet {
        TopAppBar(
            title = { Text("Name") },
            navigationIcon = {
                IconButton({}) {
                    Icon(Icons.Default.AccountCircle, null)
                }
            },
            actions = {
                IconButton({
                    if (mainState.windowSize == WindowSizeClass.Compact) {
                        mainState.updateExpandDrawer(false)
                    }
                    mainState.navigator?.push(NotificationScreen())
                }) {
                    BadgedBox(badge = { Badge { Text("1") } }) {
                        Icon(Icons.Default.Notifications, null)
                    }
                }

                MoreOptionsDropdown(mainState)
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )
        HorizontalDivider()
        LazyColumn {
//            item {
//                AppDrawerHeader("工具箱", actions = {
//                    Icon(
//                        Icons.Default.ChevronRight,
//                        null,
//                        Modifier.clip(RoundedCornerShape(25.dp))
//                            .clickable { }
//                    )
//                })
//                Spacer(Modifier.height(12.dp))
//            }
            if (taskState.tasks.isNotEmpty()) {
                item { AppDrawerTask() }
                item { HorizontalDivider() }
            }
            item { AppDrawerBookmark() }
            item { HorizontalDivider() }
            item { AppDrawerDevice() }
            item { HorizontalDivider() }
            // 展示当前连接设备ui和Task一样
            if (deviceState.remoteDeviceConnections.keys.isNotEmpty()) {
                item { AppDrawerConnectedDevice() }
                item { HorizontalDivider() }
            }
            // 展示文件共享服务状态
            item { AppDrawerFileShare() }
            item { AppDrawerShare() }
// TODO 3.0版本
//            item { HorizontalDivider() }
//            item {
//                AppDrawerItem(
//                    "网络",
//                    actions = {
//                        Row {
//                            Icon(Icons.Default.Add, null, Modifier)
//                            Spacer(Modifier.width(8.dp))
//                            Icon(
//                                if (isExpandNetwork) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
//                                null,
//                                Modifier.clip(RoundedCornerShape(25.dp))
//                                    .clickable { drawerState.updateExpandNetwork(!isExpandNetwork) }
//                            )
//                        }
//                    }
//                ) {
//                }
//            }
//            item { HorizontalDivider() }
        }
    }

    if (isDeviceAdd) {
        val allIPAddresses = getAllIPAddresses(
            type = SocketClientIPEnum.ALL
        )
        TextFieldDialog(
            "添加设备", label = "IP地址 或 IP地址:端口",
            verifyFun = { text ->
                if (text.isEmpty()) {
                    Pair(true, "请输入IP地址 或 IP地址:端口")
                } else {
                    if (allIPAddresses.any { text.indexOf(it) == 0 }) {
                        Pair(true, "禁止使用本地地址")
                    } else {
                        val regex = Regex(
                            "^(([0-9]{1,3}\\.){3}[0-9]{1,3}|\\[([a-fA-F0-9:]+)])(:([0-9]{1,5}))?$"
                        )
                        if (regex.matches(text)) {
                            Pair(false, "")
                        } else {
                            Pair(true, "请输入正确的IP地址 或 IP地址:端口")
                        }
                    }
                }
            }
        ) {
            if (it.isEmpty()) {
                deviceState.updateDeviceAdd(false)
                return@TextFieldDialog
            }
            val inputText = it

            val ip: String
            var port: String? = null

            if (inputText.startsWith("[")) {
                val rightBracketIndex = inputText.indexOf(']')
                if (rightBracketIndex != -1) {
                    // 截取完整的 IPv6 地址部分，如 [2001:db8::1]
                    ip = inputText.substring(0, rightBracketIndex + 1)
                    // 检查是否存在端口部分
                    if (rightBracketIndex + 1 < inputText.length && inputText[rightBracketIndex + 1] == ':') {
                        port = inputText.substring(rightBracketIndex + 2)
                    }
                } else {
                    // 若没有找到 ']'，则默认为整串都是 IP；也可根据需要进行异常处理
                    ip = inputText
                }
            } else {
                // 普通情况（如 IPv4 或不带中括号的 IPv6），用冒号分割
                val parts = inputText.split(":")
                ip = parts[0]
                if (parts.size > 1) {
                    port = parts[1]
                }
            }

            deviceState.updateDeviceAdd(false)

            scope.launch(Dispatchers.Default) {
                try {
                    deviceState.pingDevice(ip, (port ?: PORT).toString().toInt())
                } catch (e: Exception) {
                }
            }
        }
    }
}

@Composable
private fun MoreOptionsDropdown(mainState: MainState) {
    Box {
        var showDropdownMenu by remember { mutableStateOf(false) }

        IconButton(onClick = { showDropdownMenu = true }) {
            Icon(Icons.Default.Settings, contentDescription = "更多选项")
        }

        DropdownMenu(
            expanded = showDropdownMenu,
            onDismissRequest = { showDropdownMenu = false }
        ) {
            DropdownMenuItem(
                leadingIcon = { Icon(Icons.Default.Devices, null) },
                text = { Text("设备") },
                onClick = {
                    mainState.navigator?.push(DeviceScreen())
                    showDropdownMenu = false
                }
            )
            DropdownMenuItem(
                leadingIcon = { Icon(Icons.Default.Share, null) },
                text = { Text("分享") },
                onClick = {
                    mainState.navigator?.push(FileShareSettingsScreen())
                    showDropdownMenu = false
                }
            )
            DropdownMenuItem(
                leadingIcon = { Icon(Icons.Default.Settings, null) },
                text = { Text("设置") },
                onClick = {
                    showDropdownMenu = false
                }
            )
        }
    }
}

