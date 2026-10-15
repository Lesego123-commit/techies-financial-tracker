package com.example.budgettracker.ui

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.budgettracker.databinding.ActivityMainBinding

/**
 * MainActivity – the home dashboard of the Budget Tracker app.
 *
 * Displayed after a successful login. Provides navigation buttons to every feature:
 *  - Add a new spending category
 *  - Log a new expense entry
 *  - View the expense list for a period
 *  - View category totals for a period
 *  - View the spending graph (per-category bar chart with goal lines) – Part 3
 *  - View the visual budget-progress screen – Part 3
 *  - Open Budgeting Tips (own feature)
 *  - Set minimum and maximum monthly goals
 *  - Log out
 *
 * The logged-in user's name is shown as a welcome greeting. The user's uid (a String) is
 * read from SharedPreferences and used to scope every Firebase query to that user.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences
    private var userId: String = ""

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
        userId = prefs.getString(LoginActivity.PREF_USER_UID, "") ?: ""
        val username = prefs.getString(LoginActivity.PREF_USERNAME, "User") ?: "User"

        // Guard: if no user is logged in, redirect to login.
        if (userId.isEmpty()) {
            Log.e(TAG, "No logged-in user found, redirecting to LoginActivity")
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        Log.d(TAG, "MainActivity loaded for uid=$userId, username=$username")
        binding.tvWelcome.text = "Welcome, $username!"

        setupButtonListeners()
    }

    private fun setupButtonListeners() {
        // Navigate to Add Category screen
        binding.btnAddCategory.setOnClickListener {
            Log.d(TAG, "Navigating to AddCategoryActivity")
            startActivity(Intent(this, AddCategoryActivity::class.java))
        }

        // Navigate to Add Expense Entry screen
        binding.btnAddExpense.setOnClickListener {
            Log.d(TAG, "Navigating to AddExpenseActivity")
            startActivity(Intent(this, AddExpenseActivity::class.java))
        }

        // Navigate to Expense List screen
        binding.btnViewExpenses.setOnClickListener {
            Log.d(TAG, "Navigating to ExpenseListActivity")
            startActivity(Intent(this, ExpenseListActivity::class.java))
        }

        // Navigate to Category Totals screen
        binding.btnCategoryTotals.setOnClickListener {
            Log.d(TAG, "Navigating to CategoryTotalsActivity")
            startActivity(Intent(this, CategoryTotalsActivity::class.java))
        }

        // Navigate to the spending Graph screen (Part 3 feature)
        binding.btnGraph.setOnClickListener {
            Log.d(TAG, "Navigating to GraphActivity")
            startActivity(Intent(this, GraphActivity::class.java))
        }

        // Navigate to the visual Budget Progress screen (Part 3 feature)
        binding.btnBudgetProgress.setOnClickListener {
            Log.d(TAG, "Navigating to BudgetProgressActivity")
            startActivity(Intent(this, BudgetProgressActivity::class.java))
        }

        // Navigate to Goal Settings screen
        binding.btnGoalSettings.setOnClickListener {
            Log.d(TAG, "Navigating to GoalSettingsActivity")
            startActivity(Intent(this, GoalSettingsActivity::class.java))
        }

        // Navigate to Budgeting Tips screen (own feature)
        binding.btnTips.setOnClickListener {
            Log.d(TAG, "Navigating to TipsActivity")
            startActivity(Intent(this, TipsActivity::class.java))
        }

        // Log out button – confirms before logging out.
        binding.btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Log Out") { _, _ ->
                    Log.d(TAG, "User logging out (uid=$userId)")
                    prefs.edit()
                        .remove(LoginActivity.PREF_USER_UID)
                        .remove(LoginActivity.PREF_USERNAME)
                        .apply()
                    Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
