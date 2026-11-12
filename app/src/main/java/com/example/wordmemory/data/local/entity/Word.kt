package com.example.wordmemory.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 单词表。
 *
 * 关系：
 * - 一个 Word 可以对应多条 ReviewRecord；
 * - 一个 Word 可以对应多条 WrongRecord；
 * - 删除 Word 时，其复习记录和错题记录会级联删除。
 */
@Entity(
    tableName = "words",
    indices = [
        // 同一个单词只保留一条，避免重复导入
        Index(value = ["spelling"], unique = true),
        Index(value = ["created_at"]),
    ],
)
data class Word(
    /** 主键，自增；插入时不传或传 0 即可。 */
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    /** 单词拼写，例如 abandon。使用 NOCASE 让 Abandon 和 abandon 视为重复。 */
    @ColumnInfo(name = "spelling", collate = ColumnInfo.NOCASE)
    val spelling: String,

    /** 音标，例如 /əˈbændən/。 */
    @ColumnInfo(name = "phonetic")
    val phonetic: String = "",

    /** 中文释义，例如 v. 放弃；抛弃。 */
    @ColumnInfo(name = "meaning")
    val meaning: String,

    /** 例句，可为空。 */
    @ColumnInfo(name = "example")
    val example: String? = null,

    /**
     * 掌握程度：
     * 0 = 未学，1 = 学习中，2 = 已掌握。
     */
    @ColumnInfo(name = "mastery_level", defaultValue = "0")
    val masteryLevel: Int = 0,

    /** 创建时间，Unix 毫秒时间戳。 */
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
)
