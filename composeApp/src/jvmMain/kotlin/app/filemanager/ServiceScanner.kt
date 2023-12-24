package app.filemanager

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

class ServiceScanner {
    suspend fun scanPort(ip: String, port: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(ip, port), 1000)
                socket.close()
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}

//suspend fun main() {
//    val scanner = ServiceScanner()
//    val privateIPs = generatePrivateIPs()
//    val port = 5173
//    println(privateIPs.size)
//    privateIPs.forEach { ip ->
//        MainScope().launch {
//            val isOpen = scanner.scanPort(ip, port)
//            if (isOpen) {
//                println("Port $port is open on $ip")
//            }
//        }
//    }
//}

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