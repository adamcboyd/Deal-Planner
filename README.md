# Deal Planner

A comprehensive Android app for stretching food budgets through intelligent meal planning, deal tracking, pantry awareness, and budget management.

## Core Principle

**"Deals drive the meals."**

Parameters → Deals + Pantry → Meals

## Features

### ✅ Complete Implementation

- **Pantry Management**: Natural language input, barcode/code lookup/intake, and duplicate detection
- **Photo Pantry Intake**: Camera/gallery import with optional Gemini Vision, ML Kit OCR fallback, and edit-before-save review
- **Deal Tracking**: Camera, gallery image, PDF, and pasted flyer OCR with regex parsing
- **Receipt Tracking**: Camera, gallery image, PDF, and manual receipt OCR reconciliation
- **Meal Planning**: 7-day rule-based meal generator (no LLM required)
- **Budget Tracking**: Daily envelope system with surplus/deficit analysis
- **Receipt Reconciliation**: Fuzzy matching with Levenshtein distance
- **Menu + Shopping Refresh**: Generated meals and consolidated shopping lists stay aligned with current pantry, deals, receipts, and settings after a plan exists
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
The generated debug `BuildConfig` also tracks Git branch, commit, and dirty-state changes as build inputs. Rebuild after each checkpoint so Settings -> About and the phone-test reports identify the exact APK source; the install helper refuses a stale BuildConfig identity before installing.
The phone preflight and generated phone-test report verify whether the debug APK's generated `BuildConfig` contains a non-placeholder Gemini key and which model it will use, without printing the key.

A non-secret template is included at `local.properties.example`.
The Settings tab shows whether Gemini Vision is configured, which model the build is using, and includes a **Test AI Connection** button for real-device key/model checks with concise API error summaries.
The default `gemini-3.5-flash` model code is the stable Gemini 3.5 Flash ID listed in the official Google AI Gemini model docs and supports image inputs plus structured output. The app trims accidental whitespace and accepts either `gemini-3.5-flash` or `models/gemini-3.5-flash`, though the bare model code is preferred.
Gemini requests use the model's default sampling settings and only specify output shape/size, reducing the chance that hardcoded sampling parameters drift from current Gemini 3.x guidance.
Before the final phone AI pass, you can run a local live API check without printing the key:

