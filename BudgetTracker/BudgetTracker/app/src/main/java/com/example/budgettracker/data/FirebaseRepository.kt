package com.example.budgettracker.data

import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

/**
 * FirebaseRepository – the single point of contact between the app and the
 * online Firebase Realtime Database.
 *
 * This class fulfils the Part 3 requirement of "reading data from" and "writing data to"
 * a Firebase database. Every screen talks to Firebase through this repository instead of
 * accessing the database directly, which keeps the data-access logic in one place.
 *
 * Database layout (JSON tree):
 *   /users/{uid}                       -> User
 *   /categories/{uid}/{categoryId}     -> Category
 *   /expenses/{uid}/{expenseId}        -> ExpenseEntry
 *   /goals/{uid}                       -> Goal
 *
 * The {uid} key is the sanitised username and is the reference that ties all of a
 * user's records together.
 *
 * All public functions are suspend functions so they can be called from coroutines and
 * never block the UI thread. They use Firebase's Task.await() (kotlinx-coroutines-play-services)
 * to turn the asynchronous Firebase calls into clean sequential Kotlin code.
 */

/*
 * References:
 *  - Google (2024) Firebase Realtime Database – Read and write data on Android. Available at:
 *    https://firebase.google.com/docs/database/android/read-and-write (Accessed: 31 May 2026).
 *  - Kotlin (2024) kotlinx-coroutines-play-services – await() for Google Play Services Tasks.
 *    Available at: https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-play-services/
 *    (Accessed: 31 May 2026). The suspend-wrapper pattern using Task.await() below follows this guide.
 */
object FirebaseRepository {

    private const val TAG = "FirebaseRepository"

    // Node names – kept as constants so they are never mistyped.
    private const val NODE_USERS = "users"
    private const val NODE_CATEGORIES = "categories"
    private const val NODE_EXPENSES = "expenses"
    private const val NODE_GOALS = "goals"

    /** Root reference to the online Firebase Realtime Database. */
    private val db: FirebaseDatabase by lazy { Firebase.database }

    /** Result of a registration attempt, used by RegisterActivity to give clear feedback. */
    enum class RegisterResult { SUCCESS, USERNAME_TAKEN, ERROR }

    /**
     * Convert a username into a safe Firebase key.
     * Firebase keys may not contain '.', '#', '$', '[', ']' or '/', so we lower-case the
     * username and strip anything that is not a letter, digit or underscore.
     */
    fun keyFor(username: String): String =
        username.trim().lowercase().replace(Regex("[^a-z0-9_]"), "")

    // ---------------------------------------------------------------------
    // Authentication (users)
    // ---------------------------------------------------------------------

    /**
     * Register a new user. Writes a new User node to Firebase if the username is free.
     * @return RegisterResult indicating success, a clash, or an error.
     */
    suspend fun registerUser(username: String, password: String): RegisterResult {
        return try {
            val uid = keyFor(username)
            val ref = db.getReference(NODE_USERS).child(uid)

            // Read first to check whether the username is already taken.
            val existing = ref.get().await()
            if (existing.exists()) {
                Log.w(TAG, "registerUser: username '$username' already taken")
                return RegisterResult.USERNAME_TAKEN
            }

            // Write the new user to the online database.
            val user = User(uid = uid, username = username, password = password)
            ref.setValue(user).await()
            Log.d(TAG, "registerUser: created user '$username' (uid=$uid)")
            RegisterResult.SUCCESS
        } catch (e: Exception) {
            Log.e(TAG, "registerUser failed: ${e.message}", e)
            RegisterResult.ERROR
        }
    }

    /**
     * Log a user in by reading their record from Firebase and checking the password.
     * @return the matching [User] on success, or null on failure.
     */
    suspend fun loginUser(username: String, password: String): User? {
        return try {
            val uid = keyFor(username)
            val snapshot = db.getReference(NODE_USERS).child(uid).get().await()
            val user = snapshot.getValue(User::class.java)
            if (user != null && user.password == password) {
                Log.d(TAG, "loginUser: success for '$username'")
                user
            } else {
                Log.w(TAG, "loginUser: invalid credentials for '$username'")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "loginUser failed: ${e.message}", e)
            null
        }
    }

