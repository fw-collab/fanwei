package com.example.wordmemory.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 错题记录表。
 *
 * 每次答错就插入一条记录，保留用户答案和正确答案，方便错题本和难词榜使用。
 * 一条 WrongRecord 必须属于一个已存在的 Word。
 */
@Entity(
    tableName = "wrong_records",
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
        Index(value = ["wrong_at"]),
    ],
)
data class WrongRecord(
    /** 主键，自增。 */
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    /** 外键：对应的单词 ID。 */
    @ColumnInfo(name = "word_id")
    val wordId: Long,

    /** 答错时间，Unix 毫秒时间戳。 */
    @ColumnInfo(name = "wrong_at")
    val wrongAt: Long = System.currentTimeMillis(),

    /** 用户答案；选择题可存选项，拼写题可存拼写。 */
    @ColumnInfo(name = "user_answer")
    val userAnswer: String? = null,

    /** 正确答案。 */
    @ColumnInfo(name = "correct_answer")
    val correctAnswer: String? = null,

    /** 题型：CHOICE（选择）或 SPELLING（拼写）。 */
    @ColumnInfo(name = "question_type")
    val questionType: String? = null,
)
