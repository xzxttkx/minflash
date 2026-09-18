package com.shanji.minflash.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class TimeParserTest {

    @Test
    fun parse_afternoonFive_buyDetergent_extractsCorrectTime() {
        // 测试"明天下午五点买洗衣粉"（加"明天"保证时间在未来，避免依赖运行时刻）
        val result = TimeParser.parse("明天下午五点买洗衣粉")
        assertEquals("买洗衣粉", result.content)
        assertNotNull(result.remindTime)

        val calendar = Calendar.getInstance().apply { timeInMillis = result.remindTime!! }
        assertEquals(17, calendar.get(Calendar.HOUR_OF_DAY)) // 下午5点=17点
        assertEquals(0, calendar.get(Calendar.MINUTE))
    }

    @Test
    fun parse_tomorrowEight_meeting_extractsTomorrowTime() {
        // 测试"明天早上八点开会"
        val result = TimeParser.parse("明天早上八点开会")
        assertEquals("开会", result.content)
        assertNotNull(result.remindTime)

        val calendar = Calendar.getInstance().apply { timeInMillis = result.remindTime!! }
        assertEquals(8, calendar.get(Calendar.HOUR_OF_DAY))
        // 应该是明天的日期
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        assertEquals(tomorrow.get(Calendar.DAY_OF_YEAR), calendar.get(Calendar.DAY_OF_YEAR))
    }

    @Test
    fun parse_thirtyMinutesLater_drinkWater_extractsRelativeTime() {
        // 测试"半小时后喝水"
        val result = TimeParser.parse("半小时后喝水")
        assertEquals("喝水", result.content)
        assertNotNull(result.remindTime)

        val diff = (result.remindTime!! - System.currentTimeMillis()) / 1000.0 / 60.0
        assertEquals(30.0, diff, 2.0) // 允许2分钟误差
    }

    @Test
    fun parse_noTime_returnsNullRemindTime() {
        // 测试没有时间的输入
        val result = TimeParser.parse("随便记个东西")
        assertEquals("随便记个东西", result.content)
        assertNull(result.remindTime)
    }

    @Test
    fun parse_halfPastThree_returns30Minutes() {
        // 测试"明天下午三点半买菜"（加"明天"保证时间在未来，避免依赖运行时刻）
        val result = TimeParser.parse("明天下午三点半买菜")
        assertEquals("买菜", result.content)
        val calendar = Calendar.getInstance().apply { timeInMillis = result.remindTime!! }
        assertEquals(15, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, calendar.get(Calendar.MINUTE))
    }

    @Test
    fun parse_pastTimeToday_returnsNullRemindTime() {
        // 今天凌晨3点（相对于绝大多数运行时刻都已过去），应返回 null 提醒时间
        val result = TimeParser.parse("凌晨三点浇花")
        assertEquals("浇花", result.content)
        // 凌晨3点基本已过；若恰好在 0-3 点运行则可能非 null，故仅在已过时断言 null
        val cal = Calendar.getInstance()
        if (cal.get(Calendar.HOUR_OF_DAY) >= 3) {
            assertNull(result.remindTime)
        }
    }

    @Test
    fun parse_colonTime_withAfternoonPeriod_extractsCorrectTime() {
        // 测试"明天下午4:35测试"——冒号格式带下午时段，应解析为16:35
        val result = TimeParser.parse("明天下午4:35测试")
        assertEquals("测试", result.content)
        assertNotNull(result.remindTime)

        val calendar = Calendar.getInstance().apply { timeInMillis = result.remindTime!! }
        assertEquals(16, calendar.get(Calendar.HOUR_OF_DAY)) // 下午4点=16点
        assertEquals(35, calendar.get(Calendar.MINUTE))
    }

    @Test
    fun parse_colonTime_withMorningPeriod_extractsCorrectTime() {
        // 测试"明天上午9:05开会"——冒号格式带上午时段，应解析为9:05
        val result = TimeParser.parse("明天上午9:05开会")
        assertEquals("开会", result.content)
        assertNotNull(result.remindTime)

        val calendar = Calendar.getInstance().apply { timeInMillis = result.remindTime!! }
        assertEquals(9, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(5, calendar.get(Calendar.MINUTE))
    }

    @Test
    fun parse_chineseTime_withPreciseMinutes_extractsCorrectTime() {
        // 测试"明天下午4点35分测试"——中文格式带精确分钟，应解析为16:35
        val result = TimeParser.parse("明天下午4点35分测试")
        assertEquals("测试", result.content)
        assertNotNull(result.remindTime)

        val calendar = Calendar.getInstance().apply { timeInMillis = result.remindTime!! }
        assertEquals(16, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(35, calendar.get(Calendar.MINUTE))
    }

    @Test
    fun parse_colonTime_24hourFormat_extractsCorrectTime() {
        // 测试"明天16:35测试"——24小时制冒号格式，应解析为16:35
        val result = TimeParser.parse("明天16:35测试")
        assertEquals("测试", result.content)
        assertNotNull(result.remindTime)

        val calendar = Calendar.getInstance().apply { timeInMillis = result.remindTime!! }
        assertEquals(16, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(35, calendar.get(Calendar.MINUTE))
    }

    // ===== 中文分钟数全面测试（之前 >9 的中文分钟全部解析为0，这是核心bug）=====

    @Test
    fun parse_chineseMinute_fifteen_extractsCorrectTime() {
        val result = TimeParser.parse("明天下午五点十五分测试")
        assertEquals("测试", result.content)
        val calendar = Calendar.getInstance().apply { timeInMillis = result.remindTime!! }
        assertEquals(17, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(15, calendar.get(Calendar.MINUTE))
    }

    @Test
    fun parse_chineseMinute_twenty_extractsCorrectTime() {
        val result = TimeParser.parse("明天下午五点二十分测试")
        val calendar = Calendar.getInstance().apply { timeInMillis = result.remindTime!! }
        assertEquals(17, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(20, calendar.get(Calendar.MINUTE))
    }

    @Test
    fun parse_chineseMinute_zeroFive_extractsCorrectTime() {
        // "零五"=05分
        val result = TimeParser.parse("明天早上八点零五分开会")
        val calendar = Calendar.getInstance().apply { timeInMillis = result.remindTime!! }
        assertEquals(8, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(5, calendar.get(Calendar.MINUTE))
    }

    @Test
    fun parse_chineseMinute_twentyFive_extractsCorrectTime() {
        val result = TimeParser.parse("明天下午五点二十五分测试")
        val calendar = Calendar.getInstance().apply { timeInMillis = result.remindTime!! }
        assertEquals(17, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(25, calendar.get(Calendar.MINUTE))
    }

    @Test
    fun parse_chineseMinute_fortyFive_extractsCorrectTime() {
        val result = TimeParser.parse("明天下午五点四十五分测试")
        val calendar = Calendar.getInstance().apply { timeInMillis = result.remindTime!! }
        assertEquals(17, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(45, calendar.get(Calendar.MINUTE))
    }

    @Test
    fun parse_chineseMinute_fiftyNine_extractsCorrectTime() {
        val result = TimeParser.parse("明天下午五点五十九分测试")
        val calendar = Calendar.getInstance().apply { timeInMillis = result.remindTime!! }
        assertEquals(17, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, calendar.get(Calendar.MINUTE))
    }

    @Test
    fun parse_chineseMinute_ten_extractsCorrectTime() {
        // "十分"=10分
        val result = TimeParser.parse("明天下午五点十分测试")
        val calendar = Calendar.getInstance().apply { timeInMillis = result.remindTime!! }
        assertEquals(17, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(10, calendar.get(Calendar.MINUTE))
    }

    // ===== 各时段测试 =====

    @Test
    fun parse_noonTwelve_extractsCorrectTime() {
        // 中午12点 = 12:00
        val result = TimeParser.parse("明天中午十二点吃饭")
        val calendar = Calendar.getInstance().apply { timeInMillis = result.remindTime!! }
        assertEquals(12, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, calendar.get(Calendar.MINUTE))
    }

    @Test
    fun parse_eveningEight_extractsCorrectTime() {
        // 晚上8点 = 20:00
        val result = TimeParser.parse("明天晚上八点看电视")
        val calendar = Calendar.getInstance().apply { timeInMillis = result.remindTime!! }
        assertEquals(20, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, calendar.get(Calendar.MINUTE))
    }

    @Test
    fun parse_earlyMorningThree_extractsCorrectTime() {
        // 凌晨3点 = 03:00（加明天保证在未来）
        val result = TimeParser.parse("明天凌晨三点吃药")
        val calendar = Calendar.getInstance().apply { timeInMillis = result.remindTime!! }
        assertEquals(3, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, calendar.get(Calendar.MINUTE))
    }

    // ===== 阿拉伯数字带"点"格式 =====

    @Test
    fun parse_arabicDigit_withMinutes_extractsCorrectTime() {
        // "下午5点15分" 阿拉伯数字
        val result = TimeParser.parse("明天下午5点15分测试")
        val calendar = Calendar.getInstance().apply { timeInMillis = result.remindTime!! }
        assertEquals(17, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(15, calendar.get(Calendar.MINUTE))
    }

    @Test
    fun parse_arabicDigit_tenOClock_extractsCorrectTime() {
        // "上午10点"
        val result = TimeParser.parse("明天上午10点开会")
        val calendar = Calendar.getInstance().apply { timeInMillis = result.remindTime!! }
        assertEquals(10, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, calendar.get(Calendar.MINUTE))
    }

    // ===== 相对时间 =====

    @Test
    fun parse_tenMinutesLater_extractsCorrectTime() {
        val result = TimeParser.parse("10分钟后喝水")
        assertEquals("喝水", result.content)
        assertNotNull(result.remindTime)
        val diff = (result.remindTime!! - System.currentTimeMillis()) / 1000.0 / 60.0
        assertEquals(10.0, diff, 2.0)
    }

    // ===== 冒号格式各时段 =====

    @Test
    fun parse_colonTime_eveningPeriod_extractsCorrectTime() {
        // "晚上8:15" = 20:15
        val result = TimeParser.parse("明天晚上8:15看电视")
        val calendar = Calendar.getInstance().apply { timeInMillis = result.remindTime!! }
        assertEquals(20, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(15, calendar.get(Calendar.MINUTE))
    }

    @Test
    fun parse_colonTime_noonPeriod_extractsCorrectTime() {
        // "中午12:30" = 12:30
        val result = TimeParser.parse("明天中午12:30吃饭")
        val calendar = Calendar.getInstance().apply { timeInMillis = result.remindTime!! }
        assertEquals(12, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, calendar.get(Calendar.MINUTE))
    }

    @Test
    fun parse_colonTime_zeroMinute_extractsCorrectTime() {
        // "9:00" = 09:00
        val result = TimeParser.parse("明天上午9:00开会")
        val calendar = Calendar.getInstance().apply { timeInMillis = result.remindTime!! }
        assertEquals(9, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, calendar.get(Calendar.MINUTE))
    }
}
