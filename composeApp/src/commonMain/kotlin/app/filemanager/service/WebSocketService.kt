package app.filemanager.service

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.utils.io.core.*
import kotlin.use

expect class WebSocketService() {
    suspend fun scanService(): List<String>

    fun startService()

    fun stopService()
}