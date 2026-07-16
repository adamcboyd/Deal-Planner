# Deal Planner

A comprehensive Android app for stretching food budgets through intelligent meal planning, deal tracking, pantry awareness, and budget management.

## Core Principle

**"Deals drive the meals."**

Parameters → Deals + Pantry → Meals

## Features

### ✅ Complete Implementation

- **Pantry Management**: Natural language input, barcode/code lookup/intake, and duplicate detection
- **Photo Pantry Intake**: Camera/gallery import with optional Gemini Vision and ML Kit OCR fallback
- **Deal Tracking**: Camera, gallery image, PDF, and pasted flyer OCR with regex parsing
- **Receipt Tracking**: Camera/gallery/manual receipt OCR reconciliation
- **Meal Planning**: 7-day rule-based meal generator (no LLM required)
- **Budget Tracking**: Daily envelope system with surplus/deficit analysis
- **Receipt Reconciliation**: Fuzzy matching with Levenshtein distance
- **Shopping Lists**: Consolidated lists with planned-quantity estimated costs, PPU, deal scores, coupon tracking, and startup restore from current pantry/deals/settings after a plan exists
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
│   │   ├── lookup/             # Barcode product lookup
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
Rebuild the debug APK after changing `local.properties` so the key/model values are compiled into `BuildConfig`.
The phone install helper blocks `-SkipBuild` when `local.properties` is newer than the existing APK, so a newly added Gemini key is not accidentally left out of the installed build.

A non-secret template is included at `local.properties.example`.
The Settings tab shows whether Gemini Vision is configured, which model the build is using, and includes a **Test AI Connection** button for real-device key/model checks with concise API error summaries.
The app trims accidental whitespace and accepts either `gemini-3.5-flash` or `models/gemini-3.5-flash`, though the bare model code is preferred.

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

Command-line phone install helper:

```powershell
.\scripts\phone-debug-preflight.ps1
```

This checks the repo state, debug APK, APK identity/permissions, APK freshness against app source/resources/build config, ADB/device visibility, Gemini configuration without printing secrets, and Open Food Facts barcode lookup reachability.

```powershell
.\scripts\phone-debug-install.ps1
```

If the debug APK is already built and you only want to reinstall/launch on a connected phone:

```powershell
.\scripts\phone-debug-install.ps1 -SkipBuild
```

Use `-SkipBuild` only when you have not changed app code/resources, Gradle config, `local.properties`, or Gemini environment values since the APK was built.

Phone log capture helper:

```powershell
.\scripts\phone-debug-logs.ps1
```

Before reproducing a phone-only failure, use:

```powershell
.\scripts\phone-debug-logs.ps1 -Clear -Launch -DurationSeconds 90
```

Logs are saved under `phone-test-logs\`, which is ignored by Git. Start with `logcat-dealplanner-filtered.txt` when debugging a failure.

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
   - **Shopping**: See consolidated shopping list with planned-quantity estimated costs and PPU; after a plan exists, app relaunch repopulates it from current pantry/deals/settings
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
1,5 lb ground beef in freezer
Kroger yogurt 5,3oz fridge
2 cans black beans 15oz pantry best by 2026-12-31
Great Value peanut butter opened yesterday best by 2026-12-31
Great Value peanut butter opened on 2026-07-01 best by date 2026-12-31
Kroger yogurt best before 2026-12-31
milk use by 12/31/2026
milk use-by 12/31/2026
pasta exp 12/31/2026
milk expiration date 12/31/2026
yogurt best-by 2026-12-31
yogurt best by date 2026-12-31
rice 5 lb bag
frozen broccoli 12oz
```

The parser handles:
- Quantities (numeric, comma-decimal OCR, fractions, words)
- Units (lb, oz, cans, etc.)
- Brands (Great Value, Kroger, etc.)
- Locations (pantry, fridge, freezer)
- Dates (opened and best-by style cues independently, including `YYYY-MM-DD`, `opened on`, `best before`, `best-by`, `use by`, `use-by`, `exp`, `expiration date`, and `best by date`)
- Forms (canned, frozen, fresh)

Repeated typed/photo/barcode imports merge into existing pantry rows when the app can safely identify the same item. Product barcodes only merge with the same barcode, so two different UPCs stay separate until reviewed.
If the manual pantry text field is blank, **Add** shows a visible no-text status instead of silently doing nothing.

### Adding Pantry Items From Photos

