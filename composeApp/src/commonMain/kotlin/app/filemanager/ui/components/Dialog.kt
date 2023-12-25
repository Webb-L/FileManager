package app.filemanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.filemanager.data.FileInfo
import app.filemanager.extensions.timestampToSyncDate


@Composable
fun FileInfoDialog(fileInfo: FileInfo, onCancel: () -> Unit) {
    AlertDialog(
        icon = { FileIcon(fileInfo) },
        title = { SelectionContainer { Text(fileInfo.name) } },
        text = {
            SelectionContainer {
                Column {
                    LinearProgressIndicator(progress = (100 / 1232).toFloat(), Modifier.fillMaxWidth().height(4.dp))
                    Spacer(Modifier.height(4.dp))
                    DisableSelection {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("100GB 总计")
                            Spacer(Modifier.width(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Spacer(
                                    Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(ProgressIndicatorDefaults.linearColor)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("100GB 已用")
                            }
                            Spacer(Modifier.width(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Spacer(
                                    Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(ProgressIndicatorDefaults.linearTrackColor)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("100GB 剩余")
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth().padding(4.dp)) {
                        DisableSelection {
                            Text("位置", Modifier.weight(0.3f))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(fileInfo.path, Modifier.weight(0.7f))
                    }
                    Row(Modifier.fillMaxWidth().padding(4.dp)) {
                        DisableSelection {
                            Text("类型", Modifier.weight(0.3f))
                        }
                        Spacer(Modifier.width(8.dp))
                        DisableSelection {
                            Text(if (fileInfo.isDirectory) "文件夹" else "文件", Modifier.weight(0.7f))
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(4.dp)) {
                        DisableSelection {
                            Text("权限", Modifier.weight(0.3f))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("Column 2", Modifier.weight(0.7f))
                    }
                    Row(Modifier.fillMaxWidth().padding(4.dp)) {
                        DisableSelection {
                            Text("创建时间", Modifier.weight(0.3f))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(fileInfo.createdDate.timestampToSyncDate(), Modifier.weight(0.7f))
                    }
                    Row(Modifier.fillMaxWidth().padding(4.dp)) {
                        DisableSelection {
                            Text("更新时间", Modifier.weight(0.3f))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(fileInfo.updatedDate.timestampToSyncDate(), Modifier.weight(0.7f))
                    }
                }
            }
        },
        onDismissRequest = onCancel,
        confirmButton = {},
        dismissButton = {}
    )
}

@Composable
fun FileRenameDialog(fileInfo: FileInfo) {
    var text by rememberSaveable { mutableStateOf(fileInfo.name) }

    AlertDialog(
        icon = { Icon(Icons.Outlined.Edit, null) },
        title = { Text("重命名") },
        text = {
            TextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("名称") },
                leadingIcon = { FileIcon(fileInfo) },
                trailingIcon = {
                    if (text.isNotEmpty()) {
                        IconButton({ text = "" }) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                }
            )
        },
        onDismissRequest = {},
        confirmButton = {
            TextButton(
                onClick = {
//                    openDialog.value = false
                }
            ) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
//                    openDialog.value = false
                }
            ) {
                Text("取消")
            }
        }

    )
}