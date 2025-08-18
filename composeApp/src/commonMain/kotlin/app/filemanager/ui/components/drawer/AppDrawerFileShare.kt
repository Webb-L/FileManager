package app.filemanager.ui.components.drawer

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import app.filemanager.service.HttpShareFileServer
import app.filemanager.service.rpc.SocketClientIPEnum
import app.filemanager.service.rpc.getAllIPAddresses
import app.filemanager.ui.screen.file.FileShareScreen
import app.filemanager.ui.state.file.FileShareState
import app.filemanager.ui.state.main.MainState
import app.filemanager.utils.WindowSizeClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun AppDrawerFileShare() {
    val mainState = koinInject<MainState>()
    val fileShareState = koinInject<FileShareState>()
    val httpShareFileServer = HttpShareFileServer.getInstance(fileShareState)
    val scope = rememberCoroutineScope()
    
    // 使用共享状态
    val isServerRunning by fileShareState.isHttpServerRunning.collectAsState()
    
    // 初始化状态
    LaunchedEffect(Unit) {
        fileShareState.updateHttpServerRunning(httpShareFileServer.isRunning())
    }

    // 只有在服务运行时才显示
    if (!isServerRunning) return

    // 关闭服务确认对话框状态
    var showStopServiceDialog by remember { mutableStateOf(false) }

    val allIPAddresses = getAllIPAddresses(type = SocketClientIPEnum.IPV4_UP)
    val address = allIPAddresses.firstOrNull() ?: "localhost"
    val connectedDevicesCount = fileShareState.authorizedLinkShareDevices.size
    val filesCount = fileShareState.files.size

    AppDrawerItem("文件共享", actions = {}) {
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Share, null) },
            label = {
                Column {
                    Text("$address:12040")
                    Text(
                        "文件: ${filesCount}个 • 连接: ${connectedDevicesCount}个",
                        style = typography.bodySmall
                    )
                }
            },
            selected = false,
            onClick = {
                if (mainState.windowSize == WindowSizeClass.Compact) {
                    mainState.updateExpandDrawer(false)
                }
                mainState.navigator?.push(FileShareScreen(fileShareState.files))
            },
            badge = {
                IconButton(
                    onClick = {
                        showStopServiceDialog = true
                    }
                ) {
                    Icon(
                        Icons.Default.Close,
                        "关闭服务"
                    )
                }
            },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
    }

    // 关闭服务确认对话框
    if (showStopServiceDialog) {
        AlertDialog(
            onDismissRequest = { showStopServiceDialog = false },
            title = { Text("确认关闭服务") },
            text = { Text("确定要关闭文件共享服务吗？这将断开所有连接的设备。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch(Dispatchers.Default) {
                            httpShareFileServer.stop()
                            fileShareState.authorizedLinkShareDevices.clear()
                            fileShareState.pendingLinkShareDevices.clear()
                            fileShareState.rejectedLinkShareDevices.clear()
                            // 更新共享状态
                            fileShareState.updateHttpServerRunning(false)
                        }
                        showStopServiceDialog = false
                    },
                ) {
                    Text("关闭服务")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopServiceDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 添加分隔线
    HorizontalDivider()
}