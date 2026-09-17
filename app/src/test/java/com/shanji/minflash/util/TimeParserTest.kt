package com.shanji.minflash.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class TimeParserTest {

    @Test
    fun parse_afternoonFive_buyDetergent_extractsCorrectTime() {
        // 测试"下午五点买洗衣粉"
        val result = TimeParser.parse("下午五点买洗衣粉")
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

        val diff = (result.remindTime!! - System.currentTimeMillis()) / 1000 / 60
        assertEquals(30L, diff, 2.0) // 允许2分钟误差
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
        // 测试"下午三点半买菜"
        val result = TimeParser.parse("下午三点半买菜")
        assertEquals("买菜", result.content)
        val calendar = Calendar.getInstance().apply { timeInMillis = result.remindTime!! }
        assertEquals(15, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, calendar.get(Calendar.MINUTE))
    }
}
