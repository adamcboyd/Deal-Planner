# Deal Planner

A comprehensive Android app for stretching food budgets through intelligent meal planning, deal tracking, pantry awareness, and budget management.

## Core Principle

**"Deals drive the meals."**

Parameters → Deals + Pantry → Meals

## Features

### ✅ Complete Implementation

- **Pantry Management**: Natural language input, barcode/code intake, and duplicate detection
- **Photo Pantry Intake**: Camera/gallery import with optional Gemini Vision and ML Kit OCR fallback
- **Deal Tracking**: Camera, gallery image, PDF, and pasted flyer OCR with regex parsing
- **Receipt Tracking**: Camera/gallery/manual receipt OCR reconciliation
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

A non-secret template is included at `local.properties.example`.
The Settings tab shows whether Gemini Vision is configured, which model the build is using, and includes a **Test AI Connection** button for real-device key/model checks.

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
   - Re-tapping "Load Demo" resets the demo budget and meal settings to that baseline for repeatable phone testing.

2. **Explore Features**:
   - **Pantry**: Add items via natural language (e.g., "2 cans black beans 15oz") or barcode/code intake
   - **Pantry Photo**: Tap Photo or Gallery to import a food label/photo
   - **Deals**: Scan flyer photos, choose flyer images, import flyer PDFs, or paste flyer text and view deal scores/details
   - **Receipts**: Scan receipt photos, choose receipt images, or paste OCR text to update spending
   - **Shopping**: See consolidated shopping list with PPU
   - **Menu**: Browse 7-day meal plan with freezer directives
   - **Budget**: Track spending and see surplus/deficit analysis
   - **Settings**: Configure dietary preferences and verify AI setup status

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

Repeated typed/photo/barcode imports merge into existing pantry rows when the app can safely identify the same item. Product barcodes only merge with the same barcode, so two different UPCs stay separate until reviewed.

### Adding Pantry Items From Photos

On the Pantry tab:

1. Tap **Photo** to capture a full-resolution app-cache image, or **Gallery** to choose an image.
2. If `gemini.api.key` is configured, Gemini Vision extracts brand, product, amount, size, dates, and clarification questions.
3. If Gemini is not configured or fails, ML Kit OCR reads visible label text and the pantry parser imports the best candidate.
4. Missing brand, amount, size, or expiration information is marked with a VERIFY badge and notes such as "What is the brand? Use Generic if none."
5. Tap the edit icon on any pantry card to correct item name, quantity, unit, size, brand, location, best-by date, notes, and verification status.

### Adding Pantry Items From Barcodes

On the Pantry tab:

1. Tap **Scan** to scan a product barcode, or type/paste a code into **Barcode / UPC** and tap **Add Code**.
2. Deal Planner creates a pantry item marked VERIFY with the barcode saved in notes.
3. Tap the edit icon to fill in the product name, brand, package size, quantity, and expiration details.

Barcode product lookup is not connected yet; barcode intake is a reviewable seeding path.

### Scanning Flyers

On the Deals tab, enter the store name or leave it as `Unknown`, then use one of four input paths:

1. **Take Flyer Photo** captures a full-resolution app-cache flyer image without saving it to the camera roll.
2. **Choose Flyer Image** imports an existing screenshot or photo.
3. **Choose Flyer PDF** renders PDF pages locally and OCRs them with ML Kit.
4. Paste flyer OCR text and tap **Process Text**.

ML Kit OCR extracts visible text, then the Deals parser looks for:

- `$3.99/lb` (per pound)
- `3 lb bag $2.99` or standalone package prices after an item name
- `2 for $10` (N for X)
- `Buy 2 Get 1 Free` (buy N get M)
- `25% off` (percent off)
- `Member Price` (coupon flag)
- `Limit 2` (purchase limits)

The built-in demo flyer covers the same formats.

Tap the edit icon on any deal card to correct OCR guesses for item name, price, unit, store, brand, size, deal type, limit, coupon flag, PPU, discount, score, confidence, and valid-until date.

### Processing Receipts

On the Receipts tab:

