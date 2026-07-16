# Deal Planner

A comprehensive Android app for stretching food budgets through intelligent meal planning, deal tracking, pantry awareness, and budget management.

## Core Principle

**"Deals drive the meals."**

Parameters → Deals + Pantry → Meals

## Features

### ✅ Complete Implementation

- **Pantry Management**: Natural language input parser with duplicate detection
- **Photo Pantry Intake**: Camera/gallery import with optional Gemini Vision and ML Kit OCR fallback
- **Deal Tracking**: Camera, gallery image, and PDF flyer OCR with regex parsing
- **Meal Planning**: 7-day rule-based meal generator (no LLM required)
- **Budget Tracking**: Daily envelope system with surplus/deficit analysis
- **Receipt Reconciliation**: Fuzzy matching with Levenshtein distance
- **Shopping Lists**: Consolidated lists with PPU, deal scores, and coupon tracking
- **Offline-First**: All data stored locally in Room/SQLite

### 🎯 Core Algorithms

1. **Deal Scoring**:
   - 40% discount percentage
   - 25% price per unit vs baseline
   - 20% stackability (coupons, limits)
   - 15% utility fit

2. **Meal Planning**:
   - GERD-friendly filtering
   - Freshness-based scheduling
   - Automatic freezer portioning
   - Pantry anchor utilization

3. **Budget Management**:
   - Daily envelope calculation
   - Projected spend analysis
   - Auto-correction suggestions

## Technology Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Database**: Room (SQLite)
- **OCR**: ML Kit Text Recognition (on-device)
- **AI Vision**: Optional Gemini API pantry photo extraction
- **Architecture**: MVVM with Repository pattern
- **Testing**: JUnit + Truth

## Project Structure

```
app/
├── src/main/
│   ├── java/com/dealplanner/
│   │   ├── ai/                # Optional Gemini Vision pantry photo client
│   │   ├── data/
│   │   │   ├── model/          # Entities (PantryItem, DealItem, etc.)
│   │   │   ├── dao/            # Room DAOs
│   │   │   ├── database/       # AppDatabase + Converters
│   │   │   └── repository/     # Repository pattern
│   │   ├── ocr/                # ML Kit Text Recognition
│   │   ├── parser/             # Pantry + Deals parsers
│   │   ├── domain/             # Business logic engines
│   │   ├── ui/
│   │   │   ├── screens/        # Compose screens
│   │   │   ├── viewmodel/      # ViewModels
│   │   │   ├── navigation/     # Navigation setup
│   │   │   └── theme/          # Material 3 theme
│   │   └── MainActivity.kt
│   ├── assets/
│   │   ├── demo_flyer.txt      # Sample flyer for demo
│   │   └── demo_receipt.txt    # Sample receipt for demo
│   └── res/
└── src/test/                   # Unit tests
```

## Setup Instructions

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- Android SDK 26+ (minimum)
- Android SDK 34 installed for compileSdk
- JDK 17 or newer

### Optional Gemini Vision Setup

The app works without a cloud key by falling back to ML Kit label OCR. To enable AI pantry photo identification, add this to `local.properties`:

```properties
gemini.api.key=YOUR_GEMINI_API_KEY
gemini.model=gemini-3.5-flash
```

Do not commit `local.properties`; it is ignored by Git.

### Build & Run

1. **Clone/Open Project**:
   ```powershell
   cd C:\Users\adamc\AndroidStudioProjects\Deal_Planner
   # Open this directory in Android Studio
   ```

2. **Sync Gradle**:
   - Android Studio will auto-detect the project
   - Click "Sync Now" when prompted
   - Wait for Gradle sync and dependency download

3. **Run on Emulator**:
   - Create an emulator with API 26+ (recommended: Pixel 5, API 34)
   - Click "Run" (▶) in Android Studio
   - Select your emulator

4. **Run on Device**:
   - Enable Developer Mode on your Android device
   - Enable USB Debugging
   - Connect device via USB
   - Click "Run" and select your device

