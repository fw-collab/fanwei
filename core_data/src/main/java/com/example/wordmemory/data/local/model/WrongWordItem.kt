package com.example.wordmemory.data.local.model

/**
 * 错题本列表/详情使用的投影模型。
 *
 * 错题表按“每次答错一条记录”存储，所以列表需要按 word_id 聚合：
 * - wrongCount：这个单词累计答错次数；
 * - lastWrongAt：最近一次答错时间。
 */
data class WrongWordItem(
    val wordId: Long,
    val spelling: String,
    val phonetic: String,
    val meaning: String,
    val wrongCount: Int,
    val lastWrongAt: Long,
)
