# Deal Planner Recovery Status - 2026-07-16

## Source of Truth

- Current app folder: `C:\Users\adamc\AndroidStudioProjects\Deal_Planner`
- Android Studio had last opened the older-named folder: `C:\Users\adamc\AndroidStudioProjects\SNAP_Optimizer`
- Clean renamed folder to use going forward: `C:\Users\adamc\AndroidStudioProjects\Deal_Planner`
- GitHub remote: `https://github.com/adamcboyd/Deal-Planner.git`
- Current branch: `codex/deal-planner-baseline`
- Pushed recovery checkpoint at start of follow-up work: `01e3dca docs: add recovery status checkpoint`

## Other Local Copies Found

- `F:\PROJECTS\DealPlanner`: older web/worker/resources material, not current Android app.
- `F:\PROJECTS\DealPlannerAndroid`: old Android wrapper plus nested `SNAP_Optimizer`; not current.
- `F:\PROJECTS\DealPlannerAndroid_DEV`: older Android project, not current.
- `F:\PROJECTS\DealPlannerAndroid_ORIGINAL_BACKUP`: backup copy, not current.
- `F:\PROJECTS\SNAP_Optimizer_DEV`: older clean SNAP Optimizer repo, not current Deal Planner.
- `C:\Users\adamc\AndroidStudioProjects\SNAP_Optimizer`: same current commit as `Deal_Planner`, but old folder name.

## Verification Run This Session

Run from `C:\Users\adamc\AndroidStudioProjects\Deal_Planner`:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-20'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest --rerun-tasks assembleDebug
```

Result: `BUILD SUCCESSFUL`.

Latest continuation gate after pantry duplicate upsert work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `0 errors, 27 warnings`.

Latest continuation gate after receipt-aware budget analysis work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `0 errors, 27 warnings`.

Latest continuation gate after receipt no-dollar OCR parsing work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `0 errors, 27 warnings`.

Latest continuation gate after flyer no-dollar OCR parsing work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `0 errors, 27 warnings`.

Latest continuation gate after Gemini setup normalization work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `0 errors, 27 warnings`.

Latest continuation gate after camera permission/cancel status work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `0 errors, 27 warnings`.

Latest continuation gate after phone image decode hardening:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `0 errors, 27 warnings`.

Latest continuation gate after flyer PDF render cap work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `0 errors, 27 warnings`.

Latest continuation gate after gallery/PDF picker cancel status work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `0 errors, 27 warnings`.

Latest continuation gate after image-open failure status work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `0 errors, 27 warnings`.

Latest continuation gate after Gemini pantry response parser hardening:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `0 errors, 27 warnings`.

Latest continuation gate after Deal Planner database naming cleanup:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `0 errors, 27 warnings`.

Latest continuation gate after phone install helper work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `0 errors, 27 warnings`.

Latest continuation gate after navigation string-resource cleanup:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `0 errors, 21 warnings`.

Latest continuation gate after malformed Gemini pantry response hardening:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `0 errors, 21 warnings`.

Latest continuation gate after Open Food Facts barcode lookup work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `0 errors, 21 warnings`.

Latest continuation gate after repeatable meal-plan generation work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `0 errors, 21 warnings`.

Latest continuation gate after deterministic meal-plan rotation work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `0 errors, 21 warnings`.

Latest continuation gate after shopping-list consolidation identity work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `0 errors, 21 warnings`.

Latest continuation gate after pantry ISO best-by date parsing work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `77` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after receipt discount/refund filtering work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `78` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after slash-style flyer multi-buy parsing work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `79` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after receipt header-date parsing work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `80` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after flyer date false-positive filtering work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `81` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after pasted barcode normalization work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `83` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after scoped pantry opened/best-by date parsing work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `84` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after receipt card-tender filtering work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `84` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after cent-style flyer price parsing work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `85` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after label-style pantry expiration cue parsing work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `86` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after Gemini pantry field-alias parsing work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `87` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after Gemini pantry item-wrapper parsing work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `88` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after Gemini pantry quantity/storage alias parsing work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `89` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after Gemini pantry word-quantity parsing work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `90` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after BOGO/B1G1 flyer shorthand parsing work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `91` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after BOGO percent-off flyer shorthand parsing work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `92` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after SNAP/EBT/WIC receipt tender filtering work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `93` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after pantry date-label wording cleanup:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `94` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after pantry opened-on date wording cleanup:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `95` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after hyphenated pantry date-label parsing work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `96` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after word-number buy-get flyer promo parsing work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `97` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after buy-get percent-off flyer promo parsing work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `98` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after comma-decimal OCR price parsing work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `100` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after pantry comma-decimal quantity/size parsing work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `101` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after Gemini comma-decimal quantity/confidence parsing work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `102` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after inline weighted produce receipt parsing work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `103` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after labeled barcode normalization work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `104` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after comma-decimal review/edit numeric parsing work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `107` unit tests detected and `0 errors, 21 warnings`.

Latest phone preflight helper check:

```powershell
.\scripts\phone-debug-preflight.ps1
```

Result after committing the debug APK source freshness guard checkpoint: `0 failure(s), 2 warning(s)` for expected local conditions: no connected/authorized phone and no Gemini key configured. Git branch was clean, app-debug.apk was newer than app source/resources/build config, and Open Food Facts barcode lookup endpoint was reachable.

Latest continuation gate after stale Gemini APK install guard work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `107` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after Gemini single-item response parsing work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `108` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after bundled demo receipt coverage work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `109` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after debug APK source freshness guard work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `109` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after shopping-list planned-cost work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `110` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after Gemini object-shaped response parsing work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `112` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after receipt-aware budget display work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `112` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after blank manual input status work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `112` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after meal-plan generation status work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `112` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after shopping-list startup restore work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `112` unit tests detected and `0 errors, 21 warnings`.

Latest focused AI connection error-summary check:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.dealplanner.ai.GeminiPantryVisionClientTest"
```

