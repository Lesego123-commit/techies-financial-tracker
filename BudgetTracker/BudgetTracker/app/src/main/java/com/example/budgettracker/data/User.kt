package com.example.budgettracker.data

/**
 * Model class representing an app user stored in the online Firebase Realtime Database.
 *
 * Attributes / references:
 *  - uid:      unique identifier for the user (the sanitised, lower-cased username).
 *              This same value is used as the key under the "users" node AND as the
 *              foreign key (userId) on every Category, ExpenseEntry and Goal that belongs
 *              to this user – i.e. it is the reference that links all of a user's data together.
 *  - username: the display username the person registered with.
 *  - password: the account password (stored as plain text for this academic project only;
 *              a production app would use Firebase Authentication with hashing).
 *
 * NOTE: Firebase requires a public no-argument constructor to deserialise objects, which is
 * why every property has a default value and is declared as a var.
 */
data class User(
    var uid: String = "",
    var username: String = "",
    var password: String = ""
)
