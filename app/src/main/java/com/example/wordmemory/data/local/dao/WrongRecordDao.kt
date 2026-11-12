package com.example.wordmemory.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.wordmemory.data.local.entity.WrongRecord
import kotlinx.coroutines.flow.Flow

/**
 * 错题记录 DAO。
 *
 * 只负责写入错题和读取错题历史，不包含错题复习算法。
 */
@Dao
interface WrongRecordDao {

    /** 插入一条错题记录，返回自增主键。 */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(record: WrongRecord): Long

    /** 批量插入错题记录。 */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(records: List<WrongRecord>): List<Long>

    /** 查询所有错题记录，按答错时间倒序。 */
    @Query("SELECT * FROM wrong_records ORDER BY wrong_at DESC")
    fun observeAll(): Flow<List<WrongRecord>>

    /** 查询某个单词的全部错题记录，按答错时间倒序。 */
    @Query("SELECT * FROM wrong_records WHERE word_id = :wordId ORDER BY wrong_at DESC")
    fun observeByWord(wordId: Long): Flow<List<WrongRecord>>
}
