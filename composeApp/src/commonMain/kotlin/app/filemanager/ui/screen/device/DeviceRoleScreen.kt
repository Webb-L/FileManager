package app.filemanager.ui.screen.device

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.filemanager.db.DeviceRole
import app.filemanager.db.FileManagerDatabase
import app.filemanager.exception.EmptyDataException
import app.filemanager.ui.components.GridList
import app.filemanager.ui.state.main.MainState
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject

class DeviceRoleScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val mainState = koinInject<MainState>()

        val database = koinInject<FileManagerDatabase>()
        val roles = mutableStateListOf<DeviceRole>()

        LaunchedEffect(Unit) {
            roles.addAll(database.deviceRoleQueries.select().executeAsList())
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("设备角色") },
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
                ExtendedFloatingActionButton({
                    navigator.push(EditRoleScreen(onSave = { newRole ->
                        roles.add(newRole)
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
                exception = if (roles.isEmpty()) EmptyDataException() else null
            ) {
                items(roles) { role ->
                    RoleItem(
                        role = role,
                        onEdit = {
                            navigator.push(EditRoleScreen(role = role, onSave = { updatedRole ->
                                val index = roles.indexOfFirst { it.id == updatedRole.id }
                                if (index >= 0) roles[index] = updatedRole
                            }))
                        },
                        onDelete = {
                            roles.remove(role)
                            // Optionally perform database deletion here
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun RoleItem(role: DeviceRole, onEdit: () -> Unit, onDelete: () -> Unit) {
        ListItem(
            modifier = Modifier.clickable(onClick = onEdit),
            overlineContent = {
                Text("已配置33个设备")
            },
            headlineContent = { Text(role.name) },
            supportingContent =
                if (role.comment == null)
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
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val (name, setName) = remember { mutableStateOf(role?.name ?: "") }
        val (comment, setComment) = remember { mutableStateOf(role?.comment ?: "") }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (role == null) "新增角色" else "编辑角色") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            val updatedRole = DeviceRole(
                                id = role?.id ?: 0L,
                                name = name,
                                comment = comment,
                                sortOrder = role?.sortOrder ?: 0L
                            )
                            onSave(updatedRole)
                            navigator.pop()
                        }) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                        }
                    }
                )
            }
        ) {
            LazyColumn(Modifier.padding(it)) {
                item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = setName,
                            label = { Text("角色名") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = comment,
                            onValueChange = setComment,
                            label = { Text("备注") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item { Text("权限") }
            }
        }
    }
}