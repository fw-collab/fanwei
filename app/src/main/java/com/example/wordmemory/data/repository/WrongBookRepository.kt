package com.example.wordmemory.data.repository

import com.example.wordmemory.data.local.AppDatabase
import com.example.wordmemory.data.local.model.WrongWordItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * 错题本 Repository。
 *
 * 列表和详情都通过 Flow 监听 wrong_records 聚合结果；
 * 移除操作删除该单词对应的全部错题明细。
 */
class WrongBookRepository(private val db: AppDatabase) {

    private val wrongRecordDao = db.wrongRecordDao()

    /** 错题本列表：按单词聚合的错误次数和最近答错时间。 */
    fun observeWrongWords(): Flow<List<WrongWordItem>> {
        return wrongRecordDao.observeWrongWords().flowOn(Dispatchers.IO)
    }

    /** 详情页：监听单个单词的错题聚合。 */
    fun observeWrongWord(wordId: Long): Flow<WrongWordItem?> {
        return wrongRecordDao.observeWrongWord(wordId)
            .map { list -> list.firstOrNull() }
            .flowOn(Dispatchers.IO)
    }

    /** 从错题本移除一个单词的全部错题记录。 */
    suspend fun removeWord(wordId: Long): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                Result.success(wrongRecordDao.deleteByWordId(wordId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
