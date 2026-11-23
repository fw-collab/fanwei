package com.example.wordmemory.board

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.wordmemory.board.data.BoardRepository
import com.example.wordmemory.board.export.CsvExportManager
import com.example.wordmemory.data.local.AppDatabase
import com.example.wordmemory.data.local.entity.ReviewRecord
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class BoardUiState(
    val isLoading: Boolean = true,
    val isExporting: Boolean = false,
    val errorMessage: String? = null,
    val totalWords: Int = 0,
    val todayReviews: Int = 0,
    val accuracy: Double? = null,
    val wrongTotal: Int = 0,
    val levelCounts: Map<Int, Int> = emptyMap(),
    val barEntries: List<BarEntry> = emptyList(),
    val barLabels: List<String> = emptyList(),
    val pieEntries: List<PieEntry> = emptyList(),
) {
    val isEmpty: Boolean
        get() = !isLoading && errorMessage == null && totalWords == 0
}

sealed interface CsvExportEvent {
    data class Success(
        val uri: Uri,
        val recordCount: Int,
        val location: String,
    ) : CsvExportEvent

    data class Failure(val message: String) : CsvExportEvent
}

/**
 * 数据看板 ViewModel。
 *
 * 读取主 App 的 Room 数据库，不写入数据；CSV 导出也在这里触发。
 */
class BoardViewModel(
    private val repository: BoardRepository,
    private val csvExportManager: CsvExportManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BoardUiState())
    val uiState: StateFlow<BoardUiState> = _uiState.asStateFlow()

    private val _exportEvents = MutableSharedFlow<CsvExportEvent>(extraBufferCapacity = 1)
    val exportEvents: SharedFlow<CsvExportEvent> = _exportEvents.asSharedFlow()

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
                    val (barEntries, barLabels) = buildBarData(dashboard.reviewsLast7Days)
                    val pieEntries = buildPieEntries(dashboard.levelCounts)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            totalWords = dashboard.totalWords,
                            todayReviews = dashboard.todayReviews,
                            accuracy = dashboard.accuracy,
                            wrongTotal = dashboard.wrongTotal,
                            levelCounts = dashboard.levelCounts,
                            barEntries = barEntries,
                            barLabels = barLabels,
                            pieEntries = pieEntries,
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

    /**
     * 把近 7 天复习记录聚合成柱状图数据。
     * 没有复习记录时返回空列表，让页面显示图表空态。
     */
    private fun buildBarData(reviews: List<ReviewRecord>): Pair<List<BarEntry>, List<String>> {
        if (reviews.isEmpty()) return emptyList<BarEntry>() to emptyList()

        val dayKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val labelFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
        val countsByDay = reviews
            .groupingBy { dayKeyFormat.format(Date(it.reviewedAt)) }
            .eachCount()

        if (countsByDay.isEmpty()) return emptyList<BarEntry>() to emptyList()

        val today = Calendar.getInstance()
        val entries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()

        for (offset in 6 downTo 0) {
            val day = (today.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_YEAR, -offset)
            }
            val key = dayKeyFormat.format(day.time)
            val count = countsByDay[key] ?: 0

            labels.add(labelFormat.format(day.time))
            entries.add(BarEntry((6 - offset).toFloat(), count.toFloat()))
        }

        // 7 天内有记录但都是 0 的情况不会发生；这里仍以非空 counts 判断。
        return entries to labels
    }

    /** 把记忆等级分布转换成饼图 Entry；0 级叫“新学”，8 级叫“熟词”。 */
    private fun buildPieEntries(levelCounts: Map<Int, Int>): List<PieEntry> {
        return (0..8).mapNotNull { level ->
            val count = levelCounts[level] ?: 0
            if (count <= 0) {
                null
            } else {
                PieEntry(count.toFloat(), levelLabel(level))
            }
        }
    }

    private fun levelLabel(level: Int): String {
        return when (level) {
            0 -> "新学"
            8 -> "熟词"
            else -> "${level}级"
        }
    }

    /** 导出复习记录 CSV。查询、文件写入都在 IO 线程执行。 */
    fun exportCsv() {
        if (_uiState.value.isExporting) return
        _uiState.update { it.copy(isExporting = true) }

        viewModelScope.launch {
            try {
                repository.getAllReviewRecords()
                    .onSuccess { records ->
                        csvExportManager.export(records)
                            .onSuccess { info ->
                                _exportEvents.emit(
                                    CsvExportEvent.Success(
                                        uri = info.uri,
                                        recordCount = info.recordCount,
                                        location = info.location,
                                    )
                                )
                            }
                            .onFailure { throwable ->
                                _exportEvents.emit(
                                    CsvExportEvent.Failure(
                                        throwable.localizedMessage ?: "IO 异常，导出失败"
                                    )
                                )
                            }
                    }
                    .onFailure { throwable ->
                        _exportEvents.emit(
                            CsvExportEvent.Failure(
                                throwable.localizedMessage ?: "读取复习记录失败"
                            )
                        )
                    }
            } finally {
                _uiState.update { it.copy(isExporting = false) }
            }
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getInstance(context.applicationContext)
                    return BoardViewModel(
                        repository = BoardRepository(db),
                        csvExportManager = CsvExportManager(context.applicationContext),
                    ) as T
                }
            }
        }
    }
}
