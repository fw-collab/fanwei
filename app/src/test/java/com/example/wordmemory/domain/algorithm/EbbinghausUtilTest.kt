package com.example.wordmemory.domain.algorithm

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EbbinghausUtilTest {

    private val now = 1_700_000_000_000L
    private val minute = 60_000L
    private val hour = 60 * minute
    private val day = 24 * hour

    @Test
    fun `标准间隔为 9 级且数值正确`() {
        assertArrayEquals(
            longArrayOf(
                5 * minute,
                30 * minute,
                12 * hour,
                1 * day,
                2 * day,
                4 * day,
                7 * day,
                15 * day,
                30 * day,
            ),
            EbbinghausUtil.INTERVALS_MS,
        )
    }

    @Test
    fun `答对升级`() {
        assertEquals(1, EbbinghausUtil.nextLevel(0, true))
        assertEquals(4, EbbinghausUtil.nextLevel(3, true))
        assertEquals(8, EbbinghausUtil.nextLevel(7, true))
    }

    @Test
    fun `答错重置到 0`() {
        assertEquals(0, EbbinghausUtil.nextLevel(0, false))
        assertEquals(0, EbbinghausUtil.nextLevel(5, false))
        assertEquals(0, EbbinghausUtil.nextLevel(8, false))
    }

    @Test
    fun `最高等级不会超过 8`() {
        assertEquals(8, EbbinghausUtil.nextLevel(8, true))
        assertEquals(8, EbbinghausUtil.nextLevel(99, true))
        assertEquals(0, EbbinghausUtil.nextLevel(-10, false))
    }

    @Test
    fun `0 级答对安排 30 分钟后复习`() {
        val schedule = EbbinghausUtil.schedule(0, true, now)
        assertEquals(1, schedule.nextLevel)
        assertEquals(30 * minute, schedule.intervalMillis)
        assertEquals(now + 30 * minute, schedule.nextReviewTime)
    }

    @Test
    fun `3 级答错回到 0 级并安排 5 分钟后复习`() {
        val schedule = EbbinghausUtil.schedule(3, false, now)
        assertEquals(0, schedule.nextLevel)
        assertEquals(5 * minute, schedule.intervalMillis)
        assertEquals(now + 5 * minute, schedule.nextReviewTime)
    }

    @Test
    fun `8 级答对保持 8 级并安排 30 天后复习`() {
        val schedule = EbbinghausUtil.schedule(8, true, now)
        assertEquals(8, schedule.nextLevel)
        assertEquals(30 * day, schedule.intervalMillis)
        assertEquals(now + 30 * day, schedule.nextReviewTime)
    }

    @Test
    fun `到期判断包含等于当前时间`() {
        assertFalse(EbbinghausUtil.isDue(null, now))
        assertFalse(EbbinghausUtil.isDue(now + 1, now))
        assertTrue(EbbinghausUtil.isDue(now, now))
        assertTrue(EbbinghausUtil.isDue(now - 1, now))
    }
}
