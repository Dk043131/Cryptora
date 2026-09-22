# CRYPTORA — Secure Chat

> **A privacy-focused secure communication Android application engineered for professionals and enterprise teams.**  
> Features hardware-backed end-to-end encryption (AES-256-GCM), time-limited messages with automatic cryptographic key revocation, controlled forwarding with sender approval, and tamper-evident forward trails.

---

## 1. Technical Stack

- **Language:** Kotlin 2.0+
- **UI Framework:** Jetpack Compose with Material 3 Design System
- **Architecture:** Clean Architecture (Presentation, Domain, Data, Core, DI) + MVVM
- **Dependency Injection:** Dagger Hilt 2.51+
- **Asynchronous & Reactive:** Kotlin Coroutines + StateFlow / SharedFlow
- **Navigation:** Jetpack Navigation Compose with centralized, decoupled `AppNavigator`
- **Local Persistence:** Room Database (Offline synchronization) + Encrypted DataStore / Preferences
- **Networking:** Retrofit 2 + OkHttp 4 + Kotlinx Serialization (Zero-plaintext logging)
- **Security & Keystore:** AndroidKeyStore + AES-256-GCM + EncryptedSharedPreferences (AndroidX Security Crypto)
- **Build System:** Gradle Kotlin DSL with Version Catalog (`libs.versions.toml`)

---

## 2. Clean Architecture Package Structure

```
com.cryptora.securechat
├── CryptoraApp.kt                     # Application class with @HiltAndroidApp
├── MainActivity.kt                    # Single Activity entry point with @AndroidEntryPoint
│
├── core/                              # Infrastructure and shared system primitives
│   ├── common/                        # Resource<T>, AppError, DispatcherProvider, Constants
│   ├── security/                      # CryptoManager, KeyManager (AndroidKeyStore), SecureStorage
│   ├── network/                       # NetworkResult, AuthInterceptor, Retrofit configuration
│   ├── database/                      # CryptoraDatabase, SyncMetadataEntity, Room DAOs
│   ├── navigation/                    # Screen routes, NavigationCommand, AppNavigator, AppNavHost
│   ├── designsystem/                  # CryptoraColors (Dark Navy/Electric Cyan), Theme, Typography, Reusable Components
│   └── utils/                         # DateTimeUtils (expiry calculators, timestamp formatters)
│
├── domain/                            # Enterprise business rules (Pure Kotlin, zero Android framework dependencies)
│   ├── model/                         # User, Conversation, Message, Attachment, Note, SecureMessagePolicy,
│   │                                  # AccessRequest, AccessGrant, ForwardEvent, AuditEvent, EncryptionMetadata
│   └── repository/                    # Repository interfaces (Auth, User, Chat, Message, Note, AccessControl, Forwarding)
│
├── data/                              # Implementations of domain repository contracts and data sources
│   ├── auth/                          # AuthRepositoryImpl
│   ├── user/                          # UserRepositoryImpl
│   ├── chat/                          # ChatRepositoryImpl
│   ├── message/                       # MessageRepositoryImpl (with AES-256-GCM encryption & auto-expiry)
│   ├── notes/                         # NoteRepositoryImpl
│   ├── access/                        # AccessControlRepositoryImpl (Request/Grant lifecycle)
│   └── forwarding/                    # ForwardingRepositoryImpl (Tamper-evident forward trails)
│
├── presentation/                      # UI layer built exclusively with Jetpack Compose & StateFlow
│   ├── common/                        # UiState<T> (Idle, Loading, Success, Error), BaseViewModel
│   └── home/                          # HomeScreen, HomeViewModel (Dashboard metrics, conversations, status cards)
│
└── di/                                # Dependency Injection modules
    ├── AppModule.kt                   # Coroutine dispatchers & AppNavigator binding
    ├── SecurityModule.kt              # KeyManager, CryptoManager, SecureStorage bindings
    ├── NetworkModule.kt               # OkHttpClient, Retrofit, Json serialization provider
    ├── DatabaseModule.kt              # Room database and DAO providers
    └── RepositoryModule.kt            # Binds all 7 domain repository interfaces to data implementations
```

---

## 3. Core Architectural Principles

### Unidirectional Data Flow (UDF)
1. **User Action / Event** triggers a method in `ViewModel`.
2. `ViewModel` invokes a `Repository` method via `viewModelScope` on an injected `CoroutineDispatcher`.
3. `Repository` performs cryptographic transformations (AES-256-GCM) and persists/fetches data.
4. `ViewModel` maps the domain `Resource<T>` into a typed `UiState<T>` StateFlow.
5. Compose UI observes the StateFlow and reacts dynamically.

### Cryptographic Abstraction & Key Management
- **Never Invent Cryptography:** Uses standard `AES/GCM/NoPadding` with 256-bit keys and 12-byte random initialization vectors (IV).
- **Hardware-Backed Keystore:** Master keys are generated inside `AndroidKeyStore`, ensuring private/symmetric keys cannot be extracted from the device memory.
- **Crypto-Shredding:** Timed messages expire based on server-authoritative time and client monotonic clocks. Once expired or manually revoked, the encryption key is deleted, rendering the ciphertext permanently unrecoverable.
- **Zero Plaintext in Logs:** Network interceptors log only transport headers—never request or response bodies.

---

## 4. How to Build & Run in Android Studio

### Prerequisites
- **Android Studio:** Hedgehog (2023.1.1), Iguana (2023.2.1), Jellyfish (2024.1.1), or Ladybug (2024.2+).
- **JDK:** Java 17 (bundled with modern Android Studio).
- **Android SDK:** Platform API 34 installed (Android 14).
- **Device / Emulator:** Android 8.0 (API Level 26) or higher.

### Steps to Run
1. Open Android Studio.
2. Select **Open** and navigate to the project directory:
   ```
   /Users/deepakkumar/Desktop/MED-CLAIM/KSR/SIH/AI ASSISTANT/SENGUNTHAR
   ```
3. Android Studio will automatically detect the Gradle Kotlin DSL (`settings.gradle.kts` and `gradle/libs.versions.toml`) and perform a Gradle Sync.
4. Once sync completes, ensure a build variant is selected (**debug** is default).
5. Connect an Android device or launch an emulator (API 26+).
6. Click the green **Run (▶)** button or press `Ctrl + R` (`Shift + F10` on Windows/Linux).

---

## 5. Security & Configuration Support

The project includes built-in separation between **development** and **production** environments in `app/build.gradle.kts`:

| Parameter | Debug (`applicationIdSuffix = ".debug"`) | Release (`isMinifyEnabled = true`) |
| :--- | :--- | :--- |
| `BASE_URL` | `https://api-dev.cryptora.app/v1/` | `https://api.cryptora.app/v1/` |
| `LOG_NETWORK_CALLS` | `true` (Headers only) | `false` |
| `SECURITY_VERIFICATION_STRICT` | `false` | `true` |

---

## 6. Next Steps for Feature Expansion
Now that the core foundation is established, subsequent development will implement:
1. **Auth Flow:** Mobile OTP verification and public-key registration screens.
2. **Chat & Timed Messaging:** Interactive countdown bubble cards with manual revocation kill-switches.
3. **Controlled Forwarding:** Approval bottom sheet and cryptographic forward signatures.
4. **Encrypted Notes Vault:** Local SQLite FTS5 private search index.
