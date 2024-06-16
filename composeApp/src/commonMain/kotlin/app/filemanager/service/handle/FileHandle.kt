package app.filemanager.service.handle

import app.filemanager.service.WebSocketConnectService

class FileHandle(private val webSocketConnectService: WebSocketConnectService) {
    suspend fun rename(remoteId: String, path: String, oldName: String, newName: String) {
        webSocketConnectService.send(
            command = "/rename",
            header = "$remoteId ",
            params = "$path $oldName $newName",
            value = ""
        )
    }
}