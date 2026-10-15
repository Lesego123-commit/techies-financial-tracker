package com.example.budgettracker.ui

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.budgettracker.data.Category
import com.example.budgettracker.data.ExpenseEntry
import com.example.budgettracker.data.FirebaseRepository
import com.example.budgettracker.databinding.ActivityAddExpenseBinding
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/*
 * References:
 *  - Bumptech (2023) Glide – Image loading and caching framework for Android. Available at:
 *    https://github.com/bumptech/glide (Accessed: 31 May 2026). Used to load the receipt thumbnail.
 *  - Google (2024) Take photos (camera) and FileProvider. Available at:
 *    https://developer.android.com/training/camera-deprecated/photobasics and
 *    https://developer.android.com/reference/androidx/core/content/FileProvider (Accessed: 31 May 2026).
 *    The camera-capture flow and FileProvider URI sharing below are adapted from these guides.
 */

/**
 * AddExpenseActivity – allows the user to log a new expense entry to the online Firebase database.
 *
 * Required fields: Description, Amount, Date, Start Time, End Time, Category.
 * Optional: a receipt photo (camera or gallery).
 *
 * OWN FEATURE – SMART SPENDING ALERTS:
 * After an expense is saved, this screen recalculates the user's total spending for the
 * current month and compares it to their maximum monthly goal. If the new expense pushes
 * the user close to (>= 80%) or over (>= 100%) their budget, a warning dialog is shown so
 * they are alerted proactively rather than only finding out later.
 */
class AddExpenseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddExpenseBinding
    private var userId: String = ""

    // Category list backing the spinner.
    private val categoryList = mutableListOf<Category>()
    private lateinit var categoryAdapter: ArrayAdapter<String>

    // Photo handling
    private var photoUri: Uri? = null
    private var photoPath: String? = null

    // Currently selected date and times.
    private var selectedDate: String = ""
    private var selectedStartTime: String = ""
    private var selectedEndTime: String = ""

    companion object {
        private const val TAG = "AddExpenseActivity"
        // Smart-alert thresholds.
        private const val WARN_THRESHOLD = 0.80   // 80% of max goal
    }

    // Camera launcher – handles the result of taking a photo.
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoPath != null) {
            Log.d(TAG, "Photo captured successfully: $photoPath")
            showPhotoPreview()
        } else {
            Log.w(TAG, "Photo capture cancelled or failed")
            photoPath = null
            photoUri = null
        }
    }

    // Gallery launcher – handles the result of picking from the gallery.
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            photoUri = uri
            photoPath = uri.toString()
            Log.d(TAG, "Image selected from gallery: $photoPath")
            showPhotoPreview()
        }
    }

    // Camera permission launcher.
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            Log.d(TAG, "Camera permission granted")
            openCamera()
        } else {
            Log.w(TAG, "Camera permission denied")
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Add Expense"

        val prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE)
        userId = prefs.getString(LoginActivity.PREF_USER_UID, "") ?: ""

        if (userId.isEmpty()) { finish(); return }

        setupCategorySpinner()
        setupDateTimePickers()
        setupPhotoButtons()
        setupSaveButton()
    }

    /** Load the user's categories from Firebase and populate the spinner. */
    private fun setupCategorySpinner() {
        categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mutableListOf())
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = categoryAdapter

        lifecycleScope.launch {
            val cats = FirebaseRepository.getCategories(userId)
            categoryList.clear()
            categoryList.addAll(cats)
            categoryAdapter.clear()
            if (cats.isEmpty()) {
                categoryAdapter.add("No categories – add one first")
            } else {
                cats.forEach { categoryAdapter.add(it.name) }
            }
            categoryAdapter.notifyDataSetChanged()
        }
    }

    /** Wire up the date and time pickers. */
    private fun setupDateTimePickers() {
        // Date picker
        binding.btnPickDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    selectedDate = String.format("%04d-%02d-%02d", year, month + 1, day)
                    binding.tvSelectedDate.text = selectedDate
                    Log.d(TAG, "Date selected: $selectedDate")
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Start time picker
        binding.btnPickStartTime.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(
                this,
                { _, hour, minute ->
                    selectedStartTime = String.format("%02d:%02d", hour, minute)
                    binding.tvSelectedStartTime.text = selectedStartTime
                    Log.d(TAG, "Start time: $selectedStartTime")
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true
            ).show()
        }

        // End time picker
        binding.btnPickEndTime.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(
                this,
                { _, hour, minute ->
                    selectedEndTime = String.format("%02d:%02d", hour, minute)
                    binding.tvSelectedEndTime.text = selectedEndTime
                    Log.d(TAG, "End time: $selectedEndTime")
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true
            ).show()
        }
    }

    /** Set up the camera and gallery photo buttons. */
    private fun setupPhotoButtons() {
        binding.btnTakePhoto.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                openCamera()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        binding.btnPickGallery.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnRemovePhoto.setOnClickListener {
            photoPath = null
            photoUri = null
            binding.ivPhotoPreview.visibility = View.GONE
            binding.btnRemovePhoto.visibility = View.GONE
            Log.d(TAG, "Photo removed")
        }
    }

    /** Launch the camera intent and create a temp file for the photo. */
    private fun openCamera() {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val photoFile = File(
                getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "BUDGET_${timestamp}.jpg"
            )
            photoPath = photoFile.absolutePath
            photoUri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                photoFile
            )
            Log.d(TAG, "Camera URI created: $photoUri")
            takePictureLauncher.launch(photoUri)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening camera: ${e.message}", e)
            Toast.makeText(this, "Cannot open camera", Toast.LENGTH_SHORT).show()
        }
    }

    /** Display the captured/selected photo in the preview ImageView. */
    private fun showPhotoPreview() {
        binding.ivPhotoPreview.visibility = View.VISIBLE
        binding.btnRemovePhoto.visibility = View.VISIBLE
        Glide.with(this)
            .load(photoUri ?: File(photoPath!!))
            .centerCrop()
            .into(binding.ivPhotoPreview)
    }

    /** Validate all inputs and save the expense entry to Firebase. */
    private fun setupSaveButton() {
        binding.btnSaveExpense.setOnClickListener {
            val description = binding.etDescription.text.toString().trim()
            val amountText = binding.etAmount.text.toString().trim()

            // Validate required fields.
            if (description.isEmpty()) {
                binding.etDescription.error = "Description is required"
                return@setOnClickListener
            }
            if (amountText.isEmpty()) {
                binding.etAmount.error = "Amount is required"
                return@setOnClickListener
            }
            val amount = amountText.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                binding.etAmount.error = "Enter a valid positive amount"
                return@setOnClickListener
            }
            if (selectedDate.isEmpty()) {
                Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedStartTime.isEmpty()) {
                Toast.makeText(this, "Please select a start time", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedEndTime.isEmpty()) {
                Toast.makeText(this, "Please select an end time", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (categoryList.isEmpty()) {
                Toast.makeText(this, "Please create a category first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Get the selected category.
            val selectedIndex = binding.spinnerCategory.selectedItemPosition
            val selectedCategory = categoryList[selectedIndex]

            binding.btnSaveExpense.isEnabled = false
            lifecycleScope.launch {
                try {
                    val entry = ExpenseEntry(
                        userId = userId,
                        categoryId = selectedCategory.id,
                        categoryName = selectedCategory.name,
                        description = description,
                        amount = amount,
                        date = selectedDate,
                        startTime = selectedStartTime,
                        endTime = selectedEndTime,
                        photoPath = photoPath
                    )
                    FirebaseRepository.addExpense(entry)
                    Log.d(TAG, "Expense saved online: $description, R$amount on $selectedDate")

                    // OWN FEATURE: run the smart spending alert check after saving.
                    checkSmartSpendingAlert()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save expense: ${e.message}", e)
                    binding.btnSaveExpense.isEnabled = true
                    Toast.makeText(
                        this@AddExpenseActivity,
                        "Could not save. Check your internet connection.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /**
     * OWN FEATURE – SMART SPENDING ALERTS.
     *
     * Recomputes this month's total spending (from Firebase) and compares it to the user's
     * maximum monthly goal. Shows a friendly toast when well within budget, a warning dialog
     * when nearing the limit, and a strong alert dialog when the limit is exceeded.
     */
    private suspend fun checkSmartSpendingAlert() {
        val goal = FirebaseRepository.getGoal(userId)

        // Work out the first and last day of the CURRENT month.
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val startOfMonth = String.format("%04d-%02d-01", year, month)
        val endOfMonth = String.format("%04d-%02d-%02d", year, month, lastDay)

        val monthTotal = FirebaseRepository.getTotalSpent(userId, startOfMonth, endOfMonth)

        // If no max goal has been set we cannot compare, so just confirm the save.
        if (goal == null || goal.maxGoal <= 0.0) {
            Toast.makeText(this, "Expense logged successfully!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val ratio = monthTotal / goal.maxGoal
        Log.d(TAG, "Smart alert: monthTotal=$monthTotal, maxGoal=${goal.maxGoal}, ratio=$ratio")

        when {
            ratio >= 1.0 -> showAlertDialog(
                "⚠ Budget Exceeded",
                "Heads up! You have now spent R${"%.2f".format(monthTotal)} this month, " +
                    "which is OVER your maximum goal of R${"%.2f".format(goal.maxGoal)}. " +
                    "Consider holding off on further spending."
            )
            ratio >= WARN_THRESHOLD -> showAlertDialog(
                "Approaching Your Limit",
                "You've spent R${"%.2f".format(monthTotal)} this month — that's " +
                    "${(ratio * 100).toInt()}% of your R${"%.2f".format(goal.maxGoal)} budget. " +
                    "You're getting close to your limit."
            )
            else -> {
                Toast.makeText(
                    this,
                    "Saved! R${"%.2f".format(monthTotal)} of R${"%.2f".format(goal.maxGoal)} used this month.",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    /** Show a smart-alert dialog and close the screen once the user acknowledges it. */
    private fun showAlertDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("Got it") { _, _ -> finish() }
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
