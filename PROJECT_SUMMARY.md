# Deal Planner - Complete Project Summary

## ✅ Project Status: BUILDABLE PHONE-TEST BASELINE

This is a working Android MVP baseline that compiles, passes unit tests, builds a debug APK, and is ready for physical-phone testing.

## 📊 Project Statistics

- **Total Kotlin Files**: 77
- **Configuration Files**: 15
- **Test Files**: 22 (comprehensive unit tests)
- **Lines of Code**: ~5,000+

## 📁 Complete File Structure

```
Deal_Planner/
├── README.md                          ✅ Complete setup guide
├── PROJECT_SUMMARY.md                 ✅ This file
├── .gitignore                         ✅ Git configuration
├── settings.gradle.kts                ✅ Project settings
├── build.gradle.kts                   ✅ Root build config
├── gradle.properties                  ✅ Gradle properties
├── gradle/wrapper/
│   ├── gradle-wrapper.jar             ✅ Gradle wrapper runtime
│   ├── gradle-wrapper.properties      ✅ Gradle 8.2
│   ├── gradlew                        ✅ Unix wrapper script
│   └── gradlew.bat                    ✅ Windows wrapper script
└── app/
    ├── build.gradle.kts               ✅ App build config with all dependencies
    ├── proguard-rules.pro             ✅ ProGuard rules
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml    ✅ App manifest with permissions
        │   ├── java/com/dealplanner/
        │   │   ├── MainActivity.kt    ✅ Main entry point
        │   │   ├── ai/
        │   │   │   └── GeminiPantryVisionClient.kt ✅ Optional AI pantry photo extraction
        │   │   ├── data/
        │   │   │   ├── model/         ✅ 7 entities (PantryItem, DealItem, etc.)
        │   │   │   ├── dao/           ✅ 7 DAOs with flows
        │   │   │   ├── database/      ✅ AppDatabase + Converters
        │   │   │   └── repository/    ✅ AppRepository
        │   │   ├── ocr/
        │   │   │   └── TextRecognitionHelper.kt  ✅ ML Kit integration
        │   │   ├── lookup/
        │   │   │   └── OpenFoodFactsBarcodeClient.kt ✅ Barcode product lookup
        │   │   ├── parser/
        │   │   │   ├── PantryPhraseParser.kt     ✅ NLP parser
        │   │   │   └── DealsParser.kt            ✅ Regex-based parser
        │   │   ├── domain/
        │   │   │   ├── MealPlanningEngine.kt     ✅ Rules-based engine
        │   │   │   ├── BudgetEngine.kt           ✅ Budget tracking
        │   │   │   ├── ReceiptAdjustmentCalculator.kt ✅ Receipt edit/delete deltas
        │   │   │   ├── ReceiptReconciler.kt      ✅ Fuzzy matching
        │   │   │   └── ShoppingListExportFormatter.kt ✅ Shopping PDF contents
        │   │   ├── util/
        │   │   │   ├── FlexibleNumberParsing.kt  ✅ Flexible numeric edit parsing
        │   │   │   └── DietaryRestrictions.kt    ✅ Custom avoid-list parsing
        │   │   └── ui/
        │   │       ├── camera/CapturePhotoUriFactory.kt ✅ Full-resolution capture URIs
        │   │       ├── state/                   ✅ UI validation and clear/retain helpers
        │   │       ├── export/                  ✅ Shopping list PDF export
        │   │       ├── viewmodel/AppViewModel.kt ✅ MVVM ViewModel
        │   │       ├── navigation/Screen.kt      ✅ Navigation setup
        │   │       ├── screens/                  ✅ 7 Compose screens
        │   │       │   ├── PantryScreen.kt
        │   │       │   ├── DealsScreen.kt
        │   │       │   ├── ReceiptsScreen.kt
        │   │       │   ├── ShoppingListScreen.kt
        │   │       │   ├── MenuScreen.kt
        │   │       │   ├── BudgetScreen.kt
        │   │       │   └── ParamsScreen.kt
        │   │       └── theme/                    ✅ Material 3 theme
        │   ├── assets/
        │   │   ├── demo_flyer.txt     ✅ Sample OCR input
        │   │   └── demo_receipt.txt   ✅ Sample receipt
        │   └── res/
        │       ├── values/
        │       │   ├── strings.xml    ✅ String resources
        │       │   ├── themes.xml     ✅ Material theme
        │       │   └── colors.xml     ✅ Color palette
        │       ├── drawable/
        │       │   ├── ic_launcher_foreground.xml  ✅ Launcher icon
        │       │   └── ic_launcher_monochrome.xml  ✅ Themed launcher icon
        │       ├── xml/
        │       │   └── file_paths.xml              ✅ FileProvider cache paths
        │       └── mipmap-anydpi-v26/
        │           ├── ic_launcher.xml             ✅ Adaptive icon
        │           └── ic_launcher_round.xml       ✅ Round icon
        └── test/java/com/dealplanner/
            ├── ai/
            │   ├── GeminiPantryVisionClientTest.kt ✅ AI response/client parsing tests
            │   └── PantryVisionItemMapperTest.kt ✅ AI-to-pantry mapping tests
            ├── lookup/
            │   ├── BarcodePantryMapperTest.kt ✅ Barcode pantry-row/status mapping tests
            │   └── OpenFoodFactsBarcodeClientTest.kt ✅ Barcode lookup/normalization tests
            ├── parser/
            │   ├── PantryPhraseParserTest.kt  ✅ Pantry parser tests
            │   └── DealsParserTest.kt         ✅ Flyer parser tests
            ├── domain/
            │   ├── MealPlanningEngineTest.kt  ✅ Meal planning tests
            │   ├── BudgetEngineTest.kt        ✅ Budget tests
            │   ├── ReceiptAdjustmentCalculatorTest.kt ✅ Receipt edit/delete delta tests
            │   ├── ReceiptReconcilerTest.kt   ✅ Receipt parser/reconciliation tests
            │   └── ShoppingListExportFormatterTest.kt ✅ Shopping PDF content tests
            ├── ocr/
            │   └── PantryOcrCandidateExtractorTest.kt ✅ Pantry OCR fallback tests
            ├── ui/state/
            │   ├── BudgetInputValidatorTest.kt ✅ Budget numeric validation tests
            │   ├── DealItemInputValidatorTest.kt ✅ Deal edit validation tests
            │   ├── ManualInputClearPolicyTest.kt ✅ Manual input clear/retain tests
            │   ├── PantryImportReviewQueueTest.kt ✅ Photo import review queue tests
            │   ├── PantryItemInputValidatorTest.kt ✅ Pantry edit validation tests
            │   ├── ReceiptItemInputValidatorTest.kt ✅ Receipt edit validation tests
            │   └── SettingsInputValidatorTest.kt ✅ Settings numeric validation tests
            └── util/
                ├── DietaryRestrictionsTest.kt ✅ Custom avoid-list parsing tests
                ├── FlexibleDateParsingTest.kt ✅ Flexible date parsing tests
                └── FlexibleNumberParsingTest.kt ✅ Flexible number parsing tests
```

