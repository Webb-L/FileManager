package app.filemanager.service.response

import app.filemanager.service.WebSocketConnectService
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class DeviceResponse(private val webSocketConnectService: WebSocketConnectService) {
    fun deviceList(content: String) {
        MainScope().launch {
            for (message in content.split("\n")) {
//                webSocketConnectService.deviceState.addDevices(
//                    message,
//                    webSocketConnectService,
//                )
            }
        }
    }
}