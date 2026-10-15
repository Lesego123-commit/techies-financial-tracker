package com.example.budgettracker.ui

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.budgettracker.data.CategoryTotal
import com.example.budgettracker.data.FirebaseRepository
import com.example.budgettracker.data.Goal
import com.example.budgettracker.databinding.ActivityGraphBinding
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.launch
import java.util.*

/**
 * GraphActivity – Part 3 required feature.
 *
 * Displays a bar chart (using the MPAndroidChart library) of the amount the user spent in
 * each category over a user-selectable period. Two horizontal limit lines are drawn on the
 * chart to show the user's MINIMUM and MAXIMUM monthly goals, so the user can immediately
 * see how their category spending compares to their budget targets.
 *
 * All data is read live from the online Firebase Realtime Database via [FirebaseRepository].
 */

/*
 * References:
 *  - Jahoda, P. (2021) MPAndroidChart [Software library]. Available at:
 *    https://github.com/PhilJay/MPAndroidChart (Accessed: 31 May 2026).
 *    The BarChart configuration, BarDataSet/BarData population and the LimitLine technique used
 *    below to draw the minimum/maximum goal lines are based on the MPAndroidChart wiki and docs.
 */
class GraphActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGraphBinding
    private var userId: String = ""

    private var startDate: String = ""
    private var endDate: String = ""

    companion object {
        private const val TAG = "GraphActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGraphBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Spending Graph"

        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
        userId = prefs.getString(LoginActivity.PREF_USER_UID, "") ?: ""
        if (userId.isEmpty()) { finish(); return }

        setupDefaultDates()
        setupChartAppearance()
        setupDatePickers()

        binding.btnFilter.setOnClickListener {
            if (startDate > endDate) {
                Toast.makeText(this, "Start date must be before end date", Toast.LENGTH_SHORT).show()
            } else {
                loadGraph()
            }
        }

        // Draw the graph immediately using the default (current month) date range.
        loadGraph()
    }

    /** Default the date range to the current calendar month. */
    private fun setupDefaultDates() {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        startDate = String.format("%04d-%02d-01", year, month)
        endDate = String.format("%04d-%02d-%02d", year, month, lastDay)
        binding.tvStartDate.text = startDate
        binding.tvEndDate.text = endDate
    }

    /** One-time styling of the bar chart. */
    private fun setupChartAppearance() {
        binding.barChart.apply {
            description.isEnabled = false
            setNoDataText("Loading…")
            setFitBars(true)
            axisRight.isEnabled = false
            legend.isEnabled = true
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            xAxis.setDrawGridLines(false)
            axisLeft.axisMinimum = 0f
            animateY(800)
        }
    }

    private fun setupDatePickers() {
        binding.btnPickStartDate.setOnClickListener {
            showDatePicker { startDate = it; binding.tvStartDate.text = it }
        }
        binding.btnPickEndDate.setOnClickListener {
            showDatePicker { endDate = it; binding.tvEndDate.text = it }
        }
    }

    /** Read the per-category totals and the user's goals from Firebase, then draw the chart. */
    private fun loadGraph() {
        Log.d(TAG, "Loading graph for period $startDate to $endDate")
        lifecycleScope.launch {
            val totals = FirebaseRepository.getCategoryTotals(userId, startDate, endDate)
            val goal = FirebaseRepository.getGoal(userId)
            renderChart(totals, goal)
        }
    }

    /** Build the BarData from the category totals and add the min/max goal limit lines. */
    private fun renderChart(totals: List<CategoryTotal>, goal: Goal?) {
        if (totals.isEmpty()) {
            binding.barChart.clear()
            binding.barChart.invalidate()
            binding.tvEmptyMessage.visibility = View.VISIBLE
            binding.tvGoalsSummary.text = ""
            return
        }
        binding.tvEmptyMessage.visibility = View.GONE

        // One bar per category.
        val entries = totals.mapIndexed { index, total ->
            BarEntry(index.toFloat(), total.total.toFloat())
        }
        val labels = totals.map { it.categoryName }

        val dataSet = BarDataSet(entries, "Amount spent (R)").apply {
            colors = listOf(
                Color.parseColor("#1B5E20"), Color.parseColor("#4CAF50"),
                Color.parseColor("#FF6F00"), Color.parseColor("#FFB300"),
                Color.parseColor("#00897B"), Color.parseColor("#5E35B1"),
                Color.parseColor("#C62828"), Color.parseColor("#1565C0")
            )
            valueTextSize = 11f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String = "R${"%.0f".format(value)}"
            }
        }

        binding.barChart.data = BarData(dataSet).apply { barWidth = 0.5f }
        binding.barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        binding.barChart.xAxis.labelCount = labels.size

        // Add the min/max goal lines on the left (Y) axis.
        val leftAxis = binding.barChart.axisLeft
        leftAxis.removeAllLimitLines()

        if (goal != null && (goal.minGoal > 0 || goal.maxGoal > 0)) {
            if (goal.minGoal > 0) {
                val minLine = LimitLine(goal.minGoal.toFloat(), "Min goal R${"%.0f".format(goal.minGoal)}").apply {
                    lineColor = Color.parseColor("#2E7D32")
                    lineWidth = 2f
                    textColor = Color.parseColor("#2E7D32")
                    textSize = 11f
                }
                leftAxis.addLimitLine(minLine)
            }
            if (goal.maxGoal > 0) {
                val maxLine = LimitLine(goal.maxGoal.toFloat(), "Max goal R${"%.0f".format(goal.maxGoal)}").apply {
                    lineColor = Color.parseColor("#C62828")
                    lineWidth = 2f
                    textColor = Color.parseColor("#C62828")
                    textSize = 11f
                }
                leftAxis.addLimitLine(maxLine)
            }
            // Make sure both goal lines are visible within the chart's vertical range.
            val maxBar = totals.maxOf { it.total }.toFloat()
            leftAxis.axisMaximum = maxOf(maxBar, goal.maxGoal.toFloat(), goal.minGoal.toFloat()) * 1.15f
            binding.tvGoalsSummary.text =
                "Monthly goals — Min: R${"%.2f".format(goal.minGoal)}  |  Max: R${"%.2f".format(goal.maxGoal)}"
        } else {
            leftAxis.resetAxisMaximum()
            binding.tvGoalsSummary.text = "Tip: set monthly goals to see your min/max budget lines here."
        }

        binding.barChart.invalidate()
        binding.barChart.animateY(800)
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                onDateSelected(String.format("%04d-%02d-%02d", year, month + 1, day))
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
