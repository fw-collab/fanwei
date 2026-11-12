package com.example.wordmemory

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wordmemory.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: WordAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = WordAdapter(WordRepository.learned(this))
        binding.wordList.layoutManager = LinearLayoutManager(this)
        binding.wordList.adapter = adapter
        adapter.submitWords(WordRepository.words)

        binding.btnStart.setOnClickListener {
            startActivity(Intent(this, StudyActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshProgress()
    }

    private fun refreshProgress() {
        val learned = WordRepository.learned(this)
        adapter.updateLearned(learned)

        val total = WordRepository.words.size
        val done = WordRepository.words.count { learned.contains(it.spelling) }
        binding.progressBar.max = total
        binding.progressBar.progress = done
        binding.tvProgress.text = getString(R.string.progress_format, done, total)
    }
}
