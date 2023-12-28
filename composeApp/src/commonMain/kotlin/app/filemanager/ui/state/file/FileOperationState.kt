package app.filemanager.ui.state.file

import androidx.compose.runtime.mutableStateListOf
import app.filemanager.data.FileInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FileOperationState {
    var title: String = ""

    private val _isOperationDialog: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isOperationDialog: StateFlow<Boolean> = _isOperationDialog
    fun updateOperationDialog(value: Boolean) {
        _isOperationDialog.value = value
    }

    val fileInfos = mutableListOf<FileInfo>()

    private val _currentIndex: MutableStateFlow<Int> = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex
    fun updateCurrentIndex() {
        _currentIndex.value += 1
        if (_currentIndex.value == fileInfos.size) {
            _isOperationDialog.value = false
        }
    }

    var isStop = false
    var isCancel = false

    private val _isFinished: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isFinished: StateFlow<Boolean> = _isFinished
    fun updateFinished(value: Boolean) {
        _isFinished.value = value
    }

    val logs = mutableStateListOf<String>()

    fun updateFileInfos(value: List<FileInfo>) {
        fileInfos.clear()
        fileInfos.addAll(value)
        _currentIndex.value = 0
        _isFinished.value = false
        isStop = false
        isCancel = false
        logs.clear()
    }

    fun addLog(path: String) {
        logs.add("失败 - $path")
    }
}