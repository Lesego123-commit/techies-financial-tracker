
![Android CI Build & Test](https://github.com/EMGPMD/prog7313-poe-techies2/actions/workflows/build.yml/badge.svg)

# Techies Financial Tracker 

A personal budgeting Android app built with **Kotlin** and a **Firebase Realtime Database** for the PROG7313 Portfolio of Evidence (Part 3 – Final App Development).

Techies Financial Tracker (TFT) helps a user take control of their money: they capture their spending, organise it into categories, set monthly minimum and maximum budget goals, and then **see** how they are doing through a category spending graph and a visual budget-progress screen. All of the user's data is stored online in Firebase so it is never lost and could be accessed from another device.

##  Demonstration Video

**Watch the demo here:** <!-- TODO: paste your unlisted YouTube link here --> `https://youtu.be/your-video-id`

The video demonstrates every feature running on a physical Android phone, with a voice-over, and shows the data being written to and read from the online Firebase database.

---

## Features

### Core data capture and views (carried over from the prototype and now backed by Firebase)

- **User registration & login** – each user registers a unique username and password; credentials are checked against the online database.
- **Spending categories** – create custom categories such as Groceries, Transport and Entertainment.
- **Expense entries** – log an expense with a description, amount, date, start time, end time, category and an optional receipt photo (camera or gallery).
- **Expense list** – view all expenses for a user-selectable date range; tap a photo-linked entry to view the receipt full-screen.
- **Category totals** – see the total spent per category for any selected period.
- **Monthly goals** – set a minimum and a maximum monthly spending goal.

### Part 3 required features

- ** Spending graph with goals** – a bar chart (MPAndroidChart) showing the amount spent **per category** over a **user-selectable period**, with the user's **minimum and maximum goals** drawn on the chart as horizontal reference lines.
- ** Visual budget progress** – a screen that visually shows how well the user is doing at staying **between their minimum and maximum goals over the past month**, using a colour-coded status banner (Under Budget / On Track / Over Budget), a progress bar with min/max markers, and a numeric breakdown.
- ** Online database** – the app stores **all** of its data (users, categories, expenses and goals) in a **Firebase Realtime Database**, reading from and writing to it through a single `FirebaseRepository`.

### Our two own features (as described in the design document)

> These are the two custom features required by the rubric. Both come from our Part 1 design document.

**1. Smart Spending Alerts.**
Every time a new expense is saved, the app recalculates the user's total spending for the current month and compares it to their maximum monthly goal. If the new expense pushes them to **80% or more** of their budget, a warning dialog appears; if they go **over** their maximum goal, a stronger alert is shown. This is *proactive* – the user is warned before overspending becomes a problem, rather than only finding out afterwards. *(See `AddExpenseActivity.checkSmartSpendingAlert()`.)*

**2. Budgeting Tips.**
A dedicated Budgeting Tips screen gives the user practical financial advice. It shows a featured **"Tip of the day"** with a **New Tip** button that shuffles to a random tip, plus a full list of tips grouped by topic (Saving Strategies, Reducing Debt, Grocery Budgeting, Emergency Funds, and more). The tips are stored locally in the app so they work offline. *(See `TipsActivity`.)*

### Other improvements made for the final app

- A new **app icon and brand assets** – a gold coin with an upward growth arrow on a teal-to-green gradient, matching the TFT brand from the design document.
- Firebase **offline persistence** so the app still shows data during brief connection drops and syncs back automatically.
- Input is disabled while network calls are in flight, with clear error messages if the database can't be reached.

---

## Screenshots

<p align="center">
  <img src="WhatsApp%20Image%202026-04-21%20at%2015.36.35.jpeg" width="220" />
  <img src="WhatsApp%20Image%202026-04-21%20at%2015.36.58.jpeg" width="220" />
  <img src="WhatsApp%20Image%202026-04-21%20at%2015.37.14.jpeg" width="220" />
</p>
<p align="center">
  <img src="WhatsApp%20Image%202026-04-21%20at%2015.37.33.jpeg" width="220" />
  <img src="WhatsApp%20Image%202026-04-21%20at%2015.37.50.jpeg" width="220" />
  <img src="WhatsApp%20Image%202026-04-21%20at%2015.38.06.jpeg" width="220" />
</p>

> _These are development screenshots. Capture fresh screenshots of the final Part 3 UI on a physical phone before submission. The final UI is also shown in the demonstration video._

---

## Tech Stack & Architecture

| Area | Choice |
|------|--------|
| Language | Kotlin |
| Online database | Firebase Realtime Database |
| Charts | MPAndroidChart |
| Async | Kotlin Coroutines (`Task.await()` via `kotlinx-coroutines-play-services`) |
| Image loading | Glide |
| UI | Material Components, ViewBinding, RecyclerView + ListAdapter/DiffUtil |
| Build | Gradle 8.2, AGP 8.2, Google Services plugin |
| CI | GitHub Actions |

The app follows a simple, layered structure. Every screen is an `Activity` that talks to the database **only** through `FirebaseRepository`, which keeps all of the data-access logic (reads and writes) in one place. The model classes (`User`, `Category`, `ExpenseEntry`, `Goal`) are plain Kotlin data classes with default values so Firebase can serialise and deserialise them, and each one references its owning user through a `userId`.

### Firebase data layout

```
/users/{uid}                    -> User      (uid = sanitised username)
/categories/{uid}/{categoryId}  -> Category
/expenses/{uid}/{expenseId}     -> ExpenseEntry
/goals/{uid}                    -> Goal
```

The `{uid}` key is the reference that ties all of a single user's records together.

---

## 🔥 Firebase Setup

**The app is already connected to a live Firebase project (`TechiesBudgetTracker`), and `app/google-services.json` is committed to this repository — so it builds and runs straight after cloning, with no extra Firebase setup required.**

For reference, the online database was set up as follows:

1. Go to the [Firebase console](https://console.firebase.google.com/) and **Add project** (any name, e.g. *Techies Financial Tracker*). Google Analytics is optional.
2. Inside the project, **Add app → Android**. Use the package name **`com.example.budgettracker`**. Register the app.
3. **Download `google-services.json`** and place it in `BudgetTracker/BudgetTracker/app/`.
4. In the left menu choose **Build → Realtime Database → Create Database**. Pick a location and start in **test mode** (this sets rules that allow reads/writes while developing).
5. Build and run the app. Registering a user and adding an expense will now read and write to your online database, which you can watch update live in the Firebase console.

> The Realtime Database security rules allow reads and writes (the app uses its own username login rather than Firebase Authentication). These rules are **permanent**, so the database does not expire:
> ```json
> { "rules": { ".read": true, ".write": true } }
> ```

---

## ▶️ How to Build and Run

1. Open the `BudgetTracker/BudgetTracker` folder in **Android Studio** (Giraffe or newer) and let Gradle sync. (Android Studio will regenerate the Gradle wrapper if needed.)
2. Add your `google-services.json` as described above.
3. Connect a physical Android phone (API 26+) with USB debugging enabled, or use an emulator.
4. Press **Run ▶**.

The built debug APK is produced at `app/build/outputs/apk/debug/app-debug.apk`.

---

##  Automated Testing & GitHub Actions

This project uses **GitHub Actions** (`.github/workflows/build.yml`) to automatically build the app and run the unit tests on every push and pull request, proving the code works on more than one machine. The workflow:

1. Checks out the code and sets up JDK 17 and the Android SDK.
2. Sets up Gradle 8.2.
3. Runs the JUnit unit tests (`testDebugUnitTest`).
4. Builds the debug APK (`assembleDebug`) and uploads it as a downloadable artifact.

The unit tests (`app/src/test/.../BudgetTrackerUnitTest.kt`) cover input validation, amount formatting, the Firebase username-key sanitising, the smart-alert thresholds, and the budget-progress status logic.

---

##  Project Structure

```
app/src/main/java/com/example/budgettracker/
├── BudgetTrackerApp.kt          # Application class – enables Firebase offline persistence
├── data/                        # Models + the Firebase data layer
│   ├── User.kt / Category.kt / ExpenseEntry.kt / Goal.kt
│   ├── CategoryTotal.kt         # Calculated totals (not stored)
│   └── FirebaseRepository.kt    # All reads/writes to the online database
├── ui/                          # All Activities (screens)
│   ├── LoginActivity.kt / RegisterActivity.kt / MainActivity.kt
│   ├── AddCategoryActivity.kt / AddExpenseActivity.kt
│   ├── ExpenseListActivity.kt / CategoryTotalsActivity.kt
│   ├── GoalSettingsActivity.kt / PhotoViewActivity.kt
│   ├── GraphActivity.kt         # Part 3: spending graph with goal lines
│   ├── BudgetProgressActivity.kt# Part 3: visual budget progress
│   └── TipsActivity.kt          # Own feature: budgeting tips
└── adapter/                     # RecyclerView adapters
```

---

##  Design Considerations

The interface uses a consistent green/teal financial palette with a warm amber accent, Material components, and clear, single-purpose screens reached from one home dashboard. Destructive and important actions (log out, over-budget alerts) are confirmed with dialogs. The colour language is reused throughout: green means "on track", amber means "caution / under budget", and red means "over budget", so the user learns it once and recognises it everywhere (graph goal lines, the progress screen and the smart alerts all follow it).

---

## Team

- Lesego Masike – ST10058999
- Sameer Singh – ST10454205
- Senamiso Ndlovu – ST10456656

## References

- Google (2025) *Firebase Realtime Database documentation*. Available at: https://firebase.google.com/docs/database (Accessed: 2026).
- PhilJay (2025) *MPAndroidChart*. Available at: https://github.com/PhilJay/MPAndroidChart (Accessed: 2026).
- IMAD5112 (2025) *GitHub Actions build.yml*. Available at: https://github.com/IMAD5112/Github-actions/blob/main/.github/workflows/build.yml (Accessed: 03 November 2025).
- GitHub Marketplace (2025) *Automated build Android app with GitHub Action*. Available at: https://github.com/marketplace/actions/automated-build-android-app-with-github-action (Accessed: 03 November 2025).

---
*PROG7313 – Programming 3B | Portfolio of Evidence Part 3 | Group: Techies*
