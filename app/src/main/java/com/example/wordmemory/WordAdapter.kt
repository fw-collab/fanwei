package com.example.wordmemory

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.wordmemory.databinding.ItemWordBinding

class WordAdapter(private var learned: Set<String>) :
    RecyclerView.Adapter<WordAdapter.WordViewHolder>() {

    private val items = mutableListOf<Word>()

    fun submitWords(words: List<Word>) {
        items.clear()
        items.addAll(words)
        notifyDataSetChanged()
    }

    fun updateLearned(learned: Set<String>) {
        this.learned = learned
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
        val binding = ItemWordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class WordViewHolder(private val binding: ItemWordBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(word: Word) {
            binding.tvWord.text = word.spelling
            binding.tvPhonetic.text = word.phonetic
            binding.tvMeaning.text = word.meaning
            val isLearned = learned.contains(word.spelling)
            binding.tvStatus.text = binding.root.context.getString(
                if (isLearned) R.string.status_learned else R.string.status_new
            )
        }
    }
}
