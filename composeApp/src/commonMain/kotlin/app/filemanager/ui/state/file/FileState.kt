package app.filemanager.ui.state.file

import app.filemanager.data.FileInfo
import app.filemanager.extensions.getFileAndFolder
import app.filemanager.utils.FileUtils
import app.filemanager.utils.PathUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FileState {
    var srcPath = ""

    // 是否在复制文件
    private val _isPasteCopyFile: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isPasteCopyFile: StateFlow<Boolean> = _isPasteCopyFile
    fun copyFile(path: String) {
        _isPasteCopyFile.value = true
        srcPath = path
    }

    suspend fun pasteCopyFile(destPath: String, fileOperationState: FileOperationState) {
        println(destPath)
        val srcFileInfo = FileUtils.getFile(srcPath)
        val destFileInfo = FileUtils.getFile(destPath)

        var newDestPath = destPath
        if (srcFileInfo.isDirectory) {
            newDestPath = destPath + PathUtils.getPathSeparator() + srcFileInfo.name
        }
        val newFileName = generateNewName(srcFileInfo, destFileInfo, fileOperationState)
        if (newFileName.isNotEmpty()) {
            newDestPath = newFileName
        }

        fileOperationState.title = "复制中..."
        val fileInfos = PathUtils.traverse(srcPath)
            .sortedWith(compareByDescending<FileInfo> { it.isDirectory }.thenByDescending { it.path })
        fileOperationState.updateFileInfos(fileInfos)
        for (fileInfo in fileInfos) {
            var toPath = fileInfo.path.replace(srcPath, newDestPath)
            if (!fileInfo.isDirectory && destFileInfo.isDirectory) {
                try {
                    val newDestFile = FileUtils.getFile(fileInfo.path.replace(srcPath, newDestPath), fileInfo.name)
                    val newName = generateNewName(srcFileInfo, newDestFile, fileOperationState)
                    println("newName = $newName")
                    if (newName.isNotEmpty()) {
                        toPath = newName
                    }
                } catch (e: Exception) {
                }
            }
            println("toPath = $toPath")

            if (fileOperationState.isContinue) {
                fileOperationState.isContinue = false
                continue
            }
            if (fileOperationState.isCancel) return
            while (fileOperationState.isStop) {
                delay(100)
            }
//            val path = FileUtils.getFile(fileInfo.path.replace(srcPath,newDestPath),fileInfo.name).path
//            println("${fileInfo.path} $path")
            val status = FileUtils.copyFile(fileInfo.path, toPath)
            if (status) {
                fileOperationState.updateCurrentIndex()
            } else {
                fileOperationState.addLog(fileInfo.path)
            }
        }
        fileOperationState.updateFinished(true)
        _isPasteCopyFile.value = false
        srcPath = ""
    }

    private suspend fun generateNewName(
        srcFileInfo: FileInfo,
        destFileInfo: FileInfo,
        fileOperationState: FileOperationState,
    ): String {
        println("${!srcFileInfo.isDirectory} && ${!destFileInfo.isDirectory}")
        // 文件复制到文件 显示操作弹框
        if (!srcFileInfo.isDirectory && !destFileInfo.isDirectory) {
            println(Pair(srcFileInfo.path, destFileInfo.path))
            fileOperationState.updateWarningFiles(Pair(srcFileInfo, destFileInfo))
            // 阻止下面代码执行
            while (fileOperationState.isWarningOperationDialog.value) {
                delay(300)
            }
        } else {
            return ""
        }

        // 保留文件
        if (fileOperationState.warningOperationType.value == FileOperationType.Reserve) {
            val path = destFileInfo.path.replace(destFileInfo.name, "")
            val fileName = destFileInfo.name.replace(destFileInfo.mineType, "")
            val fileNameRegex = "${Regex.escape(fileName)}(\\(\\d*\\))?".toRegex()
            // 获取相同文件名
            val files = path.getFileAndFolder()
                .asSequence()
                .filter { !it.isDirectory }
                .map { it.name.replace(it.mineType, "") }
                .map { fileNameRegex.find(it) }
                .filter { it != null }
                .map { it!!.value }
                .sortedByDescending { it }
                .toSet()

            // 只存在一个文件
            val size: Int
            if (files.isEmpty() || files.size == 1) {
                size = 1
            } else {
                // 存在多个文件
                val oldSize = "\\([^()]*\\)\$".toRegex().find(files.first())!!.value
                    .replace("(", "")
                    .replace(")", "")
                size = oldSize.toInt()
            }

            return "$path$fileName(${size + 1})${destFileInfo.mineType}"
        }
        return ""
    }

    fun cancelCopyFile() {
        _isPasteCopyFile.value = false
        srcPath = ""
    }

    // 是否在移动文件
    private val _isPasteMoveFile: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isPasteMoveFile: StateFlow<Boolean> = _isPasteMoveFile
    fun moveFile(path: String) {
        _isPasteMoveFile.value = true
        srcPath = path
    }

    suspend fun pasteMoveFile(destPath: String, fileOperationState: FileOperationState) {
        fileOperationState.title = "移动中..."
        val fileInfos = PathUtils.traverse(srcPath)
            .sortedWith(compareByDescending<FileInfo> { it.isDirectory }.thenByDescending { it.path })
        fileOperationState.updateFileInfos(fileInfos)
        for (fileInfo in fileInfos) {
            if (fileOperationState.isCancel) return
            while (fileOperationState.isStop) {
                delay(100)
            }
            val status = FileUtils.moveFile(fileInfo.path, fileInfo.path.replace(srcPath, destPath))
            if (status) {
                fileOperationState.updateCurrentIndex()
            } else {
                fileOperationState.addLog(fileInfo.path)
            }
        }
        fileOperationState.updateFinished(true)
        _isPasteMoveFile.value = false
        srcPath = ""
    }

    fun cancelMoveFile() {
        _isPasteMoveFile.value = false
        srcPath = ""
    }

    private val _isRenameFile: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isRenameFile: StateFlow<Boolean> = _isRenameFile
    fun updateRenameFile(status: Boolean) {
        _isRenameFile.value = status
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