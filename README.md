# Auto DND — Geofence-Based Automatic Do Not Disturb for Campus

A native Android application written in Kotlin and Jetpack Compose that automatically enables Do Not Disturb (Priority Only) when your phone enters your college or campus geofence, and restores normal sound/notifications when you leave.

---

## 🚀 Key Features

- **Battery-Efficient Geofencing**: Powered exclusively by Google Play Services `GeofencingClient` (Fused Location Provider) without background GPS battery drains.
- **Do Not Disturb Interruption Filter**: Automatically transitions `NotificationManager` interruption filter:
  - **ENTER Geofence** ➔ `NotificationManager.INTERRUPTION_FILTER_PRIORITY` (Silent/Priority mode during classes)
  - **EXIT Geofence** ➔ `NotificationManager.INTERRUPTION_FILTER_ALL` (Normal interruptions restored)
- **Survives Reboots & App Kill**: Re-registers active geofences on device boot (`RECEIVE_BOOT_COMPLETED` & `MY_PACKAGE_REPLACED`).
- **Complete Onboarding & Permission Workflow**:
  - `ACCESS_NOTIFICATION_POLICY` (Direct deep-link to Android DND settings)
  - `ACCESS_FINE_LOCATION` (Foreground precise location)
  - `ACCESS_BACKGROUND_LOCATION` (Android 10+ "Allow all the time" flow)
  - Battery Optimization exemption prompt (`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`)
- **Interactive Map / Radius Configurator**:
  - Adjust latitude, longitude, and custom radius (50m – 1000m, default 150m).
  - Quick "Current GPS" button to instantly autofill your college coordinates.
- **Built-in Transition Simulator**: Test entering/exiting transitions and verify DND switching on your phone without leaving your desk.
- **Real-Time Transition Logs**: Timeline of geofence triggers and DND mode state updates.

---

## 🛠️ How to Build and Side-Load the APK for Testing

### 1. Build the APK
Run the Gradle assemble task from the project root:
```bash
./gradlew assembleDebug
```
The compiled debug APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### 2. Side-Load onto Physical Android Device or Emulator
Connect your Android phone with USB Debugging enabled:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 3. First-Launch Setup Checklist
1. Open **Auto DND**.
2. Tap **Open DND Settings** on the first permission card and grant Do Not Disturb access to Auto DND.
3. Grant **Precise Location** and select **Allow all the time** for Background Location.
4. Set your college location (or tap **Current GPS** when on campus).
5. Toggle the **Master Switch** to **Armed**.
6. Use the **Transition Simulator** buttons (**Simulate ENTER** and **Simulate EXIT**) to verify DND toggling immediately.

---

## 📱 Architecture

- **`MainActivity`**: Single-activity Jetpack Compose container.
- **`GeofenceHelper`**: Wraps Google Play Services `GeofencingClient` to add/remove circular geofences.
- **`GeofenceBroadcastReceiver`**: Background receiver that intercepts `GEOFENCE_TRANSITION_ENTER` and `GEOFENCE_TRANSITION_EXIT` to adjust the system interruption filter.
- **`BootReceiver`**: Listens for system reboot and restores the armed geofence.
- **`DndManager`**: System `NotificationManager` interface handling interruption filters and channel notifications.
- **`GeofenceRepository`**: Persistent SharedPreferences storage for coordinates, radius, enabled state, and event logs.
- **`MainViewModel`**: StateFlow reactive state holder driving the Compose UI.

---

## ℹ️ Notes & OS Constraints
- **1–3 Minute Geofence Batching**: Android batches geofence evaluations to conserve battery. A slight delay of 1–3 minutes upon crossing the boundary is standard OS behavior.
- **OEM Battery Aggression**: For devices by Xiaomi, Oppo, Vivo, or Samsung, ensure battery optimization is disabled for this app to prevent OS background killing.
