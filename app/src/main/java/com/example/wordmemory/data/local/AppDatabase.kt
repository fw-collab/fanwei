package com.example.wordmemory.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun wordDao(): WordDao

    abstract fun reviewRecordDao(): ReviewRecordDao

    abstract fun wrongRecordDao(): WrongRecordDao

    companion object {
        private const val DATABASE_NAME = "word_memory.db"

        /**
         * v1 → v2：
         * - 给 words 增加艾宾浩斯记忆等级 memory_level；
         * - 给 words 增加下次复习时间 next_review_time；
         * - 为待复习队列增加时间索引。
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE words ADD COLUMN memory_level INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE words ADD COLUMN next_review_time INTEGER"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_words_next_review_time " +
                        "ON words(next_review_time)"
                )
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        /** 获取单例数据库；Context 只使用 applicationContext，避免持有 Activity。 */
        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME,
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
