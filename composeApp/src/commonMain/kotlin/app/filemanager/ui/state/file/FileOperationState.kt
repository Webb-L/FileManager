package app.filemanager.ui.state.file

import androidx.compose.runtime.mutableStateListOf
import app.filemanager.data.FileInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FileOperationState {
    private val _isOperationDialog: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isOperationDialog: StateFlow<Boolean> = _isOperationDialog
    fun updateOperationDialog(value: Boolean) {
        _isOperationDialog.value = value
    }

    val fileInfos = mutableStateListOf<FileInfo>()

    private val _currentIndex: MutableStateFlow<Int> = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex
    fun updateCurrentIndex(value: Int) {
        _currentIndex.value = value
    }

    var isStop = false

    val logs = mutableStateListOf<String>()

    fun updateFileInfos(value: List<FileInfo>){
        fileInfos.clear()
        fileInfos.addAll(value)
        _currentIndex.value = 0
        isStop = false
        logs.clear()
    }
}