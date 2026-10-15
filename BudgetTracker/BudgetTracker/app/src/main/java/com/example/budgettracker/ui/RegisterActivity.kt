package com.example.budgettracker.ui

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.budgettracker.data.FirebaseRepository
import com.example.budgettracker.databinding.ActivityRegisterBinding
import kotlinx.coroutines.launch

/**
 * RegisterActivity – allows a new user to create an account.
 *
 * Validates the username/password fields and writes a new User record to the ONLINE
 * Firebase Realtime Database via [FirebaseRepository.registerUser].
 * Navigates back to LoginActivity on success.
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    companion object {
        private const val TAG = "RegisterActivity"
        private const val MIN_PASSWORD_LENGTH = 4
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Back arrow / up navigation
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.btnRegister.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            // Validate inputs before contacting Firebase.
            if (!validateInputs(username, password, confirmPassword)) return@setOnClickListener

            binding.btnRegister.isEnabled = false

            lifecycleScope.launch {
                // Write the new user to the online database (also checks for a clash).
                when (FirebaseRepository.registerUser(username, password)) {
                    FirebaseRepository.RegisterResult.SUCCESS -> {
                        Log.d(TAG, "New user registered online: $username")
                        Toast.makeText(
                            this@RegisterActivity,
                            "Account created! Please log in.",
                            Toast.LENGTH_LONG
                        ).show()
                        finish() // Return to LoginActivity
                    }
                    FirebaseRepository.RegisterResult.USERNAME_TAKEN -> {
                        binding.btnRegister.isEnabled = true
                        binding.etUsername.error = "Username already taken"
                    }
                    FirebaseRepository.RegisterResult.ERROR -> {
                        binding.btnRegister.isEnabled = true
                        Toast.makeText(
                            this@RegisterActivity,
                            "Could not reach the database. Check your internet connection.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        // Already have an account? Go back to login.
        binding.tvLoginLink.setOnClickListener { finish() }
    }

    /** Validate all registration fields and show inline errors if anything is invalid. */
    private fun validateInputs(username: String, password: String, confirmPassword: String): Boolean {
        if (username.isEmpty()) {
            binding.etUsername.error = "Username is required"
            return false
        }
        if (username.length < 3) {
            binding.etUsername.error = "Username must be at least 3 characters"
            return false
        }
        // Usernames are used as Firebase keys, so restrict them to safe characters.
        if (!username.matches(Regex("^[A-Za-z0-9_]+$"))) {
            binding.etUsername.error = "Use only letters, numbers and underscores"
            return false
        }
        if (password.isEmpty()) {
            binding.etPassword.error = "Password is required"
            return false
        }
        if (password.length < MIN_PASSWORD_LENGTH) {
            binding.etPassword.error = "Password must be at least $MIN_PASSWORD_LENGTH characters"
            return false
        }
        if (password != confirmPassword) {
            binding.etConfirmPassword.error = "Passwords do not match"
            return false
        }
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
