package com.example.wordmemory.ui.wordlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.wordmemory.R
import com.example.wordmemory.data.local.entity.Word
import com.example.wordmemory.databinding.ItemWordBinding

/**
 * 单词本列表适配器。
 *
 * 点击条目进入编辑，长按条目删除；具体行为由 Fragment 回调处理。
 */
class WordListAdapter(
    private val onEdit: (Word) -> Unit,
    private val onDelete: (Word) -> Unit,
) : ListAdapter<Word, WordListAdapter.WordViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
        val binding = ItemWordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class WordViewHolder(
        private val binding: ItemWordBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(word: Word) {
            binding.tvWord.text = word.spelling
            binding.tvPhonetic.text = word.phonetic.ifBlank { "—" }
            binding.tvMeaning.text = word.meaning
            binding.tvStatus.text = binding.root.context.getString(
                when (word.masteryLevel) {
                    2 -> R.string.status_learned
                    1 -> R.string.status_learning
                    else -> R.string.status_new
                }
            )

            binding.root.setOnClickListener { onEdit(word) }
            binding.root.setOnLongClickListener {
                onDelete(word)
                true
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Word>() {
        override fun areItemsTheSame(oldItem: Word, newItem: Word): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Word, newItem: Word): Boolean {
            return oldItem == newItem
        }
    }
}
