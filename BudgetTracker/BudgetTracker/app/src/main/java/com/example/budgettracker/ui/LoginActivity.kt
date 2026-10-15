package com.example.budgettracker.ui

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.budgettracker.data.FirebaseRepository
import com.example.budgettracker.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

/**
 * LoginActivity – the entry point of the Budget Tracker app.
 *
 * Allows an existing user to log in with their username and password.
 * Credentials are verified against the ONLINE Firebase Realtime Database via
 * [FirebaseRepository.loginUser]. On success the user's uid is saved to SharedPreferences
 * and MainActivity is launched. New users can navigate to RegisterActivity.
 */
class LoginActivity : AppCompatActivity() {

    // View binding reference to all the views in activity_login.xml
    private lateinit var binding: ActivityLoginBinding
    // SharedPreferences reference – stores the logged-in user's session.
    private lateinit var prefs: SharedPreferences

    companion object {
        private const val TAG = "LoginActivity"
        const val PREF_NAME = "budget_tracker_prefs"
        // Stores the logged-in user's uid (a String now that data lives in Firebase).
        const val PREF_USER_UID = "logged_in_user_uid"
        const val PREF_USERNAME = "logged_in_username"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialise shared preferences for session handling.
        prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)

        // If the user is already logged in, skip straight to MainActivity.
        if (prefs.getString(PREF_USER_UID, null) != null) {
            Log.d(TAG, "User already logged in, navigating to MainActivity")
            navigateToMain()
            return
        }

        // Login button click listener
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            // Input validation – fields must not be empty.
            if (username.isEmpty()) {
                binding.etUsername.error = "Username is required"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                binding.etPassword.error = "Password is required"
                return@setOnClickListener
            }

            // Disable the button while we contact Firebase to avoid double submissions.
            binding.btnLogin.isEnabled = false

            // Query the online database on a background coroutine.
            lifecycleScope.launch {
                Log.d(TAG, "Attempting Firebase login for username: $username")
                val user = FirebaseRepository.loginUser(username, password)
                binding.btnLogin.isEnabled = true

                if (user != null) {
                    // Save session to SharedPreferences.
                    prefs.edit()
                        .putString(PREF_USER_UID, user.uid)
                        .putString(PREF_USERNAME, user.username)
                        .apply()
                    Log.d(TAG, "Login successful for user: ${user.username} (uid=${user.uid})")
                    Toast.makeText(this@LoginActivity, "Welcome back, ${user.username}!", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                } else {
                    Log.w(TAG, "Login failed – invalid credentials for username: $username")
                    Toast.makeText(this@LoginActivity, "Invalid username or password", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Navigate to RegisterActivity
        binding.tvRegisterLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    /** Launch MainActivity and close LoginActivity so the user cannot back-navigate to it. */
    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
