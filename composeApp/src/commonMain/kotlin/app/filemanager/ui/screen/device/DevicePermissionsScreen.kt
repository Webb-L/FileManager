package app.filemanager.ui.screen.device

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.filemanager.db.DevicePermission
import app.filemanager.exception.EmptyDataException
import app.filemanager.ui.components.GridList
import app.filemanager.ui.state.device.DevicePermissionState
import app.filemanager.ui.state.main.MainState
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class DevicePermissionScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val state = koinInject<DevicePermissionState>()

        val mainState = koinInject<MainState>()
        val scope = rememberCoroutineScope()
        var devicePermission by remember { mutableStateOf<DevicePermission?>(null) }
        var openDialog by remember { mutableStateOf(false) }

        val snackbarHostState = remember { SnackbarHostState() }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("权限") },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                mainState.updateScreen(null)
                                navigator.pop()
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
                        }
                    },
                    actions = {
                        IconButton(onClick = {}) {
                            Icon(Icons.Filled.Search, contentDescription = null)
                        }
                    }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton({ openDialog = true }) {
                    Icon(Icons.Filled.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("新增")
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { paddingValues ->
            GridList(
                modifier = Modifier.padding(paddingValues),
                exception = if (state.permissions.isEmpty()) EmptyDataException() else null
            ) {
                itemsIndexed(state.permissions, key = { _, permission -> permission.id }) { index, permission ->
                    PermissionItem(
                        permission = permission,
                        onToggleStatus = { state.update(it, index) },
                        onEdit = {
                            devicePermission = permission
                            openDialog = true
                        },
                        onDelete = {
                            scope.launch {
                                when (snackbarHostState.showSnackbar(
                                    message = permission.path,
                                    actionLabel = "删除",
                                    withDismissAction = true,
                                    duration = SnackbarDuration.Short
                                )) {
                                    SnackbarResult.Dismissed -> {}
                                    SnackbarResult.ActionPerformed -> {
                                        state.delete(permission, index)
                                    }
                                }
                            }
                        }
                    )
                }
            }

            if (openDialog) {
                DevicePermissionEditDialog(
                    initialPermission = devicePermission,
                    onDismiss = {
                        devicePermission = null
                        openDialog = false
                    },
                    onSave = { path, comment ->
                        if (devicePermission != null) {
                            state.update(
                                devicePermission!!.copy(
                                    path = path,
                                    comment = comment
                                ),
                                state.permissions.indexOf(devicePermission!!)
                            )
                        } else {
                            state.create(path, comment)
                        }
                    }
                )
            }
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun PermissionItem(
        permission: DevicePermission,
        onToggleStatus: (DevicePermission) -> Unit,
        onEdit: () -> Unit,
        onDelete: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(8.dp),
        ) {
            Column(modifier = Modifier) {
                Box(Modifier.clickable(onClick = onEdit)) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = permission.path,
                                style = typography.titleMedium
                            )
                            if (permission.comment != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = permission.comment,
                                    style = typography.bodyMedium,
                                    color = colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, null)
                        }
                    }
                }
                Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("权限列表")
                    FlowRow {
                        FilterChip(
                            selected = permission.useAll,
                            onClick = { onToggleStatus(permission.copy(useAll = !permission.useAll)) },
                            shape = RoundedCornerShape(25.dp),
                            modifier = Modifier.padding(horizontal = 4.dp)
                                .align(Alignment.CenterVertically),
                            label = { Text("应用到子级") }
                        )
                        FilterChip(
                            selected = permission.read,
                            onClick = { onToggleStatus(permission.copy(read = !permission.read)) },
                            shape = RoundedCornerShape(25.dp),
                            modifier = Modifier.padding(horizontal = 4.dp)
                                .align(Alignment.CenterVertically),
                            label = { Text("读取") }
                        )
                        FilterChip(
                            selected = permission.write,
                            onClick = { onToggleStatus(permission.copy(write = !permission.write)) },
                            shape = RoundedCornerShape(25.dp),
                            modifier = Modifier.padding(horizontal = 4.dp)
                                .align(Alignment.CenterVertically),
                            label = { Text("写入") }
                        )
                        FilterChip(
                            selected = permission.remove,
                            onClick = { onToggleStatus(permission.copy(remove = !permission.remove)) },
                            shape = RoundedCornerShape(25.dp),
                            modifier = Modifier.padding(horizontal = 4.dp)
                                .align(Alignment.CenterVertically),
                            label = { Text("删除") }
                        )
                        FilterChip(
                            selected = permission.rename,
                            onClick = { onToggleStatus(permission.copy(rename = !permission.rename)) },
                            shape = RoundedCornerShape(25.dp),
                            modifier = Modifier.padding(horizontal = 4.dp)
                                .align(Alignment.CenterVertically),
                            label = { Text("重命名") }
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun DevicePermissionEditDialog(
        initialPermission: DevicePermission?,
        onDismiss: () -> Unit,
        onSave: (String, String?) -> Unit
    ) {
        var path by remember { mutableStateOf(initialPermission?.path ?: "") }
        var comment by remember { mutableStateOf(initialPermission?.comment ?: "") }

        AlertDialog(
            onDismissRequest = { onDismiss() },
            title = {
                Text(if (initialPermission == null) "新增权限" else "编辑权限")
            },
            text = {
                Column {
                    // 设备路径
                    TextField(
                        value = path,
                        onValueChange = { path = it },
                        singleLine = true,
                        label = { Text("路径") },
                        isError = path.isEmpty(),
                        leadingIcon = {
                            IconButton({}) {
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                        },
                        trailingIcon = {
                            if (path.isNotEmpty()) {
                                IconButton({ path = "" }) {
                                    Icon(Icons.Default.Close, null)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    // 备注 (comment)
                    TextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text("备注") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        singleLine = false,
                        trailingIcon = {
                            if (comment.isNotEmpty()) {
                                IconButton({ comment = "" }) {
                                    Icon(Icons.Default.Close, null)
                                }
                            }
                        },
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSave(path, comment.ifEmpty { null })
                        onDismiss()
                    },
                    enabled = path.isNotEmpty(),
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        )
    }
}