Command-line verification on this Windows machine:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-20'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest assembleDebug
```

Debug APK output:

```text
C:\Users\adamc\AndroidStudioProjects\Deal_Planner\app\build\outputs\apk\debug\app-debug.apk
```

### First Launch

1. **Load Demo Data**:
   - Tap "Load Demo" button in the top-right corner
   - This seeds:
     - 5 pantry anchors (rice, pasta, oats, beans, oil)
     - 4 sample deals (pork, broccoli, mandarins, chicken)
     - Default budget ($292 food budget, $45 spent)
     - 7-day meal plan

2. **Explore Features**:
   - **Pantry**: Add items via natural language (e.g., "2 cans black beans 15oz")
   - **Pantry Photo**: Tap Photo or Gallery to import a food label/photo
   - **Deals**: Scan flyer photos, choose flyer images, or import flyer PDFs and view deal scores/details
   - **Shopping**: See consolidated shopping list with PPU
   - **Menu**: Browse 7-day meal plan with freezer directives
   - **Budget**: Track spending and see surplus/deficit analysis
   - **Settings**: Configure dietary preferences

## Usage Guide

### Adding Pantry Items

Enter natural language descriptions:

```
2 cans black beans 15oz
Great Value peanut butter 16oz in pantry
1.5 lb ground beef in freezer best by 12/25
rice 5 lb bag
frozen broccoli 12oz
```

The parser handles:
- Quantities (numeric, fractions, words)
- Units (lb, oz, cans, etc.)
- Brands (Great Value, Kroger, etc.)
- Locations (pantry, fridge, freezer)
- Dates (opened, best by)
- Forms (canned, frozen, fresh)

### Adding Pantry Items From Photos

On the Pantry tab:

1. Tap **Photo** to capture an item, or **Gallery** to choose an image.
2. If `gemini.api.key` is configured, Gemini Vision extracts brand, product, amount, size, dates, and clarification questions.
3. If Gemini is not configured or fails, ML Kit OCR reads visible label text and the pantry parser imports the best candidate.
4. Missing brand, amount, size, or expiration information is marked with a VERIFY badge and notes such as "What is the brand? Use Generic if none."

### Scanning Flyers

On the Deals tab, use one of three input paths:

1. **Take Flyer Photo** captures a flyer image without saving it to the camera roll.
2. **Choose Flyer Image** imports an existing screenshot or photo.
3. **Choose Flyer PDF** renders PDF pages locally and OCRs them with ML Kit.

ML Kit OCR extracts visible text, then the Deals parser looks for:

- `$3.99/lb` (per pound)
- `2 for $10` (N for X)
- `Buy 2 Get 1 Free` (buy N get M)
- `25% off` (percent off)
- `Member Price` (coupon flag)
- `Limit 2` (purchase limits)

The built-in demo flyer covers the same formats.

### Meal Planning

Click "Generate" in the Menu tab to create a 7-day plan:
- Breakfast uses pantry anchors (oats, cereal) if enabled
- Lunch/Dinner pairs: Protein + Veg + Starch
- Proteins from top-scored deals
- Vegetables filtered by dietary preferences
- Freezer directives for bulk purchases

### Budget Management

The Budget tab shows:
- Current balance
- Daily envelope (auto-calculated)
- Projected spend
- Surplus/deficit warnings
- Smart suggestions (stock up, pull from freezer, etc.)

## Testing

Run unit tests:

```bash
.\gradlew.bat testDebugUnitTest
```

Tests cover:
- Pantry phrase parsing (fractions, brands, dates)
- Deal regex patterns (all deal types)
- Meal planning (GERD-filtering, anchors)
- Budget calculations (surplus, deficit)
- Receipt reconciliation (fuzzy matching, VPP)

## Key Algorithms

### Pantry Phrase Parser

```kotlin
// Input: "2 cans Great Value black beans 15oz in pantry"
// Output: PantryItem(
//   item = "black beans",
//   qty = 2.0,
//   unit = "can",
//   size = "15oz",
//   brand = "Great Value",
//   location = "pantry"
// )
```

### Deal Score Formula

```
dealScore = 0.40 × (discountPercent / 100)
          + 0.25 × ((baseline - PPU) / baseline)
          + 0.20 × stackability
          + 0.15 × utilityFit
```

### Value Per Pound (VPP)

```
VPP = packagePrice / (packageWeight / servingSize)
```

For proteins (default 0.5 lb servings):
- 2 lb package @ $6.00 = $6 / (2 / 0.5) = $1.50/serving

## Verified Status

As of the latest local pass:

- Builds debug APK successfully.
- Unit tests pass with `testDebugUnitTest`.
- Pantry parser handles quantity, brand, size, location, dates, low-confidence review flags, and duplicate merging.
- Deals parser handles price/lb, N-for-X, buy-N-get-M, percent-off, Member Price/coupon flags, and limits.
- Deals screen imports flyer photos, gallery images, and PDFs through ML Kit OCR.
- Receipt reconciliation handles fuzzy matching and split receipt quantity lines.
- Phone install was not verified because `adb devices` showed no connected/authorized device.

## Constraints & Design Decisions

1. **Offline-First Core**: Room database remains the source of truth for app data
2. **Optional Cloud AI**: Gemini Vision is used only when a local API key is configured
3. **On-Device OCR Fallback**: ML Kit Text Recognition keeps photo intake usable without a key
4. **Rules-Based Meals**: No AI, pure algorithmic logic
5. **GERD-Friendly**: Excludes acidic vegetables (tomatoes, peppers, onions)
6. **Anchor Strategy**: Pantry staples (rice, pasta, oats) drive meal plans

## Future Enhancements

- [x] Camera/gallery pantry photo import
- [ ] Full multi-item shelf review flow with edit-before-save
- [x] Flyer PDF import through local page rendering and OCR
- [ ] Barcode scanning for pantry seeding
- [ ] Nutrition lookup by verified brand/product/size
- [ ] Export shopping list as PDF
- [ ] Weekly budget reports
- [ ] Custom dietary restrictions
- [ ] Multi-store comparison

## Troubleshooting

### Gradle Sync Issues

```bash
# Clean and rebuild
./gradlew clean build
```

### Emulator Performance

- Use x86_64 emulator images (faster)
- Enable hardware acceleration
- Allocate 2GB+ RAM to emulator

### Room Database Errors

- Database version is 1
- Schema exported to `app/schemas/`
- Migrations handled in `AppDatabase.kt`

## License

This project is a demonstration MVP. All dependencies follow their respective licenses:
- Jetpack Compose: Apache 2.0
- Room: Apache 2.0
- ML Kit: Google Terms of Service
- ZXing: Apache 2.0

## Contact

For issues or questions, please open a GitHub issue.

---

**Version**: 1.0.0
**Compile SDK**: 34 (Android 14)
**Target SDK**: 33
**Min SDK**: 26 (Android 8.0)
