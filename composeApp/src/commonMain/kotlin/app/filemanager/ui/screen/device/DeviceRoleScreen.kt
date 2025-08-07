@file:OptIn(kotlin.time.ExperimentalTime::class)

package app.filemanager.ui.screen.device

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.filemanager.data.device.DeviceRole
import app.filemanager.db.DevicePermission
import app.filemanager.db.FileManagerDatabase
import app.filemanager.exception.EmptyDataException
import app.filemanager.ui.components.GridList
import app.filemanager.ui.state.device.DevicePermissionState
import app.filemanager.ui.state.device.DeviceRoleState
import app.filemanager.ui.state.main.MainState
import app.filemanager.utils.WindowSizeClass
import app.filemanager.utils.calculateWindowSizeClass
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class DeviceRoleScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val state = koinInject<DeviceRoleState>()

        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("设备角色") },
                    navigationIcon = {
                        IconButton(
                            onClick = navigator::pop
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
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                ExtendedFloatingActionButton({
                    navigator.push(EditRoleScreen(onSave = { newRole ->
                        state.roles.add(newRole)
                    }))
                }) {
                    Icon(Icons.Filled.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("新增")
                }
            }
        ) { paddingValues ->
            GridList(
                modifier = Modifier.padding(paddingValues),
                exception = if (state.roles.isEmpty()) EmptyDataException() else null
            ) {
                itemsIndexed(state.roles) { index, role ->
                    RoleItem(
                        role = role,
                        onEdit = {
                            navigator.push(
                                EditRoleScreen(
                                    role = role,
                                    onSave = { updatedRole ->
                                        state.roles[index] = updatedRole
                                        println(state.roles[index])
                                        println(updatedRole)
                                    }
                                ))
                        },
                        onDelete = {
                            scope.launch(Dispatchers.Default) {
                                when (snackbarHostState.showSnackbar(
                                    message = role.name,
                                    actionLabel = "删除",
                                    withDismissAction = true,
                                    duration = SnackbarDuration.Short
                                )) {
                                    SnackbarResult.Dismissed -> {}
                                    SnackbarResult.ActionPerformed -> {
                                        state.delete(role, index)
                                    }
                                }
                            }
                        })
                }
            }
        }
    }

    @Composable
    private fun RoleItem(role: DeviceRole, onEdit: () -> Unit, onDelete: () -> Unit) {
        ListItem(
            modifier = Modifier.clickable(onClick = onEdit),
            overlineContent = {
                Text("已配置${role.permissionCount}个权限")
            },
            headlineContent = { Text(role.name) },
            supportingContent =
                if (role.comment.isNullOrEmpty())
                    null
                else {
                    {
                        Text(role.comment)
                    }
                },
            trailingContent = {
                IconButton(onDelete) { Icon(Icons.Default.Delete, null) }
            }
        )
    }
}