## 🎯 Core Features Implemented

### 1. Data Layer (Room Database)
- ✅ 7 Entity classes with proper TypeConverters
- ✅ 7 DAO interfaces with Flow support
- ✅ AppDatabase with singleton pattern and `deal_planner_db` filename
- ✅ Repository pattern implementation
- ✅ Migration support structure

### 2. Parsers (Production-Ready)
- ✅ **PantryPhraseParser**:
  - Handles quantities (numeric, comma-decimal OCR, fractions, words)
  - Extracts brands, units, sizes, locations
  - Handles common label sizes including `16-ounce`, `12-count`, fluid-ounce, gallon, quart, and pint wording
  - Parses dates (relative and absolute, including label cues such as opened on, best before, best-by, best by date, use by, use-by, expiration date, exp, and punctuated label cues like best by:, plus unpadded year-first and two-digit dash label dates)
  - Duplicate detection and merging
  - Confidence scoring

- ✅ **DealsParser**:
  - $X.XX/lb pattern
  - Cent-style prices such as 99c/lb and 88c
  - Plain package prices such as 3 lb bag $2.99
  - Dollar/no-dollar/comma-decimal flyer OCR price parsing
  - Savings-only flyer callout filtering so coupon savings do not become fake deals
  - N for $X pattern
  - Buy N Get M with numeric or word numbers, buy-get percent-off promos, plus BOGO/B1G1/BOGO-percent shorthand patterns
  - Percent off pattern
  - Coupon/limit detection
  - Multi-line flyer names and modifiers from bundled demo flyer text
  - Deal score calculation (4-factor algorithm)