On the Pantry tab:

1. Tap **Photo** to capture a full-resolution app-cache image, or **Gallery** to choose an image.
2. If `gemini.api.key` is configured, Gemini Vision extracts brand, product, amount, size, dates, and clarification questions.
3. If Gemini is not configured or fails, ML Kit OCR reads visible label text and the pantry parser imports the best candidate.
4. Missing brand, amount, size, or expiration information is marked with a VERIFY badge and notes such as "What is the brand? Use Generic if none."
5. Tap the edit icon on any pantry card to correct item name, quantity, unit, size, brand, location, best-by date, notes, and verification status. Quantity corrections accept dot or comma decimals, such as `1.5` or `1,5`.

### Adding Pantry Items From Barcodes

On the Pantry tab:

1. Tap **Scan** to scan a product barcode, or type/paste a code into **Barcode / UPC** and tap **Add Code**.
2. Deal Planner accepts plain UPC/EAN/GTIN digits or pasted label text such as `UPC: 0 12345-67890 5`, then looks up the cleaned code with Open Food Facts when the phone has network access. If pasted label text also has item numbers or dates, labeled UPC/EAN/GTIN text is preferred.
3. If a product is found, Deal Planner creates a VERIFY pantry item with the product name, brand, package quantity, barcode, and lookup source in notes.
4. If lookup misses or the phone is offline, Deal Planner still creates a reviewable barcode item with the code saved in notes.
5. If the text is blank or does not contain an 8-14 digit product barcode, Deal Planner shows `No barcode found.` instead of creating a junk pantry row.
6. Re-scanning the same barcode merges quantity into the same pantry row. Different barcodes stay separate until reviewed.
7. Tap the edit icon to fill in or correct the product name, brand, package size, quantity, location, and expiration details.

### Scanning Flyers

On the Deals tab, enter the store name or leave it as `Unknown`, then use one of four input paths:

1. **Take Flyer Photo** captures a full-resolution app-cache flyer image without saving it to the camera roll.
2. **Choose Flyer Image** imports an existing screenshot or photo.
3. **Choose Flyer PDF** renders PDF pages locally and OCRs them with ML Kit.
4. Paste flyer OCR text and tap **Process Text**.

ML Kit OCR extracts visible text, then the Deals parser looks for:

- `$3.99/lb` or `3.99/lb` (per pound)
- `2,99/lb` or `3 lb bag 2,99` when OCR uses comma decimals
- `99c/lb` or `88c` (cent-style flyer/OCR prices)
- `3 lb bag $2.99`, `3 lb bag 2.99`, or standalone package prices after an item name
- `2 for $10`, `10 for 10`, `2/$5`, or `10 / $10` (N for X)
- `Buy 2 Get 1 Free`, `Buy One Get One Free`, `Buy Two Get One Free`, or `Buy One Get One 50% off` (buy N get M)
- `BOGO Free`, `B1G1`, or `BOGO 50% off` (buy-one-get-one shorthand)
- `25% off` (percent off)
- `Member Price` (coupon flag)
- `Limit 2` (purchase limits)

The built-in demo flyer covers the same formats.
Blank pasted flyer text shows `No flyer text found.` instead of failing silently.

Tap the edit icon on any deal card to correct OCR guesses for item name, price, unit, store, brand, size, deal type, limit, coupon flag, PPU, discount, score, confidence, and valid-until date. Numeric corrections accept dot or comma decimals.

### Processing Receipts

On the Receipts tab:

1. Enter the store name, or leave it as `Unknown`.
2. Paste receipt OCR text and tap **Process Text**, or use **Photo** / **Gallery**. Photo capture uses a full-resolution app-cache image for better OCR.
3. The app reconciles receipt lines against current deals and pantry items.
4. Matched receipt items update the receipt list, pantry quantities, and budget spending.
5. Receipt header dates such as `Date: 10/27/2025` are applied to imported receipt rows when available; imports without a readable date use today.
6. Low-confidence matches are marked with a review warning.
7. Tap the edit icon on any receipt item to correct the line text, quantity, total, store, match metadata, confidence, date, and review status. Quantity, total, and confidence corrections accept dot or comma decimals.
8. Receipt edits and deletes adjust pantry quantities, budget spending, daily envelope, and projected spend so Pantry and Budget stay in sync.
9. Subtotal, tax, total, payment, card tender, EBT/card, SNAP EBT, WIC benefit, coupon, discount, savings, reward, and refund lines are ignored so only grocery purchase items affect spending.
10. Split quantity lines such as `3.25 lb @ $3.99/lb` or `2 @ $0.89` attach to the previous grocery item instead of importing as separate items.
11. OCR prices work with or without dollar signs, including comma-decimal OCR such as `BLACK BEANS 1,78` or `2 @ 0,89 BLACK BEANS 1,78`.
12. One-line weighted produce rows such as `BANANAS 1.50 lb @ $0.69/lb $1.04` are imported with the item name, weight, and total separated.
Blank pasted receipt text shows `No receipt text found.` instead of failing silently.

