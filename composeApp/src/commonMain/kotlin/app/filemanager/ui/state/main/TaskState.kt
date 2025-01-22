package app.filemanager.ui.state.main

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import app.filemanager.data.StatusEnum
import app.filemanager.data.file.FileProtocol
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlin.random.Random


enum class TaskType {
    Copy,
    Move,
    Delete
}

@Serializable
data class Task(
    val taskType: TaskType,
    val key: Long = Clock.System.now().toEpochMilliseconds() + Random.nextInt(),
    var status: StatusEnum,
    // 任务参数
    val values: Map<String, String> = mapOf(),
    // 任务结果
    val result: MutableMap<String, String> = mutableMapOf(),
    var protocol: FileProtocol = FileProtocol.Local,
    var protocolId: String = "",
) {
    @Composable
    fun toTitle() {
        if (status == StatusEnum.SUCCESS) {
            return when (taskType) {
                TaskType.Copy -> Text("复制成功")
                TaskType.Move -> Text("移动成功")
                TaskType.Delete -> Text("删除成功")
            }
        }
        if (status == StatusEnum.PAUSE) {
            return when (taskType) {
                TaskType.Copy -> Text("复制暂停中")
                TaskType.Move -> Text("移动暂停中")
                TaskType.Delete -> Text("删除暂停中")
            }
        }
        if (status == StatusEnum.FAILURE) {
            return when (taskType) {
                TaskType.Copy -> Text("复制失败")
                TaskType.Move -> Text("移动失败")
                TaskType.Delete -> Text("删除失败")
            }
        }

        return when (taskType) {
            TaskType.Copy -> Text("复制中")
            TaskType.Move -> Text("移动中")
            TaskType.Delete -> Text("删除中")
        }
    }
}

class TaskState {
    val tasks = mutableStateListOf<Task>()
    val taskCancelKeys = mutableStateListOf<Long>()
    val taskStopKeys = mutableStateListOf<Long>()
}