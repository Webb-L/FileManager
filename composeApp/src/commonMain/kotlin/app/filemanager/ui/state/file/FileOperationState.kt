package app.filemanager.ui.state.file

import androidx.compose.runtime.mutableStateListOf
import app.filemanager.data.file.FileInfo
import app.filemanager.data.file.FileSimpleInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class FileOperationType {
    Replace,
    Jump,
    Reserve
}

class FileOperationState {
    // 弹框标题
    var title: String = ""

    private val _isOperationDialog: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isOperationDialog: StateFlow<Boolean> = _isOperationDialog
    fun updateOperationDialog(value: Boolean) {
        _isOperationDialog.value = value
    }

    val fileInfos = mutableListOf<FileSimpleInfo>()

    private val _currentIndex: MutableStateFlow<Int> = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex
    fun updateCurrentIndex() {
        _currentIndex.value += 1
//        if (_currentIndex.value == fileInfos.size) {
//            _isOperationDialog.value = false
//        }
    }

    var isStop = false
    var isCancel = false
    var isContinue = false

    private val _isFinished: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isFinished: StateFlow<Boolean> = _isFinished
    fun updateFinished(value: Boolean) {
        _isFinished.value = value
        if (value) {
            _isOperationDialog.value = false
            _currentIndex.value = 0
            isStop = false
            isCancel = false
            logs.clear()
            fileInfos.clear()
        }
    }

    val logs = mutableStateListOf<String>()

    fun updateFileInfos(value: List<FileSimpleInfo>) {
        _currentIndex.value = 0
        _isFinished.value = false
        isStop = false
        isCancel = false
        logs.clear()
        fileInfos.clear()
        fileInfos.addAll(value)
    }

    fun addLog(path: String) {
        logs.add("失败 - $path")
    }

    // 警告状态
    private val _isWarningOperationDialog: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isWarningOperationDialog: StateFlow<Boolean> = _isWarningOperationDialog
    fun updateWarningOperationDialog(value: Boolean) {
        _isWarningOperationDialog.value = value
    }

    private val _warningFiles: MutableStateFlow<Pair<FileSimpleInfo, FileSimpleInfo>> =
        MutableStateFlow(Pair(FileSimpleInfo.nullFileSimpleInfo(), FileSimpleInfo.nullFileSimpleInfo()))
    val warningFiles: StateFlow<Pair<FileSimpleInfo, FileSimpleInfo>> = _warningFiles
    fun updateWarningFiles(value: Pair<FileSimpleInfo, FileSimpleInfo>) {
        _warningFiles.value = value
        _isWarningOperationDialog.value = true
        _warningOperationType.value = FileOperationType.Reserve
        _warningUseAll.value = false
    }

    private val _warningOperationType: MutableStateFlow<FileOperationType> =
        MutableStateFlow(FileOperationType.Replace)
    val warningOperationType: StateFlow<FileOperationType> = _warningOperationType
    fun updateWarningOperationType(value: FileOperationType) {
        _warningOperationType.value = value
    }


    private val _warningUseAll: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val warningUseAll: StateFlow<Boolean> = _warningUseAll
    fun updateWarningUseAll(value: Boolean) {
        _warningUseAll.value = value
    }

}