### 3. Photo, OCR, and AI Integration
- ✅ ML Kit Text Recognition helper
- ✅ Bitmap processing
- ✅ URI support for image selection
- ✅ Full-resolution app-cache camera capture
- ✅ Software bitmap decode and max-size cap for camera/gallery OCR inputs
- ✅ Camera/gallery image-open failure status feedback
- ✅ Bounded flyer/receipt PDF page rendering with capped-page status for OCR reliability
- ✅ Camera/gallery/PDF permission and cancel status feedback for phone testing
- ✅ Blank manual input status feedback for pantry, barcode, flyer text, and receipt text actions
- ✅ Pantry camera/gallery review-before-save import plus barcode/manual code import
- ✅ Open Food Facts product lookup for barcode/manual code intake with reviewable fallback
- ✅ Flyer camera/gallery/PDF/manual text import
- ✅ Receipt camera/gallery/PDF/manual text import
- ✅ Optional Gemini Vision client using local.properties or GEMINI_API_KEY
- ✅ Gemini key/model trimming and model-prefix normalization
- ✅ Stable `gemini-3.5-flash` default model with Gemini 3.x default sampling behavior
- ✅ Concise Gemini API success, empty-response, missing-key, and failure summaries for Settings connection testing
- ✅ Gemini pantry response parsing for fenced JSON, minor model-output variations, alternate review-question/warning aliases, top-level arrays, single-item objects, item-wrapper aliases, snake_case/name aliases, numeric/comma-decimal/word/object quantity aliases, storage aliases, malformed string/list fields, and non-JSON model text fallback
- ✅ AI pantry saved-row normalization for raw model unit, brand, size, and storage wording
- ✅ Settings screen AI configuration status and Gemini connection test
- ✅ Local non-secret Gemini live connection script for pre-phone AI setup verification
- ✅ ML Kit OCR fallback when Gemini is not configured, including pantry `NET WT` package-label handling, hyphenated package-size multi-item splitting, wrapped date continuation handling, and edit-before-save staging
- ✅ ZXing barcode scanner intake for reviewable pantry seeding

### 4. Business Logic Engines
- ✅ **MealPlanningEngine**:
  - 7-day plan generation
  - GERD-friendly filtering
  - Custom dietary restriction filtering
  - Recognized meal-side filtering so household/non-food flyer deals are ignored
  - Pantry anchor utilization
  - Freezer directive calculation
  - Shopping list consolidation, planned-quantity estimated costs, PDF export formatting, and startup restore from current inputs after a plan exists
  - Freshness reordering

- ✅ **BudgetEngine**:
  - Daily envelope calculation
  - Surplus/deficit analysis
  - Receipt-aware projected spend tracking
  - Daily envelope recalculation after receipt edits/deletes
  - Smart suggestions

- ✅ **ReceiptReconciler**:
  - Levenshtein distance fuzzy matching
  - PPU variance detection
  - VPP calculation for proteins
  - Confidence scoring
  - Cumulative pantry quantity updates for repeated pantry-matched receipt rows
  - Dollar/no-dollar/comma-decimal OCR price parsing

### 5. UI Layer (Jetpack Compose)
- ✅ **MainActivity**: Navigation + bottom bar
- ✅ **PantryScreen**: Natural language input, barcode/manual code intake, photo/gallery import, VERIFY badges
- ✅ **Pantry Review Dialog**: Edit imported pantry items, flexible best-by date text, and clear/keep verification flags
- ✅ **DealsScreen**: Flyer photo/gallery/PDF/manual text import, processing status, failed-parse text retention, store-aware deal cards with scores, coupon flags
- ✅ **Deal Review Dialog**: Edit imported deals, coupon flags, scores, confidence, and flexible valid-until date text
- ✅ **ReceiptsScreen**: Receipt photo/gallery/PDF/manual text import, processing status, failed-parse text retention, review flags, budget updates
- ✅ **Receipt Review Dialog**: Edit imported receipt lines, totals, match metadata, confidence, flexible date text, and review status
- ✅ **ShoppingListScreen**: Consolidated list with planned-quantity estimated costs, PPU, shareable PDF export, and startup restore from current pantry/deals/settings after a plan exists
- ✅ **MenuScreen**: 7-day plan with freezer directives and generation status/warnings
- ✅ **BudgetScreen**: Receipt-aware balance, envelope, analysis, suggestions
- ✅ **ParamsScreen**: Dietary preferences, custom avoid list, meal settings, AI status, Gemini connection test
- ✅ Material 3 theming with dark/light support

