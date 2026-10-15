package com.example.budgettracker.data

/**
 * Model class representing the user's monthly spending goals, stored in the online
 * Firebase Realtime Database under /goals/{userId}.
 *
 * Each user has exactly one goal record (keyed by userId), so saving again overwrites it.
 *
 * Attributes / references:
 *  - userId:  reference to the owning [User].uid (also the Firebase key for this record).
 *  - minGoal: minimum amount the user intends to spend per month.
 *  - maxGoal: maximum monthly budget cap.
 *
 * Default values allow Firebase to deserialise the object.
 */
data class Goal(
    var userId: String = "",
    var minGoal: Double = 0.0,
    var maxGoal: Double = 0.0
)