    // ---------------------------------------------------------------------
    // Categories
    // ---------------------------------------------------------------------

    /** Write a new category for the user to Firebase. */
    suspend fun addCategory(userId: String, name: String) {
        val ref = db.getReference(NODE_CATEGORIES).child(userId).push()
        val category = Category(id = ref.key ?: "", userId = userId, name = name)
        ref.setValue(category).await()
        Log.d(TAG, "addCategory: '$name' saved for user $userId")
    }

    /** Read all categories that belong to a user, sorted alphabetically by name. */
    suspend fun getCategories(userId: String): List<Category> {
        return try {
            val snapshot = db.getReference(NODE_CATEGORIES).child(userId).get().await()
            snapshot.children.mapNotNull { it.getValue(Category::class.java) }
                .sortedBy { it.name.lowercase() }
        } catch (e: Exception) {
            Log.e(TAG, "getCategories failed: ${e.message}", e)
            emptyList()
        }
    }

    // ---------------------------------------------------------------------
    // Expense entries
    // ---------------------------------------------------------------------

    /** Write a new expense entry for the user to Firebase. */
    suspend fun addExpense(entry: ExpenseEntry) {
        val ref = db.getReference(NODE_EXPENSES).child(entry.userId).push()
        val toSave = entry.copy(id = ref.key ?: "")
        ref.setValue(toSave).await()
        Log.d(TAG, "addExpense: R${entry.amount} (${entry.categoryName}) saved for ${entry.userId}")
    }

    /** Read every expense for a user (newest first). */
    suspend fun getAllExpenses(userId: String): List<ExpenseEntry> {
        return try {
            val snapshot = db.getReference(NODE_EXPENSES).child(userId).get().await()
            snapshot.children.mapNotNull { it.getValue(ExpenseEntry::class.java) }
                .sortedWith(compareByDescending<ExpenseEntry> { it.date }.thenByDescending { it.startTime })
        } catch (e: Exception) {
            Log.e(TAG, "getAllExpenses failed: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Read all expenses for a user within an inclusive date range.
     * Dates are stored as yyyy-MM-dd strings, so a simple string comparison is correct.
     */
    suspend fun getExpensesForPeriod(
        userId: String,
        startDate: String,
        endDate: String
    ): List<ExpenseEntry> {
        return getAllExpenses(userId).filter { it.date in startDate..endDate }
    }

    /**
     * Calculate the total spent per category for a user within a date range.
     * The expenses are read from Firebase and then grouped/summed in code.
     */
    suspend fun getCategoryTotals(
        userId: String,
        startDate: String,
        endDate: String
    ): List<CategoryTotal> {
        return getExpensesForPeriod(userId, startDate, endDate)
            .groupBy { it.categoryName }
            .map { (name, entries) -> CategoryTotal(name, entries.sumOf { it.amount }) }
            .sortedByDescending { it.total }
    }

    /** Convenience helper – the grand total spent across all categories in a period. */
    suspend fun getTotalSpent(userId: String, startDate: String, endDate: String): Double {
        return getExpensesForPeriod(userId, startDate, endDate).sumOf { it.amount }
    }

    // ---------------------------------------------------------------------
    // Goals
    // ---------------------------------------------------------------------

    /** Save (insert or overwrite) the user's monthly min/max goal in Firebase. */
    suspend fun saveGoal(goal: Goal) {
        db.getReference(NODE_GOALS).child(goal.userId).setValue(goal).await()
        Log.d(TAG, "saveGoal: min=${goal.minGoal}, max=${goal.maxGoal} for ${goal.userId}")
    }

    /** Read the user's goal from Firebase, or null if they have not set one yet. */
    suspend fun getGoal(userId: String): Goal? {
        return try {
            db.getReference(NODE_GOALS).child(userId).get().await().getValue(Goal::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "getGoal failed: ${e.message}", e)
            null
        }
    }
}
