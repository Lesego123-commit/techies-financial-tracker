package com.example.budgettracker.ui

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.budgettracker.data.FirebaseRepository
import com.example.budgettracker.databinding.ActivityBudgetProgressBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * BudgetProgressActivity – Part 3 required feature.
 *
 * Provides a VISUAL display of how well the user is doing at staying between their minimum
 * and maximum monthly spending goals over the past month. It reads this month's total
 * spending and the user's goals from the online Firebase Realtime Database and shows:
 *   - a colour-coded status banner (Under Budget / On Track / Over Budget),
 *   - a progress bar of spend against the maximum goal, with min/max markers,
 *   - a numeric breakdown.
 */
class BudgetProgressActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBudgetProgressBinding
    private var userId: String = ""

    companion object {
        private const val TAG = "BudgetProgressActivity"
        // Status colours.
        private val GREEN = Color.parseColor("#2E7D32")
        private val AMBER = Color.parseColor("#FF8F00")
        private val RED = Color.parseColor("#C62828")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBudgetProgressBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Budget Progress"

        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
        userId = prefs.getString(LoginActivity.PREF_USER_UID, "") ?: ""
        if (userId.isEmpty()) { finish(); return }

        loadProgress()
    }

    /** Read this month's spending and the user's goals from Firebase, then render the visual. */
    private fun loadProgress() {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val startOfMonth = String.format("%04d-%02d-01", year, month)
        val endOfMonth = String.format("%04d-%02d-%02d", year, month, lastDay)

        val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
        binding.tvPeriodLabel.text = "Progress for $monthName"

        lifecycleScope.launch {
            val total = FirebaseRepository.getTotalSpent(userId, startOfMonth, endOfMonth)
            val goal = FirebaseRepository.getGoal(userId)
            Log.d(TAG, "Progress: total=$total, goal=$goal")
            render(total, goal?.minGoal ?: 0.0, goal?.maxGoal ?: 0.0, goalSet = goal != null && (goal.minGoal > 0 || goal.maxGoal > 0))
        }
    }

    /** Update all of the views based on the spend and goals. */
    private fun render(total: Double, minGoal: Double, maxGoal: Double, goalSet: Boolean) {
        binding.tvSpentBig.text = "R${"%.2f".format(total)}"
        binding.tvMinLabel.text = "Min: R${"%.0f".format(minGoal)}"
        binding.tvMaxLabel.text = "Max: R${"%.0f".format(maxGoal)}"

        if (!goalSet) {
            // No goals to compare against – guide the user.
            binding.tvNoGoal.visibility = View.VISIBLE
            binding.cardStatus.setCardBackgroundColor(AMBER)
            binding.tvStatusEmoji.text = "🧭"
            binding.tvStatusTitle.text = "No Goals Set"
            binding.tvStatusMessage.text = "Set your monthly goals to track your progress."
            binding.progressBar.progress = 0
            binding.tvBreakdown.text = "Spent this month: R${"%.2f".format(total)}"
            return
        }
        binding.tvNoGoal.visibility = View.GONE

        // Progress bar = percentage of the maximum goal used (capped at 100 for the bar).
        val percentOfMax = if (maxGoal > 0) (total / maxGoal * 100).toInt() else 0
        binding.progressBar.progress = percentOfMax.coerceIn(0, 100)

        // Decide the status (and colour) based on where the spend falls.
        val (statusColor, emoji, title, message) = when {
            maxGoal > 0 && total > maxGoal -> StatusInfo(
                RED, "🚨", "Over Budget",
                "You've gone over your maximum goal by R${"%.2f".format(total - maxGoal)}. Time to ease off spending."
            )
            total < minGoal -> StatusInfo(
                AMBER, "🌱", "Under Budget",
                "You're below your minimum goal. You still have room, but keep an eye on essentials."
            )
            else -> StatusInfo(
                GREEN, "🎯", "On Track",
                "Great job! Your spending is sitting comfortably between your min and max goals."
            )
        }

        binding.cardStatus.setCardBackgroundColor(statusColor)
        binding.tvStatusEmoji.text = emoji
        binding.tvStatusTitle.text = title
        binding.tvStatusMessage.text = message
        binding.progressBar.progressDrawable?.setColorFilter(statusColor, PorterDuff.Mode.SRC_IN)

        val remaining = (maxGoal - total)
        binding.tvBreakdown.text = buildString {
            appendLine("Spent this month:  R${"%.2f".format(total)}")
            appendLine("Minimum goal:  R${"%.2f".format(minGoal)}")
            appendLine("Maximum goal:  R${"%.2f".format(maxGoal)}")
            append(
                if (remaining >= 0) "Remaining to max:  R${"%.2f".format(remaining)}"
                else "Over max by:  R${"%.2f".format(-remaining)}"
            )
        }
    }

    /** Small holder for the four status values so they can be returned together. */
    private data class StatusInfo(
        val color: Int,
        val emoji: String,
        val title: String,
        val message: String
    )

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
