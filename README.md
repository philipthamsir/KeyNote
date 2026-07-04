# KeyNote 🔑📝

KeyNote is a native, secure, and modern Android application designed for offline-first note-taking and robust credential management. Built entirely using **Kotlin** and **Jetpack Compose (Material 3)**, KeyNote integrates industry-standard encryption, local-first cryptography, biometric authentication, and a dynamically themed user experience.

---

## 🛡️ Security Pillars (v3.0 Hardening)

KeyNote is designed with a zero-trust model to ensure your sensitive notes and password vaults are secure from both local snooping and remote intercepts.

*   **🔒 PBKDF2 PIN Cryptography**: Your master PIN is never stored in plain text. It is hashed using **PBKDF2WithHmacSHA256** combined with a dynamically generated random salt.
*   **🚫 Anti-Brute-Force Lockout**: A built-in lockout mechanism blocks database login attempts temporarily after repeated incorrect PIN inputs.
*   **👁️ Screen Capture Prevention (`SecureScreen`)**: KeyNote wraps all password screens and locked note details in a secure layout component (`FLAG_SECURE`). Screenshots, screen recordings, and recent app switcher previews are blocked (rendered as black screens) to prevent screen scraping.
*   **☁️ Zero-Knowledge Cloud Backups**: Instead of using static backup passwords, KeyNote locally generates a unique 32-character encryption key (`BackupKeyManager.kt`) for backups. Google Drive backups reside in your Google account's isolated **App Data Folder** and cannot be read by any third-party.
*   **📋 Auto-Wipe Clipboard**: Whenever you copy credentials (usernames or passwords), the app triggers an automatic clipboard wipe after 30 seconds to prevent digital footprints.
*   **🛡️ Biometric Fallback Enforcer**: Strictly prevents biometric bypass. If biometric authentication is unavailable or deactivated, the app strictly requires the master PIN challenge.
*   **📦 Obfuscation & Minification**: Release builds are optimized and obfuscated using **R8/ProGuard** rules, making binary reverse-engineering extremely difficult.
*   **🧹 Log Sanitization**: All security tokens, Google Drive auth keys, and database passphrases are completely stripped from Android Logcat streams.

---

## 🚀 Key Features

### 1. **Password Vault Manager**
*   **Categorized Credentials**: Separate your accounts by groups (e.g., Social, Work, Finance, Personal).
*   **Secure Details**: Store usernames, passwords, custom URLs, and extra notes securely.
*   **One-Tap Copy**: Quick copy tools for credentials with auto-clearing clipboard security.

### 2. **Productivity Note-Taking (v2.0 Updates)**
*   **🗂️ Note Multi-Selection**: Select multiple notes at once to batch delete or batch archive them.
*   **Notebook Layout**: Ruled paper lines for a classic notebook writing feel.
*   **Archive & Trash Bin**:
    *   *Archive*: Archive completed notes to keep your workspace tidy.
    *   *Trash Bin*: Accidental deletion prevention. Restore deleted notes or permanently delete them.
*   **⚡ Click Debouncing (`SafeClick`)**: Custom safe click extensions prevent rapid double-tap bugs, preventing duplicate screens or duplicate database actions.

### 3. **Background Google Backup & Sync**
*   **WorkManager Integration (`BackupWorker.kt`)**: Automates backups in the background silently based on system constraints.
*   **Manual Export**: Export and import encrypted JSON backups locally.

### 4. **Dynamic Themes & Localization**
*   **9 Color Theme Palette**: Switch between Red, Orange, Yellow, Green, Blue, Purple, Gray, Black (AMOled Dark), and System default colors.
*   **Full Dark Mode Support**: Adaptive UI designed for both day and night use.
*   **Bilingual**: Quick language switching (English and Indonesian) inside settings.

---

## 🛠️ Tech Stack & Libraries

*   **UI Framework**: Jetpack Compose (Material 3)
*   **Language**: Kotlin
*   **Database**: Room Database with SQLCipher (Full database encryption)
*   **Background Jobs**: Android WorkManager
*   **Navigation**: Jetpack Navigation Compose
*   **Asynchronous Flow**: Kotlin Coroutines & Flow
*   **Security**: Android Biometrics (`androidx.biometric:biometric`), Android Cryptography, SQLCipher for Android
*   **Architecture**: Clean MVVM (Model-View-ViewModel) with Repository pattern

---

## 📦 Getting Started

### Prerequisites
*   **Android Studio** (Koala / Ladybug or newer recommended)
*   **JDK 17** or **JDK 21**
*   **Android Device or Emulator** running API 26 (Android 8.0) or higher.

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/philipthamsir/KeyNote.git
   ```
2. Open the project in **Android Studio**.
3. Allow Gradle to sync and download all dependencies.
4. Run the project on your device or emulator (`Shift + F10`).

---

## 📄 License
This project is licensed under the MIT License - see the LICENSE file for details.
