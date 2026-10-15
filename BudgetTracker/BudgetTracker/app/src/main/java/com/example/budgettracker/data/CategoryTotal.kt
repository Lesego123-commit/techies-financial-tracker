package com.example.budgettracker.data

/**
 * Lightweight data class representing the total amount spent in one category over a period.
 *
 * It is NOT stored in Firebase directly – instead it is calculated in code by the
 * [FirebaseRepository] by grouping a user's [ExpenseEntry] records by categoryName and
 * summing their amounts. It is used by the Category Totals screen and the spending graph.
 *
 * Attributes:
 *  - categoryName: the name of the category.
 *  - total:        the summed amount spent in that category for the selected period.
 */
data class CategoryTotal(
    val categoryName: String = "",
    val total: Double = 0.0
)
