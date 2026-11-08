# Contributing to DroidIsland

Thank you for your interest in contributing to **DroidIsland**! We welcome contributions from developers, designers, and testers worldwide.

---

## 🧭 Project Architecture & Philosophy

DroidIsland is designed around three fundamental principles:
1. **Camera Cutout Absorption:** In the idle collapsed state, the island uses a 100% OLED black mask to physically absorb the front punch-hole camera lens. Expanding animations organically spring outward without ever drawing artificial double rings over the hardware lens.
2. **Material You Expressive / Dark OLED Design:** High contrast surfaces (`#0B0E14`), pastel status colors (Sky, Lilac, Peach, Mint), bouncy spring physics, and tactile haptic feedback.
3. **100% Privacy & Zero Telemetry:** The app operates strictly offline. No analytics, tracking IDs, or remote server transmissions. Settings are stored locally in Jetpack DataStore.

---

## 🛠️ Getting Started

### Prerequisites
- **JDK 17** (Temurin, Corretto, or Android Studio JBR).
- **Android Studio** (Hedgehog 2023.1.1 or newer recommended).
- **Android SDK:** Compile SDK 34, Target SDK 34, Minimum SDK 26 (Android 8.0+).

### Cloning & Building
```bash
# Clone the repository
git clone https://github.com/miory-cane/DroidIsland.git
cd DroidIsland

# Run code check and lint
./gradlew check

# Build debug APK
./gradlew assembleDebug

# Build release bundle
./gradlew bundleRelease
```

---

## 📐 Coding Standards

1. **Jetpack Compose Guidelines:**
   - Adhere to Unidirectional Data Flow (UDF).
   - Use `StateFlow` and `@Immutable` data models.
   - Screen structure follows the 4 primary tabs: `DashboardScreen`, `TriggersScreen`, `SettingsScreen`, and `TestsScreen`.
2. **Cutout Alignment & Insets:**
   - Always verify that overlay coordinates correctly respect `WindowInsetsCompat.getDisplayCutout()`.
   - Never hardcode screen margins or assume top-center camera placement.
3. **Services & Background Architecture:**
   - `IslandOverlayService`: Controls the floating Compose overlay window via `WindowManager`.
   - `IslandAccessibilityService`: Detects fullscreen application transitions to hide or reposition the overlay.
   - `IslandNotificationListener`: Interacts with Android MediaSession to feed album artwork and playback states.
   - `BluetoothHeadphoneManager`: Detects connected audio peripherals and battery telemetry.

---

## 🚀 Submitting a Pull Request (PR)

1. **Fork & Branch:** Create a feature branch from `main`:
   ```bash
   git checkout -b feature/your-awesome-feature
   ```
2. **Commit Conventions:** Follow Conventional Commits:
   - `feat: add custom EQ visualizer preset`
   - `fix: adjust top padding for teardrop camera cutouts`
   - `docs: update Play Store metadata`
3. **Verify Before Pushing:**
   Ensure your code compiles cleanly:
   ```bash
   ./gradlew check assembleDebug
   ```
4. **Open PR:** Submit a PR with a clear description, device specifications, and screenshots.

---

## 📄 License
By contributing to DroidIsland, you agree that your contributions will be licensed under the project's [MIT License](LICENSE).
