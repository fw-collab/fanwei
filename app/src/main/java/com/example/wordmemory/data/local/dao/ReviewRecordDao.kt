package com.example.wordmemory.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.wordmemory.data.local.entity.ReviewRecord
import kotlinx.coroutines.flow.Flow

/**
 * 复习记录 DAO。
 *
 * 只负责写入复习记录和读取历史，不包含复习间隔算法。
 */
@Dao
interface ReviewRecordDao {

    /** 插入一条复习记录，返回自增主键。 */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(record: ReviewRecord): Long

    /** 批量插入复习记录。 */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(records: List<ReviewRecord>): List<Long>

    /** 查询所有复习记录，按复习时间倒序。 */
    @Query("SELECT * FROM review_records ORDER BY reviewed_at DESC")
    fun observeAll(): Flow<List<ReviewRecord>>

    /** 查询某个单词的全部复习记录，按复习时间倒序。 */
    @Query("SELECT * FROM review_records WHERE word_id = :wordId ORDER BY reviewed_at DESC")
    fun observeByWord(wordId: Long): Flow<List<ReviewRecord>>
}
