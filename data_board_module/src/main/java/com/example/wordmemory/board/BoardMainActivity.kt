package com.example.wordmemory.board

import android.graphics.Color
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.wordmemory.board.databinding.ActivityBoardMainBinding
import com.example.wordmemory.board.databinding.ItemBoardLevelBinding
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
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

        configureBarChart(binding.barChart)
        configurePieChart(binding.pieChart)

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
        renderCharts(state)
    }

    private fun configureBarChart(chart: BarChart) {
        chart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setDrawValueAboveBar(true)
            setScaleEnabled(false)
            setPinchZoom(false)

            axisRight.isEnabled = false
            axisLeft.apply {
                axisMinimum = 0f
                granularity = 1f
                textColor = ContextCompat.getColor(context, R.color.board_text_secondary)
                gridColor = ContextCompat.getColor(context, R.color.board_divider)
                axisLineColor = ContextCompat.getColor(context, R.color.board_divider)
            }
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textColor = ContextCompat.getColor(context, R.color.board_text_secondary)
                textSize = 9f
                labelRotationAngle = -35f
                axisLineColor = ContextCompat.getColor(context, R.color.board_divider)
                setAvoidFirstLastClipping(true)
            }
            setExtraOffsets(4f, 12f, 4f, 20f)
        }
    }

    private fun configurePieChart(chart: PieChart) {
        chart.apply {
            description.isEnabled = false
            setUsePercentValues(true)
            setDrawHoleEnabled(true)
            holeRadius = 55f
            transparentCircleRadius = 60f
            setHoleColor(Color.TRANSPARENT)
            setCenterText("记忆等级")
            setCenterTextSize(12f)
            setCenterTextColor(ContextCompat.getColor(context, R.color.board_text))
            setEntryLabelColor(ContextCompat.getColor(context, R.color.board_text))
            setEntryLabelTextSize(10f)
            setExtraOffsets(8f, 8f, 8f, 8f)

            legend.apply {
                isEnabled = true
                textColor = ContextCompat.getColor(context, R.color.board_text_secondary)
                textSize = 10f
                form = Legend.LegendForm.CIRCLE
                orientation = Legend.LegendOrientation.HORIZONTAL
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                isWordWrapEnabled = false
                xEntrySpace = 10f
                yEntrySpace = 0f
            }
        }
    }

    private fun renderCharts(state: BoardUiState) {
        val hasBarData = state.barEntries.isNotEmpty()
        binding.barChart.isVisible = hasBarData
        binding.tvBarEmpty.isVisible = !hasBarData

        if (hasBarData) {
            val barDataSet = BarDataSet(state.barEntries, "复习次数").apply {
                colors = chartColors()
                valueTextColor = ContextCompat.getColor(this@BoardMainActivity, R.color.board_text)
                valueTextSize = 10f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return value.toInt().toString()
                    }
                }
            }
            binding.barChart.xAxis.valueFormatter = IndexAxisValueFormatter(state.barLabels)
            binding.barChart.xAxis.labelCount = state.barLabels.size
            binding.barChart.data = BarData(barDataSet).apply {
                barWidth = 0.55f
            }
            binding.barChart.setFitBars(true)
            binding.barChart.invalidate()
            binding.barChart.animateY(450)
        }

        val hasPieData = state.pieEntries.isNotEmpty()
        binding.pieChart.isVisible = hasPieData
        binding.tvPieEmpty.isVisible = !hasPieData

        if (hasPieData) {
            val pieDataSet = PieDataSet(state.pieEntries, "").apply {
                colors = chartColors()
                setDrawValues(true)
                valueTextColor = Color.WHITE
                valueTextSize = 10f
                valueFormatter = PercentFormatter(binding.pieChart)
            }
            binding.pieChart.data = PieData(pieDataSet).apply {
                setValueTextColor(Color.WHITE)
            }
            binding.pieChart.highlightValues(null)
            binding.pieChart.invalidate()
            binding.pieChart.animateY(450)
        }
    }

    /** 蓝色 + 橙色交替配色，不使用绿色。 */
    private fun chartColors(): List<Int> {
        return listOf(
            R.color.board_blue,
            R.color.board_orange,
            R.color.board_blue_light,
            R.color.board_orange_light,
            R.color.board_blue_dark,
            R.color.board_orange_dark,
            R.color.board_blue,
            R.color.board_orange,
            R.color.board_blue_light,
        ).map { ContextCompat.getColor(this, it) }
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
