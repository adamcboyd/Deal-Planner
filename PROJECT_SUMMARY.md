# Deal Planner - Complete Project Summary

## ✅ Project Status: BUILDABLE PHONE-TEST BASELINE

This is a working Android MVP baseline that compiles, passes unit tests, builds a debug APK, and is ready for physical-phone testing.

## 📊 Project Statistics

- **Total Kotlin Files**: 47
- **Configuration Files**: 15
- **Test Files**: 8 (comprehensive unit tests)
- **Lines of Code**: ~4,000+

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
        │   │   │   └── ReceiptReconciler.kt      ✅ Fuzzy matching
        │   │   ├── util/
        │   │   │   └── FlexibleNumberParsing.kt  ✅ Flexible numeric edit parsing
        │   │   └── ui/
        │   │       ├── camera/CapturePhotoUriFactory.kt ✅ Full-resolution capture URIs
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
        │       │   └── ic_launcher_foreground.xml  ✅ Launcher icon
        │       ├── xml/
        │       │   └── file_paths.xml              ✅ FileProvider cache paths
        │       └── mipmap-anydpi-v26/
        │           ├── ic_launcher.xml             ✅ Adaptive icon
        │           └── ic_launcher_round.xml       ✅ Round icon
        └── test/java/com/dealplanner/
            ├── ai/
            │   └── GeminiPantryVisionClientTest.kt ✅ 15 test cases
            ├── lookup/
            │   └── OpenFoodFactsBarcodeClientTest.kt ✅ 8 test cases
            ├── parser/
            │   ├── PantryPhraseParserTest.kt  ✅ 26 test cases
            │   └── DealsParserTest.kt         ✅ 20 test cases
            ├── domain/
                ├── MealPlanningEngineTest.kt  ✅ 8 test cases
                ├── BudgetEngineTest.kt        ✅ 10 test cases
                └── ReceiptReconcilerTest.kt   ✅ 17 test cases
            └── util/
                └── FlexibleNumberParsingTest.kt ✅ 3 test cases
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
  - Parses dates (relative and absolute, including label cues such as opened on, best before, best-by, best by date, use by, use-by, expiration date, and exp)
  - Duplicate detection and merging
  - Confidence scoring

- ✅ **DealsParser**:
  - $X.XX/lb pattern
  - Cent-style prices such as 99c/lb and 88c
  - Plain package prices such as 3 lb bag $2.99
  - Dollar/no-dollar/comma-decimal flyer OCR price parsing
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
- ✅ Bounded flyer PDF page rendering for OCR reliability
- ✅ Camera/gallery/PDF permission and cancel status feedback for phone testing
- ✅ Pantry camera/gallery/barcode/manual code import
- ✅ Open Food Facts product lookup for barcode/manual code intake with reviewable fallback
- ✅ Flyer camera/gallery/PDF/manual text import
- ✅ Receipt camera/gallery/manual text import
- ✅ Optional Gemini Vision client using local.properties or GEMINI_API_KEY
- ✅ Gemini key/model trimming and model-prefix normalization
- ✅ Gemini pantry response parsing for fenced JSON, minor model-output variations, top-level arrays, item-wrapper aliases, snake_case/name aliases, numeric/comma-decimal/word quantity aliases, storage aliases, and malformed string/list fields
- ✅ Settings screen AI configuration status and Gemini connection test
- ✅ ML Kit OCR fallback when Gemini is not configured
- ✅ ZXing barcode scanner intake for reviewable pantry seeding

### 4. Business Logic Engines
- ✅ **MealPlanningEngine**:
  - 7-day plan generation
  - GERD-friendly filtering
  - Pantry anchor utilization
  - Freezer directive calculation
  - Shopping list consolidation
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
  - Pantry quantity updates
  - Dollar/no-dollar/comma-decimal OCR price parsing

### 5. UI Layer (Jetpack Compose)
- ✅ **MainActivity**: Navigation + bottom bar
- ✅ **PantryScreen**: Natural language input, barcode/manual code intake, photo/gallery import, VERIFY badges
- ✅ **Pantry Review Dialog**: Edit imported pantry items and clear/keep verification flags
- ✅ **DealsScreen**: Flyer photo/gallery/PDF/manual text import, store-aware deal cards with scores, coupon flags
- ✅ **Deal Review Dialog**: Edit imported deals, coupon flags, scores, and confidence
- ✅ **ReceiptsScreen**: Receipt photo/gallery/manual text import, review flags, budget updates
- ✅ **Receipt Review Dialog**: Edit imported receipt lines, totals, match metadata, confidence, date, and review status
- ✅ **ShoppingListScreen**: Consolidated list with PPU
- ✅ **MenuScreen**: 7-day plan with freezer directives
- ✅ **BudgetScreen**: Balance, envelope, analysis, suggestions
- ✅ **ParamsScreen**: Dietary preferences, meal settings, AI status, Gemini connection test
- ✅ Material 3 theming with dark/light support