### 6. Demo Data
- ✅ Seed function with:
  - 5 pantry anchors (rice, pasta, oats, beans, oil)
  - 4 sample deals (pork, chicken, broccoli, mandarins)
  - Resettable budget state ($292 food budget, $45 spent)
  - Resettable default meal params
  - Auto-generated 7-day meal plan

### 7. Unit Tests (256 Test Cases)
- ✅ GeminiPantryVisionClientTest
- ✅ PantryVisionItemMapperTest
- ✅ BarcodePantryMapperTest
- ✅ OpenFoodFactsBarcodeClientTest
- ✅ PantryOcrCandidateExtractorTest
- ✅ PantryPhraseParserTest
- ✅ DealsParserTest
- ✅ MealPlanningEngineTest
- ✅ BudgetEngineTest
- ✅ ReceiptAdjustmentCalculatorTest
- ✅ ReceiptReconcilerTest
- ✅ BudgetInputValidatorTest
- ✅ DealItemInputValidatorTest
- ✅ ManualInputClearPolicyTest
- ✅ PantryItemInputValidatorTest
- ✅ ReceiptItemInputValidatorTest
- ✅ SettingsInputValidatorTest
- ✅ FlexibleDateParsingTest
- ✅ FlexibleNumberParsingTest

## 🔧 Technology Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin 1.9.20 |
| UI | Jetpack Compose + Material 3 |
| Database | Room 2.6.1 |
| OCR | ML Kit Text Recognition 16.0.0 |
| AI Vision | Optional Gemini API |
| Barcode | ZXing 3.5.2 + JourneyApps scanner |
| Coroutines | Kotlinx Coroutines 1.7.3 |
| Architecture | MVVM + Repository |
| Testing | JUnit 4.13.2 + Truth 1.1.5 |
| Min SDK | 26 (Android 8.0) |
| Compile SDK | 34 (Android 14) |
| Target SDK | 33 |

## 🚀 How to Build & Run

1. **Open in Android Studio**:
   ```bash
   cd Deal_Planner
   # Open in Android Studio Hedgehog or later
   ```

2. **Sync Gradle**:
   - Click "Sync Now" when prompted
   - All dependencies will download automatically

3. **Run**:
   - Select emulator (API 26+) or physical device
   - Click Run (▶)
   - App will launch with all features ready

4. **Load Demo Data**:
   - Tap "Load Demo" button in top-right
   - Explore all 7 tabs

## 📱 User Flows

### Adding Pantry Items
1. Go to Pantry tab
2. Type: "2 cans black beans 15oz", scan a barcode, enter a UPC, or import a photo
3. Tap Add, Scan, Add Code, Photo, or Gallery
4. Item parsed or seeded as a VERIFY item and stored
5. Safe duplicates merge quantities instead of creating repeated rows; barcode items merge only when the barcode matches

### Viewing Deals
1. Go to Deals tab
2. Enter a store name or leave it as Unknown
3. Use Photo, Gallery, PDF, pasted OCR text, or tap "Load Demo" to see sample deals
4. View deal scores, PPU, coupon flags
5. Flyer prices can include or omit dollar signs

### Processing Receipts
1. Go to Receipts tab
2. Enter a store name
3. Use Photo, Gallery, or pasted OCR text
4. Receipt lines reconcile against pantry/deals and update budget spending
5. Edit any receipt item that needs OCR, match, date, or total correction
6. Receipt OCR prices can include or omit dollar signs

### Generating Meal Plan
1. Go to Menu tab
2. Tap "Generate"
3. View 7-day plan with proteins, veg, starches
4. See freezer directives for bulk purchases

### Checking Budget
1. Go to Budget tab
2. View current balance, daily envelope
3. See surplus/deficit analysis
4. Read smart suggestions

