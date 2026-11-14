package com.example.wordmemory.data.repository

import com.example.wordmemory.data.local.dao.WordDao
import com.example.wordmemory.data.local.entity.Word
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * 单词本 Repository。
 *
 * - 读列表：返回 Flow，由 Room 在数据库线程查询；
 * - 写操作：suspend + Dispatchers.IO，避免阻塞主线程；
 * - 所有数据库异常在 Repository 内捕获并封装为 Result，交给 ViewModel 决定如何提示。
 */
class WordRepository(private val wordDao: WordDao) {

    /** 观察全部单词，按创建时间倒序。 */
    fun observeWords(): Flow<List<Word>> {
        return wordDao.observeAll().flowOn(Dispatchers.IO)
    }

    /** 一次性读取全部单词，用于下拉刷新。 */
    suspend fun getAllWords(): Result<List<Word>> = dbCall {
        wordDao.getAllOnce()
    }

    /** 新增单词。 */
    suspend fun insert(word: Word): Result<Long> = dbCall {
        wordDao.insert(word)
    }

    /** 批量新增单词。 */
    suspend fun insertAll(words: List<Word>): Result<List<Long>> = dbCall {
        wordDao.insertAll(words)
    }

    /** 修改单词。 */
    suspend fun update(word: Word): Result<Unit> = dbCall {
        wordDao.update(word)
    }

    /** 删除单词；关联复习记录/错题记录由数据库级联删除。 */
    suspend fun delete(word: Word): Result<Unit> = dbCall {
        wordDao.delete(word)
    }

    /** 按 ID 删除单词。 */
    suspend fun deleteById(wordId: Long): Result<Int> = dbCall {
        wordDao.deleteById(wordId)
    }

    /** 按 ID 查询单词。 */
    suspend fun findById(wordId: Long): Result<Word?> = dbCall {
        wordDao.findById(wordId)
    }

    /** 按拼写查询单词，用于判重。 */
    suspend fun findBySpelling(spelling: String): Result<Word?> = dbCall {
        wordDao.findBySpelling(spelling)
    }

    /**
     * 统一在 IO 线程执行数据库操作，并捕获异常。
     * CancellationException 必须继续抛出，否则协程取消会被误当成业务失败。
     */
    private suspend fun <T> dbCall(block: suspend () -> T): Result<T> {
        return withContext(Dispatchers.IO) {
            try {
                Result.success(block())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
