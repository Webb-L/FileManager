package app.filemanager.service.response

import app.filemanager.service.WebSocketConnectService

class FileResponse(private val webSocketConnectService: WebSocketConnectService) {
    fun replyCreateFolder(headerKey: Long, content: String) {
        webSocketConnectService.replyMessage[headerKey] = content
    }
}