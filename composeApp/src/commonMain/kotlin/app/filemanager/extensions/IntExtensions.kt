package app.filemanager.extensions

import kotlin.random.Random

/**
 * 将整型 [Int] 作为扩展，用于生成指定长度的随机密码（不依赖 SecureRandom）。
 * @param includeUpperCase 是否包含大写字母
 * @param includeLowerCase 是否包含小写字母
 * @param includeDigits 是否包含数字
 * @param includeSpecial 是否包含特殊字符
 * @throws IllegalArgumentException 当所有字符组都被禁用时抛出异常
 */
fun Int.randomString(
    includeUpperCase: Boolean = true,
    includeLowerCase: Boolean = true,
    includeDigits: Boolean = true,
    includeSpecial: Boolean = true
): String {
    // 根据参数构建可用字符集
    val uppercaseChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    val lowercaseChars = "abcdefghijklmnopqrstuvwxyz"
    val digitChars = "0123456789"
    val specialChars = "!@#\$%^&*()-_=+"

    // 将用户选定的字符组汇总
    val characterPool = buildString {
        if (includeUpperCase) append(uppercaseChars)
        if (includeLowerCase) append(lowercaseChars)
        if (includeDigits) append(digitChars)
        if (includeSpecial) append(specialChars)
    }

    // 若没有选择任何字符类型，则抛出异常
    require(characterPool.isNotEmpty()) {
        "没有选择任何可用的字符类型！请至少启用一种字符组。"
    }

    // Kotlin 标准库的 Random 生成随机数
    return buildString(this) {
        repeat(this@randomString) {
            val index = Random.nextInt(characterPool.length)
            append(characterPool[index])
        }
    }
}