### Configuring Settings
1. Go to Settings tab
2. Toggle GERD-friendly, avoid peppers
3. Adjust protein per meal
4. Check AI Pantry Photo Status
5. Tap Test AI Connection after adding a Gemini key
6. Save settings

## 🧪 Testing

Run all tests:
```bash
cd Deal_Planner
./gradlew test
```

Expected output: all unit tests passing

## 🎨 Design Principles

1. **Offline-First Core**: Data storage, meal planning, flyer OCR, and parser flows run locally
2. **Optional LLM**: Gemini Vision is used only for pantry photo extraction when configured
3. **Rules-Based Meals**: Meal planning remains deterministic
4. **User Control**: All actions explicit, no surprises
5. **Deals Drive Meals**: Core principle implemented throughout

## 📐 Key Algorithms

### Deal Score Formula
```
score = 0.40 × (discount% / 100)
      + 0.25 × ((baseline - PPU) / baseline)
      + 0.20 × stackability
      + 0.15 × utilityFit
```

### Pantry Parsing
- Tokenization → Quantity extraction → Unit normalization
- Brand detection (common brands list)
- Location/form keyword matching
- Date parsing (relative + absolute)
- Confidence scoring with VERIFY flag

### Meal Planning
- Anchor discovery (rice, pasta, oats)
- Deal filtering (GERD, peppers)
- Protein selection (top 3 deals)
- Vegetable selection (top 4 deals)
- Daily meal composition: Protein + Veg + Starch
- Freezer portioning for bulk buys

### Budget Analysis
- Daily envelope = remaining_balance / days_left
- Projected spend = avg_daily_spend × days_remaining
- Surplus = balance - projected_spend
- Suggestions: surplus → stock up, deficit → freezer pull

### Receipt Reconciliation
- Line parsing (regex for price/qty patterns)
- Fuzzy matching (Levenshtein distance)
- Confidence scoring (0-1)
- VPP calculation: price / (weight / serving_size)

## ✨ Build-Ready Features

- ✅ Error handling in all parsers
- ✅ Null safety throughout
- ✅ Flow-based reactive UI
- ✅ Proper lifecycle management
- ✅ Material Design 3 compliance
- ✅ Dark/light theme support
- ✅ Accessibility (content descriptions)
- ✅ Memory efficient (Room paging ready)
- ✅ No hardcoded strings (strings.xml)
- ✅ Proper resource management

## 🔒 Constraints Met

- ✅ Android-first, phone-only MVP
- ✅ Kotlin + Jetpack Compose
- ✅ Room (SQLite) for all data
- ✅ App-cache photo capture so OCR photos do not clutter the camera roll
- ✅ ML Kit on-device OCR
- ✅ ZXing barcode scan/manual-code pantry intake
- ✅ Optional cloud AI for pantry photos, with on-device OCR fallback
- ✅ Offline-first architecture
- ✅ All data local

## 🎯 What's Working

