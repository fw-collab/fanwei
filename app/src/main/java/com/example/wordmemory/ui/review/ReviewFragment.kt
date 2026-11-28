package com.example.wordmemory.ui.review

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.wordmemory.R
import com.example.wordmemory.core.ui.ToastUtils
import com.example.wordmemory.databinding.FragmentReviewBinding
import kotlinx.coroutines.launch

/**
 * 复习卡片页。
 *
 * 页面只负责：
 * - 展示当前队列的第一个单词；
 * - 点击卡片做翻转动画；
 * - 点击记住/忘记，把结果交给 ViewModel；
 * - 根据队列状态切换加载态/卡片/完成空态。
 *
 * 算法、数据库写入、错题写入都在 ViewModel / Repository 中完成。
 */
class ReviewFragment : Fragment() {

    private var _binding: FragmentReviewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReviewViewModel by viewModels {
        ReviewViewModel.factory(requireContext())
    }

    private var currentWordId: Long? = null
    private var isFlipped = false
    private var isAnimating = false
    private var isSubmitting = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentReviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cardReview.setOnClickListener { flipCard() }
        binding.btnRemember.setOnClickListener { viewModel.submit(correct = true) }
        binding.btnForget.setOnClickListener { viewModel.submit(correct = false) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state -> render(state) }
                }
                launch {
                    viewModel.events.collect { message ->
                        ToastUtils.show(requireContext(), message)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 从其他 Tab 返回时，重新按“当前时间”查询到期队列
        viewModel.refreshQueue()
    }

    private fun render(state: ReviewUiState) {
        val word = state.queue.firstOrNull()
        isSubmitting = state.isSubmitting

        binding.tvRemaining.text = getString(R.string.review_remaining, state.queue.size)
        binding.btnRemember.isEnabled = word != null && !state.isSubmitting && state.errorMessage == null
        binding.btnForget.isEnabled = word != null && !state.isSubmitting && state.errorMessage == null

        when {
            state.errorMessage != null -> {
                binding.stateLayout.showError(state.errorMessage) {
                    viewModel.refreshQueue()
                }
            }

            state.isLoading && word == null -> {
                binding.stateLayout.showLoading()
            }

            word == null -> {
                binding.stateLayout.showEmpty(
                    title = getString(R.string.review_complete_title),
                    message = getString(R.string.review_complete_desc),
                )
            }

            else -> {
                binding.stateLayout.showContent()
            }
        }

        if (word == null) {
            currentWordId = null
            return
        }

        // 只有换到新单词时才重置翻转状态，提交中的状态变化不会把卡片翻回去
        if (currentWordId != word.id) {
            currentWordId = word.id
            resetCard()
            binding.tvWord.text = word.spelling
            binding.tvBackWord.text = word.spelling
            binding.tvPhonetic.text = word.phonetic.ifBlank { "—" }
            binding.tvMeaning.text = word.meaning
        }
    }

    /**
     * 翻转动画：
     * 0° → 90°（正面转走）→ 切换正反面 → -90° → 0°（反面转回来）。
     * 这样避免直接 visibility 切换显得生硬。
     */
    private fun flipCard() {
        if (isAnimating || isSubmitting || currentWordId == null) return
        isAnimating = true

        binding.cardReview.animate()
            .rotationY(90f)
            .setDuration(150)
            .withEndAction {
                isFlipped = !isFlipped
                binding.frontContent.isVisible = !isFlipped
                binding.backContent.isVisible = isFlipped
                binding.cardReview.rotationY = -90f
                binding.cardReview.animate()
                    .rotationY(0f)
                    .setDuration(150)
                    .withEndAction { isAnimating = false }
                    .start()
            }
            .start()
    }

    /** 换到下一个单词时，取消动画并回到正面。 */
    private fun resetCard() {
        binding.cardReview.animate().cancel()
        binding.cardReview.rotationY = 0f
        isFlipped = false
        isAnimating = false
        binding.frontContent.isVisible = true
        binding.backContent.isVisible = false
    }

    override fun onDestroyView() {
        binding.cardReview.animate().cancel()
        _binding = null
        super.onDestroyView()
    }
}
