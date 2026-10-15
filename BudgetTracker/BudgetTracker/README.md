# Techies Financial Tracker – Android App (PROG7313 Part 3)

A personal budgeting Android application built with **Kotlin** and a **Firebase Realtime Database**, as part of the PROG7313 Portfolio of Evidence (Part 3 – Final App Development).

> 📖 **The full project documentation, feature list, Firebase setup steps, screenshots, demo-video link and references are in the [main README at the repository root](../../README.md).**

## Quick summary

- **Online database:** all users, categories, expenses and goals are stored in Firebase Realtime Database via `FirebaseRepository`.
- **Part 3 features:** a per-category spending graph with min/max goal lines, and a visual budget-progress screen.
- **Own features:** Smart Spending Alerts and a Budgeting Tips screen.
- **Charts:** MPAndroidChart. **Images:** Glide. **Async:** Kotlin Coroutines.

## To run

1. Open this `BudgetTracker` folder in Android Studio and let Gradle sync.
2. Add your `google-services.json` to `app/` (see the main README for the Firebase steps).
3. Run on a physical device or emulator (API 26+).

---
*PROG7313 – Programming 3B | Portfolio of Evidence Part 3 | Group: Techies*
