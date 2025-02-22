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

/**
 * 生成当前 IP 所在子网的所有 IP 地址。
 * 当前方法假设子网掩码为 /24，即最后一段 IP 范围为 0~255。
 *
 * @return 返回包含子网中所有 IP 地址的列表。如果当前字符串格式不为有效 IP 格式，则返回空列表。
 */
fun String.getSubnetIps(): List<String> {
    // 假设使用 /24 网段
    // 1. 将传入的 IP 切分为四段
    val parts = split(".")
    if (parts.size != 4) {
        // 如果格式不正确，直接返回空列表或根据需要抛出异常
        return emptyList()
    }

    // 2. 取前三段作为固定部分，最后一段作为子网内可变段
    val prefix = "${parts[0]}.${parts[1]}.${parts[2]}"
    val result = mutableListOf<String>()

    // 3. 生成 0～255 共 256 个 IP（可根据需要排除 0 或 255）
    for (i in 0..255) {
        val completeIp = "$prefix.$i"
        result.add(completeIp)
    }
    return result

}

internal expect fun String.parsePath(): List<String>