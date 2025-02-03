package app.filemanager.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SwitchLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import app.filemanager.data.file.FileFilterSort
import app.filemanager.service.rpc.SocketClientIPEnum
import app.filemanager.service.rpc.getAllIPAddresses
import app.filemanager.ui.state.file.FileFilterState
import app.filemanager.ui.state.main.DeviceState
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun SortButton() {
    val fileFilterState = koinInject<FileFilterState>()
    val sortType by fileFilterState.sortType.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        IconButton({ expanded = true }) {
            Icon(Icons.AutoMirrored.Default.Sort, null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("文件名称") },
                onClick = {
                    if (sortType == FileFilterSort.NameAsc) {
                        fileFilterState.updateSortType(FileFilterSort.NameDesc)
                    } else {
                        fileFilterState.updateSortType(FileFilterSort.NameAsc)
                    }
                },
                trailingIcon = {
                    if (listOf(FileFilterSort.NameAsc, FileFilterSort.NameDesc).contains(sortType)) {
                        val modifier =
                            if (sortType == FileFilterSort.NameAsc)
                                Modifier.rotate(270f)
                            else
                                Modifier.rotate(90f)
                        Icon(
                            Icons.Default.SwitchLeft,
                            null,
                            modifier
                        )
                    }
                }
            )
            DropdownMenuItem(
                text = { Text("文件大小") },
                onClick = {
                    if (sortType == FileFilterSort.SizeAsc) {
                        fileFilterState.updateSortType(FileFilterSort.SizeDesc)
                    } else {
                        fileFilterState.updateSortType(FileFilterSort.SizeAsc)
                    }
                },
                trailingIcon = {
                    if (listOf(FileFilterSort.SizeAsc, FileFilterSort.SizeDesc).contains(sortType)) {
                        val modifier =
                            if (sortType == FileFilterSort.SizeAsc)
                                Modifier.rotate(270f)
                            else
                                Modifier.rotate(90f)
                        Icon(
                            Icons.Default.SwitchLeft,
                            null,
                            modifier
                        )
                    }
                }
            )
            DropdownMenuItem(
                text = { Text("文件类型") },
                onClick = {
                    if (sortType == FileFilterSort.TypeAsc) {
                        fileFilterState.updateSortType(FileFilterSort.TypeDesc)
                    } else {
                        fileFilterState.updateSortType(FileFilterSort.TypeAsc)
                    }
                },
                trailingIcon = {
                    if (listOf(FileFilterSort.TypeAsc, FileFilterSort.TypeDesc).contains(sortType)) {
                        val modifier =
                            if (sortType == FileFilterSort.TypeAsc)
                                Modifier.rotate(270f)
                            else
                                Modifier.rotate(90f)
                        Icon(
                            Icons.Default.SwitchLeft,
                            null,
                            modifier
                        )
                    }
                }
            )
            DropdownMenuItem(
                text = { Text("文件创建时间") },
                onClick = {
                    if (sortType == FileFilterSort.CreatedDateAsc) {
                        fileFilterState.updateSortType(FileFilterSort.CreatedDateDesc)
                    } else {
                        fileFilterState.updateSortType(FileFilterSort.CreatedDateAsc)
                    }
                },
                trailingIcon = {
                    if (listOf(FileFilterSort.CreatedDateAsc, FileFilterSort.CreatedDateDesc).contains(sortType)) {
                        val modifier =
                            if (sortType == FileFilterSort.CreatedDateAsc)
                                Modifier.rotate(270f)
                            else
                                Modifier.rotate(90f)
                        Icon(
                            Icons.Default.SwitchLeft,
                            null,
                            modifier
                        )
                    }
                }
            )
            DropdownMenuItem(
                text = { Text("文件修改时间") },
                onClick = {
                    if (sortType == FileFilterSort.UpdatedDateAsc) {
                        fileFilterState.updateSortType(FileFilterSort.UpdatedDateDesc)
                    } else {
                        fileFilterState.updateSortType(FileFilterSort.UpdatedDateAsc)
                    }
                },
                trailingIcon = {
                    if (listOf(FileFilterSort.UpdatedDateAsc, FileFilterSort.UpdatedDateDesc).contains(sortType)) {
                        val modifier =
                            if (sortType == FileFilterSort.UpdatedDateAsc)
                                Modifier.rotate(270f)
                            else
                                Modifier.rotate(90f)
                        Icon(
                            Icons.Default.SwitchLeft,
                            null,
                            modifier
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun IpsButton() {
    val deviceState = koinInject<DeviceState>()
    val loadingDevices by deviceState.loadingDevices.collectAsState()

    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()


    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart).alpha(if (loadingDevices) 0.5f else 1f)) {
        Icon(
            Icons.Default.Sensors,
            null,
            Modifier
                .clip(RoundedCornerShape(25.dp))
                .clickable {
                    if (loadingDevices) return@clickable
                    expanded = true
                }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            for (ip in getAllIPAddresses(type = SocketClientIPEnum.IPV4_UP)) {
                DropdownMenuItem(
                    text = { Text(ip) },
                    onClick = {
                        scope.launch {
                            deviceState.scanner(listOf(ip))
                        }
                        expanded = false
                    },
                    leadingIcon = { Icon(Icons.Default.Dns, null) },
                )
            }
        }
    }
}