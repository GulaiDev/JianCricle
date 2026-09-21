package com.rdbb.jiancircle.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * 把后端返回的 ISO 时间字符串格式化为信息流常用的相对时间：
 * 刚刚 / x分钟前 / x小时前 / 昨天 / MM月dd日 / yyyy年MM月dd日。
 *
 * 后端（SQLite func.now()）存储的是不带时区的 UTC 时间，
 * Pydantic 序列化为 "yyyy-MM-dd'T'HH:mm:ss[.ffffff]"，
 * 这里统一按 UTC 解析。仅使用 API 24 起即可用的 [SimpleDateFormat]/[Calendar]，
 * 不依赖 java.time 脱糖。
 */
fun formatTime(iso: String?): String {
    if (iso.isNullOrBlank()) return ""

    val raw = iso.trim().removeSuffix("Z")
    val pattern = when {
        raw.contains(' ') -> "yyyy-MM-dd HH:mm:ss"
        raw.length > 19 && raw.getOrNull(19) == '.' -> "yyyy-MM-dd'T'HH:mm:ss.SSS"
        else -> "yyyy-MM-dd'T'HH:mm:ss"
    }
    // 含微秒（6 位）时截断到毫秒（3 位）供 SSS 解析
    val parseable = if (pattern.endsWith("SSS")) raw.take(23) else raw.take(19)

    val timeMillis = runCatching {
        SimpleDateFormat(pattern, Locale.getDefault())
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .parse(parseable)?.time
    }.getOrNull() ?: return raw.take(10)

    val delta = System.currentTimeMillis() - timeMillis
    return when {
        delta < 0L -> formatAbsoluteDate(timeMillis)
        delta < MINUTE_MS -> "刚刚"
        delta < HOUR_MS -> "${delta / MINUTE_MS}分钟前"
        delta < DAY_MS -> "${delta / HOUR_MS}小时前"
        delta < 2 * DAY_MS -> "昨天"
        else -> formatAbsoluteDate(timeMillis)
    }
}

/** 同年只显示月日，跨年显示完整日期 */
private fun formatAbsoluteDate(timeMillis: Long): String {
    val utc = TimeZone.getTimeZone("UTC")
    val now = Calendar.getInstance(utc)
    val then = Calendar.getInstance(utc).apply { time = Date(timeMillis) }
    val pattern = if (now.get(Calendar.YEAR) == then.get(Calendar.YEAR)) {
        "MM月dd日"
    } else {
        "yyyy年MM月dd日"
    }
    return SimpleDateFormat(pattern, Locale.getDefault())
        .apply { timeZone = utc }
        .format(Date(timeMillis))
}

private const val MINUTE_MS = 60_000L
private const val HOUR_MS = 60 * MINUTE_MS
private const val DAY_MS = 24 * HOUR_MS
