package com.example.wordmemory.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.wordmemory.data.local.AppDatabase
import com.example.wordmemory.notification.NotificationHelper

/**
 * 每日复习提醒 Worker。
 *
 * 每次执行时：
 * 1. 查询当前时间已经到期的单词；
 * 2. 没有到期单词就静默结束；
 * 3. 有到期单词且通知权限可用时发送通知；
 * 4. 通知权限关闭时静默降级，首页仍会显示待复习数字。
 */
class RemindWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val dueWords = AppDatabase.getInstance(applicationContext)
                .wordDao()
                .getDueWords(System.currentTimeMillis())

            if (dueWords.isNotEmpty()) {
                NotificationHelper.showReviewReminder(applicationContext, dueWords.size)
            }
            Result.success()
        } catch (e: Exception) {
            // 交给 WorkManager 按退避策略重试
            Result.retry()
        }
    }
}
