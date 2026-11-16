package com.example.wordmemory.ui.wrongbook

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.wordmemory.R
import com.example.wordmemory.data.local.model.WrongWordItem
import com.example.wordmemory.databinding.ItemWrongWordBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 错题本列表适配器。
 * 点击条目进入详情页，移除操作放在详情页完成。
 */
class WrongBookAdapter(
    private val onClick: (WrongWordItem) -> Unit,
) : ListAdapter<WrongWordItem, WrongBookAdapter.WrongViewHolder>(DiffCallback) {

    private val timeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WrongViewHolder {
        val binding = ItemWrongWordBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return WrongViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WrongViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class WrongViewHolder(
        private val binding: ItemWrongWordBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: WrongWordItem) {
            binding.tvWord.text = item.spelling
            binding.tvPhonetic.text = item.phonetic.ifBlank { "—" }
            binding.tvMeaning.text = item.meaning
            binding.tvWrongCount.text = binding.root.context.getString(
                R.string.wrong_count_badge,
                item.wrongCount,
            )
            binding.tvLastWrong.text = binding.root.context.getString(
                R.string.wrong_last_time,
                timeFormatter.format(Date(item.lastWrongAt)),
            )
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<WrongWordItem>() {
        override fun areItemsTheSame(oldItem: WrongWordItem, newItem: WrongWordItem): Boolean {
            return oldItem.wordId == newItem.wordId
        }

        override fun areContentsTheSame(oldItem: WrongWordItem, newItem: WrongWordItem): Boolean {
            return oldItem == newItem
        }
    }
}
