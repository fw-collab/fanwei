package com.example.wordmemory.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.wordmemory.data.local.entity.Word
import kotlinx.coroutines.flow.Flow

/**
 * 单词表 DAO。
 *
 * 所有挂起函数由调用方在协程中执行；Flow 查询由 Room 自动在数据库线程执行。
 */
@Dao
interface WordDao {

    /** 新增单词，返回自增主键。spelling 唯一，重复插入会抛异常。 */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(word: Word): Long

    /** 批量新增单词。 */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(words: List<Word>): List<Long>

    /** 修改单词。 */
    @Update
    suspend fun update(word: Word)

    /** 删除单词实体；对应复习记录和错题会级联删除。 */
    @Delete
    suspend fun delete(word: Word)

    /** 按主键删除单词；返回受影响行数。 */
    @Query("DELETE FROM words WHERE id = :wordId")
    suspend fun deleteById(wordId: Long): Int

    /** 查询所有单词，按创建时间倒序。 */
    @Query("SELECT * FROM words ORDER BY created_at DESC")
    fun observeAll(): Flow<List<Word>>

    /** 一次性查询所有单词，供下拉刷新使用。 */
    @Query("SELECT * FROM words ORDER BY created_at DESC")
    suspend fun getAllOnce(): List<Word>

    /**
     * 待复习队列：next_review_time 非空，且已到复习时间（<= now）。
     * 按到期时间升序，最紧急的排在最前面。
     *
     * 注意：Flow 的 now 是调用时传入的快照；时间推移后需要重新订阅或定时刷新。
     */
    @Query(
        "SELECT * FROM words " +
            "WHERE next_review_time IS NOT NULL AND next_review_time <= :now " +
            "ORDER BY next_review_time ASC"
    )
    fun observeDueWords(now: Long): Flow<List<Word>>

    /** 一次性查询待复习单词，供刷新/测试使用。 */
    @Query(
        "SELECT * FROM words " +
            "WHERE next_review_time IS NOT NULL AND next_review_time <= :now " +
            "ORDER BY next_review_time ASC"
    )
    suspend fun getDueWords(now: Long): List<Word>

    /** 按主键查询单词。 */
    @Query("SELECT * FROM words WHERE id = :wordId LIMIT 1")
    suspend fun findById(wordId: Long): Word?

    /** 按拼写查询单词，用于判重。 */
    @Query("SELECT * FROM words WHERE spelling = :spelling LIMIT 1")
    suspend fun findBySpelling(spelling: String): Word?
}
