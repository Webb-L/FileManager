package app.filemanager.service

expect class WebSocketService() {
    suspend fun scanService(): List<String>

    fun startService()

    fun stopService()
}