### 6. Demo Data
- ✅ Seed function with:
  - 5 pantry anchors (rice, pasta, oats, beans, oil)
  - 4 sample deals (pork, chicken, broccoli, mandarins)
  - Resettable budget state ($292 food budget, $45 spent)
  - Resettable default meal params
  - Auto-generated 7-day meal plan

### 7. Unit Tests (107 Test Cases)
- ✅ GeminiPantryVisionClientTest (15 tests)
- ✅ OpenFoodFactsBarcodeClientTest (8 tests)
- ✅ PantryPhraseParserTest (26 tests)
- ✅ DealsParserTest (20 tests)
- ✅ MealPlanningEngineTest (8 tests)
- ✅ BudgetEngineTest (10 tests)
- ✅ ReceiptReconcilerTest (17 tests)
- ✅ FlexibleNumberParsingTest (3 tests)

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
- ✅ Seeds reviewable pantry items from scanned or manually entered barcodes
- ✅ Prefers labeled UPC/EAN/GTIN values over unrelated item/date numbers in pasted barcode text
- ✅ Looks up scanned/manually entered barcodes with Open Food Facts and falls back to reviewable barcode rows
- ✅ Merges safe duplicate pantry imports from typed, OCR/AI photo, and barcode paths
- ✅ Imports pantry items from camera/gallery photos
- ✅ Lets users correct pantry OCR/AI output and verification status
- ✅ Uses Gemini Vision when configured and ML Kit OCR when not configured
- ✅ Normalizes Gemini key/model setup mistakes before API calls
- ✅ Parses Gemini pantry responses with fenced JSON, scalar warnings/questions, top-level arrays, item-wrapper aliases, snake_case/name aliases, numeric/comma-decimal/word quantity aliases, storage aliases, and clamped confidence
- ✅ Captures full-resolution app-cache photos for pantry, flyer, and receipt OCR
- ✅ Decodes phone images as software bitmaps and caps oversized inputs before OCR/Gemini processing
- ✅ Shows recovery status if a selected camera/gallery image cannot be opened
- ✅ Caps rendered flyer PDF page size before OCR processing
- ✅ Shows clear status when camera permission is denied or capture/scan/gallery/PDF selection is canceled
- ✅ Shows Gemini/OCR fallback status in Settings
- ✅ Tests Gemini key/model/network connectivity from Settings
- ✅ Lets users correct flyer OCR/PDF/text deal output before using it in meal plans
- ✅ Imports store-aware flyer deals from camera/gallery photos, PDFs, and pasted OCR text
- ✅ Parses flyer prices with or without dollar signs, including comma-decimal and cent-style prices such as 2,99/lb, 99c/lb, and 88c
- ✅ Parses bundled demo flyer structures and shorthand flyer promos including package prices, multi-line names, limits, coupons, numeric/word-number buy-get promos, buy-get percent-off promos, BOGO, B1G1, and BOGO-percent modifiers
- ✅ Imports receipt items from camera/gallery photos or pasted OCR text
- ✅ Lets users correct receipt OCR/reconciliation output and review status
- ✅ Attaches split quantity and one-line weighted price-per-pound lines to their grocery items
- ✅ Parses receipt OCR item totals and inline quantity lines with or without dollar signs and with comma decimals
- ✅ Accepts comma-decimal manual corrections in pantry, deal, receipt, and settings numeric edit fields
- ✅ Ignores receipt subtotal, tax, total, savings, SNAP/EBT/WIC benefit tender, and tender/payment/card-tender lines
- ✅ Rounds receipt totals to cents before applying budget updates
- ✅ Keeps pantry quantities and budget spending in sync when receipt items are edited or deleted
- ✅ Recalculates budget daily envelope and projected spend from current-month receipt history
- ✅ Scores deals
- ✅ Generates meal plans
- ✅ Generates deterministic meal plans from the same pantry/deals/settings inputs
- ✅ Replaces the active generated week so repeated Generate taps do not duplicate meal-plan rows
- ✅ Tracks budget
- ✅ Creates shopping lists
- ✅ Keeps different shopping-list deal identities separate before and after Room assigns ids
- ✅ Passes all unit tests

## 📦 Deliverables Checklist

- ✅ Complete Android Studio project
- ✅ All Gradle files with dependencies
- ✅ 7 Room entities + DAOs + Database
- ✅ Production-ready pantry parser
- ✅ Pantry item review/edit flow
- ✅ Barcode/manual code pantry lookup and intake
- ✅ Production-ready deals parser
- ✅ Deal review/edit flow
- ✅ OCR integration (ML Kit)
- ✅ Optional Gemini Vision integration
- ✅ Meal planning engine (rules-based)
- ✅ Budget tracking engine
- ✅ Receipt reconciliation engine
- ✅ Receipt review/edit flow
- ✅ 7 Compose UI screens
- ✅ Repeatable demo data + seed function
- ✅ PowerShell phone install/launch helper
- ✅ PowerShell phone/Gemini/barcode preflight helper
- ✅ 107 unit tests
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
