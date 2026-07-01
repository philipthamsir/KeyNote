# KeyNote 🔑📝

KeyNote is a native, secure, and modern Android application designed for writing notes and managing passwords. Built entirely using **Kotlin** and **Jetpack Compose**, KeyNote prioritizes security with local database encryption, biometric authentication, and a rich, customizable interface.

---

## 🚀 Features

### 1. **Ultra-Secure Local Storage**
*   **Database Encryption**: Powered by **Room Database** integrated with **SQLCipher** for full database-level encryption.
*   **Dynamic Passphrase Management**: Securely derives and handles the database passphrase to prevent reverse-engineering of data.
*   **Biometric Authentication**: Utilizes the Android Biometric library (`BiometricPrompt`) to protect access to the Password Manager section.

### 2. **Intuitive Note Management**
*   **Notebook Layout**: Custom paper-like notebook line designs for a familiar writing feel.
*   **Structured Notes**: Support for rich content structures, list views, and grid views.
*   **Archive & Trash**:
    *   **Archive**: Keep your active screen clean by archiving completed notes.
    *   **Trash**: Accidental deletion protection with a recycle bin that allows you to restore or permanently delete notes.

### 3. **Secure Password Manager**
*   **Categorized Credentials**: Organize credentials under categories (e.g., Social, Work, Finance, Personal).
*   **Credential Detail Storage**: Store usernames, passwords, URLs, and notes securely.
*   **Master Password Setup**: Access is locked behind a master credential + biometric prompt verification.

### 4. **Dynamic Personalization**
*   **9 Color Theme Palette**: Switch between Red, Orange, Yellow, Green, Blue, Purple, Gray, Black (Amoled Dark), and System default colors.
*   **Full Dark Mode Support**: Sleek UI designed for both day and night use.
*   **Multi-language Support**: Easily switch languages inside the app settings (Indonesian & English).

### 5. **Data Portability**
*   **Secure Backup & Restore**: Export and import encrypted JSON backups to migrate your data securely between devices.

---

## 🛠️ Tech Stack & Libraries

*   **UI Framework**: Jetpack Compose (Material 3)
*   **Language**: Kotlin
*   **Database**: Room Database with SQLCipher
*   **Navigation**: Jetpack Navigation Compose
*   **Asynchronous Flow**: Kotlin Coroutines & Flow
*   **Security**: Android Biometrics (`androidx.biometric:biometric`), SQLCipher for Android
*   **Architecture**: MVVM (Model-View-ViewModel) with Repository pattern

---

## 📦 Getting Started

### Prerequisites
*   **Android Studio** (Koala or newer recommended)
*   **JDK 17**
*   **Android Device or Emulator** running API 26 (Android 8.0) or higher.

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/philipthamsir/KeyNote.git
   ```
2. Open the project in **Android Studio**.
3. Let Gradle sync and download dependencies.
4. Run the project on your device/emulator by clicking **Run** (`Shift + F10`).

---

## 📄 License
This project is licensed under the MIT License - see the LICENSE file for details.
