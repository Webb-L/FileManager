@file:OptIn(kotlin.time.ExperimentalTime::class)

package app.filemanager.extensions

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun Long.timestampToMonthDay(): String {
    val instant = Instant.fromEpochMilliseconds(this)
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val month = localDateTime.monthNumber
    val day = localDateTime.dayOfMonth
    return "${month}月${day}号"
}

fun Long.timestampToYearMonthDay(): String {
    val instant = Instant.fromEpochMilliseconds(this)
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${localDateTime.year}-${localDateTime.monthNumber}-${localDateTime.dayOfMonth}"
}

fun Long.timestampToHourMinute(): String {
    val instant = Instant.fromEpochMilliseconds(this)
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${localDateTime.hour}:${localDateTime.minute}"
}

fun Long.timestampToYMDHM(): String {
    val instant = Instant.fromEpochMilliseconds(this)
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${localDateTime.year.toString().padStart(2, '0')}-${
        localDateTime.monthNumber.toString().padStart(2, '0')
    }-${localDateTime.dayOfMonth.toString().padStart(2, '0')} ${
        localDateTime.hour.toString().padStart(2, '0')
    }:${localDateTime.minute.toString().padStart(2, '0')}"
}

fun Long.timestampToSyncDate(): String {
    val instant = Instant.fromEpochMilliseconds(this)
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${localDateTime.year.toString().padStart(2, '0')}-${
        localDateTime.monthNumber.toString().padStart(2, '0')
    }-${localDateTime.dayOfMonth.toString().padStart(2, '0')} ${
        localDateTime.hour.toString().padStart(2, '0')
    }:${localDateTime.minute.toString().padStart(2, '0')}:${localDateTime.second.toString().padStart(2, '0')}"
}

fun Long.timestampToLocalDateTime(): LocalDateTime =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault())

fun Long.formatFileSize(): String {
    return if (this < 1024) {
        "$this B"
    } else if (this < 1024 * 1024) {
        val kilobytes = this / 1024
        "$kilobytes KB"
    } else if (this < 1024 * 1024 * 1024) {
        val megabytes = this / (1024 * 1024)
        "$megabytes MB"
    } else {
        val gigabytes = this / (1024 * 1024 * 1024)
        "$gigabytes GB"
    }
}

fun Long.toBoolean(): Boolean = this != 0L