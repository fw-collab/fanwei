package com.example.wordmemory.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 复习记录表。
 *
 * 每次用户完成一次单词复习就插入一条记录，用于统计复习量、正确率和学习趋势。
 * 一条 ReviewRecord 必须属于一个已存在的 Word。
 */
@Entity(
    tableName = "review_records",
    foreignKeys = [
        ForeignKey(
            entity = Word::class,
            parentColumns = ["id"],
            childColumns = ["word_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["word_id"]),
        Index(value = ["reviewed_at"]),
    ],
)
data class ReviewRecord(
    /** 主键，自增。 */
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    /** 外键：对应的单词 ID。 */
    @ColumnInfo(name = "word_id")
    val wordId: Long,

    /** 复习时间，Unix 毫秒时间戳。 */
    @ColumnInfo(name = "reviewed_at")
    val reviewedAt: Long = System.currentTimeMillis(),

    /** 本次复习是否答对。 */
    @ColumnInfo(name = "result")
    val result: Boolean,

    /** 回答质量，0–5，数值越高表示越熟悉；后续复习算法会使用。 */
    @ColumnInfo(name = "quality")
    val quality: Int,

    /** 本次复习耗时，单位秒；没有计时则为 0。 */
    @ColumnInfo(name = "duration_seconds", defaultValue = "0")
    val durationSeconds: Int = 0,
)
