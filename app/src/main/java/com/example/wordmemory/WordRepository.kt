package com.example.wordmemory

import android.content.Context

object WordRepository {

    private const val PREFS = "word_memory"
    private const val KEY_LEARNED = "learned_words"

    val words: List<Word> = listOf(
        Word("abandon", "/əˈbændən/", "v. 放弃；抛弃"),
        Word("ability", "/əˈbɪləti/", "n. 能力；才能"),
        Word("absorb", "/əbˈzɔːrb/", "v. 吸收；理解"),
        Word("accurate", "/ˈækjərət/", "adj. 准确的；精确的"),
        Word("achieve", "/əˈtʃiːv/", "v. 实现；达到"),
        Word("adapt", "/əˈdæpt/", "v. 适应；改编"),
        Word("admire", "/ədˈmaɪər/", "v. 钦佩；欣赏"),
        Word("advance", "/ədˈvæns/", "v. 前进；促进"),
        Word("affect", "/əˈfekt/", "v. 影响；作用"),
        Word("approach", "/əˈproʊtʃ/", "v. 接近；n. 方法"),
    )

    fun learned(context: Context): Set<String> {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_LEARNED, emptySet())
            ?: emptySet()
    }

    fun setLearned(context: Context, spelling: String, learned: Boolean) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val values = prefs.getStringSet(KEY_LEARNED, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (learned) values.add(spelling) else values.remove(spelling)
        prefs.edit().putStringSet(KEY_LEARNED, values).apply()
    }
}
