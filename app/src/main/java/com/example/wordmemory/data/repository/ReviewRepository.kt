package com.example.wordmemory.data.repository

import androidx.room.withTransaction
import com.example.wordmemory.data.local.AppDatabase
import com.example.wordmemory.data.local.entity.ReviewRecord
import com.example.wordmemory.data.local.entity.Word
import com.example.wordmemory.data.local.entity.WrongRecord
import com.example.wordmemory.domain.algorithm.EbbinghausUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * 复习流程 Repository。
 *
 * 每次提交复习都在一个 Room 事务里完成：
 * - 更新 Word 的记忆等级和下一次复习时间；
 * - 插入 ReviewRecord；
 * - 如果答错，再插入 WrongRecord。
 *
 * 任意一步失败，整个事务回滚，避免出现“等级更新了但没有复习记录”的脏数据。
 */
class ReviewRepository(private val db: AppDatabase) {

    private val wordDao = db.wordDao()
    private val reviewRecordDao = db.reviewRecordDao()
    private val wrongRecordDao = db.wrongRecordDao()

    /** 观察当前已经到期的复习队列。 */
    fun observeDueWords(now: Long): Flow<List<Word>> {
        return wordDao.observeDueWords(now).flowOn(Dispatchers.IO)
    }

    /**
     * 提交一次复习结果。
     *
     * @param word 当前正在复习的单词
     * @param correct true = 记住，false = 忘记
     * @param now 本次复习时间戳
     */
    suspend fun submitReview(
        word: Word,
        correct: Boolean,
        now: Long = System.currentTimeMillis(),
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. 用艾宾浩斯算法算出新等级、新间隔、下一次复习时间
                val schedule = EbbinghausUtil.schedule(
                    currentLevel = word.memoryLevel,
                    correct = correct,
                    now = now,
                )

                // 2. 同步更新列表页使用的掌握状态
                val newMasteryLevel = when {
                    schedule.nextLevel >= 2 -> 2 // 已掌握
                    schedule.nextLevel == 1 -> 1 // 学习中
                    else -> 0                    // 未学/重置
                }

                val updatedWord = word.copy(
                    memoryLevel = schedule.nextLevel,
                    nextReviewTime = schedule.nextReviewTime,
                    masteryLevel = newMasteryLevel,
                )

                // 3. 更新单词 + 写复习记录 + 答错时写错题，放在同一个事务里
                db.withTransaction {
                    wordDao.update(updatedWord)

                    reviewRecordDao.insert(
                        ReviewRecord(
                            wordId = word.id,
                            reviewedAt = now,
                            result = correct,
                            quality = if (correct) 5 else 1,
                        )
                    )

                    if (!correct) {
                        wrongRecordDao.insert(
                            WrongRecord(
                                wordId = word.id,
                                wrongAt = now,
                                correctAnswer = word.spelling,
                                questionType = "REVIEW",
                            )
                        )
                    }
                }

                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
