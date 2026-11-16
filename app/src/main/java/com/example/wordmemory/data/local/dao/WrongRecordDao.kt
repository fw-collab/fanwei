package com.example.wordmemory.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.wordmemory.data.local.entity.WrongRecord
import com.example.wordmemory.data.local.model.WrongWordItem
import kotlinx.coroutines.flow.Flow

/**
 * 错题记录 DAO。
 *
 * 错题表保存“每次答错”的明细；错题本页面还需要按单词聚合出次数和最近时间。
 */
@Dao
interface WrongRecordDao {

    /** 插入一条错题记录，返回自增主键。 */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(record: WrongRecord): Long

    /** 批量插入错题记录。 */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(records: List<WrongRecord>): List<Long>

    /** 查询所有错题明细，按答错时间倒序。 */
    @Query("SELECT * FROM wrong_records ORDER BY wrong_at DESC")
    fun observeAll(): Flow<List<WrongRecord>>

    /** 查询某个单词的全部错题明细，按答错时间倒序。 */
    @Query("SELECT * FROM wrong_records WHERE word_id = :wordId ORDER BY wrong_at DESC")
    fun observeByWord(wordId: Long): Flow<List<WrongRecord>>

    /**
     * 错题本列表：按单词聚合，统计错误次数和最近答错时间。
     * wrong_records 与 words 做 INNER JOIN，保证能拿到单词释义。
     */
    @Query(
        """
        SELECT w.id AS wordId,
               w.spelling AS spelling,
               w.phonetic AS phonetic,
               w.meaning AS meaning,
               COUNT(wr.id) AS wrongCount,
               MAX(wr.wrong_at) AS lastWrongAt
        FROM wrong_records wr
        INNER JOIN words w ON w.id = wr.word_id
        GROUP BY w.id, w.spelling, w.phonetic, w.meaning
        ORDER BY lastWrongAt DESC
        """
    )
    fun observeWrongWords(): Flow<List<WrongWordItem>>

    /** 单个单词的错题聚合，供详情页使用。 */
    @Query(
        """
        SELECT w.id AS wordId,
               w.spelling AS spelling,
               w.phonetic AS phonetic,
               w.meaning AS meaning,
               COUNT(wr.id) AS wrongCount,
               MAX(wr.wrong_at) AS lastWrongAt
        FROM wrong_records wr
        INNER JOIN words w ON w.id = wr.word_id
        WHERE w.id = :wordId
        GROUP BY w.id, w.spelling, w.phonetic, w.meaning
        LIMIT 1
        """
    )
    fun observeWrongWord(wordId: Long): Flow<List<WrongWordItem>>

    /** 把某个单词从错题本移除；返回删除的错题明细条数。 */
    @Query("DELETE FROM wrong_records WHERE word_id = :wordId")
    suspend fun deleteByWordId(wordId: Long): Int
}