class EditRoleScreen(
    private val role: DeviceRole? = null,
    private val onSave: (DeviceRole) -> Unit
) : Screen {
    private val isEdit = role != null
    private val permissionIds = mutableStateListOf<Long>()
    private val oldPermissionIds = mutableStateListOf<Long>()

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val database = koinInject<FileManagerDatabase>()
        val mainState = koinInject<MainState>()
        val permissionState = koinInject<DevicePermissionState>()
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }

        val (name, setName) = remember { mutableStateOf(role?.name ?: "") }
        val (comment, setComment) = remember { mutableStateOf(role?.comment ?: "") }

        LaunchedEffect(Unit) {
            if (!isEdit) return@LaunchedEffect
            database
                .deviceRoleDevicePermissionQueries
                .queryDevicePermissionIdByRoleId(role?.id ?: 0L)
                .executeAsList()
                .apply {
                    permissionIds.addAll(this)
                    oldPermissionIds.addAll(this)
                }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (role == null) "新增角色" else "编辑角色") },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (isNotSave(name, comment)) {
                                scope.launch(Dispatchers.Default) {
                                    when (snackbarHostState.showSnackbar(
                                        message = "数据没有保存",
                                        actionLabel = "舍弃",
                                        withDismissAction = true,
                                        duration = SnackbarDuration.Short
                                    )) {
                                        SnackbarResult.Dismissed -> {}
                                        SnackbarResult.ActionPerformed -> {
                                            navigator.pop()
                                        }
                                    }
                                }
                                return@IconButton
                            }
                            navigator.pop()
                        }) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
                        }
                    },
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                FloatingActionButton(onClick = {
                    var id = role?.id ?: 0L
                    if (isEdit) {
                        database.deviceRoleQueries.updateRoleById(
                            name = name,
                            comment = comment,
                            id = role?.id ?: 0L
                        )
                    } else {
                        database.deviceRoleQueries.insert(
                            name,
                            comment,
                            role?.sortOrder ?: 0L
                        )
                        id = database.deviceRoleQueries.lastInsertRowId().executeAsOne()
                    }

                    if (permissionIds != oldPermissionIds) {
                        val addedPermissions = permissionIds.filterNot { oldPermissionIds.contains(it) }
                        val removedPermissions = oldPermissionIds.filterNot { permissionIds.contains(it) }
                        for (permissionId in addedPermissions) {
                            database.deviceRoleDevicePermissionQueries.insert(id, permissionId)
                        }
                        if (removedPermissions.isNotEmpty()) {
                            database.deviceRoleDevicePermissionQueries.deleteByDevicePermissionIds(removedPermissions)
                        }
                    }

                    database.deviceRoleQueries.selectById(id).executeAsOneOrNull()?.let {
                        onSave(
                            DeviceRole(
                                id = it.id,
                                name = it.name,
                                comment = it.comment,
                                sortOrder = it.sortOrder,
                                permissionCount = database.deviceRoleDevicePermissionQueries.queryCountByDeviceRoleId(id)
                                    .executeAsOne()
                            )
                        )
                    }

                    navigator.pop()
                }) {
                    Icon(Icons.Default.Done, null)
                }
            }
        ) {
            BoxWithConstraints(Modifier.padding(it)) {
                val columnCount = when (calculateWindowSizeClass(maxWidth, maxHeight)) {
                    WindowSizeClass.Compact -> 1
                    WindowSizeClass.Medium -> 2
                    WindowSizeClass.Expanded -> 3
                }
                LazyVerticalGrid(columns = GridCells.Fixed(columnCount)) {
                    item(span = { GridItemSpan(columnCount) }) {
                        Column(Modifier.padding(horizontal = 16.dp)) {
                            TextField(
                                value = name,
                                onValueChange = setName,
                                label = { Text("角色名") },
                                modifier = Modifier.fillMaxWidth(),
                                isError = name.isEmpty(),
                                trailingIcon = {
                                    if (name.isNotEmpty()) {
                                        IconButton({ setName("") }) {
                                            Icon(Icons.Default.Close, null)
                                        }
                                    }
                                },
                                supportingText = {
                                    if (name.isEmpty()) {
                                        Text("角色名不能为空")
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            TextField(
                                value = comment,
                                onValueChange = setComment,
                                label = { Text("备注") },
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    if (comment.isNotEmpty()) {
                                        IconButton({ setComment("") }) {
                                            Icon(Icons.Default.Close, null)
                                        }
                                    }
                                },
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    item(span = { GridItemSpan(columnCount) }) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("权限")
                            IconButton({ navigator.push(DevicePermissionScreen()) }) {
                                Icon(Icons.Default.Add, null)
                            }
                        }
                    }

                    itemsIndexed(
                        permissionState.permissions,
                        key = { _, permission -> permission.id }) { index, permission ->
                        PermissionItem(
                            isChecked = permissionIds.contains(permission.id),
                            permission = permission,
                            onToggleStatus = { new -> permissionState.update(new, index) },
                            onClick = {
                                if (permissionIds.contains(permission.id)) {
                                    permissionIds.remove(permission.id)
                                } else {
                                    permissionIds.add(permission.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun PermissionItem(
        isChecked: Boolean,
        permission: DevicePermission,
        onToggleStatus: (DevicePermission) -> Unit,
        onClick: () -> Unit,
    ) {
        ListItem(
            leadingContent = if (isChecked) {
                { Icon(Icons.Default.Done, null) }
            } else null,
            headlineContent = {
                Text(
                    text = permission.path,
                    style = typography.titleMedium
                )
            },
            supportingContent = {
                FlowRow {
                    FilterChip(
                        selected = permission.useAll,
                        enabled = false,
                        onClick = { onToggleStatus(permission.copy(useAll = !permission.useAll)) },
                        shape = RoundedCornerShape(25.dp),
                        modifier = Modifier.padding(horizontal = 4.dp)
                            .align(Alignment.CenterVertically),
                        label = { Text("应用到子级") }
                    )
                    FilterChip(
                        selected = permission.read,
                        enabled = false,
                        onClick = { onToggleStatus(permission.copy(read = !permission.read)) },
                        shape = RoundedCornerShape(25.dp),
                        modifier = Modifier.padding(horizontal = 4.dp)
                            .align(Alignment.CenterVertically),
                        label = { Text("读取") }
                    )
                    FilterChip(
                        selected = permission.write,
                        enabled = false,
                        onClick = { onToggleStatus(permission.copy(write = !permission.write)) },
                        shape = RoundedCornerShape(25.dp),
                        modifier = Modifier.padding(horizontal = 4.dp)
                            .align(Alignment.CenterVertically),
                        label = { Text("写入") }
                    )
                    FilterChip(
                        selected = permission.remove,
                        enabled = false,
                        onClick = { onToggleStatus(permission.copy(remove = !permission.remove)) },
                        shape = RoundedCornerShape(25.dp),
                        modifier = Modifier.padding(horizontal = 4.dp)
                            .align(Alignment.CenterVertically),
                        label = { Text("删除") }
                    )
                    FilterChip(
                        selected = permission.rename,
                        enabled = false,
                        onClick = { onToggleStatus(permission.copy(rename = !permission.rename)) },
                        shape = RoundedCornerShape(25.dp),
                        modifier = Modifier.padding(horizontal = 4.dp)
                            .align(Alignment.CenterVertically),
                        label = { Text("重命名") }
                    )
                }
            },
            modifier = Modifier.clickable(onClick = onClick)
        )
    }

    private fun isNotSave(name: String, comment: String): Boolean {
        return if (isEdit) {
            name != role!!.name || comment != role.comment || permissionIds.toList() != oldPermissionIds.toList()
        } else {
            name.isNotEmpty() || comment.isNotEmpty() || permissionIds.isNotEmpty()
        }
    }
}