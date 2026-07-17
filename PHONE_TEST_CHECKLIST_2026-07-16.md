# Deal Planner Phone Test Checklist - 2026-07-16

## Source of Truth

- Project folder: `C:\Users\adamc\AndroidStudioProjects\Deal_Planner`
- GitHub repo: `https://github.com/adamcboyd/Deal-Planner`
- Branch: `codex/deal-planner-baseline`
- Current validated app-code checkpoint: current `codex/deal-planner-baseline` branch head after ADB setup guidance work; confirm the exact commit with `git log -1 --oneline`.
- Debug APK: `C:\Users\adamc\AndroidStudioProjects\Deal_Planner\app\build\outputs\apk\debug\app-debug.apk`

## Install on Android Phone

Run from `C:\Users\adamc\AndroidStudioProjects\Deal_Planner`:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-20'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\scripts\start-phone-test-run.ps1
.\scripts\phone-debug-preflight.ps1
adb devices
.\scripts\phone-debug-install.ps1
```

Use `start-phone-test-run.ps1` for the normal phone setup path. It runs required-phone preflight, generates/transfers deterministic sample files, installs/launches the app, and creates a report with `Setup status: Completed`. If setup fails before the final report step, it creates a failure-state report with `Setup status: Failed` and the stopping reason unless `-SkipReport` was used. If running helpers individually, expected before install: preflight shows no failures, confirms the branch is clean and synced with GitHub, reports `APK source identity` with a clean generated `BuildConfig`, and `adb devices` shows exactly one authorized phone. If no phone is ready, preflight/install should say whether ADB listed no devices or a phone was unauthorized/offline, then prompt to enable USB debugging, use a data-capable USB mode/cable, accept the USB debugging prompt, and retry after `adb devices` shows `device`. During install, the helper should print the generated APK source identity, generated APK Gemini model/configured state, and that `com.dealplanner` was verified on the device.
Expected APK permission check: preflight reports required network/camera permissions and `APK storage permissions` as OK, confirming gallery/PDF imports use picker-scoped grants instead of broad storage/media permissions.

If the APK is already built:

```powershell
.\scripts\phone-debug-preflight.ps1
.\scripts\phone-debug-install.ps1 -SkipBuild
```

Do not use `-SkipBuild` after changing app code/resources, Gradle config, `local.properties`, or Gemini environment values. The install helper blocks a stale APK when app source/config or `local.properties` is newer than `app-debug.apk`.

If a phone-only issue appears, capture logs from the same project folder:

```powershell
.\scripts\phone-debug-logs.ps1
```

To clear old logs, launch the app, wait while you reproduce the issue, and then save logs:

```powershell
.\scripts\phone-debug-logs.ps1 -Clear -Launch -DurationSeconds 90
```

Start debugging with `phone-test-logs\<timestamp>\logcat-dealplanner-filtered.txt`.

To create a timestamped report before or during the phone run:

```powershell
.\scripts\new-phone-test-report.ps1
```

Use the generated `phone-test-results\<timestamp>\PHONE_TEST_REPORT.md` to mark pass/fail notes, log folders, and follow-ups. The report Source Snapshot includes the repo commit, compiled APK source branch/commit/dirty state, current lint snapshot when available, latest sample folder/manifest, and sample transfer report when available. The Setup Run Summary records whether the starter completed, failed, or was not recorded.

To create deterministic sample files for pasted text, gallery image, and PDF picker checks:

```powershell
.\scripts\new-phone-test-samples.ps1
.\scripts\send-phone-test-samples.ps1
```

Copy or upload the generated `phone-test-samples\<timestamp>\` folder to a location the phone can open, or use `send-phone-test-samples.ps1` after USB debugging is authorized. The transfer helper requires the generated `SAMPLE_MANIFEST.md`, verifies its byte counts and SHA-256 hashes, copies the latest generated folder to `/sdcard/Download/DealPlannerPhoneTestSamples/<timestamp>/`, verifies remote byte sizes, requests Android media scans so picker apps can see the files sooner, and writes `PHONE_SAMPLE_TRANSFER.md` with the Android destination and verified byte sizes. The folder contains demo receipt/flyer TXT, PDF, and PNG files plus pantry-label and UPC-A barcode samples. The pantry-label sample includes hyphenated `16-ounce` and `12-count` rows, punctuated `net wt:`/`best by:` cues, slash dates, and two-digit dash dates for OCR fallback checks.
Each generated sample folder also includes `SAMPLE_MANIFEST.md` with byte counts and SHA-256 hashes. To verify the latest local bundle before phone transfer, run:

```powershell
.\scripts\new-phone-test-samples.ps1 -VerifyOnly
```

## Optional Gemini Setup

The app works without Gemini by using on-device ML Kit OCR fallback. To test Gemini pantry photo recognition, create `local.properties` locally:

```properties
gemini.api.key=YOUR_REAL_GEMINI_KEY
gemini.model=gemini-3.5-flash
```

Then rebuild and reinstall:

```powershell
.\scripts\phone-debug-install.ps1
```

Before the AI-specific phone pass, verify the key, rebuilt APK, and connected phone are all ready:

```powershell
.\scripts\phone-debug-preflight.ps1 -RequirePhone -RequireGemini
```

Expected with a real key after rebuild: preflight reports `Gemini key`, `APK Gemini key`, and `APK Gemini model` as OK without printing the key value. A generated `phone-test-results\<timestamp>\PHONE_TEST_REPORT.md` should also show `APK Gemini configured: True` and `APK Gemini model: gemini-3.5-flash`.

Do not commit `local.properties`.

## Baseline Smoke Test

1. Launch Deal Planner.
2. Tap `Load Demo`.
3. Confirm these tabs open without crashing:
   - `Pantry`
   - `Deals`
   - `Receipts`
   - `Shopping`
   - `Menu`
   - `Budget`
   - `Settings`
4. Confirm demo data appears:
   - Pantry has rice, pasta, oats, beans, oil.
   - Deals has pork, chicken, broccoli, mandarins.
   - Shopping has a generated shopping list with estimated totals.
   - Budget shows the demo budget baseline.
   - Menu has a generated 7-day plan and visible generation status.
5. Close and relaunch Deal Planner.
6. Confirm the `Shopping` tab still has the generated list without tapping `Generate` again.
7. After a generated plan exists, edit or add a pantry/deal/receipt/settings input and confirm `Shopping` rederives from the current inputs without needing an app relaunch.

## Deterministic Text Tests

Use these before camera/photo tests because they remove OCR uncertainty.

### Pantry

1. In `Pantry`, enter: `2 cans black beans 15oz pantry best by 2026-12-31`
2. Tap `Add`.
3. Confirm a visible added/updated status appears and a pantry row appears or merges into an existing black beans row.
4. Tap edit and confirm the item can be reviewed and saved.
5. In the edit dialog, enter quantity `1,5`, save, and confirm it is accepted as 1.5.
6. Reopen edit, enter quantity `.5` or `,5`, save, and confirm it is accepted as 0.5.
7. Reopen edit, enter quantity `abc`, and confirm Save disables with a non-negative number message.
8. Reopen edit, enter best-by `12/31/2026` or `12-31-26`, save, and confirm it is accepted as December 31, 2026.
9. Also enter `Great Value peanut butter opened yesterday best by 2026-12-31`.
10. Confirm opened date and best-by date stay separate on the pantry item.
11. Optional year-first pantry date check: enter `Kroger yogurt opened 2026-7-1 best by 2026-12-31`, then confirm the opened date is July 1, 2026.
12. Optional label-date check: enter `Kroger yogurt best before 2026-12-31`, `milk use by 12/31/2026`, `milk use by 12-31-26`, or `pasta exp 12/31/2026`, then confirm the best-by date is captured.
13. Optional missing-brand duplicate check: add `black beans 15oz pantry`, then add or OCR/import `Great Value black beans 15 oz pantry`; confirm the existing row updates instead of creating an unbranded duplicate. Add `Kroger black beans 15 oz pantry` and confirm it remains separate from the Great Value row.
14. Optional hyphenated label-size check: enter `Great Value peanut butter 16-ounce pantry` and `Kroger eggs 12-count fridge`; confirm item names do not include `16-ounce` or `12-count`, sizes are captured as `16oz` and `12ct`, and storage is preserved.
15. Optional label-punctuation check: enter `Great Value black beans net wt: 15 oz pantry best by: 12/31/2026`, `milk opened: 2026-07-01 use by: 12/31/2026`, or `pasta exp: 12/31/2026`; confirm cue words such as `wt`, `by`, `opened`, and `exp` do not appear in the item name.
16. Clear the pantry text field and tap `Add`; confirm a visible no-text status appears.

### Deals

1. Open `app\src\main\assets\demo_flyer.txt`, or use `deal-planner-demo-flyer.txt` from a generated `phone-test-samples\<timestamp>\` folder.
2. Copy the full text into `Deals` -> `Paste flyer OCR text`.
3. Set store to `Kroger`.
4. Tap `Process Text`.
5. Confirm multiple deals are added with store `Kroger`, prices, deal scores, and coupon/limit flags where applicable.
6. Confirm the pasted flyer field clears after successful import.
7. Edit one deal and save it.
8. In the edit dialog, enter price `2,99`, save, and confirm it is accepted as 2.99.
9. Reopen edit, enter price `.99` or `,99`, save, and confirm it is accepted as 0.99.
10. Reopen edit, enter score `7`, and confirm Save disables with a 0-to-1 value message.
11. Reopen edit, enter valid-until `12/31/2026` or `12-31-26`, save, and confirm it is accepted as December 31, 2026.
12. Optional store whitespace check: set store to ` Kroger `, import one deal, and confirm the deal shows store `Kroger`.
13. Optional cent-price check: paste `Roma Tomatoes` on one line and `99c/lb` on the next, then confirm it imports as a $0.99/lb deal.
14. Optional each-price check: paste `Kroger Milk` / `$3.99 each`, `Store Yogurt` / `$1.25 per ea`, and `Black Beans` / `88c each`; confirm imported item names do not include `each` or `ea`.
15. Optional whole-dollar unit price check: paste `Chicken Drumsticks` on one line and `$3/lb` on the next, then confirm it imports as a $3.00/lb deal.
16. Optional whole-dollar package price check: paste `Milk` on one line and `$3` on the next, then confirm it imports as a $3.00 deal.
17. Optional package-size OCR check: paste `Kroger Yogurt 5,3 oz $1,49`; confirm it imports as a $1.49 deal with package size `5.3 oz`.
18. Optional savings-callout check: paste `Milk`, `$3`, and `Save $1 when you buy 2`; confirm only the $3 Milk deal imports and no `Save` deal appears.
19. Optional household/non-food meal-side check: paste `Tide Laundry Detergent $9.99` as a deal, generate or refresh the meal plan, and confirm detergent does not appear in `Menu` meals or `Shopping` totals.
20. Paste text with no deal prices, tap `Process Text`, and confirm the text remains available for correction.
21. Clear the flyer text field and tap `Process Text`; confirm `No flyer text found.` appears.

### Receipts

1. Open `app\src\main\assets\demo_receipt.txt`, or use `deal-planner-demo-receipt.txt` from a generated `phone-test-samples\<timestamp>\` folder.
2. Copy the full text into `Receipts` -> `Paste receipt OCR text`.
3. Set store to `Kroger`.
4. Tap `Process Text`.
5. Confirm receipt items are added.
6. Confirm the pasted receipt field clears after successful import.
7. Confirm Budget spending/projection changes after receipt import.
8. Edit one receipt line and confirm Budget updates.
9. In the edit dialog, enter total `1,78`, save, and confirm it is accepted as 1.78.
10. Reopen edit, enter total `.89` or `,89`, save, and confirm it is accepted as 0.89.
11. Reopen edit, enter confidence `abc`, and confirm Save disables with a 0-to-1 value message.
12. Reopen edit, enter date `10/27/2025` or `10-27-25`, save, and confirm it is accepted as October 27, 2025.
13. Optional store whitespace check: set store to ` Kroger `, import one receipt, and confirm the receipt line shows store `Kroger`.
14. Optional whole-dollar receipt check: paste `RICE 5 LB $3`, `2 @ $1 BLACK BEANS $2`, and `BANANAS 1 lb @ $1/lb $1`; confirm the rows import with totals 3.00, 2.00, and 1.00.
15. Optional item-first quantity check: paste `BLACK BEANS 2 @ 0.89 1.78` and `KROGER PASTA 3 @ 1,00 3,00`; confirm quantities import as 2 and 3 instead of blank.
16. Optional `x` separator quantity check: paste `BLACK BEANS 1.78`, `2 x 0.89`, `3 X 1.00 KROGER PASTA 3.00`, and `APPLES 1,25 lb x 1,99/lb 2,49`; confirm quantities import as 2, 3, and 1.25.
17. Optional year-first date check: paste `Transaction Date: 2025/10/27` above `BLACK BEANS $1.78`; confirm the imported receipt row uses October 27, 2025.
18. Optional repeated pantry-match check: with Black Beans already in Pantry, paste two Black Beans receipt rows with quantities `2 @ $0.89` and `1 @ $0.89`; confirm Pantry quantity increases by 3 total, not only the last row.
19. Delete one receipt line and confirm Budget updates again.
20. Optional saved-total check: append `YOU SAVED $4.25`, `SAVED TODAY 4.25`, and `TOTAL SAVED $4.25`; confirm those savings lines do not appear as receipt items or affect Budget spending.
21. Optional tender-line check: append `VISA DEBIT $40.65` and `CARD TENDER $40.65`, process again, and confirm those payment lines do not appear as receipt items.
22. Optional negative return/refund check: append `MILK -$1.99`, `EGGS $-2.49`, `BREAD (3.29)`, and `APPLES -1,25`; confirm those negative amount rows do not appear as receipt items or increase Budget spending.
23. Paste text with no receipt line items, tap `Process Text`, and confirm the text remains available for correction.
24. Clear the receipt text field and tap `Process Text`; confirm `No receipt text found.` appears.

### Budget

1. In `Budget`, change monthly food budget to `292,50`, tap `Save Budget`, and confirm `Budget saved.` appears.
2. Change breakfast anchor cost to `.55` or `,55`, tap `Save Budget`, and confirm it is accepted as 0.55.
3. Enter invalid budget text such as `abc`, and confirm Save is disabled with a visible non-negative-number message.

## Phone Input Tests

### Pantry Inputs

1. Manual barcode/code:
   - Enter a UPC-like code in `Barcode / UPC`, such as `012345678905` from `deal-planner-demo-upc-a.txt`.
   - Also test a pasted label form such as `UPC: 0 12345-67890 5`.
   - Tap `Add Code`.
   - Expected with network/product match: item appears with product name, brand when available, package quantity when available, VERIFY status, barcode in notes, and `Product lookup: Open Food Facts`.
   - Expected without network/product match: item appears as `Scanned barcode item` with VERIFY status and barcode in notes.
   - Expected with bare product code near date text such as `012345678905 Best By 20261231`: the product code is used and the date text is ignored for barcode matching.
   - Expected with date/item-only label text such as `Best By 20261231` or `Item #12345678`: visible `No barcode found.` status, no junk barcode item, and the entered text remains available for correction.
   - Expected with text that has no 8-14 digit product code: visible `No barcode found.` status, no junk barcode item, and the entered text remains available for correction.
   - Expected with a blank code field: visible `No barcode found.` status and no junk barcode item.
