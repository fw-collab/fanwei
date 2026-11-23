package com.example.wordmemory.board

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.wordmemory.board.data.BoardRepository
import com.example.wordmemory.data.local.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BoardUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val totalWords: Int = 0,
    val todayReviews: Int = 0,
    val accuracy: Double? = null,
    val wrongTotal: Int = 0,
    val levelCounts: Map<Int, Int> = emptyMap(),
) {
    val isEmpty: Boolean
        get() = !isLoading && errorMessage == null && totalWords == 0
}

/**
 * 数据看板 ViewModel。
 *
 * 读取主 App 的 Room 数据库，不写入数据。
 */
class BoardViewModel(
    private val repository: BoardRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BoardUiState())
    val uiState: StateFlow<BoardUiState> = _uiState.asStateFlow()

    private var loading = false

    init {
        refresh()
    }

    fun refresh() {
        if (loading) return
        loading = true
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            repository.loadDashboard()
                .onSuccess { dashboard ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            totalWords = dashboard.totalWords,
                            todayReviews = dashboard.todayReviews,
                            accuracy = dashboard.accuracy,
                            wrongTotal = dashboard.wrongTotal,
                            levelCounts = dashboard.levelCounts,
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.localizedMessage ?: "数据库读取失败",
                        )
                    }
                }
            loading = false
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getInstance(context.applicationContext)
                    return BoardViewModel(BoardRepository(db)) as T
                }
            }
        }
    }
}
