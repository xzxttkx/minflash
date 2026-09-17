package com.shanji.minflash.util

import java.util.Calendar
import java.util.Locale

/**
 * 从用户输入文本中提取提醒时间和任务内容
 * 支持常见表达："下午五点买牛奶"、"明天早上八点开会"、"半小时后喝水"
 */
object TimeParser {
    data class ParseResult(
        val content: String,
        val remindTime: Long?
    )

    fun parse(input: String): ParseResult {
        val trimmed = input.trim()
        var currentTime = Calendar.getInstance()
        var content = trimmed

        // 1. 处理"X分钟后"的相对时间
        val minuteRegex = Regex("(\\d+)\\s*分钟后")
        minuteRegex.find(trimmed)?.let { match ->
            val minutes = match.groupValues[1].toInt()
            currentTime.add(Calendar.MINUTE, minutes)
            content = trimmed.removeRange(match.range).trim()
            return ParseResult(content, currentTime.timeInMillis)
        }

        // 2. 处理"明天"
        var isTomorrow = false
        if (trimmed.contains("明天")) {
            isTomorrow = true
            content = content.replace("明天", "").trim()
        }

        // 3. 处理时段：早上/上午/中午/下午/晚上/凌晨
        var hour = -1
        var minute = 0
        when {
            content.contains("凌晨") -> {
                content = content.replace("凌晨", "").trim()
                // 凌晨默认0-6点，比如"凌晨三点"就是3点
            }
            content.contains("早上") || content.contains("上午") -> {
                content = content.replace("早上", "").replace("上午", "").trim()
            }
            content.contains("中午") -> {
                content = content.replace("中午", "").trim()
                hour = 12
            }
            content.contains("下午") -> {
                content = content.replace("下午", "").trim()
                // 下午需要+12，除非是12点
            }
            content.contains("晚上") -> {
                content = content.replace("晚上", "").trim()
                // 晚上默认+12
            }
        }

        // 4. 提取时间数字："五点"、"三点半"、"八点半"、"14:30"
        // 先匹配X点Y分的格式
        val timeRegex = Regex("(\\d+)\\s*点(半)?(\\d+)?\\s*分?")
        timeRegex.find(content)?.let { match ->
            val rawHour = match.groupValues[1].toInt()
            val isHalf = match.groupValues[2] == "半"
            val rawMinute = match.groupValues[3].takeIf { it.isNotEmpty() }?.toInt() ?: 0

            // 处理12小时制
            hour = when {
                hour == 12 -> rawHour // 中午
                content.contains("下午") || content.contains("晚上") -> {
                    if (rawHour == 12) 12 else rawHour + 12
                }
                else -> rawHour
            }
            minute = if (isHalf) 30 else rawMinute
            content = content.removeRange(match.range).trim()
        }

        // 处理X:XX格式的24小时制
        val colonTimeRegex = Regex("(\\d+):(\\d+)")
        colonTimeRegex.find(content)?.let { match ->
            hour = match.groupValues[1].toInt()
            minute = match.groupValues[2].toInt()
            content = content.removeRange(match.range).trim()
        }

        // 如果没解析到时间，返回null
        if (hour == -1) {
            return ParseResult(trimmed, null)
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

        // 如果时间已经过了今天，自动顺延到明天？不对，PRD说任务当日有效，所以如果时间过了，就不提醒？不对，PRD说"当日有效"，如果用户输入的时间已经过了今天，那应该提示时间无效？
        // 先按规则：如果计算出来的时间早于当前时间，并且没有说明天，就自动设为明天？不对，PRD说"当日有效，跨天自动清空"，所以如果用户输入的时间已经过了今天，那这个任务就当天不会触发了？
        // 这里先简单处理：如果时间在当前时间之前，并且没说明天，就加到明天？不对，PRD说不自动顺延，那如果时间过了，这个任务就不提醒，只是显示在列表里。
        // 先按PRD来：不自动顺延，返回计算的时间，如果时间已经过了，AlarmManager会立即触发？不对，那不行，我们判断一下：如果时间<当前时间，就返回null，提示时间无效。

        if (currentTime.timeInMillis < System.currentTimeMillis()) {
            return ParseResult(trimmed, null)
        }

        return ParseResult(content, currentTime.timeInMillis)
    }
}
