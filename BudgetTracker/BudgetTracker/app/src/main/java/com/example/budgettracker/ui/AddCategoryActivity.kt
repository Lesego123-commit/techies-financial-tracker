package com.example.budgettracker.ui

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.budgettracker.data.FirebaseRepository
import com.example.budgettracker.databinding.ActivityAddCategoryBinding
import kotlinx.coroutines.launch

/**
 * AddCategoryActivity – lets the logged-in user create a new spending category.
 *
 * The category name is validated (non-empty, reasonable length) before being written to
 * the ONLINE Firebase Realtime Database via [FirebaseRepository.addCategory]. The category
 * is linked to the current user by their uid.
 */
class AddCategoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddCategoryBinding
    private var userId: String = ""

    companion object {
        private const val TAG = "AddCategoryActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Add Category"

        // Retrieve logged-in user's uid from SharedPreferences.
        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
        userId = prefs.getString(LoginActivity.PREF_USER_UID, "") ?: ""

        if (userId.isEmpty()) {
            Log.e(TAG, "No uid found in prefs, finishing activity")
            finish()
            return
        }

        binding.btnSaveCategory.setOnClickListener {
            val name = binding.etCategoryName.text.toString().trim()

            // Validate category name.
            if (name.isEmpty()) {
                binding.etCategoryName.error = "Category name cannot be empty"
                return@setOnClickListener
            }
            if (name.length > 50) {
                binding.etCategoryName.error = "Category name is too long (max 50 characters)"
                return@setOnClickListener
            }

            // Save the category to the online database.
            binding.btnSaveCategory.isEnabled = false
            lifecycleScope.launch {
                try {
                    FirebaseRepository.addCategory(userId, name)
                    Log.d(TAG, "Category saved online: '$name' for uid=$userId")
                    Toast.makeText(
                        this@AddCategoryActivity,
                        "Category '$name' created!",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save category: ${e.message}", e)
                    binding.btnSaveCategory.isEnabled = true
                    Toast.makeText(
                        this@AddCategoryActivity,
                        "Could not save. Check your internet connection.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
