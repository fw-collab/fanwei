package com.example.wordmemory.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * 复习提醒调度器。
 *
 * - 每天 20:00 执行一次；
 * - 使用唯一任务名 + ExistingPeriodicWorkPolicy.KEEP，避免重复入队；
 * - ViewModel 还会保存一个 scheduled 状态，避免页面重复调用。
 */
object ReminderScheduler {

    const val WORK_NAME = "daily_review_reminder"
    private const val REMINDER_HOUR = 20

    /** 确保每日提醒任务存在；重复调用不会产生多个任务。 */
    fun ensureScheduled(context: Context, now: Long = System.currentTimeMillis()): Boolean {
        return try {
            val request = PeriodicWorkRequestBuilder<RemindWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(initialDelayMillis(now), TimeUnit.MILLISECONDS)
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 计算距离下一次 20:00 的毫秒数。
     * 如果今天 20:00 已过，就安排到明天 20:00。
     */
    fun initialDelayMillis(now: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, REMINDER_HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (calendar.timeInMillis <= now) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis - now
    }
}
