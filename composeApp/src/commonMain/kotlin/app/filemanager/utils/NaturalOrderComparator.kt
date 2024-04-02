package app.filemanager.utils

import app.filemanager.data.file.FileInfo

abstract class BaseNaturalOrderComparator<T> : Comparator<T> {
    override fun compare(o1: T, o2: T): Int {
        val a = extractComparableString(o1)
        val b = extractComparableString(o2)

        var ia = 0
        var ib = 0
        var nza: Int
        var nzb: Int
        var ca: Char
        var cb: Char

        while (true) {
            nza = 0
            nzb = 0

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

            if (ca.isDigit() && cb.isDigit()) {
                val bias = compareRight(a.substring(ia), b.substring(ib))
                if (bias != 0) {
                    return bias
                }
            }

            if (ca == 0.toChar() && cb == 0.toChar()) {
                return compareEqual(a, b, nza, nzb)
            }
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

    protected abstract fun extractComparableString(obj: T): String

    private fun compareRight(a: String, b: String): Int {
        var bias = 0
        var ia = 0
        var ib = 0

        while (true) {
            val ca = charAt(a, ia)
            val cb = charAt(b, ib)

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

    private fun charAt(s: String, i: Int): Char {
        return if (i >= s.length) 0.toChar() else s[i]
    }

    private fun compareEqual(a: String, b: String, nza: Int, nzb: Int): Int {
        return if (nza - nzb != 0) {
            nza - nzb
        } else if (a.length == b.length) {
            a.compareTo(b)
        } else {
            a.length - b.length
        }
    }
}

class NaturalOrderComparator : BaseNaturalOrderComparator<FileInfo>() {
    override fun extractComparableString(obj: FileInfo): String {
        return obj.name
    }
}

class NaturalOrderStringComparator : BaseNaturalOrderComparator<String>() {
    override fun extractComparableString(obj: String): String {
        return obj
    }
}
