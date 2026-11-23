package com.example.wordmemory.ui.wordlist

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.wordmemory.data.local.AppDatabase
import com.example.wordmemory.data.local.dao.WordDao
import com.example.wordmemory.notification.NotificationHelper
import com.example.wordmemory.work.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReminderUiState(
    val dueCount: Int = 0,
    val notificationsEnabled: Boolean = true,
    val workerScheduled: Boolean = false,
)

/**
 * 首页提醒状态 ViewModel。
 *
 * 负责：
 * - 统计当前待复习数量；
 * - 保存提醒任务是否已经调度，避免页面重复调度；
 * - 在权限变化后刷新降级提示。
 */
class ReminderViewModel(
    private val appContext: Context,
    private val wordDao: WordDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ReminderUiState(
            notificationsEnabled = NotificationHelper.areNotificationsEnabled(appContext),
        )
    )
    val uiState: StateFlow<ReminderUiState> = _uiState.asStateFlow()

    private var workerScheduled = false

    init {
        observeDueCount()
    }

    private fun observeDueCount() {
        wordDao.observeAll()
            .onEach { words ->
                val now = System.currentTimeMillis()
                val due = words.count { word ->
                    val nextReviewTime = word.nextReviewTime
                    nextReviewTime != null && nextReviewTime <= now
                }
                _uiState.update {
                    it.copy(
                        dueCount = due,
                        notificationsEnabled = NotificationHelper.areNotificationsEnabled(appContext),
                        workerScheduled = workerScheduled,
                    )
                }
            }
            .catch { throwable ->
                _uiState.update {
                    it.copy(
                        notificationsEnabled = NotificationHelper.areNotificationsEnabled(appContext),
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * 页面创建时调用。
     * 已调度过就直接返回；即使进程重启，WorkManager 的唯一任务名也会兜底防重复。
     */
    fun ensureReminderScheduled() {
        if (workerScheduled) return
        workerScheduled = ReminderScheduler.ensureScheduled(appContext)
        _uiState.update { it.copy(workerScheduled = workerScheduled) }
    }

    /** 通知权限结果回调。 */
    fun onPermissionResult(granted: Boolean) {
        _uiState.update {
            it.copy(notificationsEnabled = granted)
        }
        if (granted) {
            ensureReminderScheduled()
        }
    }

    /** 页面重新可见时刷新待复习数量和权限状态。 */
    fun refresh() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val due = wordDao.getDueWords(now).size
            _uiState.update {
                it.copy(
                    dueCount = due,
                    notificationsEnabled = NotificationHelper.areNotificationsEnabled(appContext),
                    workerScheduled = workerScheduled,
                )
            }
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getInstance(context.applicationContext)
                    return ReminderViewModel(
                        appContext = context.applicationContext,
                        wordDao = db.wordDao(),
                    ) as T
                }
            }
        }
    }
}
