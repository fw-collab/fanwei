package com.example.wordmemory.domain.algorithm

/**
 * 艾宾浩斯遗忘曲线工具类（简化版）。
 *
 * 设计目标：
 * 1. 不依赖 Android / Room，可在 JVM 单元测试中直接运行；
 * 2. 只负责“根据当前等级和答题结果，算出下一次复习时间”；
 * 3. 不负责 UI、不负责数据库写入，方便在 Repository / ViewModel 中复用。
 *
 * 等级与间隔：
 * level 0 → 5 分钟
 * level 1 → 30 分钟
 * level 2 → 12 小时
 * level 3 → 1 天
 * level 4 → 2 天
 * level 5 → 4 天
 * level 6 → 7 天
 * level 7 → 15 天
 * level 8 → 30 天
 *
 * 规则：
 * - 答对：等级 +1，最高停在 8；
 * - 答错：等级重置为 0；
 * - 下一次复习时间 = 当前时间 + 新等级对应的间隔。
 *
 * 举例：
 * - 当前 level 0，答对 → 新 level 1，下一次是 30 分钟后；
 * - 当前 level 3，答错 → 新 level 0，下一次是 5 分钟后；
 * - 当前 level 8，答对 → 仍是 level 8，下一次是 30 天后。
 */
object EbbinghausUtil {

    private const val MINUTE = 60_000L
    private const val HOUR = 60 * MINUTE
    private const val DAY = 24 * HOUR

    /** 0–8 级对应的复习间隔，单位毫秒。 */
    val INTERVALS_MS: LongArray = longArrayOf(
        5 * MINUTE,   // level 0：5 分钟
        30 * MINUTE,  // level 1：30 分钟
        12 * HOUR,    // level 2：12 小时
        1 * DAY,      // level 3：1 天
        2 * DAY,      // level 4：2 天
        4 * DAY,      // level 5：4 天
        7 * DAY,      // level 6：7 天
        15 * DAY,     // level 7：15 天
        30 * DAY,     // level 8：30 天
    )

    /** 最高记忆等级，对应 30 天间隔。 */
    const val MAX_LEVEL: Int = 8

    /**
     * 一次答题后的复习计划。
     *
     * @property nextLevel 答题后的新等级
     * @property nextReviewTime 下一次复习时间戳（Unix 毫秒）
     * @property intervalMillis 本次安排的间隔（毫秒），便于界面展示
     */
    data class ReviewSchedule(
        val nextLevel: Int,
        val nextReviewTime: Long,
        val intervalMillis: Long,
    )

    /**
     * 根据当前等级和答题结果计算新等级。
     *
     * 步骤：
     * 1. 先把非法等级夹到 0–8，防止脏数据导致数组越界；
     * 2. 答对则 +1，并限制最大值为 8；
     * 3. 答错则直接回到 0。
     */
    fun nextLevel(currentLevel: Int, correct: Boolean): Int {
        val safeLevel = currentLevel.coerceIn(0, MAX_LEVEL)
        return if (correct) {
            (safeLevel + 1).coerceAtMost(MAX_LEVEL)
        } else {
            0
        }
    }

    /**
     * 查询某个等级对应的间隔。
     * 越界等级会被夹到合法范围，保证工具类不会因为脏数据崩溃。
     */
    fun intervalForLevel(level: Int): Long {
        return INTERVALS_MS[level.coerceIn(0, MAX_LEVEL)]
    }

    /**
     * 计算完整复习计划。
     *
     * 步骤：
     * 1. 根据答题结果算出新等级；
     * 2. 用新等级取间隔；
     * 3. 当前时间 + 间隔 = 下一次复习时间戳。
     *
     * @param currentLevel 当前记忆等级 0–8
     * @param correct 本次是否答对
     * @param now 当前时间戳，默认系统当前时间；单元测试可传固定值
     */
    fun schedule(
        currentLevel: Int,
        correct: Boolean,
        now: Long = System.currentTimeMillis(),
    ): ReviewSchedule {
        val newLevel = nextLevel(currentLevel, correct)
        val interval = intervalForLevel(newLevel)
        return ReviewSchedule(
            nextLevel = newLevel,
            nextReviewTime = now + interval,
            intervalMillis = interval,
        )
    }

    /**
     * 只返回下一次复习时间戳，方便 DAO / Repository 直接写入 Word.nextReviewTime。
     */
    fun calculateNextReviewTime(
        currentLevel: Int,
        correct: Boolean,
        now: Long = System.currentTimeMillis(),
    ): Long {
        return schedule(currentLevel, correct, now).nextReviewTime
    }

    /**
     * 判断某个单词当前是否到期。
     * nextReviewTime 为 null 表示还没排入复习计划，不进入复习队列。
     */
    fun isDue(nextReviewTime: Long?, now: Long = System.currentTimeMillis()): Boolean {
        return nextReviewTime != null && nextReviewTime <= now
    }
}
