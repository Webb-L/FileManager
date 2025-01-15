package app.filemanager.extensions

import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.utils.PathUtils

fun String.replaceLast(oldValue: String, newValue: String): String {
    val lastIndex = this.lastIndexOf(oldValue)
    if (lastIndex < 0) return this
    return this.substring(0, lastIndex) + newValue + this.substring(lastIndex + oldValue.length)
}

fun String.getFileAndFolder(): Result<List<FileSimpleInfo>> = PathUtils.getFileAndFolder(this)

fun String.isPrivateIPAddress(): Boolean {
    // 检测 IPv4 地址是否为内网地址
    fun isPrivateIPv4Address(ip: String): Boolean {
        val ipParts = ip.split(".")
        if (ipParts.size != 4) {
            return false
        }

        val firstOctet = ipParts[0].toInt()
        val secondOctet = ipParts[1].toInt()

        // 10.x.x.x
        if (firstOctet == 10) {
            return true
        }

        // 172.16.x.x to 172.31.x.x
        if (firstOctet == 172 && secondOctet in 16..31) {
            return true
        }

        // 192.168.x.x
        return firstOctet == 192 && secondOctet == 168
    }

    // 检测 IPv6 地址是否为内网地址
    fun isPrivateIPv6Address(ip: String): Boolean {
        // 带有前缀的 IPv6 地址，例如：fe80::1/64
        val ipParts = ip.split("/")
        if (ipParts.size != 2) {
            return false
        }

        val ipWithoutPrefix = ipParts[0]
        val prefix = ipParts[1].toIntOrNull()

        // 检查 IPv6 地址的前缀是否在私有地址范围内
        if (prefix != null && prefix >= 0 && prefix <= 64) {
            val ipBytes = ipWithoutPrefix.split(":")
                .map { it.toIntOrNull(16) ?: -1 }
                .toTypedArray()

            // 检查前缀是否匹配内网地址范围
            when {
                ipBytes[0] == 0xfe80 -> return true // 链路本地地址
                ipBytes[0] == 0xfc00 || ipBytes[0] == 0xfd00 -> return true // 网段本地地址
                ipBytes[0] == 0x2000 && ipBytes[1] == 0x0000 -> return true // Teredo 地址
                ipBytes[0] == 0x2001 && ipBytes[1] == 0x0000 -> return true // Teredo 地址
                ipBytes[0] == 0x2001 && ipBytes[1] == 0x0db8 -> return true // 文档地址
                ipBytes[0] == 0x2002 && ipBytes[1] == 0x0000 -> return true // 6to4 地址
                ipBytes[0] == 0x3ffe -> return true // 6bone 地址
            }
        }

        return false
    }

    // 检测 IP 地址是否为内网地址
    return isPrivateIPv4Address(this) || isPrivateIPv6Address(this)
}

fun String.pathLevel(): Int {
    return this.trim('/').split('/').size
}

internal expect fun String.parsePath(): List<String>