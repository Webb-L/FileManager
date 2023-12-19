package app.filemanager.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.SwitchLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate

@Composable
fun SortButton() {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .wrapContentSize(Alignment.TopStart)
    ) {
        IconButton({ expanded = true }) {
            Icon(Icons.Default.Sort, null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("文件名称") },
                onClick = { },
                trailingIcon = {
                    Icon(Icons.Default.SwitchLeft, null, Modifier.rotate(90f))
                }
            )
            DropdownMenuItem(
                text = { Text("文件大小") },
                onClick = { },
                trailingIcon = {
                    Icon(Icons.Default.SwitchLeft, null, Modifier.rotate(90f))
                }
            )
            DropdownMenuItem(
                text = { Text("文件类型") },
                onClick = { },
                trailingIcon = {
                    Icon(Icons.Default.SwitchLeft, null, Modifier.rotate(90f))
                }
            )
            DropdownMenuItem(
                text = { Text("文件创建时间") },
                onClick = { },
                trailingIcon = {
                    Icon(Icons.Default.SwitchLeft, null, Modifier.rotate(90f))
                }
            )
            DropdownMenuItem(
                text = { Text("文件修改时间") },
                onClick = { },
                trailingIcon = {
                    Icon(Icons.Default.SwitchLeft, null, Modifier.rotate(90f))
                }
            )
        }
    }
}