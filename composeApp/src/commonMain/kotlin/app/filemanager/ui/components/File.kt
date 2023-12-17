package app.filemanager.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.filemanager.data.FileInfo
import app.filemanager.extensions.formatFileSize
import app.filemanager.extensions.timestampToSyncDate


@Composable
fun FileCard(
    file: FileInfo,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    ListItem(
        overlineContent = if (file.description.isNotEmpty()) {
            { Text(file.description) }
        } else {
            null
        },
        headlineContent = {
            if (file.name.isNotEmpty()) {
                Text(file.name)
            }
        },
        supportingContent = {
            Row {
//                Text(file.user, style = MaterialTheme.typography.bodySmall)
//                Spacer(Modifier.width(8.dp))
//                Text(file.userGroup, style = MaterialTheme.typography.bodySmall)
//                Spacer(Modifier.width(8.dp))
                if (file.isDirectory) {
                    Text("${file.size}项", style = MaterialTheme.typography.bodySmall)
                } else {
                    Text(file.size.formatFileSize(), style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.width(8.dp))
                Text(file.createdDate.timestampToSyncDate(), style = MaterialTheme.typography.bodySmall)
            }
        },
        leadingContent = {
            if (file.isDirectory) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = file.name,
                )
            } else {
                Icon(
                    Icons.Filled.Note,
                    contentDescription = file.name,
                )
            }
        },
        trailingContent = {
            FileCardMenu()
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun FileCardMenu() {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.wrapContentSize(Alignment.TopStart)
    ) {
        Icon(
            Icons.Filled.MoreVert,
            contentDescription = "Localized description",
            modifier = Modifier.clickable {
                expanded = true
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("复制") },
                onClick = { /* Handle edit! */ },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.FileCopy,
                        contentDescription = null
                    )
                })
            DropdownMenuItem(
                text = { Text("移动") },
                onClick = { /* Handle edit! */ },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.ContentCut,
                        contentDescription = null
                    )
                })
            DropdownMenuItem(
                text = { Text("删除") },
                onClick = { /* Handle settings! */ },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = null
                    )
                })
            Divider()
            DropdownMenuItem(
                text = { Text("重命名") },
                onClick = { /* Handle settings! */ },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = null
                    )
                })
            DropdownMenuItem(
                text = { Text("设置") },
                onClick = { /* Handle settings! */ },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Settings,
                        contentDescription = null
                    )
                })
            Divider()
            DropdownMenuItem(
                text = { Text("属性") },
                onClick = { /* Handle send feedback! */ },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null
                    )
                },
                trailingIcon = { Text("F11", textAlign = TextAlign.Center) })
            DropdownMenuItem(
                text = { Text("Send Feedback") },
                onClick = { /* Handle send feedback! */ },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Email,
                        contentDescription = null
                    )
                },
                trailingIcon = { Text("F11", textAlign = TextAlign.Center) })
        }
    }
}