```powershell
.\scripts\test-gemini-connection.ps1
```

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
.\scripts\start-phone-test-run.ps1
.\scripts\phone-debug-preflight.ps1
.\scripts\new-feature-readiness-report.ps1 -RunGate
.\scripts\test-gemini-connection.ps1
```

`start-phone-test-run.ps1` is the one-command setup for an authorized Android phone: it runs required-phone preflight, creates and copies deterministic sample files, installs/launches the debug APK, and creates a timestamped report with setup status and setup mode. If setup fails before the final report step, it creates a failure-state report with the stopping reason unless `-SkipReport` was used. Use `.\scripts\start-phone-test-run.ps1 -RequireGemini -TestGeminiLive` for the final AI phone pass after adding a real Gemini key and rebuilding. The preflight helper checks the repo state, GitHub origin/upstream sync, debug APK, APK identity/permissions, generated `BuildConfig` source branch/commit/dirty state, compiled Gemini key/model readiness without printing secrets, optional live Gemini API connectivity without printing secrets, APK freshness against app source/resources/build config, ADB/device visibility, Gemini configuration, and Open Food Facts barcode lookup reachability. If no phone is ready, the helper reports whether ADB saw no devices or saw an unauthorized/offline phone, then names the next action: connect the phone, enable Developer options > USB debugging, use a data-capable USB mode/cable, accept the USB debugging prompt, and rerun after `adb devices` shows `device`.
`new-feature-readiness-report.ps1` creates an ignored timestamped readiness report that separates local source/test evidence from phone-only checks for each input path and AI feature. Use `-RunGate` when you need a current recovery snapshot; it runs `.\gradlew.bat testDebugUnitTest assembleDebug lintDebug` before writing the report, treats that successful gate as current unit/lint evidence even when cached report-file timestamps do not move, then flags stale APK evidence, an APK source identity that does not match the current clean `HEAD`, or a failed Deal Planner naming-transition audit.
Gallery image and PDF imports use Android picker URI grants, so the APK should not request broad storage/media-library permissions.
Use `.\scripts\phone-debug-preflight.ps1 -Help` or `.\scripts\phone-debug-install.ps1 -Help` to list available phone-test options.

For the final AI phone pass after adding a real Gemini key and rebuilding, run the stricter starter so the generated phone-test report records `RequireGemini=True` in its setup mode:

```powershell
.\scripts\start-phone-test-run.ps1 -RequireGemini -TestGeminiLive
```

Without `-RequireGemini`, missing Gemini configuration remains a warning because the app can still use on-device OCR fallback.

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

Phone test report helper:

```powershell
.\scripts\new-phone-test-report.ps1
```

Reports are saved under ignored `phone-test-results\` folders and capture the current commit, APK, Gemini configuration state, latest sample folder/manifest, sample transfer report when available, starter setup status/failure reason when provided, ADB/device snapshot when available, and pass/fail sections for the phone checklist, including AI review-safety checks such as zero or negative Gemini amount fallback.

Phone test sample helper:

```powershell
.\scripts\new-phone-test-samples.ps1
.\scripts\send-phone-test-samples.ps1
```

Samples are saved under ignored `phone-test-samples\` folders and include demo receipt/flyer TXT, PDF, and PNG files, a pantry-label PNG with multi-item rows, hyphenated package sizes, punctuated label cues, slash dates, and two-digit dash dates, a valid UPC-A barcode PNG/text sample, and `SAMPLE_MANIFEST.md` byte counts plus SHA-256 hashes for deterministic pasted-text, gallery-image, barcode, and PDF picker checks. `new-phone-test-samples.ps1 -VerifyOnly` verifies the latest local bundle before phone transfer. When an authorized Android phone is connected, `send-phone-test-samples.ps1` requires that manifest, verifies its byte counts and SHA-256 hashes, copies the latest generated sample folder to `/sdcard/Download/DealPlannerPhoneTestSamples/`, verifies remote byte sizes, requests Android media scans for picker visibility, and writes `PHONE_SAMPLE_TRANSFER.md` with the Android destination and verified byte-size evidence.

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
   - **Receipts**: Scan receipt photos, choose receipt images/PDFs, or paste OCR text to update spending
   - **Shopping**: See consolidated shopping list with planned-quantity estimated costs and PPU, then export it as a shareable PDF; after a plan exists, app relaunch and input changes repopulate Menu and Shopping from current pantry/deals/settings
   - **Menu**: Browse 7-day meal plan with freezer directives
   - **Budget**: Track spending and see surplus/deficit analysis
   - **Settings**: Configure dietary preferences, custom avoid terms, and AI setup status

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
Great Value peanut butter opened 2026-7-1 best by 2026-12-31
Great Value peanut butter opened on 2026-07-01 best by date 2026-12-31
Kroger yogurt best before 2026-12-31
milk use by 12/31/2026
milk use-by 12/31/2026
milk use by 12-31-26
pasta exp 12/31/2026
Great Value black beans net wt: 15 oz pantry best by: 12/31/2026
milk expiration date 12/31/2026
yogurt best-by 2026-12-31
yogurt best by date 2026-12-31
rice 5 lb bag
frozen broccoli 12oz
```

The parser handles:
- Quantities (numeric, comma-decimal OCR, leading-decimal OCR, fractions, words)
- Units (lb, oz, cans, etc.)
- Brands (Great Value, Kroger, etc.)
- Locations (pantry, fridge, freezer)
- Dates (opened and best-by style cues independently, including `YYYY-MM-DD`, unpadded `YYYY-M-D`, two-digit dash dates such as `MM-DD-YY`, `opened on`, `best before`, `best-by`, `use by`, `use-by`, `exp`, `expiration date`, `best by date`, and punctuated label cues such as `best by:` or `exp:`)
- Forms (canned, frozen, fresh)

Repeated typed/barcode imports, and reviewed photo imports after `Save All`, merge into existing pantry rows when the app can safely identify the same item. Product barcodes only merge with the same barcode, so two different UPCs stay separate until reviewed.
If the manual pantry text field is blank, **Add** shows a visible no-text status instead of silently doing nothing.

### Adding Pantry Items From Photos

On the Pantry tab:

1. Tap **Photo** to capture a full-resolution app-cache image, or **Gallery** to choose an image.
2. If `gemini.api.key` is configured, Gemini Vision extracts brand, product, amount, size, dates, and clarification questions.
3. If Gemini is not configured or fails, ML Kit OCR reads visible label text and the pantry parser stages the best candidate for review. If OCR sees at least two complete item lines, Deal Planner stages those as separate reviewable items instead of collapsing the whole photo into one pantry row.
4. Missing brand, amount/unit, location, or expiration information is marked with a VERIFY badge and notes such as `Review brand.`, `Review amount/unit.`, `Review pantry/fridge/freezer location.`, or `Review expiration or best-by date.`.
5. Review staged photo items in `Review Pantry Imports`. Edit or remove each pending row, then tap `Save All` to merge them into Pantry.
6. Tap the edit icon on any pending or saved pantry card to correct item name, quantity, unit, size, brand, location, best-by date, notes, and verification status. Quantity corrections accept dot, comma, and leading-decimal text, such as `1.5`, `1,5`, `.5`, or `,5`; best-by date corrections accept common label formats such as `2026-12-31`, `12/31/2026`, or `12-31-26`.

### Adding Pantry Items From Barcodes

On the Pantry tab:

1. Tap **Scan** to scan a product barcode, or type/paste a code into **Barcode / UPC** and tap **Add Code**.
2. Deal Planner accepts plain UPC/EAN/GTIN digits or pasted label text such as `UPC: 0 12345-67890 5`, then looks up the cleaned code with Open Food Facts when the phone has network access. If pasted label text also has item numbers or dates, labeled UPC/EAN/GTIN text is preferred; valid bare product codes near label dates are accepted, while date/item/lot/SKU-only numbers are ignored.
3. If a product is found, Deal Planner creates a VERIFY pantry item with the product name, brand, package quantity, barcode, and lookup source in notes.
4. If lookup misses or the phone is offline, Deal Planner still creates a reviewable barcode item with the code saved in notes.
5. If the text is blank or does not contain an 8-14 digit product barcode, Deal Planner shows `No barcode found.` instead of creating a junk pantry row.
6. Re-scanning the same barcode merges quantity into the same pantry row. Different barcodes stay separate until reviewed.
7. Tap the edit icon to fill in or correct the product name, brand, package size, quantity, location, and expiration details.

### Scanning Flyers

On the Deals tab, enter the store name or leave it as `Unknown`, then use one of four input paths:

1. **Take Flyer Photo** captures a full-resolution app-cache flyer image without saving it to the camera roll.
2. **Choose Flyer Image** imports an existing screenshot or photo.
3. **Choose Flyer PDF** renders PDF pages locally and OCRs them with ML Kit. Long PDFs process the first 12 pages and the status message says when only that capped page range was used.
4. Paste flyer OCR text and tap **Process Text**.

ML Kit OCR extracts visible text, then the Deals parser looks for:

- `$3.99/lb`, `3.99/lb`, or `$3/lb` (per pound)
- `2,99/lb` or `3 lb bag 2,99` when OCR uses comma decimals
- `99c/lb` or `88c` (cent-style flyer/OCR prices)
- `3 lb bag $2.99`, `3 lb bag 2.99`, `Milk $3`, or standalone package prices after an item name
- `2 for $10`, `10 for 10`, `2/$5`, or `10 / $10` (N for X)
- `Buy 2 Get 1 Free`, `Buy One Get One Free`, `Buy Two Get One Free`, or `Buy One Get One 50% off` (buy N get M)
- `BOGO Free`, `B1G1`, or `BOGO 50% off` (buy-one-get-one shorthand)
- `25% off` (percent off)
- `Member Price` (coupon flag)
- `Limit 2` (purchase limits)

The built-in demo flyer covers the same formats.
Blank pasted flyer text shows `No flyer text found.` instead of failing silently.

Tap the edit icon on any deal card to correct OCR guesses for item name, price, unit, store, brand, size, deal type, limit, coupon flag, PPU, discount, score, confidence, and valid-until date. Numeric corrections accept dot, comma, and leading-decimal text such as `2.99`, `2,99`, or `.99`; valid-until date corrections accept common date formats such as `2026-12-31`, `12/31/2026`, or `12-31-26`.

### Processing Receipts

On the Receipts tab:

1. Enter the store name, or leave it as `Unknown`.
2. Paste receipt OCR text and tap **Process Text**, or use **Photo**, **Gallery**, or **PDF**. Photo capture uses a full-resolution app-cache image for better OCR; PDF import renders pages locally before OCR. Long PDFs process the first 12 pages and the status message says when only that capped page range was used.
3. The app reconciles receipt lines against current deals and pantry items.
4. Matched receipt items update the receipt list, pantry quantities, and budget spending; repeated pantry-matched rows on the same receipt accumulate into one pantry quantity update.
5. Receipt header dates such as `Date: 10/27/2025` are applied to imported receipt rows when available; imports without a readable date use today.
6. Low-confidence matches are marked with a review warning.
7. Tap the edit icon on any receipt item to correct the line text, quantity, total, store, match metadata, confidence, date, and review status. Quantity, total, and confidence corrections accept dot, comma, and leading-decimal text such as `1.78`, `1,78`, or `.89`; date corrections accept common formats such as `2025-10-27`, `10/27/2025`, or `10-27-25`.
8. Receipt edits and deletes adjust pantry quantities, budget spending, daily envelope, and projected spend so Pantry and Budget stay in sync.
9. Subtotal, tax, total, payment, card tender, EBT/card, SNAP EBT, WIC benefit, coupon, discount, savings, saved-total, reward, refund, return, and negative amount lines are ignored so only grocery purchase items affect spending.
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
- Vegetables filtered by dietary preferences and recognized meal-side grocery terms so household/non-food flyer deals do not enter meals or Shopping
- Custom avoid terms from Settings filter matching proteins and sides out of Menu meals and Shopping
- Freezer directives for bulk purchases

The Shopping tab can export the generated list as a shareable PDF after a meal plan exists. The PDF is written to app cache through the app FileProvider, so it uses Android's share sheet without requesting broad storage/media permissions.

### Budget Management

The Budget tab shows:
- Receipt-aware current balance
- Daily envelope (auto-calculated from remaining budget and receipt spending)
- Projected spend from current-month receipt/budget history
- Surplus/deficit warnings
- Smart suggestions (stock up, pull from freezer, etc.)
- Editable monthly budget, spent-to-date baseline, and breakfast anchor cost with visible save feedback

### Checking AI Setup

The Settings tab includes **AI Pantry Photo Status**:

- If Gemini is configured, it shows the model used for pantry photo recognition.
- If Gemini is not configured, it states that pantry photos will use on-device OCR fallback.
- Placeholder keys such as `YOUR_GEMINI_API_KEY` are treated as not configured.
- The preflight helper and generated phone-test report also show whether the installed debug APK has Gemini compiled in, so Settings can be compared against the APK source snapshot before testing photos.
- **Test AI Connection** performs a small Gemini request from the phone so you can confirm the key, network, and model before testing pantry photos.

## Testing

Run unit tests:

```bash
.\gradlew.bat testDebugUnitTest
```

