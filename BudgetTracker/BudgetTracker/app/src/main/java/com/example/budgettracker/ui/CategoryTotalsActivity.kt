package com.example.budgettracker.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.budgettracker.adapter.CategoryTotalAdapter
import com.example.budgettracker.data.FirebaseRepository
import com.example.budgettracker.databinding.ActivityCategoryTotalsBinding
import kotlinx.coroutines.launch
import java.util.*

/**
 * CategoryTotalsActivity – shows the total spending per category for a user-selected period.
 *
 * The totals are calculated from the user's expenses stored in the ONLINE Firebase Realtime
 * Database via [FirebaseRepository.getCategoryTotals]. The overall total for the period is
 * also displayed. The user can filter by selecting a start and end date.
 */
class CategoryTotalsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoryTotalsBinding
    private var userId: String = ""

    private lateinit var adapter: CategoryTotalAdapter
    private var startDate: String = ""
    private var endDate: String = ""

    companion object {
        private const val TAG = "CategoryTotalsActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryTotalsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Category Totals"

        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
        userId = prefs.getString(LoginActivity.PREF_USER_UID, "") ?: ""

        if (userId.isEmpty()) { finish(); return }

        setupRecyclerView()
        setupDefaultDates()
        setupDatePickers()
        setupFilterButton()
    }

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

    private fun setupRecyclerView() {
        adapter = CategoryTotalAdapter()
        binding.rvCategoryTotals.layoutManager = LinearLayoutManager(this)
        binding.rvCategoryTotals.adapter = adapter
    }

    private fun setupDatePickers() {
        binding.btnPickStartDate.setOnClickListener { showDatePicker { startDate = it; binding.tvStartDate.text = it } }
        binding.btnPickEndDate.setOnClickListener { showDatePicker { endDate = it; binding.tvEndDate.text = it } }
    }

    private fun setupFilterButton() {
        binding.btnFilter.setOnClickListener {
            if (startDate.isEmpty() || endDate.isEmpty()) {
                Toast.makeText(this, "Please select a date range", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (startDate > endDate) {
                Toast.makeText(this, "Start date must be before end date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            loadTotals()
        }
        loadTotals()
    }

    /** Read category totals from Firebase and update the list and the grand-total summary. */
    private fun loadTotals() {
        Log.d(TAG, "Loading category totals for period: $startDate to $endDate")
        lifecycleScope.launch {
            val totals = FirebaseRepository.getCategoryTotals(userId, startDate, endDate)
            Log.d(TAG, "Loaded ${totals.size} category totals from Firebase")
            adapter.submitList(totals)

            // Calculate and display the grand total.
            val grandTotal = totals.sumOf { it.total }
            binding.tvGrandTotal.text = "Total: R${"%.2f".format(grandTotal)}"

            binding.tvEmptyMessage.visibility =
                if (totals.isEmpty()) View.VISIBLE else View.GONE
        }
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
