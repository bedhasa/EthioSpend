# EthioSpend

EthioSpend is an Android personal finance application developed in Java that automatically tracks expenses by reading transaction SMS messages from CBE . It helps users monitor spending, organize transactions, and better understand their financial activities.

---

## Problem Statement

Mobile money users in Ethiopia receive transaction notifications through SMS, but there is no simple way to automatically organize and analyze these transactions. Manually checking SMS messages is time-consuming and makes it difficult to track expenses.

EthioSpend solves this problem by automatically extracting transaction details from SMS notifications and storing them in a local database for easy access and analysis.

---

## Features

- 📩 Automatically detects transaction SMS messages
- 💳 Supports CBE SMS notifications
- 📊 Dashboard with expense summary
- 📜 View transaction history
- 🔍 Search transactions
- 🏷️ Categorize expenses
- 📈 Spending charts and analytics
- 🔐 Biometric authentication
- 💾 Offline storage using Room Database

---

## Technologies Used

- Java
- Android Studio
- Room Database
- SQLite
- XML
- Material Design Components
- RecyclerView
- ViewModel & LiveData
- SMS Broadcast Receiver

---

## Project Structure

```
app/
├── src/
├── java/
├── res/
├── AndroidManifest.xml
└── build.gradle
```

---

## Requirements

- Android Studio
- Android SDK
- Java Development Kit (JDK)
- Android 8.0 (API 26) or higher

---

## Installation

1. Clone the repository.

```bash
git clone https://github.com/yourusername/EthioSpend.git
```

2. Open the project in Android Studio.

3. Allow Gradle to sync.

4. Build and run the application on an Android device or emulator.

---

## Permissions

The application requires the following permissions:

- Read SMS
- Receive SMS
- Biometric Authenticatio

---

## Future Improvements

- Cloud backup and synchronization
- Budget planning
- Monthly financial reports
- Export transactions to Excel or PDF
- Multi-language support
- AI-powered spending analysis

---

## Screenshots

Screenshots will be added soon.

---

## Author

**Bedasa Negash**