This is a **buildable, runnable MVP baseline** that:
- ✅ Compiles without errors
- ✅ Builds a debug APK for emulator/device install
- ✅ Loads repeatable demo data with reset budget and meal params
- ✅ Parses pantry items
- ✅ Parses pantry comma-decimal OCR quantities and sizes such as 1,5 lb and 5,3oz
- ✅ Parses unpadded year-first and two-digit dash pantry/AI label dates such as `2026-7-1` and `12-31-26`
- ✅ Trims punctuation from pantry label cues such as `net wt:`, `best by:`, `opened:`, and `exp:` so cue words do not leak into item names
- ✅ Seeds reviewable pantry items from scanned or manually entered barcodes
- ✅ Prefers labeled UPC/EAN/GTIN values over unrelated item/date numbers in pasted barcode text
- ✅ Looks up scanned/manually entered barcodes with Open Food Facts and falls back to reviewable barcode rows
- ✅ Maps barcode lookup found/not-found/error results into reviewable pantry rows and phone-visible add/update status messages
- ✅ Merges safe duplicate pantry imports from typed, reviewed OCR/AI photo, and barcode paths, including missing-brand to known-brand matches
- ✅ Stages pantry camera/gallery photo results in an edit-before-save review queue
- ✅ Lets users correct or remove pantry OCR/AI output, common best-by date formats, and verification status before saving
- ✅ Validates pantry edit quantities and best-by dates with dot, comma, leading-decimal, and flexible date input while blocking invalid values
- ✅ Uses Gemini Vision when configured and ML Kit OCR when not configured
- ✅ Normalizes Gemini key/model setup mistakes before API calls
- ✅ Parses Gemini pantry responses with fenced JSON, scalar warnings/questions, alternate review-question/warning aliases, top-level arrays, single-item objects, item-wrapper aliases, snake_case/name aliases, numeric/comma-decimal/word/object quantity aliases, storage aliases, non-finite numeric fallback, non-JSON fallback, and clamped confidence
- ✅ Normalizes saved AI pantry rows so raw plural units, fluid-ounce wording, whitespace, punctuated Generic/unknown brand text, and refrigerator/cold-storage wording become canonical/reviewable pantry values
- ✅ Maps missing, zero, or negative Gemini pantry amounts to reviewable quantity defaults instead of saving invalid pantry quantities
- ✅ Captures full-resolution app-cache photos for pantry, flyer, and receipt OCR
- ✅ Decodes phone images as software bitmaps and caps oversized inputs before OCR/Gemini processing
- ✅ Shows recovery status if a selected camera/gallery image cannot be opened
- ✅ Caps rendered flyer/receipt PDF page size before OCR processing and surfaces when only the first 12 pages were processed
- ✅ Shows clear status when camera permission is denied or capture/scan/gallery/PDF selection is canceled
- ✅ Shows clear status when manual pantry, barcode, flyer text, or receipt text actions are blank
- ✅ Keeps manual barcode, pasted flyer, and pasted receipt text available after failed imports while clearing only after successful imports
- ✅ Shows Gemini/OCR fallback status in Settings
- ✅ Tests Gemini key/model/network connectivity from Settings
- ✅ Tests Gemini key/model/network connectivity locally before phone testing without printing the key
- ✅ Validates Settings protein-per-meal values with dot, comma, and leading-decimal input while blocking negative, invalid, and non-finite text
- ✅ Lets users correct flyer OCR/PDF/text deal output before using it in meal plans
- ✅ Imports store-aware flyer deals from camera/gallery photos, PDFs, and pasted OCR text
- ✅ Validates deal edit price, PPU, discount, score, confidence, limit, and valid-until date values with flexible decimal/date input and bounded score/percent ranges
- ✅ Parses flyer prices with or without dollar signs, including comma-decimal and cent-style prices such as 2,99/lb, 99c/lb, and 88c, plus comma-decimal package sizes such as 5,3 oz
- ✅ Ignores savings-only flyer callouts such as `Save $1 when you buy 2` so coupon savings do not import as fake deals
- ✅ Parses bundled demo flyer structures and shorthand flyer promos including package prices, each/ea prices, multi-line names, limits, coupons, numeric/word-number buy-get promos, buy-get percent-off promos, BOGO, B1G1, and BOGO-percent modifiers
- ✅ Imports receipt items from camera/gallery photos, PDFs, or pasted OCR text
- ✅ Lets users correct receipt OCR/reconciliation output, common receipt date formats, and review status
- ✅ Parses the bundled demo receipt used by the phone test checklist
- ✅ Applies receipt header dates including year-first slash/dash OCR formats such as `Transaction Date: 2025/10/27`
- ✅ Attaches split quantity and one-line weighted price-per-pound lines to their grocery items, including `@` and `x` quantity separators
- ✅ Parses receipt OCR item totals and inline quantity lines before or after the item name, with or without dollar signs and with comma decimals
- ✅ Accepts comma-decimal manual corrections in pantry, deal, receipt, and settings numeric edit fields while rejecting non-finite values, and accepts common date corrections in pantry, deal, and receipt review dialogs
- ✅ Ignores receipt subtotal, tax, total, savings, saved-total, SNAP/EBT/WIC benefit tender, and tender/payment/card-tender lines
- ✅ Ignores receipt return/refund rows with negative totals so they do not import as positive grocery spending
- ✅ Rounds receipt totals to cents before applying budget updates
- ✅ Keeps pantry quantities and budget spending in sync when receipt items are edited or deleted
- ✅ Recalculates budget daily envelope and projected spend from current-month receipt history
- ✅ Validates Budget Settings monthly budget, spent-to-date baseline, and breakfast anchor cost with dot, comma, and leading-decimal input while blocking negative, invalid, and non-finite text
- ✅ Scores deals
- ✅ Generates meal plans
- ✅ Generates deterministic meal plans from the same pantry/deals/settings inputs
- ✅ Filters custom dietary restrictions out of generated Menu meals and Shopping
- ✅ Ignores household/non-food flyer deals when selecting generated meal sides
- ✅ Replaces the active generated week so repeated Generate taps do not duplicate meal-plan rows
- ✅ Shows meal-generation status and planner warnings
- ✅ Prevents negative protein-per-meal settings from creating negative Shopping quantities or costs
- ✅ Refreshes the visible shopping list after pantry/deals/receipts/settings changes once meal plans exist
- ✅ Exports the generated Shopping list as a shareable PDF from app cache through the FileProvider
- ✅ Tracks budget
- ✅ Editable budget settings with validation and saved feedback
- ✅ Creates shopping lists
- ✅ Keeps different shopping-list deal identities separate before and after Room assigns ids
- ✅ Formats shopping-list PDF contents with totals, store, brand/size, coupon, limit, and meal-purpose details
- ✅ Passes all unit tests

