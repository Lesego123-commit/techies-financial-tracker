package com.example.budgettracker.data

/**
 * Model class representing a spending category (e.g. Groceries, Entertainment, Transport)
 * stored in the online Firebase Realtime Database under /categories/{userId}/{id}.
 *
 * Attributes / references:
 *  - id:     unique key of this category (the Firebase push key).
 *  - userId: reference to the owning [User].uid – links the category to one user.
 *  - name:   display name of the category.
 *
 * All properties have default values so Firebase can deserialise the object.
 */
data class Category(
    var id: String = "",
    var userId: String = "",
    var name: String = ""
)
