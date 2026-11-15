package com.example.wordmemory.ui.review

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.wordmemory.data.local.AppDatabase
import com.example.wordmemory.data.local.entity.Word
import com.example.wordmemory.data.repository.ReviewRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 复习页状态。 */
data class ReviewUiState(
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val queue: List<Word> = emptyList(),
    val reviewedCount: Int = 0,
    val rememberedCount: Int = 0,
    val forgottenCount: Int = 0,
)

class ReviewViewModel(
    private val repository: ReviewRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val events: SharedFlow<String> = _events.asSharedFlow()

    /**
     * 用查询时间驱动队列 Flow。
     * 每次提交后把时间推进到“现在”，重新查一次到期队列，
     * 这样复习过程中新到期的单词也能被纳入。
     */
    private val queryTime = MutableStateFlow(System.currentTimeMillis())

    init {
        observeQueue()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeQueue() {
        queryTime
            .flatMapLatest { now -> repository.observeDueWords(now) }
            .onEach { words ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        queue = words,
                    )
                }
            }
            .catch { throwable ->
                _uiState.update { it.copy(isLoading = false) }
                _events.emit("复习队列加载失败：${throwable.localizedMessage ?: "未知错误"}")
            }
            .launchIn(viewModelScope)
    }

    /**
     * 提交当前卡片。
     * correct = true → 记住，升级；
     * correct = false → 忘记，重置并写错题。
     */
    fun submit(correct: Boolean) {
        val word = _uiState.value.queue.firstOrNull() ?: return
        if (_uiState.value.isSubmitting) return

        _uiState.update { it.copy(isSubmitting = true) }

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repository.submitReview(word, correct, now)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            isSubmitting = false,
                            // 乐观移除当前卡片，数据库 Flow 随后会给出最终队列
                            queue = state.queue.filterNot { it.id == word.id },
                            reviewedCount = state.reviewedCount + 1,
                            rememberedCount = state.rememberedCount + if (correct) 1 else 0,
                            forgottenCount = state.forgottenCount + if (correct) 0 else 1,
                        )
                    }
                    _events.emit(
                        if (correct) "已记住 ${word.spelling}" else "已把 ${word.spelling} 加入错题本"
                    )
                    // 重新计算“现在”，让后续到期的单词进入队列
                    queryTime.value = System.currentTimeMillis()
                }
                .onFailure {
                    _uiState.update { state -> state.copy(isSubmitting = false) }
                    _events.emit("保存失败，请重试")
                }
        }
    }

    /** 页面重新可见时手动刷新一次队列。 */
    fun refreshQueue() {
        queryTime.value = System.currentTimeMillis()
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getInstance(context.applicationContext)
                    return ReviewViewModel(ReviewRepository(db)) as T
                }
            }
        }
    }
}
