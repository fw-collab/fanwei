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

data class WrongBookUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val items: List<WrongWordItem> = emptyList(),
)

class WrongBookViewModel(
    private val repository: WrongBookRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WrongBookUiState())
    val uiState: StateFlow<WrongBookUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private var listJob: Job? = null

    init {
        observeWrongWords()
    }

    private fun observeWrongWords() {
        listJob?.cancel()
        listJob = repository.observeWrongWords()
            .onEach { items ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = null,
                        items = items,
                    )
                }
            }
            .catch { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "错题加载失败：${throwable.localizedMessage ?: "未知错误"}",
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun retry() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        observeWrongWords()
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getInstance(context.applicationContext)
                    return WrongBookViewModel(WrongBookRepository(db)) as T
                }
            }
        }
    }
}