Tests cover:
- Pantry phrase parsing (fractions, dozen/count quantities, brands, dates, common container/count units, fluid-ounce, gallon/quart/pint, and hyphenated package labels such as `16-ounce` or `12-count`, net-weight label wording, punctuated label cues such as `net wt:` or `best by:`, comma-decimal and leading-decimal OCR quantities/sizes)
- Pantry OCR candidate extraction for single-label fallback and clear multi-item label rows, including package `NET WT` lines that should not become separate products, hyphenated package-size lines such as `16-ounce` or `12-count`, and wrapped date/opened continuation lines
- Pantry photo import review queue behavior for staging, editing, removing, and saving multi-item OCR/AI results
- Pantry duplicate detection/merging, including compatible missing-brand/known-brand matches and barcode-specific matching
- Open Food Facts barcode response parsing and barcode normalization, including pasted UPC/EAN label text and labels with unrelated item/date numbers
- Barcode lookup result mapping into reviewable pantry rows and phone-visible add/update status messages
- Manual input clear/retain policy for barcode, pasted flyer text, and pasted receipt text status changes
- Budget Settings validation for monthly budget, spent-to-date baseline, and breakfast anchor cost dot, comma, leading-decimal, invalid, negative, and non-finite text
- Settings protein-per-meal validation for dot, comma, leading-decimal, invalid, negative, and non-finite text
- Settings custom dietary restriction normalization and preview text
- Pantry edit validation for quantity and flexible best-by date text, including dot, comma, leading-decimal, invalid, negative, and non-finite quantity text
- Deal edit validation for price, PPU, discount, score, confidence, limit, and flexible valid-until date text including 0-to-1 and 0-to-100 bounds
- Receipt edit validation for optional quantity, total, match ID, confidence, flexible date text, and comma-decimal/leading-decimal corrections
- Receipt edit/delete adjustment deltas for Budget spending and pantry-matched receipt quantities
- Deal regex patterns (all deal types, dollar/no-dollar/comma-decimal/leading-decimal/whole-dollar flyer OCR prices, comma-decimal package sizes, slash/no-slash per-pound prices, slash-style multi-buy prices, savings-only callout filtering, unsafe/zero multi-buy rejection, numeric/word-number buy-get promos, buy-get percent-off promos, BOGO/B1G1/BOGO-percent shorthand)
- Flexible numeric edit parsing for comma-decimal and leading-decimal manual corrections in pantry, deal, receipt, budget, and settings fields, while rejecting non-finite values such as NaN or Infinity
- Meal planning (GERD-filtering, anchors)
- Meal plan date coverage and deterministic repeatable 7-day generation
- Custom dietary restriction filtering for matching proteins and side deals
- Meal-side filtering so household/non-food flyer deals are ignored by generated meals and Shopping
- Menu refresh policy and shopping list consolidation with persisted and pre-database deal identities, planned-quantity estimated costs, and shareable PDF export formatting
- Budget calculations (surplus, deficit, receipt-aware projection, daily envelope recalculation)
- Receipt reconciliation (bundled demo receipt, fuzzy/token matching, weak-match rejection, VPP, receipt header dates including year-first slash/dash formats, split and inline item-first/quantity-first decimal/weighted quantities, dollar/no-dollar/comma-decimal/leading-decimal/whole-dollar OCR prices, discount/coupon/saved-total/negative-return line filtering)
- Gemini configuration guardrails and pantry response parsing (placeholder keys, stable `gemini-3.5-flash` default model, model fallback, whitespace/prefix normalization, default sampling settings, fenced JSON, scalar/object-wrapped warnings/questions, alternate review-question and warning aliases, top-level arrays, single-item objects, item-wrapper aliases, snake_case/camelCase/name aliases, common label-date aliases, object/array-wrapped string fields, numeric/comma-decimal/leading-decimal/word/dozen/object quantity aliases, object-wrapped confidence, storage aliases, malformed string/list fields, and non-finite numeric fallback)
- Gemini connection-test success, empty-response, missing-key, and concise failure status handling without requiring live network calls
- AI pantry saved-row normalization for raw model unit, brand, size, and storage wording
- AI pantry review-note reasons for missing, non-positive, or uncertain brand, amount/unit, storage location, and best-by date details

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
- `scripts\phone-debug-install.ps1` can build, verify, print generated APK source/Gemini identity, install, confirm the package on-device, and launch the debug APK when an authorized Android phone is connected.
- `scripts\start-phone-test-run.ps1` orchestrates required-phone preflight, sample generation/transfer, debug APK install/launch, and timestamped report creation for the real Android run, including setup status/setup mode on success and failure-state report creation with the stopping reason when setup stops early and `-SkipReport` was not used.
- `scripts\phone-debug-install.ps1 -SkipBuild` refuses to install an APK older than app source/resources/build config or `local.properties`, preventing stale code or Gemini key/model values from reaching the phone.
- `scripts\phone-debug-preflight.ps1` and `scripts\phone-debug-install.ps1` print specific ADB recovery guidance when no phone is visible or a phone is unauthorized/offline.
- `scripts\phone-debug-install.ps1` and `scripts\phone-debug-preflight.ps1` inspect `app-debug.apk` with Android SDK `aapt` when available, confirming the APK is `com.dealplanner` / `Deal Planner`, includes network/camera permissions, and does not request broad storage/media permissions before phone testing.
- `scripts\phone-debug-preflight.ps1` verifies the local branch is clean, points at `adamcboyd/Deal-Planner`, is synced with its upstream, matches the GitHub branch SHA when network checks are enabled, and reports the generated debug `BuildConfig` source identity and compiled Gemini key/model readiness that Settings should reflect on the phone.
- `scripts\test-gemini-connection.ps1` performs an optional short live Gemini API check from `local.properties` or `GEMINI_API_KEY` without printing the key, and `-TestGeminiLive` wires that into preflight/starter runs.
- `scripts\new-feature-readiness-report.ps1 -RunGate` creates an ignored timestamped feature readiness matrix under `phone-test-results\`, runs the local unit/build/lint gate first, shows which pantry, barcode, flyer, receipt, budget, shopping, Settings, and AI paths have local source/test evidence, audits the Deal Planner app label/package/project naming transition, treats the successful gate as current unit/lint evidence even if Gradle reuses cached report files, and flags stale APK evidence or an APK source identity that does not match the current clean `HEAD`.
- `scripts\phone-debug-logs.ps1` captures device metadata, full logcat, and a Deal Planner/crash-filtered log under ignored local `phone-test-logs\`.
- `scripts\new-phone-test-report.ps1` creates ignored timestamped `phone-test-results\` report folders for recording real-phone checklist pass/fail evidence, repo commit, compiled APK source branch/commit/dirty state, compiled Gemini readiness, current lint snapshot when available, latest sample folder/manifest, latest sample transfer report when available, setup status/mode/failure reason when provided, and device context.
- `scripts\new-phone-test-samples.ps1` creates ignored timestamped `phone-test-samples\` folders with demo receipt/flyer TXT, PDF, and PNG files plus pantry-label and UPC-A barcode samples for deterministic phone input checks. The pantry-label sample includes `16-ounce`, `12-count`, punctuated `net wt:`/`best by:` cues, slash dates, and two-digit dash dates for OCR fallback checks, and `SAMPLE_MANIFEST.md` records byte counts plus SHA-256 hashes. `-VerifyOnly` checks the latest bundle without needing a connected phone.
- `scripts\send-phone-test-samples.ps1` requires the generated `SAMPLE_MANIFEST.md`, verifies its byte counts and SHA-256 hashes, copies the latest generated demo receipt/flyer/pantry/barcode TXT, PDF, and PNG files to an authorized Android phone's Downloads folder, verifies remote byte sizes, requests Android media scans for deterministic picker checks, and writes local transfer evidence to `PHONE_SAMPLE_TRANSFER.md`.
- App label, application ID, package namespace, and Room database filename use Deal Planner naming.
- Adaptive launcher icons include a monochrome themed-icon asset, reducing current debug lint to dependency/SDK drift warnings only.
- Settings -> About Deal Planner shows the actual Gradle version, package name, debug/release build identity, source branch, source commit, and dirty-build state from `BuildConfig`.
- Pasted flyer and receipt OCR text shows a processing status, stays in the field when parsing fails, and clears only after a successful import.
- Manual barcode/code text stays in the field when no UPC/EAN/GTIN code is found, and clears only after a successful barcode import.
- Load Demo resets pantry, deals, receipts, meal plans, default meal settings, and the demo budget baseline.
- Menu Generate deterministically rebuilds and replaces the active generated week so repeated phone-test taps do not duplicate meal-plan rows.
- Menu Generate shows a visible status summary and any meal-planning warnings returned by the rules engine.
- Shopping list consolidation keeps different deals separate even before Room assigns database ids, and Shopping totals use planned quantities with normalized price-per-unit estimates.
- Shopping can export the generated list as a shareable PDF from app cache through the FileProvider without broad storage/media permissions.
- After a meal plan exists, Pantry, Deals, Receipts, and Settings changes replace the generated Menu week and Shopping list from current inputs instead of leaving stale meals/totals/items.
- Meal planning only uses recognized meal-side grocery deals for vegetable slots; household/non-food flyer deals such as detergent stay out of meals and Shopping totals.
- Pantry parser handles quantity, comma-decimal and leading-decimal OCR quantity/size text, brand, size, location, opened-date wording such as `opened on`, package `net wt` labels, common expiration label cues such as `expiration date`, `best by date`, `best if used by`, `best-by`, `use-by`, and `use by 12-31-26`, low-confidence review flags, and duplicate merging.
- Pantry parser trims label punctuation on cue words, so OCR/manual text such as `net wt:`, `best by:`, `opened:`, or `exp:` does not leak cue words into item names.
- Pantry parser handles common package sizes such as `1 gal`, `1 quart`, `1 pint`, `16-ounce`, and `12-count`.
- Pantry screen supports typed entry, barcode scan/manual code intake, photo import, and gallery import.
- Typed pantry entry shows a visible added/updated status after a successful add or merge.
- Typed, barcode, and reviewed photo/OCR or AI pantry imports upsert safe duplicates instead of creating repeated pantry rows; missing, Generic, or unknown brands can merge into a known-brand row when item, size, and location match, while different known brands stay separate.
- Barcode/code pantry entries create VERIFY items with the barcode preserved in notes.
- Barcode/code normalization extracts 8-14 digit UPC/EAN/GTIN codes from pasted label text, prefers labeled codes over unrelated item/date numbers, accepts valid bare product codes near label dates, and rejects non-code date, item, lot, SKU, or plain text.
- Pantry photo OCR/AI imports stage editable pending rows before saving, so multi-item shelf scans can be corrected or removed before they change the saved Pantry list.
- Pantry cards can be edited after typed, barcode/code, OCR, or AI save so VERIFY items can be corrected during phone testing, including comma-decimal and leading-decimal quantity corrections plus common best-by date formats.
- Pantry edit quantity and best-by date fields block invalid values with visible validation instead of silently preserving old values.
- Deals parser handles price/lb, package prices, N-for-X including `2/$5`, buy-N-get-M with digits or words such as `Buy One Get One Free`, buy-get percent-off promos such as `Buy One Get One 50% off`, `BOGO Free`, `B1G1`, and `BOGO 50% off`, percent-off, Member Price/coupon flags, and limits.
- Deals parser is covered against bundled demo flyer structures including multi-line names and modifiers.
- Deals parser ignores impossible or unsafe multibuy counts such as `0 for $5` and oversized OCR counts instead of importing invalid deals.
- Deals parser ignores flyer metadata/date lines such as `Valid 7/16/2026 - 7/22/2026` so slash dates do not become fake multi-buy deals.
- Deals parser ignores savings-only flyer callouts such as `Save $1 when you buy 2` so coupon savings text does not become a fake item price.
- Deals parser accepts flyer prices when OCR drops dollar signs, drops leading zeroes such as `.99/lb`, drops price/unit slashes, uses comma decimals, or uses explicit whole-dollar prices such as `$3/lb`, `$1/ea`, `$3.99 each`, `$1.25 per ea`, and `Milk $3`, including cent-style prices such as `99c/lb`, `99c lb`, `88c`, and `88c each`; it also preserves comma-decimal package sizes such as `5,3 oz` without confusing loose per-pound prices for sizes.
- Deal cards can be edited after flyer photo/image/PDF/text import so low-confidence OCR results can be corrected during phone testing, including comma-decimal and leading-decimal price, PPU, discount, score, confidence, and flexible valid-until date corrections with visible validation for invalid values.
- Camera capture uses app-private full-resolution image files instead of low-resolution preview bitmaps.
- Gallery and PDF imports use picker-scoped URI grants instead of broad storage/media permissions.
- Camera permission denial and canceled capture/scan/gallery/PDF picker flows show on-screen status messages.
- Camera permission request launch failures show on-screen recovery messages with settings/manual/picker alternatives.
- Camera, gallery, PDF picker, and barcode scanner launch failures show on-screen recovery messages instead of closing the app.
- Blank pantry, barcode, flyer text, and receipt text actions show on-screen status messages instead of silently doing nothing.
- Imported camera/gallery images are decoded as software bitmaps and capped to a 3072px longest side for OCR/Gemini reliability.
- Camera/gallery image open failures show on-screen recovery messages instead of failing silently.
- Flyer PDF pages render locally with a 3072px longest-side cap before OCR, process up to 12 pages, and surface when only the first capped page range was used.
- Deals screen imports flyer photos, gallery images, PDFs, and pasted flyer OCR text with trimmed, store-aware deal creation.
- Receipts screen imports receipt photos, gallery images, PDFs, and pasted OCR text through ML Kit OCR/reconciliation with trimmed store names, and receipt PDF status surfaces when only the first capped page range was used.
- The bundled demo receipt used by the phone checklist is covered by unit tests.
- Receipt reconciliation handles fuzzy matching and split or inline receipt quantity lines, including quantity-first rows, item-first rows, price-per-pound produce rows, and OCR separators such as `@` or `x`.
- Receipt reconciliation applies receipt header dates to imported receipt rows when available, including common `Date: 10/27/2025`, `Transaction Date: 2025/10/27`, and `Purchase Date: 2025-10-28` formats.
- Receipt reconciliation accepts item totals and inline quantity lines when OCR drops dollar signs, puts the quantity before or after the item name, omits leading zeroes in prices such as `.89`, uses comma decimals, or returns explicit whole-dollar prices such as `$3`.
- Receipt reconciliation ignores subtotal, tax, total, savings, saved-total, coupon, discount, reward, refund, return, negative amount rows such as `MILK -$1.99`, SNAP/EBT/WIC benefit tender, and payment/card-tender lines.
- Receipt reconciliation rounds imported receipt totals to cents before budget updates.
- Receipt cards can be edited after photo, gallery, PDF, or pasted OCR import so review warnings can be corrected during phone testing, including comma-decimal and leading-decimal quantity, total, match ID, confidence, and flexible date corrections with visible validation for invalid values.
- Receipt imports, edits, and deletes adjust budget spending totals, daily envelope, and receipt-aware projected spend.
- Budget balance and monthly overview displays use receipt-aware analysis values when available, so recovered or stale stored budget totals do not contradict current receipt history.
- Budget Settings lets the user edit monthly budget, spent-to-date baseline, and breakfast anchor cost with comma-decimal and leading-decimal support, non-negative validation, and visible saved feedback.
- Pantry-matched receipt imports, edits, and deletes adjust pantry quantities, including repeated matched items on one receipt.
- Settings can test the Gemini API key/model connection from the running app.
- Phone helpers verify the generated debug `BuildConfig` Gemini key/model state without printing secrets, so stale APKs can be caught before live AI testing.
- Settings accepts comma-decimal and leading-decimal protein-per-meal values such as `0,5` or `.5`.
- Settings accepts custom avoid terms such as `pork, shellfish, peanuts`; saved restrictions filter matching deal names/details out of Menu meals and Shopping.
- Settings Save shows visible saved feedback and blocks invalid or negative protein-per-meal text instead of silently defaulting.
- Manual numeric edit fields reject non-finite text such as `NaN` or `Infinity` instead of saving invalid calculations.
- Meal planning guards against negative saved protein settings so Shopping quantities and estimated costs cannot go below zero.
- Gemini setup trims accidental key/model whitespace and normalizes a pasted `models/` prefix before calling the API.
- Gemini requests keep Gemini 3.x sampling defaults and only set the JSON response shape for pantry photo extraction or the short max output for connection testing.
- Gemini pantry response parsing handles fenced JSON, minor surrounding text, scalar/object-wrapped warnings/questions, alternate review-question aliases such as `clarifying_questions` and `followUpQuestions`, warning aliases such as `review_notes`, top-level arrays, single-item objects, plural or singular item wrappers, snake_case/camelCase/name aliases, common label-date aliases such as `sell_by_date` and `expirationDateText`, numeric/comma-decimal/leading-decimal/word/dozen/object quantity aliases such as `amount: "2 cans"`, `amount: "1,5 lb"`, `amount: ".5 lb"`, `amount: "two cans"`, `amount: "a dozen eggs"`, `quantity: { value: "half dozen" }`, or `quantity: { value: "2", unit: "cans" }`, storage aliases, malformed string/list fields, non-finite numeric text fallback, non-JSON model text fallback, and confidence clamping.
- AI pantry saved rows normalize raw model wording before storage, so values like plural cans, fluid ounces, extra whitespace, and refrigerator/cold-storage aliases become canonical pantry fields such as `can`, `oz`, and `fridge`.
- AI pantry photo dates accept common label formats such as `12/31/2026`, `12-31-26`, `2026/12/31`, and unpadded `2026-7-1` before saving review items.
- AI pantry photo review items keep unparseable best-by/opened dates in notes and require review instead of silently dropping the date text.
- AI pantry photo items with an unknown or non-positive amount require review instead of being treated as fully verified.
- AI pantry photo items with a Generic or unknown brand, including punctuated model text such as `Generic:` or `Unknown.`, require review so missing label brand details stay visible.
- AI pantry photo items with missing or unknown storage location require review so pantry/fridge/freezer placement can be corrected.
- AI pantry photo VERIFY notes include specific review prompts for missing brand, amount/unit, storage location, and best-by date details.
- ML Kit pantry OCR fallback keeps single-label photos as one combined review item, avoids treating `NET WT` package-size lines as products, and splits clear multi-item OCR rows, including hyphenated package-size rows and wrapped date/opened continuations, into separate VERIFY pantry items.
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
- [x] Full multi-item shelf review flow with edit-before-save
- [x] Flyer PDF import through local page rendering and OCR
- [x] Pasted flyer OCR text import
- [x] Receipt photo/gallery/PDF/manual text import
- [x] Deal review/edit flow after flyer OCR/PDF/text import
- [x] Receipt review/edit flow after OCR import
- [x] Barcode product lookup by verified UPC/EAN
- [ ] Nutrition lookup by verified brand/product/size
- [x] Export shopping list as PDF
- [ ] Weekly budget reports
- [x] Custom dietary restrictions
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