1. Enter the store name, or leave it as `Unknown`.
2. Paste receipt OCR text and tap **Process Text**, or use **Photo** / **Gallery**. Photo capture uses a full-resolution app-cache image for better OCR.
3. The app reconciles receipt lines against current deals and pantry items.
4. Matched receipt items update the receipt list, pantry quantities, and budget spending.
5. Low-confidence matches are marked with a review warning.
6. Tap the edit icon on any receipt item to correct the line text, quantity, total, store, match metadata, confidence, date, and review status.
7. Receipt edits and deletes adjust pantry quantities, budget spending, daily envelope, and projected spend so Pantry and Budget stay in sync.
8. Subtotal, tax, total, payment, and EBT/card lines are ignored so only grocery items affect spending.
9. Split quantity lines such as `3.25 lb @ $3.99/lb` or `2 @ $0.89` attach to the previous grocery item instead of importing as separate items.
10. OCR prices work with or without dollar signs, such as `BLACK BEANS 1.78` or `2 @ 0.89 BLACK BEANS 1.78`.

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
- Daily envelope (auto-calculated from remaining budget and receipt spending)
- Projected spend from current-month receipt/budget history
- Surplus/deficit warnings
- Smart suggestions (stock up, pull from freezer, etc.)

### Checking AI Setup

The Settings tab includes **AI Pantry Photo Status**:

- If Gemini is configured, it shows the model used for pantry photo recognition.
- If Gemini is not configured, it states that pantry photos will use on-device OCR fallback.
- Placeholder keys such as `YOUR_GEMINI_API_KEY` are treated as not configured.
- **Test AI Connection** performs a small Gemini request from the phone so you can confirm the key, network, and model before testing pantry photos.

## Testing

Run unit tests:

```bash
.\gradlew.bat testDebugUnitTest
```

Tests cover:
- Pantry phrase parsing (fractions, brands, dates)
- Pantry duplicate detection/merging, including barcode-specific matching
- Deal regex patterns (all deal types)
- Meal planning (GERD-filtering, anchors)
- Budget calculations (surplus, deficit, receipt-aware projection, daily envelope recalculation)
- Receipt reconciliation (fuzzy matching, VPP, split quantities, dollar/no-dollar OCR prices)
- Gemini configuration guardrails

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
- Load Demo resets pantry, deals, receipts, meal plans, default meal settings, and the demo budget baseline.
- Pantry parser handles quantity, brand, size, location, dates, low-confidence review flags, and duplicate merging.
- Pantry screen supports typed entry, barcode scan/manual code intake, photo import, and gallery import.
- Typed, photo/OCR, AI, and barcode pantry imports upsert safe duplicates instead of creating repeated pantry rows.
- Barcode/code pantry entries create VERIFY items with the barcode preserved in notes.
- Pantry cards can be edited after typed, barcode/code, OCR, or AI import so VERIFY items can be corrected during phone testing.
- Deals parser handles price/lb, package prices, N-for-X, buy-N-get-M, percent-off, Member Price/coupon flags, and limits.
- Deals parser is covered against bundled demo flyer structures including multi-line names and modifiers.
- Deal cards can be edited after flyer photo/image/PDF/text import so low-confidence OCR results can be corrected during phone testing.
- Camera capture uses app-private full-resolution image files instead of low-resolution preview bitmaps.
- Deals screen imports flyer photos, gallery images, PDFs, and pasted flyer OCR text with store-aware deal creation.
- Receipts screen imports receipt photos, gallery images, and pasted OCR text through ML Kit OCR/reconciliation.
- Receipt reconciliation handles fuzzy matching and split receipt quantity lines, including weighted price-per-pound lines.
- Receipt reconciliation accepts item totals and inline quantity lines when OCR drops dollar signs.
- Receipt reconciliation ignores subtotal, tax, total, savings, and payment/tender lines.
- Receipt reconciliation rounds imported receipt totals to cents before budget updates.
- Receipt cards can be edited after photo, gallery, or pasted OCR import so review warnings can be corrected during phone testing.
- Receipt imports, edits, and deletes adjust budget spending totals, daily envelope, and receipt-aware projected spend.
- Pantry-matched receipt edits and deletes adjust pantry quantities.
- Settings can test the Gemini API key/model connection from the running app.
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
- [x] Single-item pantry review/edit flow after OCR or AI import
- [x] Barcode/manual code intake for pantry seeding
- [ ] Full multi-item shelf review flow with edit-before-save
- [x] Flyer PDF import through local page rendering and OCR
- [x] Pasted flyer OCR text import
- [x] Receipt photo/gallery/manual text import
- [x] Deal review/edit flow after flyer OCR/PDF/text import
- [x] Receipt review/edit flow after OCR import
- [ ] Barcode product lookup by verified UPC/EAN
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
