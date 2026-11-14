package com.example.wordmemory.ui.wordlist

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.wordmemory.data.local.AppDatabase
import com.example.wordmemory.data.local.entity.Word
import com.example.wordmemory.data.repository.WordRepository
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

/** 单词列表页面状态。 */
data class WordListUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val words: List<Word> = emptyList(),
)

class WordListViewModel(
    private val repository: WordRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WordListUiState())
    val uiState: StateFlow<WordListUiState> = _uiState.asStateFlow()

    /** 一次性提示：新增成功、删除成功、重复单词、数据库异常等。 */
    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        observeWords()
    }

    private fun observeWords() {
        repository.observeWords()
            .onEach { words ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        words = words,
                    )
                }
            }
            .catch { throwable ->
                _uiState.update { it.copy(isLoading = false) }
                _events.emit(friendlyMessage(throwable))
            }
            .launchIn(viewModelScope)
    }

    /** 下拉刷新：重新读一次数据库快照，Flow 仍会继续监听后续变化。 */
    fun refresh() {
        if (_uiState.value.isRefreshing) return
        _uiState.update { it.copy(isRefreshing = true) }

        viewModelScope.launch {
            repository.getAllWords()
                .onSuccess { words ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            words = words,
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(isRefreshing = false) }
                    _events.emit(friendlyMessage(throwable))
                }
        }
    }

    fun addWord(spelling: String, phonetic: String, meaning: String) {
        val cleanSpelling = spelling.trim()
        val cleanPhonetic = phonetic.trim()
        val cleanMeaning = meaning.trim()

        if (cleanSpelling.isEmpty() || cleanMeaning.isEmpty()) {
            _events.tryEmit("请填写英文单词和释义")
            return
        }

        viewModelScope.launch {
            repository.insert(
                Word(
                    spelling = cleanSpelling,
                    phonetic = cleanPhonetic,
                    meaning = cleanMeaning,
                )
            ).onSuccess {
                _events.emit("已添加 $cleanSpelling")
            }.onFailure { throwable ->
                _events.emit(friendlyMessage(throwable))
            }
        }
    }

    fun updateWord(word: Word) {
        if (word.spelling.isBlank() || word.meaning.isBlank()) {
            _events.tryEmit("请填写英文单词和释义")
            return
        }

        viewModelScope.launch {
            repository.update(word)
                .onSuccess { _events.emit("已保存修改") }
                .onFailure { throwable -> _events.emit(friendlyMessage(throwable)) }
        }
    }

    fun deleteWord(word: Word) {
        viewModelScope.launch {
            repository.delete(word)
                .onSuccess { _events.emit("已删除 ${word.spelling}") }
                .onFailure { throwable -> _events.emit(friendlyMessage(throwable)) }
        }
    }

    private fun friendlyMessage(throwable: Throwable): String {
        return when (throwable) {
            is SQLiteConstraintException -> "这个单词已经在单词本里了"
            is IllegalArgumentException -> throwable.message ?: "输入内容不合法"
            else -> "操作失败，请重试"
        }
    }

    companion object {
        /** 简单工厂：创建 Room DAO → Repository → ViewModel。 */
        fun factory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val dao = AppDatabase.getInstance(context.applicationContext).wordDao()
                    return WordListViewModel(WordRepository(dao)) as T
                }
            }
        }
    }
}
