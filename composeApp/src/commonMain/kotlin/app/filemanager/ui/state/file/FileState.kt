package app.filemanager.ui.state.file

import androidx.compose.runtime.mutableStateListOf
import app.filemanager.data.StatusEnum
import app.filemanager.data.file.FileProtocol
import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.file.FileSizeInfo
import app.filemanager.data.file.PathInfo
import app.filemanager.data.main.Device
import app.filemanager.data.main.DiskBase
import app.filemanager.data.main.DrawerBookmarkType
import app.filemanager.data.main.Local
import app.filemanager.exception.EmptyDataException
import app.filemanager.extensions.getFileAndFolder
import app.filemanager.extensions.pathLevel
import app.filemanager.extensions.replaceLast
import app.filemanager.ui.state.main.DrawerState
import app.filemanager.ui.state.main.Task
import app.filemanager.ui.state.main.TaskState
import app.filemanager.utils.FileUtils
import app.filemanager.utils.PathUtils
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FileState : KoinComponent {
    val taskState: TaskState by inject()
    val fileOperationState: FileOperationState by inject()
    val drawerState: DrawerState by inject()


    val fileAndFolder = mutableStateListOf<FileSimpleInfo>()

    private val _rootPath: MutableStateFlow<PathInfo> =
        MutableStateFlow(PathUtils.getRootPaths().getOrDefault(listOf()).first())
    val rootPath: StateFlow<PathInfo> = _rootPath
    suspend fun updateRootPath(value: PathInfo) {
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
    fun updateDesk(protocol: FileProtocol, type: DiskBase) {
        if (_deskType.value == type) return
        MainScope().launch {
            _deskType.value = type

            drawerState.getBookmarks(type)
            val rootPaths = getRootPaths()
            if (rootPaths.isNotEmpty()) {
                _rootPath.value = rootPaths.first()
            }

            val homeBookmark = drawerState.bookmarks.firstOrNull { it.iconType == DrawerBookmarkType.Home }
            if (homeBookmark != null) {
                updatePath(homeBookmark.path)
            }

            updateFileAndFolder()
        }
    }

    // 加载状态
    private val _isLoading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // 错误信息
    private val _exception: MutableStateFlow<Throwable?> = MutableStateFlow(null)
    val exception: StateFlow<Throwable?> = _exception

    suspend fun getFileAndFolder(path: String): Result<List<FileSimpleInfo>> {
        if (_deskType.value is Local) {
            return path.getFileAndFolder()
        }

        var isReturn = false
        if (_deskType.value is Device) {
            val device = _deskType.value as Device
            var result: Result<List<FileSimpleInfo>> = Result.success(emptyList())
            device.getFileList(path) {
                result = it
                isReturn = true
            }

            while (!isReturn) {
                delay(100L)
            }
            return result
        }

        return Result.failure(EmptyDataException())
    }

    suspend fun updateFileAndFolder() {
        _isLoading.value = true
        _exception.value = null
        fileAndFolder.clear()
        val fileAndFolderResult = getFileAndFolder(_path.value)
        if (fileAndFolderResult.isSuccess) {
            fileAndFolder.addAll(fileAndFolderResult.getOrNull() ?: emptyList())
        }

        if (fileAndFolderResult.isFailure) {
            _exception.value = fileAndFolderResult.exceptionOrNull()
        }

        _isLoading.value = false
    }

    suspend fun getRootPaths(): List<PathInfo> {
        if (_deskType.value is Local) {
            return PathUtils.getRootPaths().getOrDefault(listOf())
        }

        var isReturn = false

        if (_deskType.value is Device) {
            val device = _deskType.value as Device
            val temp = mutableListOf<PathInfo>()
            device.getRootPaths {
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

    suspend fun rename(path: String, oldName: String, newName: String): Result<Boolean> {
        if (_deskType.value is Local) {
            FileUtils.rename(path, oldName, newName)
        }

        var isReturn = false
        if (_deskType.value is Device) {
            val device = _deskType.value as Device
            var result: Result<Boolean> = Result.success(false)
            device.rename(path, oldName, newName) {
                result = it
                isReturn = true
            }

            while (!isReturn) {
                delay(100L)
            }

            updateFileAndFolder()
            return result
        }

        return Result.failure(Exception("修改失败"))
    }

    suspend fun createFolder(path: String, name: String): Result<Boolean> {
        if (_deskType.value is Local) {
            return FileUtils.createFolder("$path${PathUtils.getPathSeparator()}$name")
        }

        var isReturn = false
        if (_deskType.value is Device) {
            val device = _deskType.value as Device
            var result: Result<Boolean> = Result.success(false)
            device.createFolder(path, name) {
                result = it
                isReturn = true
            }

            while (!isReturn) {
                delay(100L)
            }
            updateFileAndFolder()
            return result
        }

        return Result.failure(Exception("创建失败"))
    }

    suspend fun getSizeInfo(fileSimpleInfo: FileSimpleInfo, pathInfo: PathInfo): Result<FileSizeInfo> {
        if (_deskType.value is Local) {
            return Result.success(fileSimpleInfo.getSizeInfo(pathInfo.totalSpace, pathInfo.freeSpace))
        }

        var isReturn = false
        if (_deskType.value is Device) {
            val device = _deskType.value as Device
            var result: Result<FileSizeInfo> = Result.success(
                FileSizeInfo(
                    fileSize = 0,
                    fileCount = 0,
                    folderCount = 0,
                    totalSpace = 0,
                    freeSpace = 0
                )
            )
            device.getFileSizeInfo(fileSimpleInfo, pathInfo.totalSpace, pathInfo.freeSpace) {
                result = it
                isReturn = true
            }

            while (!isReturn) {
                delay(100L)
            }
            return result
        }

        return Result.failure(Exception("获取失败"))
    }

    suspend fun deleteFile(task: Task, path: String): Result<Boolean> {
        val fileInfos = mutableListOf<FileSimpleInfo>()
        var successCount = 0
        var failureCount = 0
        val mainScope = MainScope()

        taskState.tasks.add(task)
        if (_deskType.value is Local) {
            PathUtils.traverse(path) {
                if (it.isSuccess && (it.getOrNull() ?: emptyList()).isNotEmpty()) {
                    fileInfos.addAll(it.getOrNull() ?: emptyList())
                }
            }
            getFile(path).getOrNull()?.let { fileInfos.add(it) }
            task.values["progressMax"] = fileInfos.size.toString()

            if (fileInfos.size == 1) {
                val deleteFile = FileUtils.deleteFile(fileInfos.first().path)
                if (deleteFile.isSuccess && deleteFile.getOrNull() == true) {
                    successCount++
                    task.values["progressCur"] = successCount.toString()
                    taskState.tasks.remove(task)
                } else {
                    failureCount++
                    task.values["progressCur"] = failureCount.toString()
                    task.status = StatusEnum.FAILURE
                    task.result[fileInfos.first().path] = deleteFile.exceptionOrNull()?.message ?: ""
                }
                return deleteFile
            }

            val groupBy = fileInfos
                .sortedWith(compareBy<FileSimpleInfo> { it.isDirectory }
                    .thenByDescending { it.path.pathLevel() })
                .groupBy { it.isDirectory }

            groupBy.forEach { (isDir, fileList) ->
                for (filesChunk in fileList.chunked(30)) {
                    mainScope.launch {
                        for (file in filesChunk) {
                            val deleteFile = FileUtils.deleteFile(file.path)
                            if (deleteFile.isSuccess && deleteFile.getOrNull() == true) {
                                successCount++
                            } else {
                                failureCount++
                                task.result[file.path] = deleteFile.exceptionOrNull()?.message ?: ""
                            }
                            task.values["progressCur"] = (successCount + failureCount).toString()
                        }
                    }
                }

                if (!isDir) {
                    while (successCount + failureCount < fileList.size) {
                        delay(100L)
                    }
                }
            }

            while (successCount + failureCount < fileInfos.size) {
                delay(100L)
            }

            task.status = StatusEnum.FAILURE
            val isFinish = fileInfos.size == successCount && failureCount == 0
            if (isFinish) {
                taskState.tasks.remove(task)
            }
            return Result.success(isFinish)
        }

        var isReturn = false
        if (_deskType.value is Device) {
            val device = _deskType.value as Device
            task.protocol = FileProtocol.Device
            task.protocolId = device.id
            device.getTraversePath(path) {
                if (it.isSuccess) {
                    fileInfos.addAll(it.getOrNull() ?: emptyList())
                }
            }
            task.values["progressMax"] = fileInfos.size.toString()

            var result: Result<Boolean> = Result.success(false)
            if (fileInfos.size == 1) {
                device.deleteFile(listOf(fileInfos.first().path)) {
                    if (it.isSuccess) {
                        result = Result.success((it.getOrNull() ?: listOf()).first())
                    }
                    isReturn = true
                }

                while (!isReturn) {
                    delay(100L)
                }
                return result
            }

            for (fileInfo in fileInfos
                .sortedWith(compareBy<FileSimpleInfo> { it.isDirectory }
                    .thenByDescending { it.path.pathLevel() }).chunked(30)) {
                isReturn = false
                device.deleteFile(fileInfo.map { it.path }) {
                    if (it.isSuccess) {
                        for (item in it.getOrNull() ?: listOf()) {
                            if (item) {
                                successCount++
                            } else {
                                failureCount++
                            }
                        }
                    } else {
                        failureCount++
                    }
                    task.values["progressCur"] = (successCount + failureCount).toString()
                    isReturn = true
                }

                while (!isReturn) {
                    delay(100L)
                }
            }
            return Result.success(fileInfos.size + 1 == successCount && failureCount == 0)
        }

        return Result.failure(Exception("删除失败"))
    }

    init {
        MainScope().launch {
            updateFileAndFolder()
        }
    }


    val checkedPath = mutableStateListOf<String>()

    var srcPath = ""

    // 是否在复制文件
    private val _isPasteCopyFile: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isPasteCopyFile: StateFlow<Boolean> = _isPasteCopyFile
    fun copyFile(path: String) {
        _isPasteCopyFile.value = true
        if (path.isEmpty()) return
        srcPath = path
        checkedPath.add(path)
    }

    suspend fun copyFile(srcFileSimpleInfo: FileSimpleInfo, destPath: String): Result<Boolean> {
        // TODO 创建任务
        if (_deskType.value is Local) {
            return srcFileSimpleInfo.copyToFile(destPath)
        }

        var isReturn = false
        if (_deskType.value is Device) {
            val device = _deskType.value as Device
            var result: Result<Boolean> = Result.success(false)
            // println(fileState.copyFile("/home/webb/CLionProjects/Embedded C++","/home/webb/下载/Embedded C++"))
            // TODO srcFileSimpleInfo 路径
            device.copyFile(srcFileSimpleInfo.path, destPath) {
                result = it
                isReturn = true
            }

            while (!isReturn) {
                delay(100L)
            }

            updateFileAndFolder()
            return result
        }

        return Result.failure(Exception("复制失败"))
    }

    suspend fun pasteCopyFile(destPath: String, fileOperationState: FileOperationState) {
        val destFileInfo = getFile(destPath).getOrNull() ?: return

        val fileAndFolders = getFileAndFolder(destFileInfo.path).getOrDefault(listOf())

        fileOperationState.updateWarningOperationDialog(
            checkIfNameExists(
                checkedPath.map { getFile(it).getOrThrow() },
                destFileInfo,
                fileAndFolders,
                fileOperationState
            )
        )
        while (fileOperationState.isWarningOperationDialog.value) {
            delay(300)
        }
        if (fileOperationState.files.isEmpty()) return

        val fileOperationPaths = mutableListOf<Pair<FileSimpleInfo, String>>()

        for (file in fileOperationState.files) {
            if (file.isConflict) {
                // 跳过
                if (file.type == FileOperationType.Jump) continue

                // 保留
                if (file.type == FileOperationType.Reserve) {
                    val fileName = if (file.src.isDirectory)
                        file.src.name
                    else
                        file.src.name.replaceLast(file.src.mineType, "")

                    val fileNameRegex = "${Regex.escape(fileName)}(\\(\\d*\\))?".toRegex()

                    // 获取相同文件名
                    val files = fileAndFolders
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

                    fileOperationPaths.add(
                        Pair(
                            file.src,
                            "${destFileInfo.path}${PathUtils.getPathSeparator()}$fileName(${size + 1})${destFileInfo.mineType}"
                        )
                    )
                    continue
                }
            }

            fileOperationPaths.add(
                Pair(
                    file.src,
                    "${file.dest.path}${PathUtils.getPathSeparator()}${file.src.name}"
                )
            )
        }

        val mainScope = MainScope()
        var executeCount = 0
        for (fileOperationPath in fileOperationPaths) {
            mainScope.launch {
                copyFile(fileOperationPath.first, fileOperationPath.second)
                executeCount++
            }
        }

        while (executeCount < fileOperationPaths.size) {
            delay(100)
        }
        updateFileAndFolder()
        cancelCopyFile()
    }

    fun cancelCopyFile() {
        _isPasteCopyFile.value = false
        fileOperationState.files.clear()
        srcPath = ""
    }

    // 是否在移动文件
    private val _isPasteMoveFile: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isPasteMoveFile: StateFlow<Boolean> = _isPasteMoveFile
    fun moveFile(path: String) {
        _isPasteMoveFile.value = true
        if (path.isEmpty()) return
        srcPath = path
        checkedPath.add(path)
    }

    fun pasteMoveFile(destPath: String, fileOperationState: FileOperationState) {
//        val srcFileInfo = FileUtils.getFile(srcPath)
//        val destFileInfo = FileUtils.getFile(destPath)
//
//        var newDestPath = destPath
//        if (srcFileInfo.isDirectory) {
//            newDestPath = destPath + PathUtils.getPathSeparator() + srcFileInfo.name
//        }
//
//        println("${srcFileInfo.path} - ${destFileInfo.path}")
//
//        val newFileName = generateNewName(srcFileInfo, destFileInfo, fileOperationState)
//        if (newFileName.isNotEmpty()) {
//            newDestPath = newFileName
//        }
//
//        fileOperationState.title = "移动中..."
//        val fileInfos = mutableListOf<FileSimpleInfo>()
//        PathUtils.traverse(srcPath) {
//            if (it.isSuccess && (it.getOrNull() ?: emptyList()).isNotEmpty()) {
//                fileInfos.addAll(it.getOrNull() ?: emptyList())
//            }
//        }
//        fileInfos.add(FileUtils.getFile(srcPath))
//        fileOperationState.updateFileInfos(fileInfos)
//        for (fileInfo in fileInfos) {
//            var toPath: String = fileInfo.path.replace(srcPath, newDestPath)
//            try {
//                val newDestFile = FileUtils.getFile(newDestPath, fileInfo.name)
//                if (fileInfo.name == newDestFile.name) {
//                    try {
//                        val newName = generateNewName(srcFileInfo, newDestFile, fileOperationState)
//                        println(newName)
//                        if (newName.isNotEmpty()) {
//                            toPath = newName
//                        }
//                    } catch (e: Exception) {
//                    }
//                }
//            } catch (e: Exception) {
//                toPath = fileInfo.path.replace(srcPath, newDestPath)
//            }
//
//            if (fileOperationState.isContinue) {
//                fileOperationState.isContinue = false
//                continue
//            }
//            if (fileOperationState.isCancel) return
//            while (fileOperationState.isStop) {
//                delay(100)
//            }
//
//            val status = FileUtils.moveFile(fileInfo.path, toPath)
//            if (status) {
//                fileOperationState.updateCurrentIndex()
//            } else {
//                fileOperationState.addLog(fileInfo.path)
//            }
//        }
//        FileUtils.deleteFile(srcPath)
//        fileOperationState.updateFinished(true)
//        updateFileAndFolder()
    }

    fun cancelMoveFile() {
        _isPasteMoveFile.value = false
        fileOperationState.files.clear()
        srcPath = ""
    }

    private val _isRenameFile: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isRenameFile: StateFlow<Boolean> = _isRenameFile
    fun updateRenameFile(status: Boolean) {
        _isRenameFile.value = status
    }

    private val _isViewFile: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isViewFile: StateFlow<Boolean> = _isViewFile
    fun updateViewFile(status: Boolean) {
        _isViewFile.value = status
    }

    private val _fileInfo: MutableStateFlow<FileSimpleInfo?> = MutableStateFlow(null)
    val fileInfo: StateFlow<FileSimpleInfo?> = _fileInfo
    fun updateFileInfo(data: FileSimpleInfo?) {
        _fileInfo.value = data
    }

    private fun checkIfNameExists(
        srcFileInfos: List<FileSimpleInfo>,
        destFileInfo: FileSimpleInfo,
        fileAndFolders: List<FileSimpleInfo>,
        fileOperationState: FileOperationState,
    ): Boolean {
        val fileOperations = mutableListOf<FileOperation>()

        for (srcFileInfo in srcFileInfos) {
            // 文件夹 to 文件夹
            val isConflictFolder = srcFileInfo.isDirectory && destFileInfo.isDirectory && (
                    srcFileInfo.name == destFileInfo.name ||
                            fileAndFolders.any { it.name == srcFileInfo.name }
                    )
            // 文件 to 文件夹
            val isConflictFileFolder = !srcFileInfo.isDirectory && destFileInfo.isDirectory && (
                    srcFileInfo.name == destFileInfo.name ||
                            fileAndFolders.any { it.name == srcFileInfo.name }
                    )
            // 文件 to 文件
            val isConflictFile =
                !srcFileInfo.isDirectory && !destFileInfo.isDirectory && srcFileInfo.name == destFileInfo.name

            // 是否冲突
            val isConflict = isConflictFolder || isConflictFileFolder || isConflictFile

            fileOperations.add(
                FileOperation(
                    isConflict = isConflict,
                    src = srcFileInfo,
                    dest = if (isConflictFolder)
                        destFileInfo.withCopy(name = srcFileInfo.name)
                    else if (isConflictFileFolder)
                        fileAndFolders.first { it.name == srcFileInfo.name }
                    else
                        destFileInfo,
                )
            )
        }

        fileOperationState.files.apply {
            clear()
            addAll(fileOperations)
        }
        return fileOperations.any { it.isConflict }
    }

    suspend fun writeBytes(): Result<Boolean> {
        var isReturn = false
        if (_deskType.value is Device) {
            val device = _deskType.value as Device
            var result: Result<Boolean> = Result.success(false)
            device.writeBytes("/home/webb/OSX-KVM2/random_file", "/storage/emulated/0/random_file") {
                result = it
                isReturn = true
            }

            while (!isReturn) {
                delay(100L)
            }
            return result
        }

        return Result.failure(Exception("删除失败"))
    }

    suspend fun getFile(path: String): Result<FileSimpleInfo> {
        if (_deskType.value is Local) {
            return FileUtils.getFile(path)
        }

        var isReturn = false
        if (_deskType.value is Device) {
            val device = _deskType.value as Device

            var result: Result<FileSimpleInfo> = Result.failure(Exception("获取失败"))
            device.getFile(path) {
                result = it
                isReturn = true
            }

            while (!isReturn) {
                delay(100L)
            }
            return result
        }

        return Result.failure(Exception("获取失败"))
    }
}