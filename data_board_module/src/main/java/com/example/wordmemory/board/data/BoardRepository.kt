package com.example.wordmemory.board.data

import com.example.wordmemory.data.local.AppDatabase
import com.example.wordmemory.data.local.entity.ReviewRecord
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * 数据看板读取结果。
 */
data class BoardDashboard(
    val totalWords: Int,
    val todayReviews: Int,
    val accuracy: Double?,
    val wrongTotal: Int,
    val levelCounts: Map<Int, Int>,
    /** 近 7 天（含今天）的复习明细，由 ViewModel 聚合成 BarEntry。 */
    val reviewsLast7Days: List<ReviewRecord>,
)

/**
 * 数据看板 Repository。
 *
 * 直接使用主 App 的 AppDatabase / DAO，因为 board 模块最终运行在同一个 APK、同一个进程里。
 * 数据库文件是主 App 的 word_memory.db，不复制、不另建库。
 */
class BoardRepository(private val db: AppDatabase) {

    private val wordDao = db.wordDao()
    private val reviewRecordDao = db.reviewRecordDao()
    private val wrongRecordDao = db.wrongRecordDao()

    suspend fun loadDashboard(): Result<BoardDashboard> {
        return withContext(Dispatchers.IO) {
            try {
                val (todayStart, tomorrowStart) = todayRange()
                val totalWords = wordDao.countAll()
                val todayReviews = reviewRecordDao.countBetween(todayStart, tomorrowStart)
                val todayCorrect = reviewRecordDao.countCorrectBetween(todayStart, tomorrowStart)
                val wrongTotal = wrongRecordDao.countAll()
                val levelCounts = wordDao.countByMemoryLevel()
                    .associate { it.level to it.count }
                    .toSortedMap()
                val recentReviews = reviewRecordDao.getBetween(
                    dayStart(offsetDays = -6),
                    tomorrowStart,
                )

                Result.success(
                    BoardDashboard(
                        totalWords = totalWords,
                        todayReviews = todayReviews,
                        accuracy = if (todayReviews > 0) {
                            todayCorrect.toDouble() / todayReviews
                        } else {
                            null
                        },
                        wrongTotal = wrongTotal,
                        levelCounts = levelCounts,
                        reviewsLast7Days = recentReviews,
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /** 本地时区“今天 00:00”到“明天 00:00”。 */
    private fun todayRange(): Pair<Long, Long> {
        return dayStart(offsetDays = 0) to dayStart(offsetDays = 1)
    }

    /** 本地时区某一天的 00:00，offsetDays = -6 表示 6 天前。 */
    private fun dayStart(offsetDays: Int): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, offsetDays)
        }.timeInMillis
    }
}
