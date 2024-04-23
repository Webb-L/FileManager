package app.filemanager.ui.state.file

import androidx.compose.runtime.mutableStateListOf
import app.filemanager.data.file.FileProtocol
import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.main.Device
import app.filemanager.data.main.DiskBase
import app.filemanager.data.main.Local
import app.filemanager.data.main.Network
import app.filemanager.extensions.getFileAndFolder
import app.filemanager.extensions.replaceLast
import app.filemanager.utils.FileUtils
import app.filemanager.utils.PathUtils
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FileState() {
    val fileAndFolder = mutableStateListOf<FileSimpleInfo>()

    private val _rootPath: MutableStateFlow<String> = MutableStateFlow(PathUtils.getRootPaths().first())
    val rootPath: StateFlow<String> = _rootPath
    suspend fun updateRootPath(value: String) {
        _rootPath.value = value
        updateFileAndFolder()
    }

    private val _path: MutableStateFlow<String> = MutableStateFlow(PathUtils.getHomePath())
    val path: StateFlow<String> = _path
    suspend fun updatePath(value: String) {
        _path.value = value
        updateFileAndFolder()
    }

    private val _isCreateFolder: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isCreateFolder: StateFlow<Boolean> = _isCreateFolder
    fun updateCreateFolder(value: Boolean) {
        _isCreateFolder.value = value
    }

    private val _deskType: MutableStateFlow<DiskBase> = MutableStateFlow(Local())
    val deskType: StateFlow<DiskBase> = _deskType
    suspend fun updateDesk(protocol: FileProtocol, type: DiskBase) {
        _deskType.value = type
        when (protocol) {
            FileProtocol.Local -> {

            }

            FileProtocol.Device -> {
                val device = type as Device
            }

            FileProtocol.Network -> {
                val network = type as Network
            }
        }
        _rootPath.value = getRootPaths().first().path

        updateFileAndFolder()
    }

    suspend fun getFileAndFolder(path: String): List<FileSimpleInfo> {
        if (_deskType.value is Local) {
            return path.getFileAndFolder()
        }

        var isReturn = false

        if (_deskType.value is Device) {
            val device = _deskType.value as Device
            val temp = mutableListOf<FileSimpleInfo>()
            device.getFileList(path) {
                temp.addAll(it)
                isReturn = true
            }

            while (!isReturn) {
                delay(100L)
                return temp
            }
            return temp
        }

        return listOf()
    }

    suspend fun updateFileAndFolder() {
        fileAndFolder.clear()
        fileAndFolder.addAll(getFileAndFolder(_path.value))
    }


    suspend fun getRootPaths(): List<FileSimpleInfo> {
        if (_deskType.value is Local) {
            return PathUtils.getRootPaths().map {
                FileSimpleInfo.pathFileSimpleInfo(it)
            }
        }

        var isReturn = false

        if (_deskType.value is Device) {
            val device = _deskType.value as Device
            val temp = mutableListOf<FileSimpleInfo>()
            device.getRootPaths {
                temp.addAll(it.map { FileSimpleInfo.pathFileSimpleInfo(it) })
                isReturn = true
            }
            while (!isReturn) {
                delay(100L)
                return temp
            }
            return temp
        }

        return listOf()
    }

    suspend fun rename(path: String, oldName: String, newName: String) {
        if (_deskType.value is Local) {
            FileUtils.rename(path, oldName, newName)
        }

        var isReturn = false

        if (_deskType.value is Device) {
            val device = _deskType.value as Device
            device.rename(path, oldName, newName)
        }

        return
    }

    init {
        MainScope().launch {
            updateFileAndFolder()
        }
    }


    var srcPath = ""

    // 是否在复制文件
    private val _isPasteCopyFile: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isPasteCopyFile: StateFlow<Boolean> = _isPasteCopyFile
    fun copyFile(path: String) {
        _isPasteCopyFile.value = true
        srcPath = path
    }

    suspend fun pasteCopyFile(destPath: String, fileOperationState: FileOperationState) {
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
        fileOperationState.updateFileInfos(fileInfos)
        for (fileInfo in fileInfos) {
            var toPath: String = fileInfo.path.replace(srcPath, newDestPath)
            try {
                val newDestFile = FileUtils.getFile(fileInfo.path.replace(srcPath, newDestPath), fileInfo.name)
                if (fileInfo.name == newDestFile.name) {
                    try {
                        val newName = generateNewName(srcFileInfo, newDestFile, fileOperationState)
                        if (newName.isNotEmpty()) {
                            toPath = newName
                        }
                    } catch (e: Exception) {
                    }
                }
            } catch (e: Exception) {
                toPath = fileInfo.path.replace(srcPath, newDestPath)
            }

            if (fileOperationState.isContinue) {
                fileOperationState.isContinue = false
                continue
            }
            if (fileOperationState.isCancel) return
            while (fileOperationState.isStop) {
                delay(100)
            }

            val status = FileUtils.copyFile(fileInfo.path, toPath)
            if (status) {
                fileOperationState.updateCurrentIndex()
            } else {
                fileOperationState.addLog(fileInfo.path)
            }
        }
        fileOperationState.updateFinished(true)
        updateFileAndFolder()
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
        val srcFileInfo = FileUtils.getFile(srcPath)
        val destFileInfo = FileUtils.getFile(destPath)

        var newDestPath = destPath
        if (srcFileInfo.isDirectory) {
            newDestPath = destPath + PathUtils.getPathSeparator() + srcFileInfo.name
        }

        println("${srcFileInfo.path} - ${destFileInfo.path}")

        val newFileName = generateNewName(srcFileInfo, destFileInfo, fileOperationState)
        if (newFileName.isNotEmpty()) {
            newDestPath = newFileName
        }

        fileOperationState.title = "移动中..."
        val fileInfos = PathUtils.traverse(srcPath)
        fileOperationState.updateFileInfos(fileInfos)
        for (fileInfo in fileInfos) {
            var toPath: String = fileInfo.path.replace(srcPath, newDestPath)
            try {
                val newDestFile = FileUtils.getFile(newDestPath, fileInfo.name)
                if (fileInfo.name == newDestFile.name) {
                    try {
                        val newName = generateNewName(srcFileInfo, newDestFile, fileOperationState)
                        println(newName)
                        if (newName.isNotEmpty()) {
                            toPath = newName
                        }
                    } catch (e: Exception) {
                    }
                }
            } catch (e: Exception) {
                toPath = fileInfo.path.replace(srcPath, newDestPath)
            }

            if (fileOperationState.isContinue) {
                fileOperationState.isContinue = false
                continue
            }
            if (fileOperationState.isCancel) return
            while (fileOperationState.isStop) {
                delay(100)
            }

            val status = FileUtils.moveFile(fileInfo.path, toPath)
            if (status) {
                fileOperationState.updateCurrentIndex()
            } else {
                fileOperationState.addLog(fileInfo.path)
            }
        }
        FileUtils.deleteFile(srcPath)
        fileOperationState.updateFinished(true)
        updateFileAndFolder()
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
            .sortedWith(compareBy<FileSimpleInfo> { it.isDirectory }.thenByDescending { it.path })
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

    private val _fileInfo: MutableStateFlow<FileSimpleInfo?> = MutableStateFlow(null)
    val fileInfo: StateFlow<FileSimpleInfo?> = _fileInfo
    fun updateFileInfo(data: FileSimpleInfo?) {
        _fileInfo.value = data
    }

    private suspend fun generateNewName(
        srcFileInfo: FileSimpleInfo,
        destFileInfo: FileSimpleInfo,
        fileOperationState: FileOperationState,
    ): String {
        var newDestFileInfo = destFileInfo
        if (srcFileInfo.name == destFileInfo.name) {
            // 文件复制到文件
            // 显示操作弹框 阻止下面代码执行
            fileOperationState.updateWarningFiles(Pair(srcFileInfo, destFileInfo))
            while (fileOperationState.isWarningOperationDialog.value) {
                delay(300)
            }
        } else if (srcFileInfo.isDirectory && destFileInfo.isDirectory) {
            // 文件夹复制到文件夹
            try {
                newDestFileInfo = FileUtils.getFile(destFileInfo.path, srcFileInfo.name)
                // 显示操作弹框 阻止下面代码执行
                fileOperationState.updateWarningFiles(Pair(srcFileInfo, newDestFileInfo))
                while (fileOperationState.isWarningOperationDialog.value) {
                    delay(300)
                }
            } catch (e: Exception) {
                return ""
            }
        } else {
            return ""
        }

        // 保留文件
        if (fileOperationState.warningOperationType.value == FileOperationType.Reserve) {
            val path = newDestFileInfo.path.replaceLast(newDestFileInfo.name, "")
            val fileName = if (newDestFileInfo.isDirectory)
                newDestFileInfo.name
            else
                newDestFileInfo.name.replaceLast(newDestFileInfo.mineType, "")

            val fileNameRegex = "${Regex.escape(fileName)}(\\(\\d*\\))?".toRegex()
            // 获取相同文件名
            val files = getFileAndFolder(path)
                .asSequence()
                .filter {
                    if (it.isDirectory) {
                        true
                    } else {
                        !it.isDirectory
                    }
                }
                .map {
                    if (it.isDirectory) {
                        it.name
                    } else {
                        it.name.replace(it.mineType, "")
                    }
                }
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
            return "$path$fileName(${size + 1})${newDestFileInfo.mineType}"
        }
        return ""
    }
}