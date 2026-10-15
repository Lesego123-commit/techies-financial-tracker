package com.example.budgettracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for core Budget Tracker functionality.
 *
 * These tests validate business logic that does not require an Android context.
 * Room database tests (requiring an in-memory database) are located in the
 * androidTest source set.
 */
class BudgetTrackerUnitTest {

    // ========================
    // Input validation helpers
    // ========================

    /**
     * Validates that a category name is non-empty and within the max length.
     */
    private fun isValidCategoryName(name: String): Boolean {
        return name.isNotBlank() && name.length <= 50
    }

    /**
     * Validates a monetary amount string – must be a positive number.
     */
    private fun isValidAmount(amountText: String): Boolean {
        val amount = amountText.toDoubleOrNull() ?: return false
        return amount > 0
    }

    /**
     * Validates that start date is not after end date.
     */
    private fun isValidDateRange(startDate: String, endDate: String): Boolean {
        return startDate <= endDate
    }

    /**
     * Validates that a minimum goal does not exceed the maximum.
     */
    private fun isValidGoalRange(min: Double, max: Double): Boolean {
        return min >= 0 && max >= 0 && min <= max
    }

    // ========================
    // Category name validation
    // ========================

    @Test
    fun `valid category name passes validation`() {
        assertTrue(isValidCategoryName("Groceries"))
    }

    @Test
    fun `empty category name fails validation`() {
        assertFalse(isValidCategoryName(""))
    }

    @Test
    fun `blank category name fails validation`() {
        assertFalse(isValidCategoryName("   "))
    }

    @Test
    fun `category name at max length passes`() {
        val name = "A".repeat(50)
        assertTrue(isValidCategoryName(name))
    }

    @Test
    fun `category name exceeding max length fails`() {
        val name = "A".repeat(51)
        assertFalse(isValidCategoryName(name))
    }

    // ========================
    // Amount validation
    // ========================

    @Test
    fun `positive amount passes validation`() {
        assertTrue(isValidAmount("150.50"))
    }

    @Test
    fun `zero amount fails validation`() {
        assertFalse(isValidAmount("0"))
    }

    @Test
    fun `negative amount fails validation`() {
        assertFalse(isValidAmount("-10"))
    }

    @Test
    fun `non-numeric amount fails validation`() {
        assertFalse(isValidAmount("abc"))
    }

    @Test
    fun `empty amount fails validation`() {
        assertFalse(isValidAmount(""))
    }

    // ========================
    // Date range validation
    // ========================

    @Test
    fun `valid date range passes`() {
        assertTrue(isValidDateRange("2026-01-01", "2026-01-31"))
    }

    @Test
    fun `same start and end date is valid`() {
        assertTrue(isValidDateRange("2026-04-15", "2026-04-15"))
    }

    @Test
    fun `start date after end date fails`() {
        assertFalse(isValidDateRange("2026-04-30", "2026-04-01"))
    }

    // ========================
    // Goal range validation
    // ========================

    @Test
    fun `valid goal range passes`() {
        assertTrue(isValidGoalRange(500.0, 5000.0))
    }

    @Test
    fun `min equal to max is valid`() {
        assertTrue(isValidGoalRange(1000.0, 1000.0))
    }

    @Test
    fun `min greater than max fails`() {
        assertFalse(isValidGoalRange(5000.0, 500.0))
    }

    @Test
    fun `negative min fails`() {
        assertFalse(isValidGoalRange(-100.0, 1000.0))
    }

    // ========================
    // Amount formatting
    // ========================

    @Test
    fun `amount formats to two decimal places`() {
        val amount = 1234.5
        val formatted = "R${"%.2f".format(amount)}"
        assertEquals("R1234.50", formatted)
    }

    @Test
    fun `zero amount formats correctly`() {
        val amount = 0.0
        val formatted = "R${"%.2f".format(amount)}"
        assertEquals("R0.00", formatted)
    }

    // ============================================================
    // Part 3 logic – helpers mirroring the production code so the
    // new behaviour (Firebase key sanitising, smart spending alerts
    // and budget-progress status) is covered by automated tests.
    // ============================================================

    /** Mirrors FirebaseRepository.keyFor – turns a username into a safe Firebase key. */
    private fun sanitizeUsername(username: String): String =
        username.trim().lowercase().replace(Regex("[^a-z0-9_]"), "")

    /** Mirrors the smart-alert decision used in AddExpenseActivity. */
    private fun alertLevel(monthTotal: Double, maxGoal: Double): String {
        if (maxGoal <= 0.0) return "NONE"
        val ratio = monthTotal / maxGoal
        return when {
            ratio >= 1.0 -> "OVER"
            ratio >= 0.80 -> "WARN"
            else -> "OK"
        }
    }

    /** Mirrors the status decision used in BudgetProgressActivity. */
    private fun spendingStatus(total: Double, min: Double, max: Double): String = when {
        max > 0 && total > max -> "OVER_BUDGET"
        total < min -> "UNDER_BUDGET"
        else -> "ON_TRACK"
    }

    // ---- Username sanitising ----

    @Test
    fun `username is lower-cased and stripped of spaces`() {
        assertEquals("johndoe", sanitizeUsername("John Doe"))
    }

    @Test
    fun `username keeps letters digits and underscores`() {
        assertEquals("user_123", sanitizeUsername("User_123"))
    }

    @Test
    fun `username strips illegal firebase key characters`() {
        assertEquals("abc", sanitizeUsername("a.b#c"))
    }

    // ---- Smart spending alerts ----

    @Test
    fun `alert is OVER when spending exceeds the max goal`() {
        assertEquals("OVER", alertLevel(1200.0, 1000.0))
    }

    @Test
    fun `alert is WARN when spending is at least 80 percent of max`() {
        assertEquals("WARN", alertLevel(850.0, 1000.0))
    }

    @Test
    fun `alert is OK when spending is comfortably under budget`() {
        assertEquals("OK", alertLevel(400.0, 1000.0))
    }

    @Test
    fun `alert is NONE when no max goal is set`() {
        assertEquals("NONE", alertLevel(400.0, 0.0))
    }

    // ---- Budget progress status ----

    @Test
    fun `status is OVER_BUDGET when over the maximum`() {
        assertEquals("OVER_BUDGET", spendingStatus(1100.0, 500.0, 1000.0))
    }

    @Test
    fun `status is UNDER_BUDGET when below the minimum`() {
        assertEquals("UNDER_BUDGET", spendingStatus(300.0, 500.0, 1000.0))
    }

    @Test
    fun `status is ON_TRACK when between min and max`() {
        assertEquals("ON_TRACK", spendingStatus(700.0, 500.0, 1000.0))
    }
}
