package app.filemanager.service

actual class WebSocketService {
    actual suspend fun scanService(): List<String> {
        TODO("Not yet implemented")
    }

    actual fun startService() {
    }

    actual fun stopService() {
    }

    actual fun getNetworkIp(): List<String> {
        return listOf()
    }
}