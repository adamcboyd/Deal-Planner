# Deal Planner Recovery Status - 2026-07-16

## Source of Truth

- Current app folder: `C:\Users\adamc\AndroidStudioProjects\Deal_Planner`
- Android Studio had last opened the older-named folder: `C:\Users\adamc\AndroidStudioProjects\SNAP_Optimizer`
- Clean renamed folder to use going forward: `C:\Users\adamc\AndroidStudioProjects\Deal_Planner`
- GitHub remote: `https://github.com/adamcboyd/Deal-Planner.git`
- Current branch: `codex/deal-planner-baseline`
- Current commit: `b1366ad chore: rename app to Deal Planner`

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

## Current Feature Status

Verified by build/unit tests/code inspection:

- App name/package is now Deal Planner: `com.dealplanner`.
- Room local database and repository layer compile.
- Pantry natural-language parser has unit tests.
- Deals flyer parser has unit tests.
- Meal planning engine has unit tests.
- Budget engine has unit tests.
- Receipt reconciliation engine has unit tests.
- Pantry photo/gallery input exists.
- Flyer photo/gallery/PDF input exists.
- ML Kit OCR fallback exists.
- Optional Gemini pantry photo client exists.
- Demo data loading exists.

Not yet verified on a real phone:

- Camera capture UX.
- Gallery import UX.
- Flyer PDF picker UX.
- ML Kit OCR quality on real pantry/flyer photos.
- Gemini pantry photo API call.
- Android permissions flow.
- Kitchen pantry test.

Current AI configuration:

- `local.properties` was not present in the clean `Deal_Planner` folder.
- `GEMINI_API_KEY` environment variable was not set in this shell.
- Therefore Gemini Vision is not live-configured yet; the app will use ML Kit OCR fallback.
- Current default model in Gradle is `gemini-3.5-flash`, which matched the current Google AI documentation checked on 2026-07-16.

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
6. Install or run the debug app.
7. Test in this order:
   - Launch app and tap Load Demo.
   - Pantry typed entry.
   - Pantry photo.
   - Pantry gallery image.
   - Deals flyer photo.
   - Deals gallery image.
   - Deals PDF.
   - Generate meal plan.
   - Review shopping list.
   - Review budget.

After those pass, decide whether to polish current flows or add optional features such as barcode scanning, nutrition lookup, guided multi-photo flyer capture, and monetization/convenience features.
