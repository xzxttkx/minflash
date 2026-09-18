package com.shanji.minflash.util

import java.util.Calendar

/**
 * 从用户输入文本中提取提醒时间和任务内容
 * 支持常见表达："下午五点买牛奶"、"明天早上八点开会"、"半小时后喝水"
 * 同时支持中文数字（一~十二）和阿拉伯数字
 */
object TimeParser {
    data class ParseResult(
        val content: String,
        val remindTime: Long?
    )

    private val CN_DIGIT_MAP = mapOf(
        "零" to 0, "〇" to 0,
        "一" to 1, "二" to 2, "两" to 2,
        "三" to 3, "四" to 4, "五" to 5, "六" to 6,
        "七" to 7, "八" to 8, "九" to 9
    )

    /**
     * 解析中文/阿拉伯数字，支持 0~59（覆盖小时和分钟所有可能值）。
     * 支持：单字（五=5）、十几（十五=15）、几十几（二十五=25）、零开头（零五=5）、阿拉伯数字（15）。
     */
    private fun parseNumber(token: String): Int? {
        if (token.isEmpty()) return null
        // 阿拉伯数字直接转
        token.toIntOrNull()?.let { return it }

        var s = token
        // 处理"零"开头，如"零五"=5
        if (s.startsWith("零") || s.startsWith("〇")) {
            s = s.substring(1)
            if (s.isEmpty()) return 0
        }

        // 处理"十"（10-19）：十=10, 十一=11, ..., 十九=19
        if (s == "十") return 10
        if (s.startsWith("十")) {
            val digit = CN_DIGIT_MAP[s.substring(1)]
            if (digit != null && digit in 1..9) return 10 + digit
            return null
        }

        // 处理"X十"或"X十Y"（20-59）：二十=20, 二十五=25, ..., 五十九=59
        if (s.contains("十")) {
            val parts = s.split("十")
            val tens = CN_DIGIT_MAP[parts[0]] ?: return null
            val ones = if (parts.size > 1 && parts[1].isNotEmpty()) {
                CN_DIGIT_MAP[parts[1]] ?: return null
            } else 0
            return tens * 10 + ones
        }

        // 单字数字（0-9）
        return CN_DIGIT_MAP[s]
    }

    fun parse(input: String): ParseResult {
        val trimmed = input.trim()
        var currentTime = Calendar.getInstance()
        var content = trimmed

        // 1. 处理"半小时后"
        if (trimmed.contains("半小时后")) {
            currentTime.add(Calendar.MINUTE, 30)
            content = trimmed.replace("半小时后", "").trim()
            return ParseResult(content, currentTime.timeInMillis)
        }

        // 2. 处理"X分钟后"的相对时间（阿拉伯数字）
        val minuteRegex = Regex("(\\d+)\\s*分钟后")
        minuteRegex.find(trimmed)?.let { match ->
            val minutes = match.groupValues[1].toInt()
            currentTime.add(Calendar.MINUTE, minutes)
            content = trimmed.removeRange(match.range).trim()
            return ParseResult(content, currentTime.timeInMillis)
        }

        // 3. 处理"明天"
        var isTomorrow = false
        if (trimmed.contains("明天")) {
            isTomorrow = true
            content = content.replace("明天", "").trim()
        }

        // 4. 处理时段：早上/上午/中午/下午/晚上/凌晨
        var period: Period = Period.NONE
        when {
            content.contains("凌晨") -> {
                content = content.replace("凌晨", "").trim()
                period = Period.EARLY_MORNING
            }
            content.contains("早上") || content.contains("上午") -> {
                content = content.replace("早上", "").replace("上午", "").trim()
                period = Period.MORNING
            }
            content.contains("中午") -> {
                content = content.replace("中午", "").trim()
                period = Period.NOON
            }
            content.contains("下午") -> {
                content = content.replace("下午", "").trim()
                period = Period.AFTERNOON
            }
            content.contains("晚上") -> {
                content = content.replace("晚上", "").trim()
                period = Period.EVENING
            }
        }

        var hour = -1
        var minute = 0

        // 5. 提取时间数字："五点"、"三点半"、"八点半"、"五点三十分"、"14点30分"
        val timeRegex = Regex("([零〇一二两三四五六七八九十\\d]+)\\s*点(半)?([零〇一二两三四五六七八九十\\d]+)?\\s*分?")
        timeRegex.find(content)?.let { match ->
            val rawHour = parseNumber(match.groupValues[1]) ?: -1
            val isHalf = match.groupValues[2] == "半"
            val rawMinute = match.groupValues[3].takeIf { it.isNotEmpty() }?.let { parseNumber(it) } ?: 0

            hour = when (period) {
                Period.NOON -> rawHour
                Period.AFTERNOON, Period.EVENING -> {
                    if (rawHour == 12) 12 else rawHour + 12
                }
                else -> rawHour
            }
            minute = if (isHalf) 30 else rawMinute
            content = content.removeRange(match.range).trim()
        }

        // 6. 处理 X:XX 格式（支持 12 小时制带时段，如"下午4:35"；也支持 24 小时制如"16:35"）
        val colonTimeRegex = Regex("(\\d+):(\\d+)")
        colonTimeRegex.find(content)?.let { match ->
            var rawHour = match.groupValues[1].toInt()
            minute = match.groupValues[2].toInt()
            // 应用时段转换（和"X点"格式保持一致）
            hour = when (period) {
                Period.NOON -> rawHour
                Period.AFTERNOON, Period.EVENING -> {
                    if (rawHour == 12) 12 else rawHour + 12
                }
                else -> rawHour
            }
            content = content.removeRange(match.range).trim()
        }

        // 如果没解析到时间，返回null
        if (hour == -1) {
            return ParseResult(content, null)
        }

        // 设置计算好的时间
        currentTime.set(Calendar.HOUR_OF_DAY, hour)
        currentTime.set(Calendar.MINUTE, minute)
        currentTime.set(Calendar.SECOND, 0)
        currentTime.set(Calendar.MILLISECOND, 0)

        // 如果是明天，加一天
        if (isTomorrow) {
            currentTime.add(Calendar.DAY_OF_YEAR, 1)
        }

        // 智能回退：没写时段且小时在1~11之间（12小时制），如果时间已过，当成下午（+12）
        // 例如用户输入"4:35测试"，当前是下午2点，则自动解释为16:35而非04:35
        if (period == Period.NONE && hour in 1..11 && !isTomorrow &&
            currentTime.timeInMillis < System.currentTimeMillis()
        ) {
            currentTime.add(Calendar.HOUR_OF_DAY, 12)
        }

        // PRD：任务当日有效，不自动顺延。若解析出的时间已过且未说明天，则视为无效提醒时间。
        if (currentTime.timeInMillis < System.currentTimeMillis()) {
            return ParseResult(content, null)
        }

        return ParseResult(content, currentTime.timeInMillis)
    }

    private enum class Period {
        NONE, EARLY_MORNING, MORNING, NOON, AFTERNOON, EVENING
    }
}
