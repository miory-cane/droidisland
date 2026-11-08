# Privacy Policy for DroidIsland

**Effective Date:** November 9, 2026  
**Last Updated:** November 9, 2026

**DroidIsland** ("the Application", "we", "our", or "us") provides a floating interactive camera cutout overlay (dynamic island) for Android devices. We are committed to transparency and the highest standards of user privacy.

This Privacy Policy explains how information is handled when you use the Application.

---

## 1. Information Collection and Transmission: Zero Data Collection

- **No Personal Data Collected:** DroidIsland does not collect, record, harvest, transmit, or share any personally identifiable information (such as your name, email, device identifiers, IP address, or physical location).
- **No Third-Party Analytics or Advertising:** We do not integrate third-party advertising networks, analytical trackers, user tracking SDKs, or profiling telemetry.
- **No External Servers:** The Application functions entirely offline and on-device. It does not communicate with any external backend servers or cloud services.

---

## 2. Explanation of Android Permissions & Services

DroidIsland requests only the permissions strictly necessary to render and control the dynamic island overlay around the device camera cutout:

### A. Accessibility Service API (`android.permission.BIND_ACCESSIBILITY_SERVICE`)
- **Purpose:** Used strictly to detect window state transitions (such as full-screen video playback, games, or device orientation changes) and automatically realign the floating capsule with the physical display cutout (`WindowInsets.displayCutout`), or temporarily hide the island when an app requests exclusive full-screen focus.
- **Privacy Assurance:** The Accessibility Service **does NOT** read, monitor, record, or transmit user keystrokes, passwords, typed text, screen content, or financial information. All accessibility events are processed ephemerally in RAM on your device and are never saved to disk or transmitted over any network.

### B. Display Over Other Apps (`android.permission.SYSTEM_ALERT_WINDOW`)
- **Purpose:** Enables the Application to draw the dynamic island capsule on top of running applications around your camera cutout.
- **Privacy Assurance:** This permission is strictly utilized for floating UI presentation. It does not capture screenshots, record the screen, or monitor background application data.

### C. Foreground Service (`android.permission.FOREGROUND_SERVICE_SPECIAL_USE`)
- **Purpose:** Keeps the dynamic island overlay active and responsive in the background so that notifications, playback controls, and battery indicators remain accessible without system termination.
- **Notification:** A persistent system notification is displayed while the overlay service is running, allowing you to stop the service at any time.

### D. Notification Listener Service (`android.permission.BIND_NOTIFICATION_LISTENER_SERVICE`)
- **Purpose:** When enabled by the user, this allows DroidIsland to detect active media playback sessions (such as Spotify, YouTube Music, or podcast players) to display album art, track title, and media controls in the expanded island capsule.
- **Privacy Assurance:** DroidIsland does not inspect, read, or store the text or attachments of personal messages, emails, or sensitive notifications.

### E. Bluetooth Connect (`android.permission.BLUETOOTH_CONNECT`)
- **Purpose:** Declared with the `neverForLocation` flag. Used exclusively to identify when Bluetooth audio accessories (such as headphones or earbuds) are paired or connected, and to display their connection status inside the island capsule.
- **Privacy Assurance:** The Application does not track your location or monitor Bluetooth beacon telemetry.

---

## 3. Local Data Storage

DroidIsland saves user preferences and calibration settings (such as X/Y offsets, capsule dimensions, and enabled module toggles) locally on your device using encrypted Android Jetpack DataStore:
- Settings data never leaves your device.
- All stored data is immediately and permanently deleted when you uninstall the Application or choose "Clear Storage" in your device's application settings.

---

## 4. Children's Online Privacy Protection

DroidIsland is not directed to children under the age of 13. We do not knowingly collect or maintain any personal information from children.

---

## 5. Security

We take the security of your device seriously. Because DroidIsland operates completely offline without transmitting data across networks or storing sensitive data on remote servers, your device state remains isolated and safe from unauthorized external access.

---

## 6. Changes to This Policy

We may update this Privacy Policy from time to time to reflect changes in our practices or Android platform requirements. The updated policy will always be available in our GitHub repository and linked on the Google Play Store listing.

---

## 7. Contact Us

If you have questions, feedback, or concerns regarding this Privacy Policy, please open an issue in the official GitHub repository or reach out via the developer contact email listed on the Google Play Store page:
- **GitHub Issues:** [https://github.com/miory-cane/DroidIsland/issues](https://github.com/miory-cane/DroidIsland/issues)
- **Email:** support-droidisland@proton.me