2. Barcode scanner:
   - Tap `Scan`.
   - Allow camera permission.
   - Scan a pantry barcode, or display/print `deal-planner-demo-upc-a.png` on another screen and scan it.
   - Expected: same lookup/fallback behavior as manual barcode entry.
3. Barcode duplicate check:
   - Add or scan the same barcode twice.
   - Expected: the same pantry row quantity increments instead of creating duplicate rows.
   - Add or scan a different barcode.
   - Expected: different barcode stays as a separate reviewable row.
4. Pantry photo:
   - Tap `Photo`.
   - Take a clear label/photo, ideally one with a best-by date such as `12/31/2026`.
   - Expected without Gemini: ML Kit OCR fallback creates a VERIFY item or gives a visible recovery message.
   - Expected with Gemini: AI item recognition creates one or more VERIFY items when details are uncertain and preserves common label dates when visible.
5. Pantry gallery:
   - Tap `Gallery`.
   - Pick a pantry image, such as `deal-planner-demo-pantry-label.png` from a generated `phone-test-samples\<timestamp>\` folder.
   - Expected: same as pantry photo, including common label-date handling and separate rows for the generated `16-ounce` peanut butter, `12-count` eggs, punctuated label-cue, slash-date, and two-digit dash-date labels when OCR can read them. If OCR wraps the date onto the next line, the date should stay attached to that item.
6. Optional multi-item OCR fallback:
   - Use a pantry photo/gallery image where at least two visible lines each look like complete items, such as `Great Value Black Beans 15 oz pantry` and `Kroger Pasta 16 oz pantry`, or hyphenated label rows such as `Great Value Peanut Butter 16-ounce` and `Kroger Eggs 12-count`.
   - Expected without Gemini or after AI fallback: the clear item lines import as separate VERIFY pantry rows instead of one combined row.
7. Optional liquid-size check:
   - Enter or OCR `Kroger milk 1 gal fridge`, `chicken broth 1 quart pantry`, or `cream 1 pint fridge`.
   - Expected: liquid size/unit is preserved as `gal`, `qt`, or `pt`.

### Flyer Inputs

1. Flyer photo:
   - Set store name.
   - Tap `Take Flyer Photo`.
   - Take a flat, well-lit flyer photo.
   - Expected: deals are added or a clear visible recovery message appears.
2. Flyer gallery image:
   - Tap `Choose Flyer Image`.
   - Pick a flyer screenshot/photo, such as `deal-planner-demo-flyer.png` from a generated `phone-test-samples\<timestamp>\` folder.
   - Expected: deals are added with the entered store name.
3. Flyer PDF:
   - Tap `Choose Flyer PDF`.
   - Pick a flyer PDF, such as `deal-planner-demo-flyer.pdf` from a generated `phone-test-samples\<timestamp>\` folder.
   - Expected: app reads up to 12 pages and adds parsed deals, or shows a clear recovery message.

### Receipt Inputs

1. Receipt photo:
   - Set store name.
   - Tap `Photo`.
   - Take a clear receipt photo.
   - Expected: receipt line items are added and Budget updates.
2. Receipt gallery:
   - Tap `Gallery`.
   - Pick a receipt image, such as `deal-planner-demo-receipt.png` from a generated `phone-test-samples\<timestamp>\` folder.
   - Expected: receipt line items are added and Budget updates.
3. Receipt PDF:
   - Tap `PDF`.
   - Pick a receipt PDF, such as `deal-planner-demo-receipt.pdf` from a generated `phone-test-samples\<timestamp>\` folder.
   - Expected: app reads up to 12 pages and adds receipt line items, or shows a clear recovery message. Budget updates when line items import.

## AI Verification

In `Settings`:

1. Without `local.properties`, confirm text says Gemini is not configured and OCR fallback is active.
2. Tap `Test AI Connection`.
3. Expected without key: status reports that Gemini API key is not configured.
4. With a real key and rebuilt APK, tap `Test AI Connection`.
5. Expected with key/network: status reports `Gemini connection OK using gemini-3.5-flash.`
6. In the preflight/report output, confirm `APK Gemini configured` is true and `APK Gemini model` is `gemini-3.5-flash` before testing photos.
7. If the key/model/network is wrong, expected: status shows a concise `Gemini connection failed` message with the HTTP code/status instead of raw JSON.
8. With key configured, test pantry photo recognition against a real pantry item label.
9. Use at least one item with a visible sell-by, use-by, best-by, or expiration label date, then confirm that date is imported or preserved for review.
10. If Gemini imports an item with unclear amount/unit details, confirm the pantry item shows VERIFY and notes include `Review amount/unit.`.
11. If Gemini imports an item with a zero or negative amount, confirm it falls back to quantity 1.0, shows VERIFY, and notes include `Review amount/unit.`.
12. If Gemini imports an item with Generic or unknown brand details, including punctuated text such as `Generic:` or `Unknown.`, confirm the pantry item shows VERIFY and notes include `Review brand.`.
13. If Gemini imports an item with unclear pantry/fridge/freezer location, confirm the pantry item shows VERIFY and notes include `Review pantry/fridge/freezer location.`.
14. If Gemini imports an item without a clear expiration or best-by date, confirm the pantry item shows VERIFY and notes include `Review expiration or best-by date.`.
15. Open at least one AI-imported pantry item in the edit dialog and confirm saved unit/location text is clean and canonical, such as plural cans saved as `can`, fluid ounces saved as `oz`, and refrigerator/cold-storage wording saved as `fridge`.

## Settings Save Check

In `Settings`:

1. Enter protein per meal as `0,5`, tap `Save Settings`, and confirm `Settings saved.` appears.
2. Enter protein per meal as `.5` or `,5`, tap `Save Settings`, and confirm it is accepted as 0.5.
3. Enter negative protein text such as `-1`, and confirm Save is disabled with a visible non-negative number message.
4. Enter invalid protein text such as `abc`, `NaN`, or `Infinity`, and confirm Save is disabled with a visible non-negative number message.

## Build Identity Check

In `Settings` -> `About Deal Planner`, confirm:

- Version shows the Gradle build version, currently `1.0 (1)`.
- Package shows `com.dealplanner`.
- Build shows `Debug` for the command-line debug APK.
- Source shows the app-code branch and commit matching the preflight/report APK source identity, without a `(dirty)` marker for a clean debug APK.

## Cancel and Permission Tests

Verify these show visible status messages instead of silent failures or crashes:

- Deny camera permission on pantry photo.
- Cancel pantry photo capture.
- Cancel pantry gallery picker.
- Cancel barcode scanner.
- Deny camera permission on flyer photo.
- Cancel flyer image picker.
- Cancel flyer PDF picker.
- Deny camera permission on receipt photo.
- Cancel receipt gallery picker.
- If a phone cannot open the camera app, gallery picker, PDF picker, or barcode scanner, Deal Planner should stay open and show a visible recovery message with another input option.
- If Android cannot open the system camera-permission prompt, Deal Planner should stay open and show a visible recovery message pointing to Android Settings or another input option.

## Pass Criteria

- App installs and launches on the phone.
- No crash during tab navigation.
- Demo data loads repeatably.
- Manual pantry, deals, and receipt text paths work and show visible status for blank input.
- Failed pasted flyer/receipt parses keep the pasted text visible for correction.
- Camera/gallery/PDF/barcode paths either import data or show visible recovery status, including permission-request failures and launch failures when Android cannot open the external camera, picker, or scanner flow.
- Gallery/PDF picker paths work without the APK requesting broad storage/media-library permissions.
- Barcode lookup enriches pantry rows when Open Food Facts has the product, and gracefully falls back when it does not.
- Budget current balance, daily envelope, projected spend, and monthly overview update after receipt import/edit/delete.
- Receipt return/refund rows with negative totals do not import as positive spending.
- Budget Settings saves valid comma-decimal and leading-decimal values and blocks invalid numeric text.
- Pantry, deal, receipt, budget, and settings numeric edit fields accept comma-decimal and leading-decimal corrections and reject non-finite values such as `NaN` or `Infinity`.
- Pantry, deal, and receipt review dialogs accept common date corrections such as `12/31/2026` and `12-31-26`.
- Pantry typed/OCR intake preserves gallon, quart, pint, and hyphenated package sizes such as `16-ounce` and `12-count`.
- Pantry OCR fallback does not import `NET WT` package-size lines as separate products and still splits clear multi-item pantry rows, including `16-ounce` and `12-count` label rows, into separate VERIFY rows with wrapped date/opened lines attached to the matching item.
- Menu generation is deterministic for the same pantry/deals/settings inputs, shows generation status/warnings, ignores household/non-food flyer deals as meal sides, and replaces the active generated week instead of stacking duplicate meal-plan rows.
- Shopping list generation works from current pantry/deals/settings, keeps different deals separate, estimates totals from planned quantities and normalized price-per-unit values, and repopulates after app relaunch.
- Settings blocks invalid or negative protein-per-meal values so generated Shopping quantities and costs stay non-negative.
- Gemini no-key fallback is clear.
- Gemini live test passes only after a real key is configured and APK is rebuilt.
- AI pantry VERIFY rows explain what needs review in the item notes, including model-provided clarifying questions or review-note warnings when present.
- AI pantry rows save trimmed/canonical unit and storage values so review/edit fields do not contain raw model wording.
