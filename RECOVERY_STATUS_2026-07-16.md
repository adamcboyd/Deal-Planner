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
.\scripts\phone-debug-install.ps1
```

Use `.\scripts\phone-debug-install.ps1 -SkipBuild` after the APK is already built.

Phone test checklist:

`C:\Users\adamc\AndroidStudioProjects\Deal_Planner\PHONE_TEST_CHECKLIST_2026-07-16.md`

## Current Feature Status

Verified by build/unit tests/code inspection:

- App name/package is now Deal Planner: `com.dealplanner`.
- Room database filename is now `deal_planner_db`.
- `scripts\phone-debug-install.ps1` can build, verify, install, and launch the debug APK once ADB sees an authorized phone.
- Bottom navigation labels are now backed by string resources while preserving the visible tab labels.
- Room local database and repository layer compile.
- Pantry natural-language parser has unit tests.
- Deals flyer parser has unit tests, including bundled demo flyer structures.
- Deals parser handles package prices, multi-line names, and trailing modifiers such as limits, coupons, and BOGO lines.
- Deals parser accepts flyer prices when OCR drops dollar signs.
- Meal planning engine has unit tests.
- Meal plan generation has unit coverage for one generated row per requested date.
- Budget engine has unit tests.
- Receipt reconciliation engine has unit tests.
- Pantry photo/gallery/barcode/manual code input exists.
- Pantry items can be edited/reviewed after typed, barcode/manual code, OCR, or AI import.
- Barcode/manual code pantry input looks up product names, brands, and package quantities through Open Food Facts when network is available.
- Barcode/manual code pantry input still creates VERIFY fallback items with the barcode preserved in notes when lookup misses or network is unavailable.
- Open Food Facts barcode response parsing and barcode normalization have no-network unit coverage.
- Pantry typed, OCR/AI photo, and barcode imports now upsert safe duplicates instead of creating repeated rows.
- Pantry duplicate detection normalizes package size/Generic brand, keeps different locations separate, and only merges barcode items when the barcode value matches.
- Camera permission denial and canceled camera/barcode/gallery/PDF actions now show visible status messages during phone testing.
- Camera/gallery image imports decode to software bitmaps and cap oversized phone images before OCR/Gemini processing.
- Camera/gallery image-open failures show visible recovery messages instead of escaping the import coroutine.
- Flyer photo/gallery/PDF/manual text input exists.
- Flyer PDF pages render with a 3072px longest-side cap before OCR to reduce oversized-PDF failures on phones.
- Flyer imports are store-aware instead of defaulting every scanned deal to `Unknown`.
- Flyer deals can be edited/reviewed after photo, gallery, PDF, or pasted OCR import.
- Receipt photo/gallery/manual text input exists.
- Receipt items can be edited/reviewed after photo, gallery, or pasted OCR import.
- Receipt reconciliation attaches split quantity lines, including weighted price-per-pound lines, to the previous grocery item.
- Receipt reconciliation accepts item totals and inline quantity lines when OCR drops dollar signs.
- Receipt reconciliation ignores subtotal, tax, total, savings, and tender/payment lines.
- Receipt totals are rounded to cents before budget updates.
- Receipt imports, edits, and deletes adjust budget spending totals, daily envelope, and projected spend.
- Budget analysis loads actual receipts and uses current-month receipt history when calculating projected spend.
- Pantry-matched receipt edits and deletes adjust pantry quantities.
- Camera capture now uses full-resolution app-cache image files for pantry, flyer, and receipt OCR.
- ML Kit OCR fallback exists.
- Optional Gemini pantry photo client exists.
- Settings screen shows whether Gemini Vision is configured or OCR fallback is active.
- Settings screen includes a Test AI Connection button for key/model/network verification on the phone.
- Placeholder Gemini keys are treated as not configured.
- Gemini setup trims accidental key/model whitespace and normalizes a pasted `models/` prefix before calling the API.
- Gemini pantry response parsing has no-network unit coverage for fenced JSON, minor surrounding text, scalar warnings/questions, malformed string/list fields, and confidence clamping.
- Demo data loading resets pantry, deals, receipts, meal plans, default meal settings, and the `$292 / $45 spent` demo budget baseline.
- Menu Generate replaces the active generated week so repeated taps do not duplicate meal-plan rows.

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
- Kitchen pantry test.

Current AI configuration:

- `local.properties` was not present in the clean `Deal_Planner` folder.
- `GEMINI_API_KEY` environment variable was not set in this shell.
- Therefore Gemini Vision is not live-configured yet; the app will use ML Kit OCR fallback.
- Current default model in Gradle is `gemini-3.5-flash`, which matched the current Google AI model page checked on 2026-07-16.
- Rebuild the debug APK after adding or changing `local.properties`; Gemini values are compiled into `BuildConfig`.
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

7. Test in this order:
   - Launch app and tap Load Demo.
   - After receipt/budget tests, tap Load Demo again and confirm Budget returns to the `$292 / $45 spent` demo baseline.
   - Pantry typed entry.
   - Pantry photo.
   - Pantry gallery image.
   - Pantry barcode scan.
   - Pantry camera-permission denial or canceled capture/gallery/scan status.
   - Pantry manual barcode/code entry.
   - Pantry duplicate check: add the same typed/photo item twice and confirm quantity merges.
   - Pantry barcode duplicate check: add the same UPC twice and confirm quantity merges, then add a different UPC and confirm it remains separate.
   - Pantry edit/review dialog for VERIFY items.
   - Deals flyer photo.
   - Deals camera-permission denial or canceled capture/gallery/PDF status.
   - Deals gallery image.
   - Deals PDF.
   - Deals pasted OCR text.
   - Deals pasted OCR text with prices missing dollar signs.
   - Deals bundled demo flyer text via pasted OCR.
   - Deals store field applies to photo, gallery, PDF, and pasted OCR imports.
   - Deals edit/review dialog for low-confidence OCR results.
   - Receipts photo.
   - Receipts camera-permission denial or canceled capture/gallery status.
   - Receipts gallery image.
   - Receipts pasted OCR text.
   - Receipts pasted OCR text with prices missing dollar signs.
   - Receipts split quantity lines do not import as separate items.
   - Receipts subtotal/tax/total/payment lines do not import as items.
   - Receipts edit/review dialog for OCR and match corrections.
   - Receipts edit/delete budget total adjustment.
   - Budget daily envelope changes after receipt import, receipt total edit, and receipt delete.
   - Budget projected spend reflects current-month receipt history.
   - Receipts edit/delete pantry quantity adjustment for pantry matches.
   - Settings AI status before and after adding a real Gemini key.
   - Settings Test AI Connection before pantry AI photo testing.
   - Generate meal plan.
   - Review shopping list.
   - Review budget.

After those pass, decide whether to polish current flows or add optional features such as nutrition lookup, guided multi-photo flyer capture, price history, and monetization/convenience features.
