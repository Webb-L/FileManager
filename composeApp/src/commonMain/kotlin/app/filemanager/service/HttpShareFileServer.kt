package app.filemanager.service

import app.filemanager.ui.state.file.FileShareState

interface HttpShareFileServerInterface {
    fun start(port: Int = 12040)
    suspend fun stop()
    fun isRunning(): Boolean  // 添加状态检查方法
}

expect class HttpShareFileServer private constructor(fileShareState: FileShareState) : HttpShareFileServerInterface {
    override fun start(port: Int)
    override suspend fun stop()
    override fun isRunning(): Boolean

    companion object {
        fun getInstance(fileShareState: FileShareState): HttpShareFileServer
    }
}