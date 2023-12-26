package app.filemanager.ui.state.file

import app.filemanager.data.FileInfo
import app.filemanager.utils.FileUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FileState {
    var dstPath = ""

    // 是否在复制文件
    private val _isPasteCopyFile: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isPasteCopyFile: StateFlow<Boolean> = _isPasteCopyFile
    fun copyFile(path: String) {
        _isPasteCopyFile.value = true
        dstPath = path
    }

    fun pasteCopyFile(path: String) {
        FileUtils.copyFile(dstPath, path)
        _isPasteCopyFile.value = false
        dstPath = ""
    }

    // 是否在移动文件
    private val _isPasteMoveFile: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isPasteMoveFile: StateFlow<Boolean> = _isPasteMoveFile
    fun moveFile(path: String) {
        _isPasteMoveFile.value = true
        dstPath = path
    }

    fun pasteMoveFile(path: String) {
        FileUtils.copyFile(dstPath, path)
        _isPasteMoveFile.value = false
        dstPath = ""
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
    fun deleteFile(path: String) {
        FileUtils.deleteFile(path)
    }

    private val _fileInfo: MutableStateFlow<FileInfo?> = MutableStateFlow(null)
    val fileInfo: StateFlow<FileInfo?> = _fileInfo
    fun updateFileInfo(data: FileInfo?) {
        _fileInfo.value = data
    }
}