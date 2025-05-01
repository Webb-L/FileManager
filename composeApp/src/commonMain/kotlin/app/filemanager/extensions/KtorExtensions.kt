package app.filemanager.extensions

import app.filemanager.data.main.Device
import app.filemanager.data.main.DeviceType
import io.ktor.server.application.*
import io.ktor.server.plugins.origin

/**
 * 从ApplicationCall中获取客户端设备信息
 */
fun ApplicationCall.getClientDeviceInfo(): Device? {
    // 获取客户端 IP
    val xForwardedFor = request.headers["X-Forwarded-For"]
    val remoteHost = request.origin.remoteHost
    val clientIp = xForwardedFor?.split(",")?.firstOrNull()?.trim() ?: remoteHost

    // 获取User-Agent
    val userAgent = request.headers["User-Agent"] ?: return null

    // 解析浏览器类型
    val browser = parseBrowser(userAgent)

    // 解析操作系统
    val os = parseOperatingSystem(userAgent)

    // 解析设备类型
    val deviceInfo = parseDevice(userAgent)

    // 确定设备类型
    val deviceType = when {
        userAgent.contains("Android", ignoreCase = true) -> DeviceType.Android
        userAgent.contains("iPhone", ignoreCase = true) || userAgent.contains(
            "iPad",
            ignoreCase = true
        ) -> DeviceType.IOS

        else -> DeviceType.JS
    }

    // 创建 Device 对象
    return Device(
        id = clientIp,
        name = "$browser-$os",
        host = mutableMapOf(),
        type = deviceType,
        token = ""
    )
}

/**
 * 解析浏览器信息
 */
private fun parseBrowser(userAgent: String): String {
    return when {
        // 检测主流浏览器
        userAgent.contains("Firefox/", ignoreCase = true) -> {
            val regex = "Firefox/([\\d.]+)".toRegex()
            val match = regex.find(userAgent)
            "Firefox ${match?.groupValues?.getOrNull(1) ?: ""}"
        }

        userAgent.contains("OPR/", ignoreCase = true) || userAgent.contains("Opera/", ignoreCase = true) -> {
            val regex = "OPR/([\\d.]+)|Opera/([\\d.]+)".toRegex()
            val match = regex.find(userAgent)
            "Opera ${match?.groupValues?.firstOrNull { it.isNotEmpty() && it.contains(".") } ?: ""}"
        }

        userAgent.contains("Edg/", ignoreCase = true) -> {
            val regex = "Edg/([\\d.]+)".toRegex()
            val match = regex.find(userAgent)
            "Edge ${match?.groupValues?.getOrNull(1) ?: ""}"
        }

        userAgent.contains("Chrome/", ignoreCase = true) -> {
            val regex = "Chrome/([\\d.]+)".toRegex()
            val match = regex.find(userAgent)
            "Chrome ${match?.groupValues?.getOrNull(1) ?: ""}"
        }

        userAgent.contains("Safari/", ignoreCase = true) && !userAgent.contains("Chrome", ignoreCase = true) -> {
            val regex = "Version/([\\d.]+)".toRegex()
            val match = regex.find(userAgent)
            "Safari ${match?.groupValues?.getOrNull(1) ?: ""}"
        }

        userAgent.contains("MSIE", ignoreCase = true) || userAgent.contains("Trident/", ignoreCase = true) -> {
            if (userAgent.contains("MSIE", ignoreCase = true)) {
                val regex = "MSIE ([\\d.]+)".toRegex()
                val match = regex.find(userAgent)
                "Internet Explorer ${match?.groupValues?.getOrNull(1) ?: ""}"
            } else {
                "Internet Explorer 11.0"
            }
        }

        else -> "Unknown Browser"
    }
}

/**
 * 解析操作系统信息
 */
private fun parseOperatingSystem(userAgent: String): String {
    return when {
        // 移动设备操作系统
        userAgent.contains("Android", ignoreCase = true) -> {
            val regex = "Android ([\\d.]+)".toRegex()
            val match = regex.find(userAgent)
            "Android ${match?.groupValues?.getOrNull(1) ?: ""}"
        }

        userAgent.contains("iPhone", ignoreCase = true) -> {
            val regex = "iPhone OS ([\\d_]+)".toRegex()
            val match = regex.find(userAgent)
            "iOS ${match?.groupValues?.getOrNull(1)?.replace("_", ".") ?: ""}"
        }

        userAgent.contains("iPad", ignoreCase = true) -> {
            val regex = "CPU OS ([\\d_]+)".toRegex()
            val match = regex.find(userAgent)
            "iPadOS ${match?.groupValues?.getOrNull(1)?.replace("_", ".") ?: ""}"
        }

        // 桌面操作系统
        userAgent.contains("Windows NT", ignoreCase = true) -> {
            val regex = "Windows NT ([\\d.]+)".toRegex()
            val match = regex.find(userAgent)
            val version = match?.groupValues?.getOrNull(1)
            when (version) {
                "10.0" -> "Windows 10"
                "6.3" -> "Windows 8.1"
                "6.2" -> "Windows 8"
                "6.1" -> "Windows 7"
                "6.0" -> "Windows Vista"
                "5.2" -> "Windows XP x64"
                "5.1" -> "Windows XP"
                else -> "Windows ${version ?: ""}"
            }
        }

        userAgent.contains("Mac OS X", ignoreCase = true) -> {
            val regex = "Mac OS X ([\\d_.]+)".toRegex()
            val match = regex.find(userAgent)
            "macOS ${match?.groupValues?.getOrNull(1)?.replace("_", ".") ?: ""}"
        }

        userAgent.contains("Linux", ignoreCase = true) -> {
            if (userAgent.contains("Ubuntu", ignoreCase = true)) {
                "Ubuntu Linux"
            } else {
                "Linux"
            }
        }

        else -> "Unknown OS"
    }
}

/**
 * 解析设备信息
 */
private fun parseDevice(userAgent: String): String {
    return when {
        // 移动设备
        userAgent.contains("iPhone", ignoreCase = true) -> "iPhone"
        userAgent.contains("iPad", ignoreCase = true) -> "iPad"
        userAgent.contains("Android", ignoreCase = true) -> {
            if (userAgent.contains("Mobile", ignoreCase = true)) {
                "Android Mobile"
            } else {
                "Android Tablet"
            }
        }

        // 桌面设备
        userAgent.contains("Windows", ignoreCase = true) -> "PC"
        userAgent.contains("Macintosh", ignoreCase = true) -> "Mac"
        userAgent.contains("Linux", ignoreCase = true) -> "Linux PC"

        else -> "Unknown Device"
    }
}