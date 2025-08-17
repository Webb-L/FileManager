package app.filemanager.ui.components.drawer

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.filemanager.data.StatusEnum
import app.filemanager.data.file.toIcon
import app.filemanager.ui.components.TaskInfoDialog
import app.filemanager.ui.screen.task.TaskResultScreen
import app.filemanager.ui.state.main.MainState
import app.filemanager.ui.state.main.Task
import app.filemanager.ui.state.main.TaskState
import app.filemanager.ui.state.main.TaskType
import app.filemanager.utils.WindowSizeClass
import org.koin.compose.koinInject

@Composable
fun AppDrawerTask() {
    val taskState = koinInject<TaskState>()
    val mainState = koinInject<MainState>()
    var checkedTask by remember {
        mutableStateOf<Task?>(null)
    }

    AppDrawerItem("任务", actions = {
    }) {
        if (taskState.tasks.size > 1) {
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.TaskAlt, null) },
                label = {
                    Column {
                        Text("任务(${taskState.tasks.size})")
                        Text(
                            "执行中(${taskState.tasks.filter { it.status == StatusEnum.LOADING }.size})-暂停中(${taskState.tasks.filter { it.status == StatusEnum.PAUSE }.size})-失败(${taskState.tasks.filter { it.status == StatusEnum.FAILURE }.size})",
                            style = typography.bodySmall
                        )
                    }
                },
                selected = false,
                onClick = {
                },
                badge = {
                    Icon(Icons.Default.ExpandLess, null, Modifier.rotate(90f))
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
            return@AppDrawerItem
        }

        val task = taskState.tasks.first()
        NavigationDrawerItem(
            icon = {
                when (task.taskType) {
                    TaskType.Copy -> Icon(Icons.Outlined.FileCopy, null)
                    TaskType.Move -> Icon(Icons.Default.ContentCut, null)
                    TaskType.Delete -> Icon(Icons.Default.Delete, null)
                }
            },
            label = {
                Column {
                    when (task.taskType) {
                        TaskType.Copy -> Text("复制中")
                        TaskType.Move -> Text("移动中")
                        TaskType.Delete -> Text("删除中")
                    }
                    Row {
                        task.protocol.toIcon()
                        Text(
                            task.values["path"] ?: "",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = typography.bodySmall
                        )
                    }
                }
            },
            selected = false,
            onClick = {
                checkedTask = task
            },
            badge = {
                when (task.status) {
                    StatusEnum.SUCCESS -> {}
                    StatusEnum.FAILURE -> Row {
                        Icon(
                            Icons.Outlined.ErrorOutline,
                            null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.ExpandLess, null, Modifier.rotate(90f))
                    }

                    StatusEnum.PAUSE -> {
                        Icon(Icons.Default.Stop, null)
                    }

                    StatusEnum.LOADING -> CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 3.dp
                    )
                }
            },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
    }
    if (checkedTask != null) {
        TaskInfoDialog(
            checkedTask!!,
            onConfirm = {},
            onDismiss = {
                checkedTask = null
            },
            onToResult = {
                if (mainState.windowSize == WindowSizeClass.Compact) {
                    mainState.updateExpandDrawer(false)
                }
                mainState.navigator?.push(TaskResultScreen(checkedTask!!))
                checkedTask = null
            }
        )
    }
}