Result: `BUILD SUCCESSFUL`.

Latest continuation gate after Gemini API error-summary work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `115` unit tests detected and `0 errors, 21 warnings`.

Latest phone log helper checks:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\phone-debug-logs.ps1 -Help
.\scripts\phone-debug-preflight.ps1
```

Result: log helper help printed successfully, all PowerShell helper scripts parsed successfully, and preflight recognized `scripts\phone-debug-logs.ps1`.

Latest continuation gate after phone log helper work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `115` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after exact phone-checklist parser coverage:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `117` unit tests detected and `0 errors, 21 warnings`.

Latest focused manual OCR field-retention check:

```powershell
.\gradlew.bat compileDebugKotlin testDebugUnitTest --tests "com.dealplanner.parser.DealsParserTest" --tests "com.dealplanner.domain.ReceiptReconcilerTest"
```

Result: `BUILD SUCCESSFUL`.

Latest continuation gate after manual OCR field-retention work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `117` unit tests detected and `0 errors, 21 warnings`.

Latest APK identity/permission helper checks:

```powershell
.\scripts\phone-debug-preflight.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\phone-debug-install.ps1 -SkipBuild -NoLaunch
```

Result: preflight reported `APK identity` and `APK permissions` as OK. The install helper verified `com.dealplanner / Deal Planner` and required permissions before stopping at the expected no-phone-connected condition.

Latest continuation gate after APK identity/permission guard work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `117` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after manual barcode input retention work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `117` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after phone install package verification work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `117` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after Settings build identity display work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `117` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after GitHub sync preflight work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `117` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after live Shopping refresh work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `117` unit tests detected and `0 errors, 21 warnings`.

Additional check:

```powershell
.\gradlew.bat lintDebug
```

Result: `BUILD SUCCESSFUL`, with `0 errors, 27 warnings`.

ADB check:

```powershell
adb devices
```

Result: no connected/authorized Android device visible yet.

Debug APK:

`C:\Users\adamc\AndroidStudioProjects\Deal_Planner\app\build\outputs\apk\debug\app-debug.apk`

Phone install helper:

```powershell
.\scripts\phone-debug-preflight.ps1
```

```powershell
.\scripts\phone-debug-install.ps1
```

Use `.\scripts\phone-debug-preflight.ps1` to check repo/APK/ADB/Gemini/barcode lookup readiness before installing.
Use `.\scripts\phone-debug-install.ps1 -SkipBuild` after the APK is already built and app source/resources/build config plus Gemini/local configuration have not changed.
Use `.\scripts\phone-debug-logs.ps1` to capture device metadata, full logcat, and a Deal Planner/crash-filtered log if a real-phone test fails. Captured logs write to ignored local `phone-test-logs\`.

Phone test checklist:

`C:\Users\adamc\AndroidStudioProjects\Deal_Planner\PHONE_TEST_CHECKLIST_2026-07-16.md`

## Current Feature Status

Verified by build/unit tests/code inspection:

- App name/package is now Deal Planner: `com.dealplanner`.
- Room database filename is now `deal_planner_db`.
- Settings -> About Deal Planner displays the actual Gradle version, package name, and debug/release build identity from `BuildConfig`.
- `scripts\phone-debug-install.ps1` can build, verify, install, confirm the package on-device, and launch the debug APK once ADB sees an authorized phone.
- `scripts\phone-debug-preflight.ps1` reports repo, GitHub origin/upstream sync, APK, APK identity/permissions, ADB/phone, Gemini, and Open Food Facts readiness without printing secrets.
- `scripts\phone-debug-preflight.ps1` confirms origin points at `adamcboyd/Deal-Planner`, compares the branch with its configured upstream, and checks the GitHub branch SHA with `git ls-remote` when network checks are enabled.
- `scripts\phone-debug-preflight.ps1` and `scripts\phone-debug-install.ps1` inspect `app-debug.apk` with Android SDK `aapt` when available, verifying `com.dealplanner` / `Deal Planner` plus required `INTERNET` and `CAMERA` permissions before phone testing.
- `scripts\phone-debug-preflight.ps1` warns when app source/resources/build config or `local.properties` are newer than `app-debug.apk`, and `scripts\phone-debug-install.ps1 -SkipBuild` refuses that stale APK so app code and Gemini key/model values must be rebuilt before phone testing.
- `scripts\phone-debug-logs.ps1` is available for phone-test crash/log capture and writes local logs under ignored `phone-test-logs\`.
- Bottom navigation labels are now backed by string resources while preserving the visible tab labels.
- Room local database and repository layer compile.
- Pantry natural-language parser has unit tests.
- Pantry manual text input parses the phone-checklist `best by 2026-12-31` ISO date format without treating the date as part of the item name.
- Pantry manual text input keeps `opened` and `best by` dates independent when both appear in one phrase, such as `opened yesterday best by 2026-12-31`.
- Pantry manual text input parses common label-style expiration cues such as `best before`, `use by`, and `exp`.
- Pantry manual text input parses label wording such as `expiration date 12/31/2026` and `best by date 2026-12-31` without leaving `date` in the item name.
- Pantry manual text input parses `opened on 2026-07-01` without leaving `on` in the item name.
- Pantry manual text input parses hyphenated label cues such as `use-by 12/31/2026` and `best-by 2026-12-31` without leaving the cue in the item name.
- Pantry manual/OCR text input accepts comma-decimal quantities and package sizes such as `1,5 lb ground beef` and `Kroger yogurt 5,3oz`.
- Deals flyer parser has unit tests, including bundled demo flyer structures.
- Deals parser handles package prices, multi-line names, and trailing modifiers such as limits, coupons, and BOGO lines.
- Deals parser handles slash-style multi-buy prices such as `2/$5` and `10 / $10`.
- Deals parser handles word-number buy-get flyer promos such as `Buy One Get One Free` and `Buy Two Get One Free`.
- Deals parser handles buy-get percent-off promos such as `Buy One Get One 50% off` as a 25% effective overall discount and `Buy Two Get One 50% off` as about 16.7%.
- Deals parser handles BOGO flyer shorthand such as `BOGO Free` and `B1G1` without merging the next flyer item or treating `B1G1` as a package size.
- Deals parser handles BOGO second-item percent discounts such as `BOGO 50% off` as a 25% effective overall discount.
- Deals parser ignores flyer metadata/date lines such as `Valid 7/16/2026 - 7/22/2026` so slash dates do not become fake multi-buy deals.
- Deals parser accepts flyer prices when OCR drops dollar signs.
- Deals parser accepts comma-decimal flyer OCR prices such as `2,99/lb`, `2 for 5,00`, and `3 lb bag 2,99`.
- Deals parser accepts cent-style flyer/OCR prices such as `99c/lb` and `88c`, with exact unit coverage for the phone checklist `Roma Tomatoes` / `99c/lb` pasted-text test.
- Meal planning engine has unit tests.
- Meal plan generation has unit coverage for one generated row per requested date and deterministic output for the same inputs.
- Menu Generate shows visible meal-plan generation status and any rules-engine warnings, such as missing protein deals or pantry starch anchors.
- Shopping list consolidation has unit coverage for pre-database deal identities before Room assigns ids.
- Shopping list estimated costs use planned quantities and normalized price-per-unit values instead of multiplying sticker price by planned quantity in the UI.
- After saved meal plans exist, Pantry, Deals, Receipts, and Settings input changes rederive the visible Shopping list from current inputs.
- Budget engine has unit tests.
- Receipt reconciliation engine has unit tests.
- Pantry photo/gallery/barcode/manual code input exists.
- Pantry items can be edited/reviewed after typed, barcode/manual code, OCR, or AI import.
- Barcode/manual code pantry input looks up product names, brands, and package quantities through Open Food Facts when network is available.
- Barcode/manual code pantry input still creates VERIFY fallback items with the barcode preserved in notes when lookup misses or network is unavailable.
- Barcode/manual code normalization extracts 8-14 digit UPC/EAN/GTIN codes from pasted label text such as `UPC: 0 12345-67890 5`, prefers labeled UPC/EAN/GTIN values over unrelated item/date numbers, and rejects non-code text with `No barcode found.`.
- Manual barcode/code text stays available for correction when no UPC/EAN/GTIN is found, and clears only after a successful barcode import.
- Open Food Facts barcode response parsing and barcode normalization have no-network unit coverage.
- Pantry typed, OCR/AI photo, and barcode imports now upsert safe duplicates instead of creating repeated rows.
- Pantry duplicate detection normalizes package size/Generic brand, keeps different locations separate, and only merges barcode items when the barcode value matches.
- Pantry edit/review quantity fields accept comma-decimal corrections such as `1,5`.
- Camera permission denial and canceled camera/barcode/gallery/PDF actions now show visible status messages during phone testing.
- Blank manual pantry Add, barcode Add Code, flyer Process Text, and receipt Process Text taps show visible status messages instead of silently doing nothing.
- Camera/gallery image imports decode to software bitmaps and cap oversized phone images before OCR/Gemini processing.
- Camera/gallery image-open failures show visible recovery messages instead of escaping the import coroutine.
- Flyer photo/gallery/PDF/manual text input exists.
- Flyer pasted-text import shows processing status, keeps pasted text available when parsing finds no deals, and clears it only after successful deal import.
- Flyer PDF pages render with a 3072px longest-side cap before OCR to reduce oversized-PDF failures on phones.
- Flyer imports are store-aware instead of defaulting every scanned deal to `Unknown`.
- Flyer deals can be edited/reviewed after photo, gallery, PDF, or pasted OCR import.
- Flyer deal edit/review numeric fields accept comma-decimal corrections for price, PPU, discount, score, and confidence.
- Receipt photo/gallery/manual text input exists.
- Receipt pasted-text import shows processing status, keeps pasted text available when parsing finds no receipt line items, and clears it only after successful receipt import.
- Receipt items can be edited/reviewed after photo, gallery, or pasted OCR import.
- Receipt edit/review numeric fields accept comma-decimal corrections for quantity, total, and confidence.
- Bundled `demo_receipt.txt` parses into the expected 8 grocery items for the deterministic phone checklist pasted-text receipt test, ignores the EBT/card tender line, applies the `Date: 10/27/2025` header, and totals `$40.65`.
- Bundled `demo_receipt.txt` also has unit coverage for the phone checklist appended tender lines `VISA DEBIT $40.65` and `CARD TENDER $40.65`.
- Receipt header dates such as `Date: 10/27/2025` are applied to imported receipt rows when available; rows fall back to today's date when no receipt date is found.
- Receipt reconciliation attaches split quantity lines, including weighted price-per-pound lines, to the previous grocery item.
- Receipt reconciliation parses one-line weighted produce rows such as `BANANAS 1.50 lb @ $0.69/lb $1.04` and comma-decimal variants such as `APPLES 1,25 lb @ 1,99/lb 2,49`.
- Receipt reconciliation accepts item totals and inline quantity lines when OCR drops dollar signs.
- Receipt reconciliation accepts item totals and split quantity lines when OCR uses comma decimals, such as `BLACK BEANS 1,78` plus `2 @ 0,89`.
- Receipt reconciliation ignores subtotal, tax, total, savings, and tender/payment lines, including card tender lines such as `VISA DEBIT` and `CARD TENDER`.
- Receipt reconciliation ignores SNAP/EBT/WIC benefit tender lines such as `SNAP EBT`, `EBT FOOD`, and `WIC BENEFIT` so they do not inflate grocery spending.
- Receipt reconciliation ignores coupon, discount, reward, refund, return, promo, markdown, and adjustment lines so those OCR rows do not increase spending.
- Receipt totals are rounded to cents before budget updates.
- Receipt imports, edits, and deletes adjust budget spending totals, daily envelope, and projected spend.
- Budget analysis loads actual receipts and uses current-month receipt history when calculating projected spend.
- Budget screen current balance, monthly overview, and progress display use receipt-aware analysis values when available, so stale stored budget totals do not contradict current receipt history.
- Pantry-matched receipt edits and deletes adjust pantry quantities.
- Camera capture now uses full-resolution app-cache image files for pantry, flyer, and receipt OCR.
- ML Kit OCR fallback exists.
- Optional Gemini pantry photo client exists.
- Settings screen shows whether Gemini Vision is configured or OCR fallback is active.
- Settings screen includes a Test AI Connection button for key/model/network verification on the phone.
- Settings Test AI Connection summarizes Gemini API errors with concise HTTP/status messages instead of showing raw server JSON.
- Settings protein-per-meal numeric input accepts comma-decimal values such as `0,5`.
- Settings About displays version `1.0 (1)`, package `com.dealplanner`, and debug/release build identity from the installed build.
- Placeholder Gemini keys are treated as not configured.
- Gemini setup trims accidental key/model whitespace and normalizes a pasted `models/` prefix before calling the API.
- Gemini pantry response parsing has no-network unit coverage for fenced JSON, minor surrounding text, scalar warnings/questions, top-level arrays, single-item objects, plural and singular item wrappers, snake_case/name aliases, numeric/comma-decimal/word/object quantity aliases such as `amount: "2 cans"`, `amount: "1,5 lb"`, `amount: "two cans"`, or `quantity: { value: "2", unit: "cans" }`, comma-decimal confidence such as `"0,82"`, storage aliases, malformed string/list fields, and confidence clamping.
- Demo data loading resets pantry, deals, receipts, meal plans, default meal settings, and the `$292 / $45 spent` demo budget baseline.
- Menu Generate deterministically rebuilds and replaces the active generated week so repeated taps do not duplicate meal-plan rows.
- Shopping list consolidation keeps different deals separate even before Room assigns database ids, and estimated Shopping totals are covered by unit tests.
- On app startup, if saved meal plans already exist, the Shopping list is rederived from current pantry/deals/settings so a relaunched app does not show an empty transient list after meal plans have already been generated.

Not yet verified on a real phone:

- Camera capture UX.
- Full-resolution app-cache camera URI behavior on the physical phone.
- Gallery import UX.
- Pantry barcode scanner UX.
- Pantry manual barcode/code UX.
- Open Food Facts barcode lookup on the physical phone/network.
- Flyer PDF picker UX.
- Flyer pasted OCR text UX.
- Store-aware flyer import UX.
- Receipt photo/gallery/manual text UX.
- ML Kit OCR quality on real pantry/flyer photos.
- Gemini pantry photo API call.
- Android permissions flow.
- Camera permission denial/cancel and gallery/PDF picker cancel status on the physical phone.
- Shopping list startup restore on the physical phone.
- Kitchen pantry test.

Current AI configuration:

- `local.properties` was not present in the clean `Deal_Planner` folder.
- `GEMINI_API_KEY` environment variable was not set in this shell.
- Therefore Gemini Vision is not live-configured yet; the app will use ML Kit OCR fallback.
- Current default model in Gradle is `gemini-3.5-flash`, which matched the current Google AI model page checked on 2026-07-16.
- Rebuild the debug APK after adding or changing `local.properties`; Gemini values are compiled into `BuildConfig`, and `-SkipBuild` is blocked if `local.properties` is newer than the APK.
- Live Gemini connection testing is now available from Settings after adding a real key.

## Important Cautions

- `JAVA_HOME` in the environment points to `C:\Program Files\Android\Android Studio\jre`, which does not exist on this machine. Use `C:\Program Files\Java\jdk-20` for command-line builds unless Android Studio supplies its own JBR.
- Do not continue work in the broken Codex task attached to `C:\Users\adamc\Documents\NEOPUNK`; that task is attached to the wrong repo and can trigger missing-ref errors.
- Avoid editing the older `F:\PROJECTS` copies unless explicitly doing archival comparison.

## Next Recommended Task

1. Open Android Studio.
2. Open `C:\Users\adamc\AndroidStudioProjects\Deal_Planner`.
3. Add Gemini key to `local.properties` only if you want live AI pantry photo extraction:

```properties
gemini.api.key=YOUR_GEMINI_API_KEY
gemini.model=gemini-3.5-flash
```

4. Connect Android phone with USB debugging enabled.
5. Confirm `adb devices` shows the phone as `device`.
6. Install or run the debug app, or use:

```powershell
.\scripts\phone-debug-install.ps1
```

Optional if anything fails on the phone:

```powershell
.\scripts\phone-debug-logs.ps1 -Clear -Launch -DurationSeconds 90
```

7. Test in this order:
   - Launch app and tap Load Demo.
   - Close/relaunch the app and confirm Shopping still has the generated list without tapping Generate again.
   - After receipt/budget tests, tap Load Demo again and confirm Budget returns to the `$292 / $45 spent` demo baseline.
   - Pantry typed entry.
   - Pantry typed/OCR text with comma-decimal quantity or size such as `1,5 lb ground beef` or `Kroger yogurt 5,3oz`.
   - Pantry photo.
   - Pantry gallery image.
   - Pantry barcode scan.
   - Pantry camera-permission denial or canceled capture/gallery/scan status.
   - Pantry manual barcode/code entry.
   - Pantry manual barcode/code entry with pasted label text that includes unrelated item/date numbers before the UPC.
   - Pantry manual barcode/code entry with invalid text that has no product code; confirm `No barcode found.` appears and the text stays available for correction.
   - Pantry duplicate check: add the same typed/photo item twice and confirm quantity merges.
   - Pantry barcode duplicate check: add the same UPC twice and confirm quantity merges, then add a different UPC and confirm it remains separate.
   - Pantry edit/review dialog for VERIFY items.
   - Pantry edit/review dialog comma-decimal quantity correction such as `1,5`.
   - Deals flyer photo.
   - Deals camera-permission denial or canceled capture/gallery/PDF status.
   - Deals gallery image.
   - Deals PDF.
   - Deals pasted OCR text.
   - Deals pasted OCR text with prices missing dollar signs.
   - Deals pasted OCR text with comma-decimal prices such as `2,99/lb` or `2 for 5,00`.
   - Deals bundled demo flyer text via pasted OCR.
   - Deals store field applies to photo, gallery, PDF, and pasted OCR imports.
   - Deals edit/review dialog for low-confidence OCR results.
   - Deals edit/review dialog comma-decimal numeric correction such as price `2,99`.
   - Receipts photo.
   - Receipts camera-permission denial or canceled capture/gallery status.
   - Receipts gallery image.
   - Receipts pasted OCR text.
   - Receipts pasted OCR text with prices missing dollar signs.
   - Receipts pasted OCR text with comma-decimal prices such as `BLACK BEANS 1,78` and `2 @ 0,89`.
   - Receipts split quantity lines do not import as separate items.
   - Receipts one-line weighted produce rows parse item name, weight, and total.
   - Receipts subtotal/tax/total/payment lines do not import as items.
   - Receipts edit/review dialog for OCR and match corrections.
   - Receipts edit/review dialog comma-decimal numeric correction such as total `1,78`.
   - Receipts edit/delete budget total adjustment.
   - Budget daily envelope changes after receipt import, receipt total edit, and receipt delete.
   - Budget projected spend reflects current-month receipt history.
   - Receipts edit/delete pantry quantity adjustment for pantry matches.
   - Settings AI status before and after adding a real Gemini key.
   - Settings Test AI Connection before pantry AI photo testing.
   - Settings protein-per-meal comma-decimal value such as `0,5`.
   - Settings About build identity: version `1.0 (1)`, package `com.dealplanner`, and debug build.
   - Generate meal plan.
   - Review shopping list.
   - After a generated plan exists, change pantry/deal/receipt/settings inputs and confirm Shopping refreshes without app relaunch.
   - Review budget.

After those pass, decide whether to polish current flows or add optional features such as nutrition lookup, guided multi-photo flyer capture, price history, and monetization/convenience features.
