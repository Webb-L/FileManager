package app.filemanager.utils

import app.filemanager.data.file.FileSimpleInfo

/**
 * 自然排序比较器的抽象基类。
 * 提供根据自然顺序比较对象的功能。
 *
 * @param T 要比较的对象类型。
 */
abstract class BaseNaturalOrderComparator<T> : Comparator<T> {

    /**
     * 根据自然顺序比较两个对象。
     *
     * @param o1 要比较的第一个对象。
     * @param o2 要比较的第二个对象。
     * @return 负整数、零或正整数，表示第一个对象小于、等于或大于第二个对象。
     */
    override fun compare(o1: T, o2: T): Int {
        // 从对象中提取可比较的字符串
        val a = extractComparableString(o1)
        val b = extractComparableString(o2)

        var ia = 0
        var ib = 0
        var nza: Int
        var nzb: Int
        var ca: Char
        var cb: Char

        // 主比较循环
        while (true) {
            nza = 0
            nzb = 0

            // 跳过前导空格和零
            ca = charAt(a, ia)
            cb = charAt(b, ib)
            while (ca.isWhitespace() || ca == '0') {
                if (ca == '0') {
                    nza++
                } else {
                    nza = 0
                }
                ca = charAt(a, ++ia)
            }
            while (cb.isWhitespace() || cb == '0') {
                if (cb == '0') {
                    nzb++
                } else {
                    nzb = 0
                }
                cb = charAt(b, ++ib)
            }

            // 如果两个字符都是数字，则比较数字
            if (ca.isDigit() && cb.isDigit()) {
                val bias = compareRight(a.substring(ia), b.substring(ib))
                if (bias != 0) {
                    return bias
                }
            }

            // 当两个字符串都已处理完时返回结果
            if (ca == 0.toChar() && cb == 0.toChar()) {
                return compareEqual(a, b, nza, nzb)
            }
            // 根据Unicode值比较字符
            if (ca < cb) {
                return -1
            }
            if (ca > cb) {
                return 1
            }

            ia++
            ib++
        }
    }

    /**
     * 从对象中提取可比较的字符串。
     *
     * @param obj 要从中提取可比较字符串的对象。
     * @return 从对象中提取的可比较字符串。
     */
    protected abstract fun extractComparableString(obj: T): String

    /**
     * 比较两个数字型字符串的右部分。
     *
     * @param a 第一个字符串。
     * @param b 第二个字符串。
     * @return 负整数、零或正整数，表示第一个字符串小于、等于或大于第二个字符串。
     */
    private fun compareRight(a: String, b: String): Int {
        var bias = 0
        var ia = 0
        var ib = 0

        // 从右到左比较数字
        while (true) {
            val ca = charAt(a, ia)
            val cb = charAt(b, ib)

            // 当两个字符串都已处理完时返回结果
            if (!ca.isDigit() && !cb.isDigit()) {
                return bias
            }
            if (!ca.isDigit()) {
                return -1
            }
            if (!cb.isDigit()) {
                return 1
            }
            if (ca == 0.toChar() && cb == 0.toChar()) {
                return bias
            }

            // 在第一次数字比较时初始化bias
            if (bias == 0) {
                bias = when {
                    ca < cb -> -1
                    ca > cb -> 1
                    else -> 0
                }
            }

            ia++
            ib++
        }
    }

    /**
     * 获取字符串中指定索引处的字符。
     *
     * @param s 输入字符串。
     * @param i 要获取字符的索引。
     * @return 指定索引处的字符，如果索引超出范围则返回0。
     */
    private fun charAt(s: String, i: Int): Char {
        return if (i >= s.length) 0.toChar() else s[i]
    }

    /**
     * 比较两个长度相等的字符串。
     *
     * @param a 第一个字符串。
     * @param b 第二个字符串。
     * @param nza 第一个字符串中前导零的数量。
     * @param nzb 第二个字符串中前导零的数量。
     * @return 负整数、零或正整数，表示第一个字符串小于、等于或大于第二个字符串。
     */
    private fun compareEqual(a: String, b: String, nza: Int, nzb: Int): Int {
        // 根据前导零的数量和字符串本身进行比较
        return if (nza - nzb != 0) {
            nza - nzb
        } else if (a.length == b.length) {
            a.compareTo(b)
        } else {
            a.length - b.length
        }
    }
}

/**
 * FileInfo对象按名称的自然顺序比较器。
 */
class NaturalOrderComparator : BaseNaturalOrderComparator<FileSimpleInfo>() {

    /**
     * 从FileInfo对象中提取可比较的字符串。
     *
     * @param obj 要从中提取可比较字符串的FileInfo对象。
     * @return FileInfo对象的名称作为可比较字符串。
     */
    override fun extractComparableString(obj: FileSimpleInfo): String {
        return obj.name
    }
}

/**
 * 根据自然顺序比较字符串的比较器。
 */
class NaturalOrderStringComparator : BaseNaturalOrderComparator<String>() {

    /**
     * 从String对象中提取可比较的字符串。
     *
     * @param obj 要从中提取可比较字符串的String对象。
     * @return 字符串本身作为可比较字符串。
     */
    override fun extractComparableString(obj: String): String {
        return obj
    }
}
