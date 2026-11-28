package com.example.wordmemory

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.wordmemory.core.ui.ToastUtils
import com.example.wordmemory.databinding.ActivityStudyBinding

class StudyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudyBinding
    private val words = WordRepository.words
    private var index = 0
    private var revealed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cardWord.setOnClickListener { reveal() }
        binding.btnKnown.setOnClickListener { mark(true) }
        binding.btnUnknown.setOnClickListener { mark(false) }

        showCurrent()
    }

    private fun showCurrent() {
        if (index >= words.size) {
            finish()
            return
        }

        val word = words[index]
        binding.tvWord.text = word.spelling
        binding.tvPhonetic.text = word.phonetic
        binding.tvMeaning.text = word.meaning
        binding.tvMeaning.visibility = View.GONE
        binding.tvHint.visibility = View.VISIBLE
        binding.tvProgress.text = getString(R.string.progress_format, index + 1, words.size)
        revealed = false
    }

    private fun reveal() {
        if (revealed) return
        revealed = true
        binding.tvHint.visibility = View.GONE
        binding.tvMeaning.visibility = View.VISIBLE
    }

    private fun mark(learned: Boolean) {
        WordRepository.setLearned(this, words[index].spelling, learned)
        index++
        if (index >= words.size) {
            ToastUtils.show(this, R.string.round_finished)
            finish()
        } else {
            showCurrent()
        }
    }
}
