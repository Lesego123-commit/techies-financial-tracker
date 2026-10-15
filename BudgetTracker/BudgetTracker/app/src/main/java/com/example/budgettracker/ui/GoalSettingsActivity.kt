package com.example.budgettracker.ui

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.budgettracker.data.FirebaseRepository
import com.example.budgettracker.data.Goal
import com.example.budgettracker.databinding.ActivityGoalSettingsBinding
import kotlinx.coroutines.launch

/**
 * GoalSettingsActivity – allows the user to set monthly spending goals.
 *
 * The user can specify:
 *  - A minimum monthly spending goal (the least they intend to spend).
 *  - A maximum monthly budget cap (the most they allow themselves to spend).
 *
 * The goal is stored online in the Firebase Realtime Database keyed by the user's uid, so
 * saving again overwrites the existing record. These goals drive the smart spending alerts,
 * the spending graph goal lines, and the visual budget-progress screen.
 */
class GoalSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGoalSettingsBinding
    private var userId: String = ""

    companion object {
        private const val TAG = "GoalSettingsActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoalSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Monthly Goals"

        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
        userId = prefs.getString(LoginActivity.PREF_USER_UID, "") ?: ""

        if (userId.isEmpty()) { finish(); return }

        // Load any existing goal from Firebase and pre-fill the fields.
        loadCurrentGoals()

        binding.btnSaveGoals.setOnClickListener {
            saveGoals()
        }
    }

    /** Read the user's existing goal from Firebase and pre-fill the input fields. */
    private fun loadCurrentGoals() {
        lifecycleScope.launch {
            val goal = FirebaseRepository.getGoal(userId)
            if (goal != null) {
                Log.d(TAG, "Existing goal loaded: min=${goal.minGoal}, max=${goal.maxGoal}")
                binding.etMinGoal.setText("%.2f".format(goal.minGoal))
                binding.etMaxGoal.setText("%.2f".format(goal.maxGoal))
            } else {
                Log.d(TAG, "No existing goal found for uid=$userId")
            }
        }
    }

    /** Validate inputs and save/update the goal in Firebase. */
    private fun saveGoals() {
        val minText = binding.etMinGoal.text.toString().trim()
        val maxText = binding.etMaxGoal.text.toString().trim()

        if (minText.isEmpty()) {
            binding.etMinGoal.error = "Minimum goal is required"
            return
        }
        if (maxText.isEmpty()) {
            binding.etMaxGoal.error = "Maximum goal is required"
            return
        }

        val minGoal = minText.toDoubleOrNull()
        if (minGoal == null || minGoal < 0) {
            binding.etMinGoal.error = "Enter a valid positive amount"
            return
        }
        val maxGoal = maxText.toDoubleOrNull()
        if (maxGoal == null || maxGoal < 0) {
            binding.etMaxGoal.error = "Enter a valid positive amount"
            return
        }
        if (minGoal > maxGoal) {
            binding.etMinGoal.error = "Minimum goal cannot exceed maximum goal"
            return
        }

        binding.btnSaveGoals.isEnabled = false
        lifecycleScope.launch {
            try {
                val goal = Goal(userId = userId, minGoal = minGoal, maxGoal = maxGoal)
                FirebaseRepository.saveGoal(goal)
                Log.d(TAG, "Goals saved online: min=R$minGoal, max=R$maxGoal for uid=$userId")
                Toast.makeText(
                    this@GoalSettingsActivity,
                    "Goals saved! Min: R${"%.2f".format(minGoal)}, Max: R${"%.2f".format(maxGoal)}",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save goals: ${e.message}", e)
                binding.btnSaveGoals.isEnabled = true
                Toast.makeText(
                    this@GoalSettingsActivity,
                    "Could not save. Check your internet connection.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
