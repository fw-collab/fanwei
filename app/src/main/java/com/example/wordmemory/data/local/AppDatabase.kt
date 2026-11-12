package com.example.wordmemory.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.wordmemory.data.local.dao.ReviewRecordDao
import com.example.wordmemory.data.local.dao.WordDao
import com.example.wordmemory.data.local.dao.WrongRecordDao
import com.example.wordmemory.data.local.entity.ReviewRecord
import com.example.wordmemory.data.local.entity.Word
import com.example.wordmemory.data.local.entity.WrongRecord

/**
 * 单词记忆 App 的 Room 数据库。
 *
 * 当前版本只包含三张表：
 * - words：单词
 * - review_records：复习记录
 * - wrong_records：错题记录
 *
 * 后续 Repository 只依赖 DAO，不直接操作 RoomDatabase。
 * 数据库升级时在 [RoomDatabase.Builder.addMigrations] 中注册迁移。
 */
@Database(
    entities = [Word::class, ReviewRecord::class, WrongRecord::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun wordDao(): WordDao

    abstract fun reviewRecordDao(): ReviewRecordDao

    abstract fun wrongRecordDao(): WrongRecordDao

    companion object {
        private const val DATABASE_NAME = "word_memory.db"

        @Volatile
        private var instance: AppDatabase? = null

        /** 获取单例数据库；Context 只使用 applicationContext，避免持有 Activity。 */
        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME,
                ).build().also { instance = it }
            }
        }
    }
}
