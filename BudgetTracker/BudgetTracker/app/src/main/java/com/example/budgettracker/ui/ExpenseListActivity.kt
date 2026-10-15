package com.example.budgettracker.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.budgettracker.adapter.ExpenseAdapter
import com.example.budgettracker.data.ExpenseEntry
import com.example.budgettracker.data.FirebaseRepository
import com.example.budgettracker.databinding.ActivityExpenseListBinding
import kotlinx.coroutines.launch
import java.util.*

/**
 * ExpenseListActivity – displays a list of expense entries for a user-selectable date range.
 *
 * The user picks a start date and end date, and the matching entries are read from the
 * ONLINE Firebase Realtime Database via [FirebaseRepository.getExpensesForPeriod].
 * If an entry has a photo, tapping it opens PhotoViewActivity to view the image.
 */
class ExpenseListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExpenseListBinding
    private var userId: String = ""

    private lateinit var adapter: ExpenseAdapter

    // Date range state
    private var startDate: String = ""
    private var endDate: String = ""

    companion object {
        private const val TAG = "ExpenseListActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExpenseListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Expense List"

        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
        userId = prefs.getString(LoginActivity.PREF_USER_UID, "") ?: ""

        if (userId.isEmpty()) { finish(); return }

        setupRecyclerView()
        setupDefaultDates()
        setupDatePickers()
        setupFilterButton()
    }

    /** Set the default date range to the current month. */
    private fun setupDefaultDates() {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        startDate = String.format("%04d-%02d-01", year, month)
        endDate = String.format("%04d-%02d-%02d", year, month, lastDay)
        binding.tvStartDate.text = startDate
        binding.tvEndDate.text = endDate
        Log.d(TAG, "Default date range: $startDate to $endDate")
    }

    /** Initialise the RecyclerView with the ExpenseAdapter. */
    private fun setupRecyclerView() {
        adapter = ExpenseAdapter { entry -> onPhotoClicked(entry) }
        binding.rvExpenses.layoutManager = LinearLayoutManager(this)
        binding.rvExpenses.adapter = adapter
    }

    /** Wire up the start and end date picker buttons. */
    private fun setupDatePickers() {
        binding.btnPickStartDate.setOnClickListener {
            showDatePicker { date ->
                startDate = date
                binding.tvStartDate.text = date
            }
        }
        binding.btnPickEndDate.setOnClickListener {
            showDatePicker { date ->
                endDate = date
                binding.tvEndDate.text = date
            }
        }
    }

    /** Apply the filter – load expenses for the selected period. */
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
            loadExpenses()
        }

        // Load with the default dates on first open.
        loadExpenses()
    }

    /** Read expenses for the selected period from Firebase and display them. */
    private fun loadExpenses() {
        Log.d(TAG, "Loading expenses for period: $startDate to $endDate")
        lifecycleScope.launch {
            val entries = FirebaseRepository.getExpensesForPeriod(userId, startDate, endDate)
            Log.d(TAG, "Loaded ${entries.size} expenses from Firebase")
            adapter.submitList(entries)
            binding.tvEmptyMessage.visibility =
                if (entries.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    /** Open PhotoViewActivity when the user taps a photo-linked entry. */
    private fun onPhotoClicked(entry: ExpenseEntry) {
        if (entry.photoPath == null) return
        Log.d(TAG, "Opening photo for entry ID=${entry.id}: ${entry.photoPath}")
        val intent = Intent(this, PhotoViewActivity::class.java)
        intent.putExtra(PhotoViewActivity.EXTRA_PHOTO_PATH, entry.photoPath)
        intent.putExtra(PhotoViewActivity.EXTRA_DESCRIPTION, entry.description)
        startActivity(intent)
    }

    /** Utility – show a DatePickerDialog and call [onDateSelected] with the result. */
    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                val date = String.format("%04d-%02d-%02d", year, month + 1, day)
                onDateSelected(date)
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
