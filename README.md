# Deal Planner Android App

A native Android wrapper for the Deal Planner meal planning application.

**Backend is pre-configured** - AI features work automatically, no setup needed!

---

## 📱 Building the App

### Requirements
- Android Studio (Hedgehog 2023.1.1+)
- Android SDK 35
- JDK 17
- Physical Android device or emulator (API 24+)

### Build in Android Studio

1. Open this folder in Android Studio
2. Wait for Gradle sync to complete
3. Connect Android device via USB (enable USB debugging)
4. Click Run (Shift+F10)

### Command Line Build

```bash
cd DealPlannerAndroid
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk

adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## ✅ Features

| Feature | Status |
|---------|--------|
| Recipe Library (75+ recipes) | ✅ |
| Meal Plan Generation | ✅ |
| Pantry Management | ✅ |
| Voice Input | ✅ |
| Receipt Scanning (AI) | ✅ |
| Flyer Scanning (AI) | ✅ |
| AI Recipe Discovery | ✅ |
| Native Camera Access | ✅ |

---

## 🔐 Permissions

- **INTERNET**: AI features
- **CAMERA**: Native camera for scanning
- **RECORD_AUDIO**: Voice input

---

## 🐛 Troubleshooting

### Gradle Sync Failed
- Update Android Studio to latest version
- File → Invalidate Caches and Restart

### Camera Not Working
- Make sure you granted camera permission when prompted
- Check Settings → Apps → Deal Planner → Permissions
