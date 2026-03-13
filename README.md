# 💰 FinFlow — Smart Personal Finance Tracker

**FinFlow** is a modern, high-performance Android fintech application designed to help users manage their personal finances with precision and ease. Built entirely with **Jetpack Compose** and following **Material 3** design guidelines, FinFlow offers a seamless, intuitive experience for tracking income, expenses, and internal transfers using a robust ledger-based accounting system.

---

## 🚀 Features

-   **Income & Expense Tracking:** Effortlessly record every financial activity with detailed categorization.
-   **Multiple Accounts Support:** Manage Bank accounts, Wallets, and Credit Cards in one place.
-   **Internal Account Transfers:** Move money between accounts with a secure double-entry system.
-   **Double-Entry Ledger System:** Ensures data integrity by treating every transaction as a ledger entry.
-   **Transaction Consolidation Engine:** Smartly merges related transfer records for a clean UI view.
-   **Financial Health Analytics:** Visualize spending patterns and net worth trends.
-   **Spending Insights Dashboard:** At-a-glance summary of your monthly financial standing.
-   **Category-Based Tracking:** Deep dive into where your money goes with custom categories.
-   **Material 3 UI:** Modern, accessible, and beautiful interface with dynamic color support.
-   **Advanced History View:** Collapsible transaction groups with powerful search and filtering.

---

## 🏗️ Architecture

FinFlow is built on the **MVVM (Model-View-ViewModel)** architecture, emphasizing a **Single Source of Truth (SSOT)** through its Ledger-based model.

-   **Ledger-as-SSOT:** Unlike traditional apps that store static balances, FinFlow treats the transaction history as the ultimate truth.
-   **Derived Balances:** Account balances are calculated in real-time from the transaction history, preventing "balance drift" bugs.
-   **Double-Entry Transfers:** Transfers create two synchronized records (Debit from Source, Credit to Destination) to maintain accounting accuracy.
-   **Consolidation Engine:** A custom engine identifies related ledger entries (like transfers) and presents them as a single logical event to the user.

---

## 🛠️ Tech Stack

| Component | Technology |
| :--- | :--- |
| **Language** | Kotlin |
| **UI Framework** | Jetpack Compose |
| **Design System** | Material 3 |
| **Architecture** | MVVM + Repository Pattern |
| **Database** | Room (SQLite) |
| **Charts** | MPAndroidChart |
| **Concurrency** | Kotlin Coroutines & Flow |

---

## 📁 Project Structure

The codebase is organized into a clean, modular package structure:

-   **`ui/`**: Contains all Compose screens and components (Home, Transactions, Dashboard, Accounts).
-   **`data/`**: Manages data persistence, including Room Entities, DAOs, and Database configuration.
-   **`repository/`**: Acts as the mediator between the Data layer and the ViewModel.
-   **`viewmodel/`**: Holds the UI state and handles business logic.
-   **`engine/`**: The core "brain" of the app, containing logic for balance derivation and transaction consolidation.

---

## 📸 Screenshots

### Home Dashboard



### Transactions Screen



### Analytics Dashboard


---

## 🔮 Future Improvements

-   [ ] **SMS Bank Message Parsing:** Automate transaction entry by reading bank alerts.
-   [ ] **AI Spending Insights:** Personalized financial advice based on spending habits.
-   [ ] **Budget Planning Tools:** Set monthly limits for specific categories.
-   [ ] **Cloud Sync:** Securely backup data across multiple devices.
-   [ ] **Google Play Release:** Bringing FinFlow to the official Android store.

---

## ⚙️ Installation

To run this project locally, follow these steps:

1.  **Clone the repository:**
    ```sh
    git clone https://github.com/yourusername/FinFlow.git
    ```
2.  **Open in Android Studio:**
    Launch Android Studio and select `Open...`, then navigate to the cloned folder.
3.  **Sync Project:**
    Allow Gradle to download dependencies and sync the project.
4.  **Run:**
    Select your emulator or physical device and click the **Run** button.

---

## ✍️ Author

**Tanushree**  
Final Year Computer Science Student  
Android Developer | Kotlin | Jetpack Compose

---
*FinFlow — Precision in every penny.*
