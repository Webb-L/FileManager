package app.filemanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.filemanager.data.StatusEnum
import app.filemanager.ui.state.main.Task

@Composable
fun TaskInfoDialog(
    task: Task,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onToResult: () -> Unit
) {
    AlertDialog(
        title = {
            task.toTitle()
        },
        text = {
            FileOperation(task)
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onDismiss) {
                Text("取消")
            }
        },
        dismissButton = {
            when (task.status) {
                StatusEnum.SUCCESS -> Box(Modifier.size(0.dp))
                StatusEnum.FAILURE -> Row {
                    TextButton(onToResult) {
                        Text("查看结果")
                    }
                    TextButton(
                        { }
                    ) {
                        Text("删除任务")
                    }
                }

                StatusEnum.LOADING -> Row {
                    TextButton(
                        { }
                    ) {
                        Text("删除任务")
                    }
                    TextButton(
                        { }
                    ) {
                        Text("暂停任务")
                    }
                }

                StatusEnum.PAUSE -> Row {
                    TextButton(
                        { }
                    ) {
                        Text("删除任务")
                    }
                    TextButton(
                        { }
                    ) {
                        Text("继续任务")
                    }
                }
            }
        }

    )
}

@Composable
private fun FileOperation(task: Task) {
    val currentIndex = (task.result["index"] ?: "0").toInt()
    val total = (task.result["index"] ?: "0").toInt()
    Column {
        LinearProgressIndicator(
            progress = { (currentIndex / total.toFloat()) },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .height(10.dp),
        )
        Spacer(Modifier.height(16.dp))
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("$total 总计")
            Spacer(Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(
                    Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ProgressIndicatorDefaults.linearColor)
                )
                Spacer(Modifier.width(4.dp))
                Text("$currentIndex 完成")
            }
            Spacer(Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(
                    Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ProgressIndicatorDefaults.linearTrackColor)
                )
                Spacer(Modifier.width(4.dp))
                Text("${total - currentIndex} 剩余")
            }
        }
    }
}