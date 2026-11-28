package com.example.wordmemory.ui.wrongbook

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.wordmemory.data.local.AppDatabase
import com.example.wordmemory.data.local.model.WrongWordItem
import com.example.wordmemory.data.repository.WrongBookRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WrongDetailUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val item: WrongWordItem? = null,
    val isRemoving: Boolean = false,
    val removed: Boolean = false,
)

class WrongDetailViewModel(
    private val repository: WrongBookRepository,
    private val wordId: Long,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WrongDetailUiState())
    val uiState: StateFlow<WrongDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private var detailJob: Job? = null

    init {
        observeWrongWord()
    }

    private fun observeWrongWord() {
        detailJob?.cancel()
        detailJob = repository.observeWrongWord(wordId)
            .onEach { item ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = null,
                        item = item,
                    )
                }
            }
            .catch { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "错题详情加载失败：${throwable.localizedMessage ?: "未知错误"}",
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun retry() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        observeWrongWord()
    }

    fun removeFromWrongBook() {
        val item = _uiState.value.item ?: return
        if (_uiState.value.isRemoving) return

        _uiState.update { it.copy(isRemoving = true) }
        viewModelScope.launch {
            repository.removeWord(item.wordId)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isRemoving = false,
                            removed = true,
                        )
                    }
                    _events.emit("已从错题本移除")
                }
                .onFailure {
                    _uiState.update { it.copy(isRemoving = false) }
                    _events.emit("移除失败，请重试")
                }
        }
    }

    companion object {
        fun factory(context: Context, wordId: Long): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getInstance(context.applicationContext)
                    return WrongDetailViewModel(WrongBookRepository(db), wordId) as T
                }
            }
        }
    }
}
