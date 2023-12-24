package app.filemanager.service

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.net.InetSocketAddress
import java.net.Socket

actual class WebSocketService {
    @OptIn(DelicateCoroutinesApi::class)
    actual suspend fun scanService(): List<String> {

        val devices = mutableListOf<String>()
        val jobs = generatePrivateIPs().subList(0, 255).map { ip ->
            GlobalScope.async {
                try {
                    val socket = Socket()
                    socket.connect(InetSocketAddress(ip, 8080), 1000)
                    socket.close()
                    ip
                } catch (e: Exception) {
                    null
                }
            }
        }

        jobs.forEach { job ->
            job.await()?.let {
                devices.add(it)
            }
        }
        return devices
    }

    actual fun startService() {
    }

    actual fun stopService() {
    }

    fun generatePrivateIPs(): List<String> {
        val ips = mutableListOf<String>()
//    for (i in 10..10) {
//        for (j in 0..255) {
//            for (k in 0..255) {
//                for (l in 0..255) {
//                    ips.add("$i.$j.$k.$l")
//                }
//            }
//        }
//    }
//    for (i in 172..172) {
//        for (j in 16..31) {
//            for (k in 0..255) {
//                for (l in 0..255) {
//                    ips.add("$i.$j.$k.$l")
//                }
//            }
//        }
//    }
        for (i in 192..192) {
            for (j in 168..168) {
                for (k in 0..255) {
                    for (l in 0..255) {
                        ips.add("$i.$j.$k.$l")
                    }
                }
            }
        }
        return ips
    }
}