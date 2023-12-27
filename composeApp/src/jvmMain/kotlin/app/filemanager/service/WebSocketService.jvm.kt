package app.filemanager.service

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.NetworkInterface
import java.net.URL
import java.util.concurrent.Semaphore

actual class WebSocketService {
    fun getNetworkIp(): List<String> {
        val ipv4Regex =
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)){3})\$".toRegex()
        val interfaces = NetworkInterface.getNetworkInterfaces()
        val ipAddresses = mutableListOf<String>()
        while (interfaces.hasMoreElements()) {
            val networkInterface = interfaces.nextElement()
            if (!networkInterface.isLoopback && !networkInterface.isVirtual) {
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress) {
                        if (address.hostAddress.contains(ipv4Regex)) {
                            ipAddresses.add(address.hostAddress)
                        }
                    }
                }
            }
        }
        return ipAddresses
    }

    @OptIn(DelicateCoroutinesApi::class)
    actual suspend fun scanService(): List<String> {
        val devices = mutableListOf<String>()
        val semaphore = Semaphore(30) // Limit to 10 concurrent coroutines

        val jobs = generatePrivateIPs(getNetworkIp()).map { ip ->
            GlobalScope.async {
                semaphore.acquire()
                val url = URL("http://$ip:8080/ping")
                val validatorValue = "${System.currentTimeMillis()}"
                val connection = url.openConnection() as HttpURLConnection
                try {
                    connection.requestMethod = "POST"
                    connection.doOutput = true
                    connection.connectTimeout = 1000
                    connection.setRequestProperty("Content-Type", "text/plain")

                    val writer = OutputStreamWriter(connection.outputStream)
                    writer.write(validatorValue)
                    writer.flush()

                    val responseBody = String(connection.inputStream.readAllBytes())
                    return@async if (responseBody == validatorValue) {
                        ip
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                } finally {
                    connection.disconnect()
                    semaphore.release()
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

    fun generatePrivateIPs(ips: List<String>): List<String> {
        val ipsResult = mutableListOf<String>()
//        for (i in 10..10) {
//            for (j in 0..255) {
//                for (k in 0..255) {
//                    for (l in 0..255) {
//                        ipsResult.add("$i.$j.$k.$l")
//                    }
//                }
//            }
//        }
//        for (i in 172..172) {
//            for (j in 16..31) {
//                for (k in 0..255) {
//                    for (l in 0..255) {
//                        ipsResult.add("$i.$j.$k.$l")
//                    }
//                }
//            }
//        }
        for (ip in ips) {
            val split = ip.split(".")
            for (i in 0..255) {
                if (split[3].toInt() == i) continue
                ipsResult.add("${split[0]}.${split[1]}.${split[2]}.$i")
            }
        }
//        for (i in 192..192) {
//            for (j in 168..168) {
//                for (k in 0..255) {
//                    for (l in 0..255) {
//                        ipsResult.add("$i.$j.$k.$l")
//                    }
//                }
//            }
//        }
        return ipsResult
    }
}