## 📦 Deliverables Checklist

- ✅ Complete Android Studio project
- ✅ All Gradle files with dependencies
- ✅ 7 Room entities + DAOs + Database
- ✅ Production-ready pantry parser
- ✅ Pantry item review/edit flow with quantity validation
- ✅ Visible typed pantry add/update feedback
- ✅ Barcode/manual code pantry lookup and intake
- ✅ Manual barcode/code failure text retention
- ✅ Production-ready deals parser
- ✅ Deal review/edit flow with numeric validation
- ✅ OCR integration (ML Kit)
- ✅ Store-name normalization for flyer and receipt imports
- ✅ Optional Gemini Vision integration with flexible and review-safe label-date handling
- ✅ Settings save feedback and non-negative protein input validation
- ✅ Custom dietary restrictions in Settings with Menu/Shopping filtering
- ✅ Meal planning engine (rules-based)
- ✅ Budget tracking engine
- ✅ Budget settings edit flow
- ✅ Receipt reconciliation engine
- ✅ Receipt edit/delete adjustment calculator for Budget and pantry quantity deltas
- ✅ Receipt review/edit flow with numeric validation
- ✅ 7 Compose UI screens
- ✅ Settings build identity display sourced from `BuildConfig`
- ✅ Repeatable demo data + seed function
- ✅ PowerShell phone install/launch helper with on-device package verification
- ✅ PowerShell phone/Gemini/barcode preflight helper with GitHub sync, APK identity/permission checks, optional live Gemini API check, and stale source/Gemini APK warnings
- ✅ PowerShell Gemini live connection helper for pre-phone AI setup verification without printing secrets
- ✅ Phone preflight/install helpers give specific ADB recovery guidance for no-device and unauthorized/offline states
- ✅ PowerShell feature readiness report helper can run the local gate, separate local source/test evidence from remaining Android phone and real-Gemini checks, audit the Deal Planner naming transition, account for cached unit/lint report timestamps after a successful gate, and flag stale APK evidence
- ✅ One-command phone test starter writes setup status/setup mode on success and a failure-state report with stopping reason if setup stops early
- ✅ PowerShell phone log/crash capture helper with ignored local log output
- ✅ Phone test report includes setup mode, current lint snapshot, and latest sample folder/manifest evidence when available
- ✅ Phone sample generator includes pantry-label OCR rows for `16-ounce`, `12-count`, punctuated label cues, slash dates, and two-digit dash dates plus local manifest/hash verification
- ✅ Phone sample transfer verifies the generated sample manifest byte counts and SHA-256 hashes before copying files to a phone
- ✅ Phone sample transfer writes local destination/byte-size evidence for the generated phone test report
- ✅ Adaptive launcher icons include a monochrome themed-icon asset for Android launcher compatibility
- ✅ 256 unit tests
- ✅ Comprehensive README
- ✅ No placeholder blocking the core phone-test flow

## 🎉 Ready to Test

Simply:
1. Open in Android Studio
2. Sync Gradle
3. Run
4. Tap "Load Demo"
5. Explore all features!

---

**Built as a phone-testable Deal Planner MVP baseline**
