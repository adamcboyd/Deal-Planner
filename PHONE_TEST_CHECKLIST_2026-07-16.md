# Deal Planner Phone Test Checklist - 2026-07-16

## Source of Truth

- Project folder: `C:\Users\adamc\AndroidStudioProjects\Deal_Planner`
- GitHub repo: `https://github.com/adamcboyd/Deal-Planner`
- Branch: `codex/deal-planner-baseline`
- Debug APK: `C:\Users\adamc\AndroidStudioProjects\Deal_Planner\app\build\outputs\apk\debug\app-debug.apk`

## Install on Android Phone

Run from `C:\Users\adamc\AndroidStudioProjects\Deal_Planner`:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-20'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\scripts\phone-debug-preflight.ps1
adb devices
.\scripts\phone-debug-install.ps1
```

Expected before install: preflight shows no failures, and `adb devices` shows exactly one authorized phone.

If the APK is already built:

```powershell
.\scripts\phone-debug-preflight.ps1
.\scripts\phone-debug-install.ps1 -SkipBuild
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
   - Budget shows the demo budget baseline.
   - Menu has a generated 7-day plan.

## Deterministic Text Tests

Use these before camera/photo tests because they remove OCR uncertainty.

### Pantry

1. In `Pantry`, enter: `2 cans black beans 15oz pantry best by 2026-12-31`
2. Tap `Add`.
3. Confirm a pantry row appears or merges into an existing black beans row.
4. Tap edit and confirm the item can be reviewed and saved.
5. Also enter `Great Value peanut butter opened yesterday best by 2026-12-31`.
6. Confirm opened date and best-by date stay separate on the pantry item.
7. Optional label-date check: enter `Kroger yogurt best before 2026-12-31`, `milk use by 12/31/2026`, or `pasta exp 12/31/2026`, then confirm the best-by date is captured.

### Deals

1. Open `app\src\main\assets\demo_flyer.txt`.
2. Copy the full text into `Deals` -> `Paste flyer OCR text`.
3. Set store to `Kroger`.
4. Tap `Process Text`.
5. Confirm multiple deals are added with store `Kroger`, prices, deal scores, and coupon/limit flags where applicable.
6. Edit one deal and save it.
7. Optional cent-price check: paste `Roma Tomatoes` on one line and `99c/lb` on the next, then confirm it imports as a $0.99/lb deal.

### Receipts

1. Open `app\src\main\assets\demo_receipt.txt`.
2. Copy the full text into `Receipts` -> `Paste receipt OCR text`.
3. Set store to `Kroger`.
4. Tap `Process Text`.
5. Confirm receipt items are added.
6. Confirm Budget spending/projection changes after receipt import.
7. Edit one receipt line and confirm Budget updates.
8. Delete one receipt line and confirm Budget updates again.
9. Optional tender-line check: append `VISA DEBIT $40.65` and `CARD TENDER $40.65`, process again, and confirm those payment lines do not appear as receipt items.

## Phone Input Tests

### Pantry Inputs

1. Manual barcode/code:
   - Enter a UPC-like code in `Barcode / UPC`.
   - Also test a pasted label form such as `UPC: 0 12345-67890 5`.
   - Tap `Add Code`.
   - Expected with network/product match: item appears with product name, brand when available, package quantity when available, VERIFY status, barcode in notes, and `Product lookup: Open Food Facts`.
   - Expected without network/product match: item appears as `Scanned barcode item` with VERIFY status and barcode in notes.
   - Expected with text that has no 8-14 digit product code: visible `No barcode found.` status and no junk barcode item.
2. Barcode scanner:
   - Tap `Scan`.
   - Allow camera permission.
   - Scan a pantry barcode.
   - Expected: same lookup/fallback behavior as manual barcode entry.
3. Barcode duplicate check:
   - Add or scan the same barcode twice.
   - Expected: the same pantry row quantity increments instead of creating duplicate rows.
   - Add or scan a different barcode.
   - Expected: different barcode stays as a separate reviewable row.
4. Pantry photo:
   - Tap `Photo`.
   - Take a clear label/photo.
   - Expected without Gemini: ML Kit OCR fallback creates a VERIFY item or gives a visible recovery message.
   - Expected with Gemini: AI item recognition creates one or more VERIFY items when details are uncertain.
5. Pantry gallery:
   - Tap `Gallery`.
   - Pick a pantry image.
   - Expected: same as pantry photo.

### Flyer Inputs

1. Flyer photo:
   - Set store name.
   - Tap `Take Flyer Photo`.
   - Take a flat, well-lit flyer photo.
   - Expected: deals are added or a clear visible recovery message appears.
2. Flyer gallery image:
   - Tap `Choose Flyer Image`.
   - Pick a flyer screenshot/photo.
   - Expected: deals are added with the entered store name.
3. Flyer PDF:
   - Tap `Choose Flyer PDF`.
   - Pick a flyer PDF.
   - Expected: app reads up to 12 pages and adds parsed deals, or shows a clear recovery message.

### Receipt Inputs

1. Receipt photo:
   - Set store name.
   - Tap `Photo`.
   - Take a clear receipt photo.
   - Expected: receipt line items are added and Budget updates.
2. Receipt gallery:
   - Tap `Gallery`.
   - Pick a receipt image.
   - Expected: receipt line items are added and Budget updates.

## AI Verification

In `Settings`:

1. Without `local.properties`, confirm text says Gemini is not configured and OCR fallback is active.
2. Tap `Test AI Connection`.
3. Expected without key: status reports that Gemini API key is not configured.
4. With a real key and rebuilt APK, tap `Test AI Connection`.
5. Expected with key/network: status reports `Gemini connection OK using gemini-3.5-flash.`
6. With key configured, test pantry photo recognition against a real pantry item label.

## Cancel and Permission Tests

Verify these show visible status messages instead of silent failures:

- Deny camera permission on pantry photo.
- Cancel pantry photo capture.
- Cancel pantry gallery picker.
- Cancel barcode scanner.
- Deny camera permission on flyer photo.
- Cancel flyer image picker.
- Cancel flyer PDF picker.
- Deny camera permission on receipt photo.
- Cancel receipt gallery picker.

## Pass Criteria

- App installs and launches on the phone.
- No crash during tab navigation.
- Demo data loads repeatably.
- Manual pantry, deals, and receipt text paths work.
- Camera/gallery/PDF/barcode paths either import data or show visible recovery status.
- Barcode lookup enriches pantry rows when Open Food Facts has the product, and gracefully falls back when it does not.
- Budget updates after receipt import/edit/delete.
- Menu generation is deterministic for the same pantry/deals/settings inputs and replaces the active generated week instead of stacking duplicate meal-plan rows.
- Shopping list generation works from current pantry/deals/settings and keeps different deals separate.
- Gemini no-key fallback is clear.
- Gemini live test passes only after a real key is configured and APK is rebuilt.
