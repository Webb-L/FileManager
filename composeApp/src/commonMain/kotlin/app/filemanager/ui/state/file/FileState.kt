package app.filemanager.ui.state.file

import app.filemanager.data.FileInfo
import app.filemanager.utils.FileUtils
import app.filemanager.utils.PathUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FileState {
    var destPath = ""

    // 是否在复制文件
    private val _isPasteCopyFile: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isPasteCopyFile: StateFlow<Boolean> = _isPasteCopyFile
    fun copyFile(path: String) {
        _isPasteCopyFile.value = true
        destPath = path
    }

    suspend fun pasteCopyFile(path: String, fileOperationState: FileOperationState) {
        fileOperationState.title = "复制中..."
        val fileInfos = PathUtils.traverse(destPath)
            .sortedWith(compareByDescending<FileInfo> { it.isDirectory }.thenByDescending { it.path })
        fileOperationState.updateFileInfos(fileInfos)
        for (fileInfo in fileInfos) {
            if (fileOperationState.isCancel) return
            while (fileOperationState.isStop) {
                delay(100)
            }
            val status = FileUtils.copyFile(fileInfo.path, fileInfo.path.replace(destPath, path))
            if (status) {
                fileOperationState.updateCurrentIndex()
            } else {
                fileOperationState.addLog(fileInfo.path)
            }
        }
        fileOperationState.updateFinished(true)
        _isPasteCopyFile.value = false
        destPath = ""
    }

    fun cancelCopyFile() {
        _isPasteCopyFile.value = false
        destPath = ""
    }

    // 是否在移动文件
    private val _isPasteMoveFile: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isPasteMoveFile: StateFlow<Boolean> = _isPasteMoveFile
    fun moveFile(path: String) {
        _isPasteMoveFile.value = true
        destPath = path
    }

    suspend fun pasteMoveFile(path: String, fileOperationState: FileOperationState) {
        fileOperationState.title = "移动中..."
        val fileInfos = PathUtils.traverse(destPath)
            .sortedWith(compareByDescending<FileInfo> { it.isDirectory }.thenByDescending { it.path })
        fileOperationState.updateFileInfos(fileInfos)
        for (fileInfo in fileInfos) {
            if (fileOperationState.isCancel) return
            while (fileOperationState.isStop) {
                delay(100)
            }
            val status = FileUtils.moveFile(fileInfo.path, fileInfo.path.replace(destPath, path))
            if (status) {
                fileOperationState.updateCurrentIndex()
            } else {
                fileOperationState.addLog(fileInfo.path)
            }
        }
        fileOperationState.updateFinished(true)
        _isPasteMoveFile.value = false
        destPath = ""
    }

    fun cancelMoveFile() {
        _isPasteMoveFile.value = false
        destPath = ""
    }

    private val _isRenameFile: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isRenameFile: StateFlow<Boolean> = _isRenameFile
    fun updateRenameFile(status: Boolean) {
        _isRenameFile.value = status
    }

    private val _renameText: MutableStateFlow<String> = MutableStateFlow("")
    val renameText: StateFlow<String> = _renameText
    fun updateRenameText(text: String) {
        _renameText.value = text
    }


    // 删除文件
    suspend fun deleteFile(fileOperationState: FileOperationState, path: String) {
        fileOperationState.title = "删除中..."
        val fileInfos = PathUtils.traverse(path)
            .sortedWith(compareBy<FileInfo> { it.isDirectory }.thenByDescending { it.path })
        fileOperationState.updateFileInfos(fileInfos)
        for (fileInfo in fileInfos) {
            if (fileOperationState.isCancel) return
            while (fileOperationState.isStop) {
                delay(100)
            }
            val status = FileUtils.deleteFile(fileInfo.path)
            if (status) {
                fileOperationState.updateCurrentIndex()
            } else {
                fileOperationState.addLog(fileInfo.path)
            }
        }
        FileUtils.deleteFile(path)
        fileOperationState.updateFinished(true)
    }

    private val _fileInfo: MutableStateFlow<FileInfo?> = MutableStateFlow(null)
    val fileInfo: StateFlow<FileInfo?> = _fileInfo
    fun updateFileInfo(data: FileInfo?) {
        _fileInfo.value = data
    }
}