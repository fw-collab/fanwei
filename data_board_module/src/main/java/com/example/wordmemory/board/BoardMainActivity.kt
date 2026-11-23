package com.example.wordmemory.board

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.wordmemory.board.databinding.ActivityBoardMainBinding
import com.example.wordmemory.board.databinding.ItemBoardLevelBinding
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * P2 数据看板的独立入口。
 *
 * 它在 :data_board_module 中声明为 LAUNCHER Activity，点击桌面“数据看板”即可单独打开。
 * 该模块被主 App 依赖，最终运行在同一个 APK / 同一进程，因此可以直接复用主 App 的 Room。
 */
class BoardMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBoardMainBinding

    private val viewModel: BoardViewModel by viewModels {
        BoardViewModel.factory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBoardMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRefresh.setOnClickListener { viewModel.refresh() }
        binding.btnRetry.setOnClickListener { viewModel.refresh() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 从主 App 切回来时重新读取，保证看板数据是最新的
        viewModel.refresh()
    }

    private fun render(state: BoardUiState) {
        binding.loadingContainer.isVisible = state.isLoading
        binding.contentContainer.isVisible =
            !state.isLoading && state.errorMessage == null && !state.isEmpty
        binding.emptyContainer.isVisible = state.isEmpty
        binding.errorContainer.isVisible = !state.isLoading && state.errorMessage != null
        binding.btnRefresh.isEnabled = !state.isLoading

        state.errorMessage?.let { binding.tvError.text = it }

        if (state.isLoading || state.errorMessage != null || state.isEmpty) return

        binding.tvTotalWords.text = state.totalWords.toString()
        binding.tvTodayReviews.text = state.todayReviews.toString()
        binding.tvWrongTotal.text = state.wrongTotal.toString()
        binding.tvAccuracy.text = state.accuracy?.let { accuracy ->
            "${(accuracy * 100).roundToInt()}%"
        } ?: getString(R.string.board_no_accuracy)

        renderLevels(state)
    }

    private fun renderLevels(state: BoardUiState) {
        binding.levelContainer.removeAllViews()
        val total = state.totalWords.coerceAtLeast(1)

        for (level in 0..8) {
            val row = ItemBoardLevelBinding.inflate(
                layoutInflater,
                binding.levelContainer,
                false,
            )
            val count = state.levelCounts[level] ?: 0
            row.tvLevelName.text = getString(R.string.board_level_format, level)
            row.tvLevelCount.text = getString(R.string.board_level_count, count)
            row.progressLevel.progress = (count * 100 / total).coerceIn(0, 100)
            binding.levelContainer.addView(row.root)
        }
    }
}
