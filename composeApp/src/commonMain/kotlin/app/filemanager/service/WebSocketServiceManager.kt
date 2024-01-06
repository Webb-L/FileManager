package app.filemanager.service

import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent

class WebSocketServiceManager : KoinComponent {
    val services = mutableStateListOf<WebSocketConnectService>()

    suspend fun connect(host: String) {
        val webSocketConnectService = WebSocketConnectService()
        withContext(Dispatchers.Main) {
            launch {
                println(webSocketConnectService.isConnected)
                while (!webSocketConnectService.isConnected) {
                    println(webSocketConnectService.isConnected)
                    services.add(webSocketConnectService)
                    break
                }
            }
            webSocketConnectService.connect(host)
        }
    }
}