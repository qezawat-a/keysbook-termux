# KeysBook for Android

A native Android port of the Termux API key manager, bringing the fast and secure API key organization experience to mobile devices.

## 🎯 What It Does

- **Encrypted key vault** using OpenSSL-compatible AES‑256‑CBC/PBKDF2 (identical to the Termux app)
- **50+ provider catalogue** with Base URLs for services like OpenAI, Anthropic, xAI, Google, Azure, Ollama, and custom endpoints
- **Key management** – add, edit, delete, and view API keys
- **Model discovery & testing** – fetch the models a key can access and tap to test them
- **Secure clipboard** with verification and fallback for devices that block clipboard writes
- **Material‑Design UI** with dark‑mode support and smooth transitions

## 🛠️ Tech Stack

| Layer | Technology |
|-------|------------|
| **Language** | Kotlin (JVM target 17, Java 17 source compatibility) |
| **UI** | Android Jetpack Compose (Material 3) + legacy Views for compatibility |
| **Security** | AES‑256‑CBC + PBKDF2, same encryption as Termux (`~/.keysbook/vault.json`) |
| **Networking** | Java `HttpURLConnection` (no external HTTP libs) |
| **Build** | Gradle 8.13 (Kotlin DSL) with AGP 8.13.0 |
| **CI/CD** | GitHub Actions – build & release APK on tags |

## 📦 Project Structure

```
keysbook/
├── src/
│   ├── main/
│   │   ├── kotlin/          # All application code
│   │   ├── res/              # Layouts, drawables, strings, colors, themes
│   │   └── AndroidManifest.xml
│   └── ...
├── build.gradle.kts         # Modern Gradle configuration
├── keystore.properties      # Local debug signing (optional)
└── debug.keystore           # Android debug keystore (generated)
```

## 🚀 Quick Start

### Clone / Download

```bash
# If you have the repo already cloned:
cd keysbook-termux/keysbook

# Otherwise download the APK directly:
# https://github.com/qezawat-a/keysbook-termux/releases/latest
```

### Install & Run

1. **Build with CodeAssist**
   - Open the `keysbook` module in CodeAssist
   - Press **Run** (or `assemble:keysbook:debug`) – produces `keysbook-debug.apk`

2. **Install on device**
   - Transfer the APK to an Android device/emulator
   - Install normally (debug keystore signed)

3. **First launch**
   - You’ll see the **Unlock / Create Vault** screen
   - Enter a password to create or unlock the encrypted vault
   - You’re taken to the **Keys** list screen

### Key Features Walkthrough

1. **🔑 Add a key**
   - Tap **"Add key"** → select provider → fill Base URL (pre‑filled for built‑ins)
   - Enter API key (masked when displayed)
   - Save – immediately usable for model fetching

2. **🔍 View models**
   - In the key list, tap **"View models”** on any entry
   - The app fetches `/models` (or `/tags` for Ollama) and shows only models this key can access
   - Tap a model to run a lightweight chat completion (`max_tokens: 1`) and confirm it works

3. **📋 Manage keys**
   - **Copy** – securely copies the full API key to the clipboard (verifies write, fallback to editable dialog)
   - **Edit** – update provider, label, Base URL, notes
   - **Delete** – removes the key from the vault (confirmed dialog)

4. **🔐 Vault security**
   - All keys are AES‑256‑CBC encrypted with PBKDF2 (identical to Termux)
   - No plaintext keys ever stored on disk
   - Session‑locked – app returns to unlock screen after 5 min of inactivity (configurable)

## 🏭 Build & Release

### Local Development

```bash
cd keysbook-termux/keysbook
./gradlew assembleDebug   # Debug APK
./gradlew assembleRelease # Release APK (if keystore is configured)
```

**Signing options**
- **Debug** – uses the standard Android debug keystore (`debug.keystore`)
- **Release** – can be signed via GitHub Actions secrets or a local `keystore.properties`

### GitHub Actions (Automated releases)

Every time you push a tag (`v1.2.3`, for example), the workflow:

1. Checks out the repo
2. Sets up JDK 17 and Gradle
3. Decodes a production keystore from a GitHub secret
4. Builds `assembleRelease` with that keystore
5. Attaches the APK to a GitHub Release and publishes it

**To publish a release:**

```bash
git tag v1.0
git push origin v1.0
```

The workflow will handle the rest and make the APK downloadable from the release page.

### Manual Release Signing

If you prefer to sign locally:

1. Create `keystore.properties` (see example below)
2. Ensure the keystore file exists (`debug.keystore` is fine for testing)
3. Run `./gradlew assembleRelease`

```properties
# keystore.properties
storeFile=debug.keystore
storePassword=android
keyAlias=androiddebugkey
keyPassword=android
```

## 📱 Supported Environments

- **Android 8.0+** (API 26+)
- **Material You** – adapts to system theme
- **Dark Mode** – dedicated night theme included

## 🔧 Known Limitations

- No internet‑only mode – requires Wi‑Fi/cellular connectivity for model fetching
- Clipboard‑write blockers – shown as a selectable field (fallback works)
- No biometric unlock – password is the primary auth method
- Some providers (e.g., Anthropic) need custom auth headers – handled internally

## 📋 Provider Catalogue

| Provider | Base URL |
|----------|----------|
| OpenAI | `https://api.openai.com/v1` |
| Anthropic | `https://api.anthropic.com` |
| xAI (Grok) | `https://api.x.ai/v1` |
| Ollama (Local) | `http://127.0.0.1:11434/api` |
| ... and 50+ more (see `Providers.kt`) |

**Custom providers** are supported – select `Custom Provider` in the spinner and enter any Base URL.

## 🔄 Migration from Termux

The Android app is a direct port:
- **Vault format** (`vault.json`) is identical – you can move keys from Termux by copying the file
- **Provider list** matches the Termux source exactly
- **Encryption** is byte‑for‑byte compatible (OpenSSL command line swapped for in‑process crypto)

## 🆘 Getting Help

- **Issues / bugs** – open an issue in this repository
- **Feature requests** – propose new providers or UI improvements
- **Questions** – check the provider research notes (`provider_research.md`) for endpoint nuances

## 📄 License

This project is licensed under the MIT License – feel free to modify, redistribute, or contribute!

---

*Built with ❤️ for mobile API key management, inspired by the original Termux experience.*