### Meal Planning

Click "Generate" in the Menu tab to create a deterministic 7-day plan. The same pantry, deal, and settings inputs produce the same meal plan for repeatable phone testing. Re-generating replaces the active generated week instead of stacking duplicate plan rows.
- The Menu tab shows a generation status with the plan/shopping-list summary and planner warnings such as missing protein deals or pantry starch anchors.
- Breakfast uses pantry anchors (oats, cereal) if enabled
- Lunch/Dinner pairs: Protein + Veg + Starch
- Proteins from top-scored deals
- Vegetables filtered by dietary preferences
- Freezer directives for bulk purchases

### Budget Management

The Budget tab shows:
- Receipt-aware current balance
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
- Pantry phrase parsing (fractions, brands, dates, comma-decimal OCR quantities/sizes)
- Pantry duplicate detection/merging, including barcode-specific matching
- Open Food Facts barcode response parsing and barcode normalization, including pasted UPC/EAN label text and labels with unrelated item/date numbers
- Deal regex patterns (all deal types, dollar/no-dollar/comma-decimal flyer OCR prices, slash-style multi-buy prices, numeric/word-number buy-get promos, buy-get percent-off promos, BOGO/B1G1/BOGO-percent shorthand)
- Flexible numeric edit parsing for comma-decimal manual corrections in pantry, deal, receipt, and settings fields
- Meal planning (GERD-filtering, anchors)
- Meal plan date coverage and deterministic repeatable 7-day generation
- Shopping list consolidation with persisted and pre-database deal identities, plus planned-quantity estimated costs
- Budget calculations (surplus, deficit, receipt-aware projection, daily envelope recalculation)
- Receipt reconciliation (bundled demo receipt, fuzzy matching, VPP, receipt header dates, split and inline weighted quantities, dollar/no-dollar/comma-decimal OCR prices, discount/coupon line filtering)
- Gemini configuration guardrails and pantry response parsing (placeholder keys, model fallback, whitespace/prefix normalization, fenced JSON, scalar warnings/questions, top-level arrays, single-item objects, item-wrapper aliases, snake_case/name aliases, numeric/comma-decimal/word/object quantity aliases, storage aliases, malformed string/list fields)

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
- `scripts\phone-debug-install.ps1` can build, verify, install, and launch the debug APK when an authorized Android phone is connected.
- `scripts\phone-debug-install.ps1 -SkipBuild` refuses to install an APK older than app source/resources/build config or `local.properties`, preventing stale code or Gemini key/model values from reaching the phone.
- `scripts\phone-debug-install.ps1` and `scripts\phone-debug-preflight.ps1` inspect `app-debug.apk` with Android SDK `aapt` when available, confirming the APK is `com.dealplanner` / `Deal Planner` and includes network/camera permissions before phone testing.
- `scripts\phone-debug-logs.ps1` captures device metadata, full logcat, and a Deal Planner/crash-filtered log under ignored local `phone-test-logs\`.
- App label, application ID, package namespace, and Room database filename use Deal Planner naming.
- Pasted flyer and receipt OCR text shows a processing status, stays in the field when parsing fails, and clears only after a successful import.
- Manual barcode/code text stays in the field when no UPC/EAN/GTIN code is found, and clears only after a successful barcode import.
- Load Demo resets pantry, deals, receipts, meal plans, default meal settings, and the demo budget baseline.
- Menu Generate deterministically rebuilds and replaces the active generated week so repeated phone-test taps do not duplicate meal-plan rows.
- Menu Generate shows a visible status summary and any meal-planning warnings returned by the rules engine.
- Shopping list consolidation keeps different deals separate even before Room assigns database ids, and Shopping totals use planned quantities with normalized price-per-unit estimates.
- Pantry parser handles quantity, comma-decimal OCR quantity/size text, brand, size, location, opened-date wording such as `opened on`, common expiration label cues such as `expiration date`, `best by date`, `best-by`, and `use-by`, low-confidence review flags, and duplicate merging.
- Pantry screen supports typed entry, barcode scan/manual code intake, photo import, and gallery import.
- Typed, photo/OCR, AI, and barcode pantry imports upsert safe duplicates instead of creating repeated pantry rows.
- Barcode/code pantry entries create VERIFY items with the barcode preserved in notes.
- Barcode/code normalization extracts 8-14 digit UPC/EAN/GTIN codes from pasted label text, prefers labeled codes over unrelated item/date numbers, and rejects non-code text.
- Pantry cards can be edited after typed, barcode/code, OCR, or AI import so VERIFY items can be corrected during phone testing, including comma-decimal quantity corrections.
- Deals parser handles price/lb, package prices, N-for-X including `2/$5`, buy-N-get-M with digits or words such as `Buy One Get One Free`, buy-get percent-off promos such as `Buy One Get One 50% off`, `BOGO Free`, `B1G1`, and `BOGO 50% off`, percent-off, Member Price/coupon flags, and limits.
- Deals parser is covered against bundled demo flyer structures including multi-line names and modifiers.
- Deals parser ignores flyer metadata/date lines such as `Valid 7/16/2026 - 7/22/2026` so slash dates do not become fake multi-buy deals.
- Deals parser accepts flyer prices when OCR drops dollar signs or uses comma decimals, including cent-style prices such as `99c/lb` and `88c`.
- Deal cards can be edited after flyer photo/image/PDF/text import so low-confidence OCR results can be corrected during phone testing, including comma-decimal price, PPU, discount, score, and confidence corrections.
- Camera capture uses app-private full-resolution image files instead of low-resolution preview bitmaps.
- Camera permission denial and canceled capture/scan/gallery/PDF picker flows show on-screen status messages.
- Blank pantry, barcode, flyer text, and receipt text actions show on-screen status messages instead of silently doing nothing.
- Imported camera/gallery images are decoded as software bitmaps and capped to a 3072px longest side for OCR/Gemini reliability.
- Camera/gallery image open failures show on-screen recovery messages instead of failing silently.
- Flyer PDF pages render locally with a 3072px longest-side cap before OCR.
- Deals screen imports flyer photos, gallery images, PDFs, and pasted flyer OCR text with store-aware deal creation.
- Receipts screen imports receipt photos, gallery images, and pasted OCR text through ML Kit OCR/reconciliation.
- The bundled demo receipt used by the phone checklist is covered by unit tests.
- Receipt reconciliation handles fuzzy matching and split or inline weighted receipt quantity lines, including price-per-pound produce lines.
- Receipt reconciliation applies receipt header dates to imported receipt rows when available.
- Receipt reconciliation accepts item totals and inline quantity lines when OCR drops dollar signs or uses comma decimals.
- Receipt reconciliation ignores subtotal, tax, total, savings, coupon, discount, reward, refund, SNAP/EBT/WIC benefit tender, and payment/card-tender lines.
- Receipt reconciliation rounds imported receipt totals to cents before budget updates.
- Receipt cards can be edited after photo, gallery, or pasted OCR import so review warnings can be corrected during phone testing, including comma-decimal quantity, total, and confidence corrections.
- Receipt imports, edits, and deletes adjust budget spending totals, daily envelope, and receipt-aware projected spend.
- Budget balance and monthly overview displays use receipt-aware analysis values when available, so recovered or stale stored budget totals do not contradict current receipt history.
- Pantry-matched receipt edits and deletes adjust pantry quantities.
- Settings can test the Gemini API key/model connection from the running app.
- Settings accepts comma-decimal protein-per-meal values such as `0,5`.
- Gemini setup trims accidental key/model whitespace and normalizes a pasted `models/` prefix before calling the API.
- Gemini pantry response parsing handles fenced JSON, minor surrounding text, scalar warnings/questions, top-level arrays, single-item objects, plural or singular item wrappers, snake_case/name aliases, numeric/comma-decimal/word/object quantity aliases such as `amount: "2 cans"`, `amount: "1,5 lb"`, `amount: "two cans"`, or `quantity: { value: "2", unit: "cans" }`, storage aliases, malformed string/list fields, and confidence clamping.
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
- [x] Barcode product lookup by verified UPC/EAN
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
