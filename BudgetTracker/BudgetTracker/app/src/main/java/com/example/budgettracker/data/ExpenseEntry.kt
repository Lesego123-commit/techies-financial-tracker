package com.example.budgettracker.data

/**
 * Model class representing a single expense entry stored in the online Firebase
 * Realtime Database under /expenses/{userId}/{id}.
 *
 * Attributes / references:
 *  - id:           unique key of the entry (the Firebase push key).
 *  - userId:       reference to the owning [User].uid.
 *  - categoryId:   reference to the [Category].id this expense belongs to.
 *  - categoryName: cached category name, stored for quick display in lists and charts.
 *  - description:  short description of the expense.
 *  - amount:       amount spent, in Rands.
 *  - date:         date of the expense in yyyy-MM-dd format (string-sortable).
 *  - startTime:    start time of the activity in HH:mm format.
 *  - endTime:      end time of the activity in HH:mm format.
 *  - photoPath:    optional local file path / URI to an attached receipt photo.
 *
 * Default values are supplied so Firebase can construct the object during deserialisation.
 */
data class ExpenseEntry(
    var id: String = "",
    var userId: String = "",
    var categoryId: String = "",
    var categoryName: String = "",
    var description: String = "",
    var amount: Double = 0.0,
    var date: String = "",
    var startTime: String = "",
    var endTime: String = "",
    var photoPath: String? = null
)
