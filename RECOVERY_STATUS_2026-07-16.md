# Deal Planner Recovery Status - 2026-07-16

## Source of Truth

- Current app folder: `C:\Users\adamc\AndroidStudioProjects\Deal_Planner`
- Android Studio had last opened the older-named folder: `C:\Users\adamc\AndroidStudioProjects\SNAP_Optimizer`
- Clean renamed folder to use going forward: `C:\Users\adamc\AndroidStudioProjects\Deal_Planner`
- GitHub remote: `https://github.com/adamcboyd/Deal-Planner.git`
- Current branch: `codex/deal-planner-baseline`
- Latest validated checkpoint: current `codex/deal-planner-baseline` branch head after Gemini image-check, AI pantry alias-hardening, and Settings text/image connection-test work; confirm the exact commit with `git log -1 --oneline`.
- Previous pushed app-code checkpoint: `dab9add fix: refresh generated menu with shopping`
- Previous pushed helper checkpoint: `9664a3c chore: add live gemini setup check`
- GitHub `main` was also present at `6fa9a95`, but the validated recovery work is on `codex/deal-planner-baseline`.
- The branch includes helper/docs recovery commits plus app-code checkpoints; the latest full local gate used `testDebugUnitTest assembleDebug lintDebug`.
- Recent full readiness report before the starter build/preflight ordering update: `C:\Users\adamc\AndroidStudioProjects\Deal_Planner\phone-test-results\20260716-185600\FEATURE_READINESS_REPORT.md`
- Recent phone test report template before the starter build/preflight ordering update: `C:\Users\adamc\AndroidStudioProjects\Deal_Planner\phone-test-results\20260716-185552\PHONE_TEST_REPORT.md`
- After any clean rebuild, read the installable APK source identity from `.\scripts\phone-debug-preflight.ps1`, `.\scripts\new-phone-test-report.ps1`, or Settings -> About in the app. Those values come from generated debug `BuildConfig`.
- `BuildConfig` generation tracks Git branch/SHA/dirty state as Gradle task inputs. If the branch head changes, rebuild before phone testing so Settings -> About and generated reports point at the current source.

## Other Local Copies Found

- `C:\Users\adamc\AndroidStudioProjects\SNAP_Optimizer`: old Android Studio folder on the same Deal-Planner GitHub remote and branch, but behind at `b1366ad`; do not use for new work.
- `C:\Users\adamc\AndroidStudioProjects\SNAP_Optimizer_BROKEN`: old broken Android Studio folder from 2025; not current.
- `C:\Users\adamc\AndroidStudioProjects\SNAP_Optimizer.new`: old Android Studio folder from 2025; not current.
- `F:\PROJECTS\DealPlanner`: older web/worker/resources material (`deal-planner-v6.29-BACKUP.html`, `worker.js`, `RECIPTS`), not current Android app.
- `F:\PROJECTS\DEALPLANNER OLD NOTES TO PARSE`: old planning notes (`Dynamic Meal Planning System Functi.txt`, `SUBSCRIPTION TIER IDEAS.txt`), not current app source.
- `F:\PROJECTS\DealPlannerAndroid`: old Android wrapper on `https://github.com/adamcboyd/snap_optimizer`, branch `main`, head `9d9bdac`, with a modified nested `SNAP_Optimizer` entry; not current.
- `F:\PROJECTS\DealPlannerAndroid_DEV`: older Android project copy without Git metadata; not current.
- `F:\PROJECTS\DealPlannerAndroid_ORIGINAL_BACKUP`: backup Android project copy without Git metadata; not current.
- `F:\PROJECTS\SNAP SHOPPER`: old notes/resources/working backup folder, not current app source.
- `F:\PROJECTS\SNAP_Optimizer_DEV`: older clean SNAP Optimizer repo on `https://github.com/adamcboyd/SNAP_Optimizer.git`, branch `claude/snap-optimizer-mvp-011CUX1rDE9c6CixrJLHL6Kt`, head `42c76be`; not current Deal Planner.

Refreshed source audit on 2026-07-16 confirms the only folder to open in Android Studio for current work is:

```text
C:\Users\adamc\AndroidStudioProjects\Deal_Planner
```

Do not continue current app work in:

- `C:\Users\adamc\AndroidStudioProjects\SNAP_Optimizer` - stale same-remote checkout at `b1366ad`, behind the current `codex/deal-planner-baseline` branch head.
- `C:\Users\adamc\AndroidStudioProjects\SNAP_Optimizer_BROKEN` or `C:\Users\adamc\AndroidStudioProjects\SNAP_Optimizer.new` - 2025 SNAP Optimizer folders on the old `com.snapoptimizer` package.
- `F:\PROJECTS\...` Deal Planner/SNAP folders - legacy/reference material unless an explicit archival comparison task is opened.

## Verification Run This Session

Latest pushed recovery snapshot after generated Menu and Shopping refresh alignment:

```powershell
git status --short --branch
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\phone-debug-preflight.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\new-feature-readiness-report.ps1 -RunGate
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\new-phone-test-report.ps1
```

Result: branch `codex/deal-planner-baseline` is clean and synced with `origin/codex/deal-planner-baseline`; preflight reports `0 failure(s), 2 warning(s)` for the expected local conditions of no connected/authorized Android phone and no Gemini key; readiness report `20260716-184702` shows `259` unit tests, `0` failures/errors/skipped, lint at `0 errors, 19 warnings`, current debug APK freshness, APK source commit `dab9add`, and `APK source dirty: false`; phone report template `20260716-184711` records commit `dab9add`, APK source commit `dab9add`, synced Git status, and the Menu/Shopping plus strict Gemini phone-check rows.

Latest APK source identity guard checkpoint:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
.\scripts\phone-debug-preflight.ps1
```

Expected result after committing and rebuilding: `BUILD SUCCESSFUL`; preflight reports `0 failure(s)` with only the expected no-phone/no-Gemini warnings, and `APK source identity` matches the current `git log -1 --oneline` commit.

Latest one-command phone starter ordering checkpoint:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\start-phone-test-run.ps1 -Help
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\start-phone-test-run.ps1 -SkipNetwork -SkipSamples -NoLaunch
```

Expected local no-phone result: the starter builds the current debug APK first unless `-SkipBuild` is used, then required-phone preflight stops at the expected no connected/authorized phone condition and writes a failure-state phone-test report. With a connected authorized phone, this means strict Gemini runs inspect the rebuilt APK instead of an older BuildConfig.

Latest Gemini image live-check checkpoint:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-gemini-connection.ps1 -Help
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-gemini-connection.ps1 -TestPantryImage
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\phone-debug-preflight.ps1 -TestGeminiImage -SkipNetwork
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\phone-debug-preflight.ps1 -TestGeminiImage
```

Expected local no-key result: help documents the image check; the no-key image script fails before network access without printing a key; `-SkipNetwork` marks `Gemini live image API` skipped; and without `-SkipNetwork` preflight fails until a real Gemini key is configured. With a real key, final AI setup should use `.\scripts\start-phone-test-run.ps1 -RequireGemini -TestGeminiLive -TestGeminiImage`.

Latest AI pantry alias-hardening checkpoint:

```powershell
.\gradlew.bat testDebugUnitTest --tests com.dealplanner.ai.GeminiPantryVisionClientTest
```

Result: `BUILD SUCCESSFUL`. Gemini pantry parsing now accepts nested date string objects, additional label-date aliases such as `use_by_text` and `expires_on`, opened/purchase-style date aliases such as `opened_at` and `purchased_on`, and explicit liquid units such as gallons or pints when the model also emits a generic count quantity.

Latest Settings text/image Gemini connection checkpoint:

```powershell
.\gradlew.bat testDebugUnitTest --tests com.dealplanner.ai.GeminiPantryVisionClientTest
```

Result: `BUILD SUCCESSFUL`. Settings -> Test AI Connection now makes both a text request and an embedded one-pixel PNG inline image request, so a successful phone status verifies the configured model/key/network path used by pantry photo recognition as well as text generation.

Latest Android 16/API 36 target checkpoint:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`. The app now compiles and targets SDK 36, matching the locally installed Android 16 platform and current Google Play API-level direction for new apps/updates. The build toolchain was aligned to Android Gradle Plugin `8.13.2` and Gradle wrapper `8.14.5`, so the previous unsupported compile SDK warning is gone. The obsolete `android.enableDexingArtifactTransform=false` property was removed because AGP removed it after 8.3, and debug `BuildConfig` Git metadata now uses Gradle's provider-based exec API instead of deprecated project `exec`. `--warning-mode all` still reports plugin/tooling deprecation warnings for Gradle 9/10 compatibility, so a later Kotlin/Compose/plugin modernization pass remains useful before store-readiness polish.

Latest ML Kit OCR fallback dependency checkpoint:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`. ML Kit Text Recognition was updated from `16.0.0` to `16.0.1`, keeping the no-key on-device OCR fallback current enough to remove the three `Aligned16KB` native-library lint warnings. The current lint snapshot is `0` errors and `29` warnings: dependency update advisories, one obsolete launcher resource-folder warning, and two KTX suggestions.

Latest image helper KTX cleanup checkpoint:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`. `AppViewModel` now uses AndroidX KTX `Bitmap.scale` and `createBitmap` helpers in the image/PDF preprocessing paths, clearing the two `UseKtx` lint suggestions without changing the rendered bitmap dimensions or OCR input flow. The current lint snapshot is `0` errors and `27` warnings: dependency update advisories plus the launcher resource-folder warning.

Latest barcode dependency checkpoint:

```powershell
.\gradlew.bat testDebugUnitTest --tests com.dealplanner.lookup.OpenFoodFactsBarcodeClientTest --tests com.dealplanner.lookup.BarcodePantryMapperTest --tests com.dealplanner.parser.PantryPhraseParserTest
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`. ZXing core was updated from `3.5.2` to `3.5.4` for barcode/manual UPC intake while leaving the JourneyApps scanner wrapper unchanged. Barcode-focused tests passed before the full gate, covering UPC/EAN normalization, Open Food Facts response handling, pantry fallback mapping, and barcode-aware duplicate behavior. The current lint snapshot is `0` errors and `26` warnings.

Latest JSON parser dependency checkpoint:

```powershell
.\gradlew.bat testDebugUnitTest --tests com.dealplanner.ai.GeminiPantryVisionClientTest --tests com.dealplanner.lookup.OpenFoodFactsBarcodeClientTest
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`. Gson was updated from `2.10.1` to `2.14.0` for Gemini pantry parsing, Gemini error summaries, Open Food Facts product parsing, and Room list converters. Focused parser tests passed before the full gate, covering Gemini pantry response parsing, Gemini API status parsing, and Open Food Facts product parsing. The Gson 2.14.0 annotation cleanup also removed a new `CheckResult` lint warning in the Gemini pantry parser, leaving the current lint snapshot at `0` errors and `25` warnings.

Latest camera input dependency checkpoint:

```powershell
.\gradlew.bat testDebugUnitTest --tests com.dealplanner.ui.state.PantryImportReviewQueueTest --tests com.dealplanner.ocr.PantryOcrCandidateExtractorTest --tests com.dealplanner.parser.DealsParserTest --tests com.dealplanner.domain.ReceiptReconcilerTest
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`. CameraX was updated from `1.3.0` to `1.4.2` for the phone photo-capture paths used by pantry, flyer, and receipt imports. CameraX `1.6.1` was tested first and rejected for this checkpoint because its artifacts use Kotlin metadata `2.1.0` while the app is still on Kotlin `1.9.20` / Compose compiler `1.5.4`. The current lint snapshot remains `0` errors and `25` warnings.

Latest unused image-loading dependency cleanup checkpoint:

```powershell
rg "coil|AsyncImage|rememberAsyncImagePainter|SubcomposeAsyncImage" app\src\main\java app\src\test\java -n
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`. The unused Coil Compose dependency was removed after the source scan found no active Coil/AsyncImage references in app or test code. This reduces the debug APK dependency surface without changing the current camera/gallery/OCR import flow. The current lint snapshot is `0` errors and `24` warnings.

Latest unit-test assertion dependency checkpoint:

```powershell
.\gradlew.bat testDebugUnitTest --tests com.dealplanner.ai.GeminiPantryVisionClientTest --tests com.dealplanner.ai.PantryVisionItemMapperTest --tests com.dealplanner.parser.PantryPhraseParserTest --tests com.dealplanner.parser.DealsParserTest --tests com.dealplanner.domain.ReceiptReconcilerTest
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`. Truth was updated from `1.1.5` to `1.4.5` for the JVM unit-test assertion suite that covers Gemini parsing, pantry AI mapping, pantry/deal parsing, and receipt reconciliation. This keeps the local verification harness current without changing app runtime behavior. The current lint snapshot is `0` errors and `23` warnings.

Latest coroutine dependency checkpoint:

```powershell
.\gradlew.bat testDebugUnitTest --tests com.dealplanner.ai.GeminiPantryVisionClientTest --tests com.dealplanner.lookup.OpenFoodFactsBarcodeClientTest --tests com.dealplanner.ui.state.PantryImportReviewQueueTest --tests com.dealplanner.domain.ShoppingListExportFormatterTest
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`. Kotlinx Coroutines was updated from `1.7.3` to `1.8.1` across Android, core, Play Services task awaiting, and unit-test artifacts. This keeps the async paths used by Gemini calls, Open Food Facts lookup, ML Kit OCR `tasks.await()`, view-model imports, and `runTest` coverage on one verified version while staying inside the current Kotlin `1.9.20` toolchain. Lint still reports `1.11.0` availability, so a later Kotlin/Compose modernization pass is needed before jumping to that line. The current lint snapshot remains `0` errors and `23` warnings.

Latest phone starter wait-for-device checkpoint:

```powershell
powershell -NoProfile -Command '$null = [scriptblock]::Create((Get-Content -Raw -LiteralPath "scripts\start-phone-test-run.ps1")); "start-phone-test-run.ps1 parsed"'
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\start-phone-test-run.ps1 -Help
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\start-phone-test-run.ps1 -WaitForPhone -WaitSeconds 1 -SkipBuild -SkipNetwork -SkipSamples -NoLaunch -SkipReport
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: parse/help checks passed, help documents `-WaitForPhone` and `-WaitSeconds`, the wait-enabled starter timed out as expected in the current no-phone state with concrete ADB authorization guidance instead of running required-phone preflight immediately, and the Gradle gate stayed green. With a connected authorized phone, the normal setup path can use `.\scripts\start-phone-test-run.ps1 -WaitForPhone`; the final AI pass can use `.\scripts\start-phone-test-run.ps1 -WaitForPhone -RequireGemini -TestGeminiLive -TestGeminiImage`.

Run from `C:\Users\adamc\AndroidStudioProjects\Deal_Planner`:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-20'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest --rerun-tasks assembleDebug
```

Result: `BUILD SUCCESSFUL`.

Latest continuation gate after pantry OCR wrapped-date continuation work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `181` unit tests detected, `0` failures/errors/skipped, and lint at `0 errors, 21 warnings`. Focused `PantryOcrCandidateExtractorTest` also passed locally before the full gate.

Latest helper checkpoint after hyphenated pantry phone-sample work:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\new-phone-test-samples.ps1 -Help
.\scripts\new-phone-test-samples.ps1
```

Result: pending current continuation verification.

Latest generated sample folder in this continuation:

```text
C:\Users\adamc\AndroidStudioProjects\Deal_Planner\phone-test-samples\20260716-141255
```

Result: `new-phone-test-samples.ps1 -Help` printed successfully, sample generation succeeded, generated pantry TXT and PNG include the `Great Value Peanut Butter 16-ounce` and `Kroger Eggs 12-count` rows, generated receipt/flyer PDFs passed header/EOF checks, generated PNGs passed signature checks, the pantry-label PNG was visually inspected as readable, `git check-ignore` confirmed the generated sample folder is ignored, and `send-phone-test-samples.ps1 -SamplesDir phone-test-samples\20260716-141255` accepted the folder before stopping at the expected no connected/authorized phone condition. The full Gradle gate stayed `BUILD SUCCESSFUL` with `180` unit tests detected, `0` failures/errors/skipped, and lint at `0 errors, 21 warnings`.

Latest continuation gate after AI pantry saved-row normalization work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `180` unit tests detected, `0` failures/errors/skipped, and lint at `0 errors, 21 warnings`. Focused `PantryVisionItemMapperTest` also passed locally before the full gate.

Latest continuation gate after pantry OCR hyphenated package-size splitting work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `179` unit tests detected, `0` failures/errors/skipped, and lint at `0 errors, 21 warnings`. Focused `PantryOcrCandidateExtractorTest` also passed locally before the full gate.

Latest continuation gate after pantry hyphenated package-size parsing work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `178` unit tests detected, `0` failures/errors/skipped, and lint at `0 errors, 21 warnings`.

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

Latest continuation gate after typed pantry success status work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `117` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after Settings save feedback work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `117` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after Budget settings editor work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `117` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after Pantry edit quantity validation work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `117` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after Deal and Receipt edit numeric validation work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `117` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after AI pantry flexible date conversion work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `121` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after flyer/receipt store-name normalization work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `123` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after AI pantry review-safe date mapping work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `126` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after barcode date/item false-positive rejection work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `128` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after barcode valid-near-date regression work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `130` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after Gemini object-wrapped pantry field parsing work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `131` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after receipt weak-match rejection work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `133` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after receipt inline decimal quantity parsing work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `134` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after pantry container/count unit parsing work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `135` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after flyer no-slash per-pound price parsing work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `136` unit tests detected and `0 errors, 21 warnings`.

Latest continuation gate after pantry dozen count quantity parsing work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `137` unit tests detected and `0 errors, 21 warnings`.

Latest focused Gemini dozen quantity parsing check:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.dealplanner.ai.GeminiPantryVisionClientTest"
```

Result: `BUILD SUCCESSFUL`.

Latest continuation gate after Gemini dozen quantity parsing work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `138` unit tests detected and `0 errors, 21 warnings`.

Latest focused receipt leading-decimal price parsing check:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.dealplanner.domain.ReceiptReconcilerTest"
```

Result: `BUILD SUCCESSFUL`.

Latest continuation gate after receipt leading-decimal price parsing work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `139` unit tests detected and `0 errors, 21 warnings`.

Latest focused flyer leading-decimal price parsing check:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.dealplanner.parser.DealsParserTest"
```

Result: `BUILD SUCCESSFUL`.

Latest continuation gate after flyer leading-decimal price parsing work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `140` unit tests detected and `0 errors, 21 warnings`.

Latest focused pantry leading-decimal quantity/size parsing check:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.dealplanner.parser.PantryPhraseParserTest"
```

Result: `BUILD SUCCESSFUL`.

Latest continuation gate after pantry leading-decimal quantity/size parsing work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `141` unit tests detected and `0 errors, 21 warnings`.

Latest focused Gemini leading-decimal quantity/confidence parsing check:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.dealplanner.ai.GeminiPantryVisionClientTest"
```

Result: `BUILD SUCCESSFUL`.

Latest continuation gate after Gemini leading-decimal quantity/confidence parsing work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `142` unit tests detected and `0 errors, 21 warnings`.

Latest focused shared numeric edit leading-decimal parsing check:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.dealplanner.util.FlexibleNumberParsingTest"
```

Result: `BUILD SUCCESSFUL`.

Latest continuation gate after shared leading-decimal numeric edit parsing work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `143` unit tests detected and `0 errors, 21 warnings`.

Latest focused AI pantry unknown-unit review check:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.dealplanner.ai.PantryVisionItemMapperTest"
```

Result: `BUILD SUCCESSFUL`.

Latest continuation gate after AI pantry unknown-unit review work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `144` unit tests detected and `0 errors, 21 warnings`.

Latest focused AI pantry Generic/unknown brand review check:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.dealplanner.ai.PantryVisionItemMapperTest"
```

Result: `BUILD SUCCESSFUL`.

Latest continuation gate after AI pantry Generic/unknown brand review work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `145` unit tests detected and `0 errors, 21 warnings`.

Latest focused AI pantry missing/unknown location review check:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.dealplanner.ai.PantryVisionItemMapperTest"
```

Result: `BUILD SUCCESSFUL`.

Latest continuation gate after AI pantry missing/unknown location review work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `146` unit tests detected and `0 errors, 21 warnings`.

Latest focused AI pantry review-note clarity check:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.dealplanner.ai.PantryVisionItemMapperTest"
```

Result: `BUILD SUCCESSFUL`.

Latest continuation gate after AI pantry review-note clarity work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `146` unit tests detected and `0 failures, 0 errors, 0 skipped, and 21 lint warnings`.

Latest focused Gemini pantry label-alias parsing check:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.dealplanner.ai.GeminiPantryVisionClientTest"
```

Result: `BUILD SUCCESSFUL`.

Latest continuation gate after Gemini pantry label-alias parsing work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `147` unit tests detected and `0 failures, 0 errors, 0 skipped, and 21 lint warnings`.

Latest focused pantry OCR multi-item fallback and liquid-size parsing check:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.dealplanner.ocr.PantryOcrCandidateExtractorTest" --tests "com.dealplanner.parser.PantryPhraseParserTest" --tests "com.dealplanner.ai.GeminiPantryVisionClientTest"
```

Result: `BUILD SUCCESSFUL`.

Latest continuation gate after pantry OCR multi-item fallback work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `153` unit tests detected and `0 failures, 0 errors, 0 skipped, and 21 lint warnings`.

Latest continuation gate after scoped picker-permission work:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `153` unit tests detected and `0 failures, 0 errors, 0 skipped, and 21 lint warnings`.

Latest APK permission helper check:

```powershell
.\scripts\phone-debug-preflight.ps1
.\scripts\phone-debug-install.ps1 -SkipBuild -NoLaunch
```

Result: preflight reported `APK permissions` and `APK storage permissions` as OK. The install helper verified `com.dealplanner / Deal Planner` scoped permissions before stopping at the expected no-phone-connected condition.

Latest phone test report helper checks:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\new-phone-test-report.ps1 -Help
.\scripts\new-phone-test-report.ps1
.\scripts\phone-debug-preflight.ps1
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: report helper help printed successfully, generated an ignored `phone-test-results\<timestamp>\PHONE_TEST_REPORT.md` file without requiring a connected phone, preflight recognized the report helper, and the Gradle gate stayed `BUILD SUCCESSFUL` with `153` unit tests detected and `0 failures, 0 errors, 0 skipped, and 21 lint warnings`.

Latest continuation gate after camera/picker/scanner launch-failure status work:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-20'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `153` unit tests detected and `0 failures, 0 errors, 0 skipped, and 21 lint warnings`. The first attempt without overriding `JAVA_HOME` did not reach Gradle because this shell still pointed to the removed `C:\Program Files\Android\Android Studio\jre` path.

Latest continuation gate after camera permission-request failure status work:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-20'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `153` unit tests detected and `0 failures, 0 errors, 0 skipped, and 21 lint warnings`.

Latest helper checkpoint after Gemini-required preflight mode:

```powershell
.\scripts\phone-debug-preflight.ps1
.\scripts\phone-debug-preflight.ps1 -RequireGemini
```

Result: normal preflight still allowed OCR fallback with a Gemini warning. `-RequireGemini` intentionally failed with `Gemini key` and `Gemini APK freshness` failures because no real `local.properties` key or `GEMINI_*` configuration was present.

Latest helper usage checks:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\phone-debug-preflight.ps1 -Help
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\phone-debug-install.ps1 -Help
```

Result: both helpers printed usage/options successfully, including `-RequirePhone`, `-RequireGemini`, `-SkipNetwork`, `-SkipBuild`, and `-NoLaunch`.

Latest helper checkpoint after compiled APK Gemini readiness checks:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\phone-debug-preflight.ps1 -Help
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\new-phone-test-report.ps1 -Help
.\scripts\phone-debug-preflight.ps1 -SkipNetwork
.\scripts\phone-debug-preflight.ps1 -RequireGemini -SkipNetwork
.\scripts\new-phone-test-report.ps1
```

Result:

- Helper usage output worked for preflight and phone-test report scripts.
- Normal preflight still allows OCR fallback with `0 failure(s)` when no Gemini key is configured.
- Normal preflight reports `APK Gemini model` as `gemini-3.5-flash` and `APK Gemini key` as OCR fallback expected.
- Strict `-RequireGemini` intentionally fails without a real key, including an explicit `APK Gemini key` failure when generated debug `BuildConfig` has no non-placeholder key.
- Generated phone-test reports now include non-secret `APK Gemini configured` and `APK Gemini model` fields in Source Snapshot.

Latest helper checkpoint after install identity output work:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\phone-debug-install.ps1 -Help
.\scripts\phone-debug-install.ps1 -SkipBuild -NoLaunch
```

Expected no-phone local result: before stopping at the expected no connected/authorized Android phone condition, the install helper verifies APK identity/permissions and prints generated APK source identity plus generated APK Gemini model/configured state without printing secrets.

Latest helper checkpoint after phone-test sample generator work:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\new-phone-test-samples.ps1 -Help
.\scripts\new-phone-test-samples.ps1
```

Result: created an ignored timestamped `phone-test-samples\` folder with:

- `deal-planner-demo-receipt.txt`
- `deal-planner-demo-receipt.pdf`
- `deal-planner-demo-receipt.png`
- `deal-planner-demo-flyer.txt`
- `deal-planner-demo-flyer.pdf`
- `deal-planner-demo-flyer.png`
- `deal-planner-demo-pantry-label.txt`
- `deal-planner-demo-pantry-label.png`
- `deal-planner-demo-upc-a.txt`
- `deal-planner-demo-upc-a.png`
- `README.md`

The generated PDFs were checked for `%PDF-1.4` headers and `%%EOF` trailers, the generated PNGs were checked for the PNG file signature, and `git check-ignore` confirmed the sample output is ignored.

Latest continuation gate after gallery-image sample work:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\new-phone-test-samples.ps1 -Help
.\scripts\new-phone-test-samples.ps1
.\scripts\send-phone-test-samples.ps1
.\scripts\new-phone-test-report.ps1
$env:JAVA_HOME='C:\Program Files\Java\jdk-20'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: sample generation now creates TXT/PDF/PNG files for receipt/flyer checks plus pantry-label TXT/PNG files for gallery checks. PDFs passed header/EOF checks, PNGs passed signature checks, the pantry PNG was visually inspected as readable, the transfer helper validated TXT/PDF/PNG samples before the expected no-phone stop, the generated report includes the PNG checklist row, and the Gradle gate stayed `BUILD SUCCESSFUL` with `153` unit tests detected and `0 failures, 0 errors, 0 skipped, and 21 lint warnings`.

Latest continuation gate after phone sample transfer verification work:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\send-phone-test-samples.ps1 -Help
.\scripts\send-phone-test-samples.ps1
.\scripts\new-phone-test-report.ps1
$env:JAVA_HOME='C:\Program Files\Java\jdk-20'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: the transfer helper still validates the local generated sample folder before the expected no-phone stop. With an authorized phone, it now verifies each pushed remote file size and requests Android media scans for picker visibility. The generated phone-test report includes a pass/fail row for remote byte-size/media-scan evidence, and the Gradle gate stayed `BUILD SUCCESSFUL` with `153` unit tests detected and `0 failures, 0 errors, 0 skipped, and 21 lint warnings`.

Latest continuation gate after UPC-A barcode sample work:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\new-phone-test-samples.ps1 -Help
.\scripts\new-phone-test-samples.ps1
.\scripts\send-phone-test-samples.ps1
.\scripts\new-phone-test-report.ps1
$env:JAVA_HOME='C:\Program Files\Java\jdk-20'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: sample generation now creates `deal-planner-demo-upc-a.txt` and `deal-planner-demo-upc-a.png` for manual barcode/code entry and scanner checks. The UPC-A check digit is validated before image generation, the generated PNG passed signature checks and was visually inspected, the transfer helper requires the barcode files before phone copy, the generated report includes a UPC-A sample row, and the Gradle gate stayed `BUILD SUCCESSFUL` with `153` unit tests detected and `0 failures, 0 errors, 0 skipped, and 21 lint warnings`.

Latest helper checkpoint after phone-test sample transfer work:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\send-phone-test-samples.ps1 -Help
.\scripts\send-phone-test-samples.ps1
```

Result: help output printed successfully, all PowerShell helpers parsed successfully, and the helper validated the latest generated sample folder before stopping at the expected no connected/authorized Android phone condition. With one authorized phone, it copies the sample TXT/PDF/PNG files to `/sdcard/Download/DealPlannerPhoneTestSamples/<timestamp>/`, verifies remote byte sizes, and requests Android media scans for picker visibility.

Latest continuation gate after phone-test sample transfer work:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-20'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `153` unit tests detected and `0 failures, 0 errors, 0 skipped, and 21 lint warnings`.

Latest helper checkpoint after one-command phone-test starter work:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\start-phone-test-run.ps1 -Help
.\scripts\start-phone-test-run.ps1
```

Result: help output printed successfully, all PowerShell helpers parsed successfully, generated phone-test reports include the starter checklist row, and the starter stops during required-phone preflight when no connected/authorized Android phone is available. With one authorized phone, it runs preflight, generates and transfers deterministic sample files, installs/launches the debug APK, and creates a phone-test report.

Latest continuation gate after one-command phone-test starter work:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-20'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `153` unit tests detected and `0 failures, 0 errors, 0 skipped, and 21 lint warnings`.

Latest continuation gate after receipt PDF import work:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-20'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat compileDebugKotlin
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `153` unit tests detected and `0 failures, 0 errors, 0 skipped, and 21 lint warnings`.

Receipt PDF checkpoint:

- Receipts screen now has a `PDF` picker action.
- Receipt PDFs render through the existing capped local PDF renderer and ML Kit OCR path.
- Parsed PDF text uses the same receipt reconciliation, pantry update, budget update, and review flow as receipt photos/gallery/manual text.
- Phone checklist and generated phone-test report now include receipt PDF pass/fail evidence.

Latest continuation gate after helper/report source-identity work:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-20'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :app:generateDebugBuildConfig --rerun-tasks testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`, with `153` unit tests detected and `0 failures, 0 errors, 0 skipped, and 21 lint warnings`.

Generated debug `BuildConfig` source identity is intentionally commit-dependent. Use `.\scripts\phone-debug-preflight.ps1` (`APK source identity`), `.\scripts\new-phone-test-report.ps1` (Source Snapshot), or Settings -> About in the installed app to read the current APK branch/commit/dirty state after each clean rebuild.

Latest continuation gate after unsafe flyer multibuy parsing hardening:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-20'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest --tests com.dealplanner.parser.DealsParserTest
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`. Targeted `DealsParserTest` passed, then the full Gradle gate passed. The flyer parser now ignores impossible or unsafe multibuy counts such as `0 for $5` and oversized OCR counts instead of importing bad deals or throwing, and pasted flyer processing now reports `Could not process that flyer text.` if an unexpected parser error occurs.

Latest focused AI parser check after object-wrapped question/warning preservation:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-20'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest --tests com.dealplanner.ai.GeminiPantryVisionClientTest
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`. Targeted `GeminiPantryVisionClientTest` passed, then the full Gradle gate passed. Gemini pantry response parsing now preserves object-wrapped `questions` and `warnings` values, such as `{"question":"..."}`, `{"message":"..."}`, and `{"warning":"..."}`, so AI review prompts remain available for imported VERIFY rows.

Latest focused receipt parser check after explicit whole-dollar price parsing:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-20'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest --tests com.dealplanner.domain.ReceiptReconcilerTest
```

Result: `BUILD SUCCESSFUL`. Receipt OCR parsing now accepts explicit whole-dollar prices such as `$3`, including inline quantity, split quantity, and weighted produce rows, while no-dollar bare integers such as package-size text remain ignored.

Latest focused flyer parser check after explicit whole-dollar unit price parsing:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-20'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest --tests com.dealplanner.parser.DealsParserTest
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`. Targeted `DealsParserTest` passed, then the full Gradle gate passed. Flyer OCR parsing now accepts explicit whole-dollar unit prices such as `$3/lb`, `$4 per pound`, and `$1/ea`, while bare package-size text such as `5 lb bag` is not imported as a deal.

Latest focused flyer parser check after explicit whole-dollar package price parsing:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-20'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest --tests com.dealplanner.parser.DealsParserTest
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`. Targeted `DealsParserTest` passed, then the full Gradle gate passed. Flyer OCR parsing now accepts explicit whole-dollar package prices such as `Milk` / `$3` and `Flour 5 lb bag $4`, while bare package-size text such as `3 lb bag` is not imported as a deal.

Latest focused flyer parser check after savings-only flyer callout filtering:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-20'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest --tests com.dealplanner.parser.DealsParserTest
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`. Targeted `DealsParserTest` passed, then the full Gradle gate passed with `159` unit tests detected and `0` failures/errors. Savings-only flyer callouts such as `Save $1 when you buy 2`, `You save $1/lb with card`, and `Savings $2 with digital coupon` are ignored so they do not import fake deal rows, while real adjacent prices such as `Milk` / `$3`, `$2.99/lb`, and `10 for $10` still import normally.

Latest focused receipt parser check after year-first receipt header date parsing:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-20'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest --tests com.dealplanner.domain.ReceiptReconcilerTest
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`. Targeted `ReceiptReconcilerTest` passed, then the full Gradle gate passed with `160` unit tests detected and `0` failures/errors. Receipt header dates such as `Transaction Date: 2025/10/27` and `Purchase Date: 2025-10-28` apply to imported receipt rows, matching the existing `Date: 10/27/2025` behavior.

Latest focused receipt parser check after saved-total filtering:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-20'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest --tests com.dealplanner.domain.ReceiptReconcilerTest
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`. Targeted `ReceiptReconcilerTest` passed, then the full Gradle gate passed with `160` unit tests detected and `0` failures/errors. Receipt summary lines such as `YOU SAVED $4.25`, `SAVED TODAY 4.25`, and `TOTAL SAVED $4.25` are ignored so savings summaries do not import as grocery rows or inflate Budget spending.

Latest focused pantry/AI date parsing check after unpadded year-first date support:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-20'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest --tests com.dealplanner.util.FlexibleDateParsingTest --tests com.dealplanner.parser.PantryPhraseParserTest --tests com.dealplanner.ai.PantryVisionItemMapperTest
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`. Targeted `FlexibleDateParsingTest`, `PantryPhraseParserTest`, and `PantryVisionItemMapperTest` passed, then the full Gradle gate passed with `162` unit tests detected and `0` failures/errors. Manual pantry text and AI pantry photo mapping accept unpadded year-first dash dates such as `2026-7-1` for opened and best-by dates.

Latest focused pantry parser check after two-digit dash label date parsing:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-20'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest --tests com.dealplanner.parser.PantryPhraseParserTest
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`. Targeted `PantryPhraseParserTest` passed locally, then the full Gradle gate passed with `163` unit tests detected, `0` failures/errors, and `21` lint warnings. Manual pantry text now accepts two-digit dash label dates such as `milk use by 12-31-26` for best-by dates.

Latest focused Gemini parser check after review question/warning alias parsing:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-20'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest --tests com.dealplanner.ai.GeminiPantryVisionClientTest
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`. Targeted `GeminiPantryVisionClientTest` passed locally, then the full Gradle gate passed with `164` unit tests detected, `0` failures/errors, and `21` lint warnings. Gemini pantry response parsing now preserves alternate AI review-question aliases such as `clarifying_questions` and `followUpQuestions`, plus warning aliases such as `review_notes`, so imported VERIFY rows can keep model-provided review prompts.

Latest focused pantry OCR/parser check after net-weight label handling:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-20'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest --tests com.dealplanner.parser.PantryPhraseParserTest --tests com.dealplanner.ocr.PantryOcrCandidateExtractorTest
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`. Targeted `PantryPhraseParserTest` and `PantryOcrCandidateExtractorTest` passed locally, then the full Gradle gate passed with `165` unit tests detected, `0` failures/errors, and `21` lint warnings. Pantry OCR fallback now avoids treating `NET WT` package-size lines as separate products, and the pantry parser strips `net wt` label wording from the item name while preserving the package size and best-if-used-by date.

Latest focused flyer parser check after comma-decimal package-size parsing:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-20'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest --tests com.dealplanner.parser.DealsParserTest
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`. Targeted `DealsParserTest` passed locally, then the full Gradle gate passed with `166` unit tests detected, `0` failures/errors, and `21` lint warnings. Flyer parsing now preserves comma-decimal package sizes such as `5,3 oz` as `5.3 oz`, while loose per-pound prices such as `2,99 lb` are still treated as prices and not package sizes.

Latest focused receipt parser check after item-first inline quantity parsing:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-20'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest --tests com.dealplanner.domain.ReceiptReconcilerTest
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`. Targeted `ReceiptReconcilerTest` passed locally, then the full Gradle gate passed with `167` unit tests detected, `0` failures/errors, and `21` lint warnings. Receipt OCR parsing now preserves quantities in item-first inline rows such as `BLACK BEANS 2 @ 0.89 1.78`, including comma-decimal variants such as `KROGER PASTA 3 @ 1,00 3,00`.

Latest focused Gemini parser check after non-JSON pantry fallback:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-20'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest --tests com.dealplanner.ai.GeminiPantryVisionClientTest
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`. Targeted `GeminiPantryVisionClientTest` passed locally, then the full Gradle gate passed with `168` unit tests detected, `0` failures/errors, `0` skipped, and `21` lint warnings. Gemini pantry parsing now returns an empty result with a warning for non-JSON model text, letting the existing pantry photo flow continue to OCR fallback instead of depending on an exception path.

Latest focused receipt reconciler check after repeated pantry-match accumulation:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-20'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest --tests com.dealplanner.domain.ReceiptReconcilerTest
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`. Targeted `ReceiptReconcilerTest` passed locally, then the full Gradle gate passed with `169` unit tests detected, `0` failures/errors, `0` skipped, and `21` lint warnings. Repeated pantry-matched receipt rows for the same pantry item now accumulate into one pantry update instead of allowing a later row to overwrite an earlier quantity increment.

Latest focused pantry duplicate check after missing-brand merge compatibility:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-20'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest --tests com.dealplanner.parser.PantryPhraseParserTest
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`. Targeted `PantryPhraseParserTest` passed locally, then the full Gradle gate passed with `170` unit tests detected, `0` failures/errors, `0` skipped, and `21` lint warnings. Missing, Generic, or unknown brands are now compatible with a known brand for duplicate detection when item, size, and location match, while different known brands remain separate.

Latest focused meal-side check after household/non-food flyer filtering:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-20'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest --tests com.dealplanner.domain.MealPlanningEngineTest
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`. Targeted `MealPlanningEngineTest` passed locally, then the full Gradle gate passed with `171` unit tests detected, `0` failures/errors, `0` skipped, and `21` lint warnings. Meal planning now requires recognized meal-side grocery terms for generated vegetable slots and ignores household/non-food flyer deals such as detergent so they do not enter meals or Shopping totals.

Latest focused Settings protein validation check:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-20'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest --tests com.dealplanner.domain.MealPlanningEngineTest
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`. Targeted `MealPlanningEngineTest` passed locally, then the full Gradle gate passed with `172` unit tests detected, `0` failures/errors, `0` skipped, and `21` lint warnings. Settings now blocks negative protein-per-meal input, and the meal planner falls back to the default 0.5 lb value if old/corrupt saved settings contain a negative value, preventing negative Shopping quantities or estimated costs.

Latest focused non-finite numeric validation check:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-20'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest --tests com.dealplanner.util.FlexibleNumberParsingTest --tests com.dealplanner.ai.GeminiPantryVisionClientTest
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`. Targeted `FlexibleNumberParsingTest` and `GeminiPantryVisionClientTest` passed locally, then the full Gradle gate passed with `174` unit tests detected, `0` failures/errors, `0` skipped, and `21` lint warnings. Shared manual numeric parsing now rejects non-finite values such as `NaN`, `Infinity`, and `-Infinity`, and Gemini pantry response parsing treats non-finite quantity/confidence text as missing/default review data instead of saving invalid numbers.

Latest focused AI pantry quantity mapping check:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-20'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest --tests com.dealplanner.ai.PantryVisionItemMapperTest
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`. Targeted `PantryVisionItemMapperTest` passed locally, then the full Gradle gate passed with `175` unit tests detected, `0` failures/errors, `0` skipped, and `21` lint warnings. AI pantry photo mapping now treats non-positive model quantities as missing amount details, defaults the saved review item to quantity `1.0`, and adds `Review amount/unit.` instead of saving a zero or negative pantry quantity.

Latest helper checkpoint after generated phone-test report AI checklist sync:

```powershell
.\scripts\new-phone-test-report.ps1
```

Result: report helper help printed successfully, all 7 PowerShell helpers parsed successfully, and a generated ignored `PHONE_TEST_REPORT.md` included an AI verification row for zero or negative Gemini amount details falling back to quantity `1.0`, showing VERIFY, and including `Review amount/unit.` so the real-phone report matches `PHONE_TEST_CHECKLIST_2026-07-16.md`. The full Gradle gate stayed `BUILD SUCCESSFUL` with `175` unit tests detected, `0` failures/errors, `0` skipped, and `21` lint warnings.

Latest focused flyer each-price parser check:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-20'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest --tests com.dealplanner.parser.DealsParserTest
```

Result: `BUILD SUCCESSFUL`. Targeted `DealsParserTest` passed locally, then the full Gradle gate passed with `176` unit tests detected, `0` failures/errors, `0` skipped, and `21` lint warnings. Flyer text/OCR parsing now accepts `$3.99 each`, `$1.25 per ea`, and `88c each` as per-unit deals without appending `each` or `ea` to the imported item name. This strengthens pasted flyer text plus flyer photo/gallery/PDF OCR paths because all route through the same parser.

Latest focused receipt `x` quantity separator check:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-20'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest --tests com.dealplanner.domain.ReceiptReconcilerTest
```

Result: `BUILD SUCCESSFUL`. Targeted `ReceiptReconcilerTest` passed locally, then the full Gradle gate passed with `177` unit tests detected, `0` failures/errors, `0` skipped, and `21` lint warnings. Receipt OCR parsing now accepts split, quantity-first inline, and item-first weighted quantity rows with `x` or `X` separators, such as `2 x 0.89`, `3 X 1.00 KROGER PASTA 3.00`, and `APPLES 1,25 lb x 1,99/lb 2,49`. This strengthens pasted receipt text plus receipt photo/gallery/PDF OCR paths because all route through the same reconciler.

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
.\scripts\start-phone-test-run.ps1
.\scripts\phone-debug-preflight.ps1
```

```powershell
.\scripts\phone-debug-install.ps1
```

Use `.\scripts\start-phone-test-run.ps1` for the normal connected-phone run. It checks required-phone preflight, creates/copies deterministic sample files, installs/launches the APK, and creates a timestamped report.
Use `.\scripts\phone-debug-preflight.ps1` to check repo/APK/ADB/Gemini/barcode lookup readiness before installing.
Use `.\scripts\phone-debug-install.ps1 -SkipBuild` after the APK is already built and app source/resources/build config plus Gemini/local configuration have not changed.
Use `.\scripts\phone-debug-preflight.ps1 -Help` and `.\scripts\phone-debug-install.ps1 -Help` if the exact helper options are lost.
Use `.\scripts\phone-debug-logs.ps1` to capture device metadata, full logcat, and a Deal Planner/crash-filtered log if a real-phone test fails. Captured logs write to ignored local `phone-test-logs\`.
Use `.\scripts\new-phone-test-report.ps1` before or during phone testing; its Source Snapshot now records both the repo HEAD and the compiled APK source branch/commit/dirty state from generated debug `BuildConfig`.
Use `.\scripts\new-phone-test-samples.ps1` before phone testing to create ignored demo receipt/flyer TXT, PDF, and PNG files plus pantry-label and UPC-A barcode samples for pasted-text, gallery-image, barcode, and PDF picker checks.
Use `.\scripts\send-phone-test-samples.ps1` after USB debugging is authorized to copy the latest generated sample folder to the phone's Downloads folder, verify remote byte sizes, and request Android media scans for picker visibility.

Phone test checklist:

`C:\Users\adamc\AndroidStudioProjects\Deal_Planner\PHONE_TEST_CHECKLIST_2026-07-16.md`

## Current Feature Status

Verified by build/unit tests/code inspection:

- App name/package is now Deal Planner: `com.dealplanner`.
- Room database filename is now `deal_planner_db`.
- Settings -> About Deal Planner displays the actual Gradle version, package name, debug/release build identity, source branch, source commit, and dirty-build state from `BuildConfig`.
- `scripts\start-phone-test-run.ps1` is available for required-phone setup orchestration: preflight, sample generation/transfer, APK install/launch, and report creation.
- `scripts\phone-debug-install.ps1` can build, verify, print generated APK source/Gemini identity, install, confirm the package on-device, and launch the debug APK once ADB sees an authorized phone.
- `scripts\phone-debug-preflight.ps1` reports repo, GitHub origin/upstream sync, APK, APK identity/permissions, generated debug `BuildConfig` source identity, ADB/phone, Gemini, and Open Food Facts readiness without printing secrets.
- `scripts\phone-debug-preflight.ps1` confirms origin points at `adamcboyd/Deal-Planner`, compares the branch with its configured upstream, and checks the GitHub branch SHA with `git ls-remote` when network checks are enabled.
- `scripts\phone-debug-preflight.ps1` and `scripts\phone-debug-install.ps1` inspect `app-debug.apk` with Android SDK `aapt` when available, verifying `com.dealplanner` / `Deal Planner`, required `INTERNET` and `CAMERA` permissions, and no broad storage/media permissions before phone testing.
- `scripts\phone-debug-preflight.ps1` warns when app source/resources/build config or `local.properties` are newer than `app-debug.apk`, and `scripts\phone-debug-install.ps1 -SkipBuild` refuses that stale APK so app code and Gemini key/model values must be rebuilt before phone testing.
- `scripts\phone-debug-logs.ps1` is available for phone-test crash/log capture and writes local logs under ignored `phone-test-logs\`.
- `scripts\new-phone-test-report.ps1` is available for timestamped phone-test pass/fail evidence capture, records repo HEAD plus compiled APK source branch/commit/dirty state, and writes local reports under ignored `phone-test-results\`.
- `scripts\new-phone-test-samples.ps1` is available for creating ignored demo receipt/flyer TXT, PDF, and PNG files plus pantry-label and UPC-A barcode samples under `phone-test-samples\`.
- `scripts\send-phone-test-samples.ps1` is available for copying the latest generated demo receipt/flyer/pantry/barcode TXT, PDF, and PNG files to an authorized Android phone's Downloads folder, verifying remote byte sizes, and requesting Android media scans.
- Bottom navigation labels are now backed by string resources while preserving the visible tab labels.
- Room local database and repository layer compile.
- Pantry natural-language parser has unit tests.
- Pantry manual text input parses the phone-checklist `best by 2026-12-31` ISO date format without treating the date as part of the item name.
- Pantry manual text input keeps `opened` and `best by` dates independent when both appear in one phrase, such as `opened yesterday best by 2026-12-31`.
- Pantry manual text input parses common label-style expiration cues such as `best before`, `use by`, and `exp`.
- Pantry manual text input parses label wording such as `expiration date 12/31/2026` and `best by date 2026-12-31` without leaving `date` in the item name.
- Pantry manual text input parses `opened on 2026-07-01` without leaving `on` in the item name.
- Pantry manual text input parses unpadded year-first dates such as `opened 2026-7-1`.
- Pantry manual text input parses hyphenated label cues such as `use-by 12/31/2026` and `best-by 2026-12-31` without leaving the cue in the item name.
- Pantry manual/OCR text input accepts comma-decimal quantities and package sizes such as `1,5 lb ground beef` and `Kroger yogurt 5,3oz`.
- Pantry manual/OCR text input accepts leading-decimal quantities and package sizes such as `.5 lb ground beef`, `.25 cups olive oil`, and `Kroger yogurt .75oz`.
- Pantry manual/OCR text input accepts common liquid package sizes such as `Kroger milk 1 gal fridge`, `chicken broth 1 quart pantry`, and `cream 1 pint fridge`.
- Deals flyer parser has unit tests, including bundled demo flyer structures.
- Deals parser handles package prices, multi-line names, and trailing modifiers such as limits, coupons, and BOGO lines.
- Deals parser handles slash-style multi-buy prices such as `2/$5` and `10 / $10`.
- Deals parser ignores impossible or unsafe multibuy counts such as `0 for $5` and oversized OCR counts instead of importing invalid deals.
- Deals parser handles word-number buy-get flyer promos such as `Buy One Get One Free` and `Buy Two Get One Free`.
- Deals parser handles buy-get percent-off promos such as `Buy One Get One 50% off` as a 25% effective overall discount and `Buy Two Get One 50% off` as about 16.7%.
- Deals parser handles BOGO flyer shorthand such as `BOGO Free` and `B1G1` without merging the next flyer item or treating `B1G1` as a package size.
- Deals parser handles BOGO second-item percent discounts such as `BOGO 50% off` as a 25% effective overall discount.
- Deals parser ignores flyer metadata/date lines such as `Valid 7/16/2026 - 7/22/2026` so slash dates do not become fake multi-buy deals.
- Deals parser accepts flyer prices when OCR drops dollar signs.
- Deals parser accepts leading-decimal flyer OCR prices such as `.99/lb`, `.99 lb`, `.89`, `2 for .99`, and `2/.99`.
- Deals parser accepts comma-decimal flyer OCR prices such as `2,99/lb`, `2 for 5,00`, and `3 lb bag 2,99`, and preserves comma-decimal package sizes such as `5,3 oz` without treating loose per-pound prices as package sizes.
- Deals parser accepts explicit whole-dollar unit prices such as `$3/lb`, `$4 per pound`, and `$1/ea`, without treating bare package-size text as a price.
- Deals parser accepts explicit whole-dollar package prices such as `Milk` / `$3` and `Flour 5 lb bag $4`, without treating bare package-size text as a price.
- Deals parser ignores savings-only flyer callouts such as `Save $1 when you buy 2` so they do not import fake deal rows.
- Deals parser accepts cent-style flyer/OCR prices such as `99c/lb` and `88c`, with exact unit coverage for the phone checklist `Roma Tomatoes` / `99c/lb` pasted-text test.
- Deals parser accepts each/ea flyer OCR prices such as `$3.99 each`, `$1.25 per ea`, and `88c each` without appending `each` or `ea` to the imported item name.
- Meal planning engine has unit tests.
- Meal plan generation has unit coverage for one generated row per requested date and deterministic output for the same inputs.
- Meal-side filtering has unit coverage so household/non-food flyer deals do not become generated meal vegetables or Shopping items.
- Negative protein-per-meal settings have unit coverage so they fall back to the default quantity instead of producing negative Shopping quantities or estimated costs.
- Menu Generate shows visible meal-plan generation status and any rules-engine warnings, such as missing protein deals or pantry starch anchors.
- Shopping list consolidation has unit coverage for pre-database deal identities before Room assigns ids.
- Shopping list estimated costs use planned quantities and normalized price-per-unit values instead of multiplying sticker price by planned quantity in the UI.
- After saved meal plans exist, Pantry, Deals, Receipts, and Settings input changes rederive the visible Shopping list from current inputs.
- Budget engine has unit tests.
- Receipt reconciliation engine has unit tests.
- Pantry photo/gallery/barcode/manual code input exists.
- Typed pantry input shows visible added/updated status after successful add or merge.
- Pantry items can be edited/reviewed after typed, barcode/manual code, OCR, or AI import.
- Pantry item edit blocks invalid or negative quantities with visible validation instead of silently preserving the old quantity.
- Barcode/manual code pantry input looks up product names, brands, and package quantities through Open Food Facts when network is available.
- Barcode/manual code pantry input still creates VERIFY fallback items with the barcode preserved in notes when lookup misses or network is unavailable.
- Barcode/manual code normalization extracts 8-14 digit UPC/EAN/GTIN codes from pasted label text such as `UPC: 0 12345-67890 5`, prefers labeled UPC/EAN/GTIN values over unrelated item/date numbers, accepts valid bare product codes near label dates, and rejects non-code date, item, lot, SKU, or plain text with `No barcode found.`.
- Manual barcode/code text stays available for correction when no UPC/EAN/GTIN is found, and clears only after a successful barcode import.
- Open Food Facts barcode response parsing and barcode normalization have no-network unit coverage.
- Pantry typed, OCR/AI photo, and barcode imports now upsert safe duplicates instead of creating repeated rows.
- Pantry duplicate detection normalizes package size and missing/Generic/unknown brand values, allows unbranded typed/OCR rows to merge with a known-brand row when item, size, and location match, keeps different known brands or locations separate, and only merges barcode items when the barcode value matches.
- Pantry typed date parsing accepts two-digit dash label dates such as `milk use by 12-31-26`.
- Pantry edit/review quantity fields accept comma-decimal and leading-decimal corrections such as `1,5`, `.5`, and `,5`.
- Camera permission denial and canceled camera/barcode/gallery/PDF actions now show visible status messages during phone testing.
- Camera permission request launch failures now show visible recovery messages that point to Android Settings or an alternate input path.
- Camera, gallery, PDF picker, and barcode scanner launch failures now show visible recovery messages instead of crashing the app if Android cannot open the external flow.
- Blank manual pantry Add, barcode Add Code, flyer Process Text, and receipt Process Text taps show visible status messages instead of silently doing nothing.
- Camera/gallery image imports decode to software bitmaps and cap oversized phone images before OCR/Gemini processing.
- Gallery image and PDF imports rely on Android picker URI grants; the APK no longer requests `READ_EXTERNAL_STORAGE` or `READ_MEDIA_IMAGES`.
- Camera/gallery image-open failures show visible recovery messages instead of escaping the import coroutine.
- ML Kit pantry OCR fallback preserves single-label photos as one combined review item, avoids treating `NET WT` package-size lines as products, and splits clear multi-item OCR rows into separate VERIFY pantry items.
- Flyer photo/gallery/PDF/manual text input exists.
- Flyer pasted-text import shows processing status, keeps pasted text available when parsing finds no deals or processing fails, and clears it only after successful deal import.
- Flyer PDF pages render with a 3072px longest-side cap before OCR to reduce oversized-PDF failures on phones.
- Flyer imports are store-aware instead of defaulting every scanned deal to `Unknown`, and flyer store names are trimmed with blank values defaulted to `Unknown`.
- Flyer deals can be edited/reviewed after photo, gallery, PDF, or pasted OCR import.
- Flyer deal edit/review numeric fields accept comma-decimal and leading-decimal corrections for price, PPU, discount, score, and confidence, and block invalid or non-finite values with visible validation.
- Receipt photo/gallery/PDF/manual text input exists.
- Receipt pasted-text import shows processing status, keeps pasted text available when parsing finds no receipt line items, and clears it only after successful receipt import.
- Receipt imports trim store names and default blank values to `Unknown`.
- Receipt items can be edited/reviewed after photo, gallery, PDF, or pasted OCR import.
- Receipt edit/review numeric fields accept comma-decimal and leading-decimal corrections for quantity, total, and confidence, and block invalid, non-finite quantity, total, match ID, and confidence values with visible validation.
- Bundled `demo_receipt.txt` parses into the expected 8 grocery items for the deterministic phone checklist pasted-text receipt test, ignores the EBT/card tender line, applies the `Date: 10/27/2025` header, and totals `$40.65`.
- Bundled `demo_receipt.txt` also has unit coverage for the phone checklist appended tender lines `VISA DEBIT $40.65` and `CARD TENDER $40.65`.
- Receipt header dates such as `Date: 10/27/2025`, `Transaction Date: 2025/10/27`, and `Purchase Date: 2025-10-28` are applied to imported receipt rows when available; rows fall back to today's date when no receipt date is found.
- Receipt reconciliation attaches split quantity lines, including weighted price-per-pound lines, to the previous grocery item.
- Receipt reconciliation parses one-line weighted produce rows such as `BANANAS 1.50 lb @ $0.69/lb $1.04` and comma-decimal variants such as `APPLES 1,25 lb @ 1,99/lb 2,49`.
- Receipt reconciliation accepts item totals and inline quantity lines when OCR drops dollar signs, including quantity-first rows such as `2 @ 0.89 BLACK BEANS 1.78` and item-first rows such as `BLACK BEANS 2 @ 0.89 1.78`.
- Receipt reconciliation accepts leading-decimal receipt prices such as `.89` in weighted produce rows, inline quantity rows, split quantity rows, and plain item-total rows.
- Receipt reconciliation accepts item totals and split quantity lines when OCR uses comma decimals, such as `BLACK BEANS 1,78` plus `2 @ 0,89`.
- Receipt reconciliation accepts explicit whole-dollar OCR receipt prices such as `$3`, including inline quantity, split quantity, and weighted produce rows, without treating bare integer package-size text as prices.
- Receipt reconciliation accepts `x`/`X` quantity-price separators as well as `@` in split, quantity-first inline, and item-first weighted OCR rows.
- Receipt reconciliation ignores subtotal, tax, total, savings, saved-total lines such as `YOU SAVED`, and tender/payment lines, including card tender lines such as `VISA DEBIT` and `CARD TENDER`.
- Receipt reconciliation ignores SNAP/EBT/WIC benefit tender lines such as `SNAP EBT`, `EBT FOOD`, and `WIC BENEFIT` so they do not inflate grocery spending.
- Receipt reconciliation ignores coupon, discount, reward, refund, return, promo, markdown, and adjustment lines so those OCR rows do not increase spending.
- Receipt totals are rounded to cents before budget updates.
- Receipt imports, edits, and deletes adjust budget spending totals, daily envelope, and projected spend.
- Budget analysis loads actual receipts and uses current-month receipt history when calculating projected spend.
- Budget screen current balance, monthly overview, and progress display use receipt-aware analysis values when available, so stale stored budget totals do not contradict current receipt history.
- Budget Settings lets the user edit monthly budget, spent-to-date baseline, and breakfast anchor cost with comma-decimal and leading-decimal support, non-negative validation, and visible saved feedback.
- Pantry-matched receipt imports, edits, and deletes adjust pantry quantities, including repeated matched items on one receipt.
- Camera capture now uses full-resolution app-cache image files for pantry, flyer, and receipt OCR.
- ML Kit OCR fallback exists.
- Optional Gemini pantry photo client exists.
- Settings screen shows whether Gemini Vision is configured or OCR fallback is active.
- Settings screen includes a Test AI Connection button for key/model/network verification across both text and image input on the phone.
- Settings Test AI Connection summarizes Gemini API errors with concise HTTP/status messages instead of showing raw server JSON.
- Settings protein-per-meal numeric input accepts comma-decimal and leading-decimal values such as `0,5` or `.5`.
- Settings Save shows visible saved feedback and blocks invalid, negative, or non-finite protein-per-meal text instead of silently defaulting.
- Settings About displays version `1.0 (1)`, package `com.dealplanner`, debug/release build identity, and source identity from the installed build.
- Placeholder Gemini keys are treated as not configured.
- Gemini setup trims accidental key/model whitespace and normalizes a pasted `models/` prefix before calling the API.
- Gemini pantry response parsing has no-network unit coverage for fenced JSON, minor surrounding text, scalar/object-wrapped warnings/questions, alternate review-question aliases such as `clarifying_questions` and `followUpQuestions`, warning aliases such as `review_notes`, top-level arrays, single-item objects, plural and singular item wrappers, snake_case/camelCase/name aliases, common label-date aliases such as `sell_by_date` and `expirationDateText`, numeric/comma-decimal/leading-decimal/word/dozen/object quantity aliases such as `amount: "2 cans"`, `amount: "1,5 lb"`, `amount: ".5 lb"`, `amount: "two cans"`, `amount: "a dozen eggs"`, `quantity: { value: "half dozen" }`, or `quantity: { value: "2", unit: "cans" }`, liquid-unit aliases such as gallon/quart/pint, comma-decimal and leading-decimal confidence such as `"0,82"` or `".82"`, non-finite numeric text fallback such as `NaN` or `Infinity`, storage aliases including cabinet/cold-storage wording, malformed string/list fields, non-JSON model text fallback, and confidence clamping.
- AI pantry photo date conversion has unit coverage for common label formats such as `12/31/2026`, `12-31-26`, `2026/12/31`, and `2026-7-1`, so Gemini-provided best-by/opened dates are not limited to strict ISO text.
- AI pantry photo item mapping has unit coverage for unparseable best-by/opened date text; bad date text is preserved in notes and the item requires review.
- AI pantry photo item mapping has unit coverage for unknown or non-positive amount details; the item requires review instead of being treated as fully verified or saving a zero/negative pantry quantity.
- AI pantry photo item mapping has unit coverage for Generic or unknown brand values; the item requires review so missing label brand details stay visible.
- AI pantry photo item mapping has unit coverage for missing or unknown storage location; the item defaults to `pantry` but requires review so pantry/fridge/freezer placement can be corrected.
- AI pantry photo VERIFY notes include explicit review reasons for missing brand, amount/unit, storage location, and best-by date details, so the phone review flow tells the user what needs correction.
- Demo data loading resets pantry, deals, receipts, meal plans, default meal settings, and the `$292 / $45 spent` demo budget baseline.
- Menu Generate deterministically rebuilds and replaces the active generated week so repeated taps do not duplicate meal-plan rows.
- Meal planning ignores household/non-food flyer deals when choosing generated meal sides and Shopping items.
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
- Receipt photo/gallery/PDF/manual text UX.
- ML Kit OCR quality on real pantry/flyer photos.
- Gemini pantry photo API call.
- Android permissions flow.
- Camera permission denial/cancel and gallery/PDF picker cancel status on the physical phone.
- Camera permission request-failure status on the physical phone.
- External camera/picker/scanner launch-failure status on the physical phone.
- Shopping list startup restore on the physical phone.
- Kitchen pantry test.

Current AI configuration:

- `local.properties` was not present in the clean `Deal_Planner` folder.
- `GEMINI_API_KEY` environment variable was not set in this shell.
- Therefore Gemini Vision is not live-configured yet; the app will use ML Kit OCR fallback.
- `scripts\phone-debug-preflight.ps1 -RequireGemini` is now available for the AI-specific phone pass and intentionally fails until a real key is configured and the APK can be verified against that configuration.
- Current default model in Gradle is `gemini-3.5-flash`, which matched the current Google AI model page checked again on 2026-07-16. The official model page lists `gemini-3.5-flash` as stable and supports text/image/PDF-style multimodal inputs.
- Rebuild the debug APK after adding or changing `local.properties`; Gemini values are compiled into `BuildConfig`, `-SkipBuild` is blocked if `local.properties` is newer than the APK, and preflight/report output now verifies the compiled APK Gemini key/model state without printing secrets.
- Live Gemini connection testing is now available from Settings after adding a real key.

## Important Cautions

- `JAVA_HOME` in the environment points to `C:\Program Files\Android\Android Studio\jre`, which does not exist on this machine. Use `C:\Program Files\Java\jdk-20` for command-line builds unless Android Studio supplies its own JBR.
- Do not continue work in the broken Codex task attached to `C:\Users\adamc\Documents\NEOPUNK`; that task is attached to the wrong repo and can trigger missing-ref errors.
- Do not continue app work in `C:\Users\adamc\AndroidStudioProjects\SNAP_Optimizer`; it is the old-named Android Studio folder and is behind the current Deal Planner branch.
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
6. Start the guided phone run:

```powershell
.\scripts\start-phone-test-run.ps1
```

Or run the install helper directly:

```powershell
.\scripts\phone-debug-install.ps1
```

For the final AI-specific pass after adding `local.properties` and rebuilding, require both the phone and Gemini setup:

```powershell
.\scripts\phone-debug-preflight.ps1 -RequirePhone -RequireGemini
```

Optional if anything fails on the phone:

```powershell
.\scripts\phone-debug-logs.ps1 -Clear -Launch -DurationSeconds 90
```

Optional before the phone test run:

```powershell
.\scripts\start-phone-test-run.ps1
.\scripts\new-phone-test-report.ps1
.\scripts\new-phone-test-samples.ps1
.\scripts\send-phone-test-samples.ps1
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
   - Pantry camera-permission denial, canceled capture/gallery/scan status, and external camera/gallery/scanner launch-failure status if reproducible.
   - Pantry manual barcode/code entry.
   - Pantry manual barcode/code entry with pasted label text that includes unrelated item/date numbers before the UPC.
   - Pantry manual barcode/code entry with invalid text that has no product code; confirm `No barcode found.` appears and the text stays available for correction.
   - Pantry duplicate check: add the same typed/photo item twice and confirm quantity merges.
   - Pantry barcode duplicate check: add the same UPC twice and confirm quantity merges, then add a different UPC and confirm it remains separate.
   - Pantry edit/review dialog for VERIFY items.
   - Pantry edit/review dialog comma-decimal and leading-decimal quantity correction such as `1,5` or `.5`.
   - Deals flyer photo.
   - Deals camera-permission denial, canceled capture/gallery/PDF status, and external camera/gallery/PDF launch-failure status if reproducible.
   - Deals gallery image.
   - Deals PDF.
   - Deals pasted OCR text.
   - Deals pasted OCR text with prices missing dollar signs.
   - Deals pasted OCR text with comma-decimal prices such as `2,99/lb` or `2 for 5,00`.
   - Deals bundled demo flyer text via pasted OCR.
   - Deals store field applies to photo, gallery, PDF, and pasted OCR imports.
   - Deals edit/review dialog for low-confidence OCR results.
   - Deals edit/review dialog comma-decimal and leading-decimal numeric correction such as price `2,99` or `.99`.
   - Receipts photo.
   - Receipts camera-permission denial, canceled capture/gallery/PDF status, and external camera/gallery/PDF launch-failure status if reproducible.
   - Receipts gallery image.
   - Receipts PDF.
   - Receipts pasted OCR text.
   - Receipts pasted OCR text with prices missing dollar signs.
   - Receipts pasted OCR text with comma-decimal prices such as `BLACK BEANS 1,78` and `2 @ 0,89`.
   - Receipts split quantity lines do not import as separate items.
   - Receipts one-line weighted produce rows parse item name, weight, and total.
   - Receipts subtotal/tax/total/payment lines do not import as items.
   - Receipts edit/review dialog for OCR and match corrections.
   - Receipts edit/review dialog comma-decimal and leading-decimal numeric correction such as total `1,78` or `.89`.
   - Receipts edit/delete budget total adjustment.
   - Budget daily envelope changes after receipt import, receipt total edit, and receipt delete.
   - Budget projected spend reflects current-month receipt history.
   - Budget Settings comma-decimal and leading-decimal save such as monthly budget `292,50` or breakfast cost `.55`, plus invalid text such as `abc`.
   - Receipts edit/delete pantry quantity adjustment for pantry matches.
   - Settings AI status before and after adding a real Gemini key.
   - Settings Test AI Connection before pantry AI photo testing.
   - Preflight/report APK Gemini configured/model fields after adding a real key and rebuilding.
   - Settings protein-per-meal comma-decimal and leading-decimal value such as `0,5` or `.5`.
   - Settings invalid protein-per-meal text such as `abc`; confirm Save is disabled and a visible format message appears.
   - Settings About build identity: version `1.0 (1)`, package `com.dealplanner`, debug build, and source branch/commit matching the preflight/report APK source identity without a dirty marker.
   - Generate meal plan.
   - Review shopping list.
   - After a generated plan exists, change pantry/deal/receipt/settings inputs and confirm Shopping refreshes without app relaunch.
   - Review budget.

After those pass, decide whether to polish current flows or add optional features such as nutrition lookup, guided multi-photo flyer capture, price history, and monetization/convenience features.

Latest continuation note after phone starter help refresh:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\start-phone-test-run.ps1 -Help
```

Result: help output now matches the real deterministic sample bundle: TXT, PDF, PNG, pantry-label, and UPC-A barcode samples. The starter help also states that sample transfer verifies the copy after placing files on the phone.

PowerShell helper parse check:

```powershell
scripts\start-phone-test-run.ps1
scripts\new-phone-test-samples.ps1
scripts\send-phone-test-samples.ps1
scripts\new-phone-test-report.ps1
```

Result: all listed helpers parsed successfully.

Full local gate:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`; `181` unit tests, `0` failures/errors/skipped, and lint reported `0` errors with `21` warnings.

Latest continuation note after barcode pantry mapper extraction:

Code checkpoint:

- Added `BarcodePantryMapper` as a pure mapping layer for Open Food Facts barcode lookup results.
- `AppViewModel` now uses the mapper for manual barcode/scanner pantry rows and phone-visible add/update status messages.
- Added direct unit coverage for found, not-found, and error barcode lookup results mapping into reviewable pantry rows.
- Added direct unit coverage for the add/update status strings shown after barcode intake.

Targeted lookup gate:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.dealplanner.lookup.*"
```

Result: `BUILD SUCCESSFUL`.

Full local gate:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`; `185` unit tests, `0` failures/errors/skipped, and lint reported `0` errors with `21` warnings.

Latest continuation note after manual input clear policy extraction:

Code checkpoint:

- Added `ManualInputClearPolicy` as a pure UI-state helper for manual input retention/clear decisions.
- Pantry barcode text now uses the shared policy: successful barcode import clears the field; `No barcode found.` stops waiting and keeps the entered text available for correction.
- Deals pasted flyer text now uses the shared policy: successful import clears the field; no-deal or processing failures keep the pasted text available.
- Receipts pasted receipt text now uses the shared policy: successful import clears the field; no-line-item or processing failures keep the pasted text available.
- Added direct unit coverage for barcode, flyer, and receipt manual input clear/retain decisions.

Targeted UI-state gate:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.dealplanner.ui.state.*"
```

Result: `BUILD SUCCESSFUL`.

Full local gate:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`; `193` unit tests, `0` failures/errors/skipped, and lint reported `0` errors with `21` warnings.

Latest continuation note after Settings input validator extraction:

Code checkpoint:

- Added `SettingsInputValidator` as a pure UI-state helper for Settings numeric validation.
- `ParamsScreen` now uses the shared validator for protein-per-meal parsing, helper/error text, and Save enablement.
- Added direct unit coverage for dot decimal, comma decimal, leading decimal, zero, negative, blank, invalid, and non-finite protein-per-meal text.

Targeted UI-state gate:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.dealplanner.ui.state.*"
```

Result: `BUILD SUCCESSFUL`.

Full local gate:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`; `197` unit tests, `0` failures/errors/skipped, and lint reported `0` errors with `21` warnings.

Latest continuation note after Budget input validator extraction:

Code checkpoint:

- Added `BudgetInputValidator` as a pure UI-state helper for Budget Settings numeric validation.
- `BudgetScreen` now uses the shared validator for monthly food budget, spent-to-date baseline, breakfast anchor cost parsing, visible error text, and Save enablement.
- Added direct unit coverage for dot decimal, comma decimal, leading decimal, zero, negative, blank, invalid, and non-finite Budget Settings text.

Targeted UI-state gate:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.dealplanner.ui.state.*"
```

Result: `BUILD SUCCESSFUL`.

Full local gate:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`; `201` unit tests, `0` failures/errors/skipped, and lint reported `0` errors with `21` warnings.

Latest continuation note after Pantry item input validator extraction:

Code checkpoint:

- Added `PantryItemInputValidator` as a pure UI-state helper for pantry edit quantity validation.
- `PantryItemEditDialog` now uses the shared validator for quantity parsing, visible error text, and Save enablement.
- Added direct unit coverage for dot decimal, comma decimal, leading decimal, zero, negative, blank, invalid, and non-finite pantry quantity text.

Targeted UI-state gate:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.dealplanner.ui.state.*"
```

Result: `BUILD SUCCESSFUL`.

Full local gate:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`; `205` unit tests, `0` failures/errors/skipped, and lint reported `0` errors with `21` warnings.

Latest continuation note after Deal item input validator extraction:

Code checkpoint:

- Added `DealItemInputValidator` as a pure UI-state helper for deal edit validation.
- `DealItemEditDialog` now uses the shared validator for price, limit, PPU, discount percent, deal score, confidence parsing, visible error text, and Save enablement.
- Added direct unit coverage for dot decimal, comma decimal, leading decimal, invalid, negative, and non-finite price values.
- Added direct unit coverage for blank/non-negative whole-number limits, invalid limits, PPU, 0-to-100 discount percent, and 0-to-1 score/confidence bounds.

Targeted UI-state gate:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.dealplanner.ui.state.*"
```

Result: `BUILD SUCCESSFUL`.

Full local gate:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`; `212` unit tests, `0` failures/errors/skipped, and lint reported `0` errors with `21` warnings.

Latest continuation note after Receipt item input validator extraction:

Code checkpoint:

- Added `ReceiptItemInputValidator` as a pure UI-state helper for receipt edit validation.
- `ReceiptItemEditDialog` now uses the shared validator for optional quantity, total cost, match ID, confidence, date parsing, visible error text, and Save enablement.
- Added direct unit coverage for blank optional quantity, dot decimal, comma decimal, leading decimal, zero, negative, blank required total, invalid, non-finite, match ID, confidence range, and ISO date text.

Targeted UI-state gate:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.dealplanner.ui.state.*"
```

Result: `BUILD SUCCESSFUL`.

Full local gate:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`; `220` unit tests, `0` failures/errors/skipped, and lint reported `0` errors with `21` warnings.

Latest continuation note after Receipt adjustment calculator extraction:

Code checkpoint:

- Added `ReceiptAdjustmentCalculator` as a pure domain helper for receipt edit/delete effects.
- `AppViewModel` now uses the helper for receipt Budget deltas and pantry-matched quantity deltas while preserving the existing receipt update/delete behavior.
- Added direct unit coverage for receipt Budget update/delete deltas, same-pantry net quantity edits, changed pantry matches, removed/added pantry matches, delete reversals, and ignored non-pantry or incomplete match metadata.
- Refreshed `PHONE_TEST_CHECKLIST_2026-07-16.md` source-of-truth wording so the phone run points at the current receipt-adjustment branch head instead of an older pantry checkpoint.

Targeted domain gate:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.dealplanner.domain.*"
```

Result: `BUILD SUCCESSFUL`.

Full local gate:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`; `228` unit tests, `0` failures/errors/skipped, and lint reported `0` errors with `21` warnings.

Latest continuation note after Gemini connection-test seam:

Code checkpoint:

- Added a narrow `GeminiContentTransport` test seam to `GeminiPantryVisionClient` so Settings -> Test AI Connection result handling can be tested without live network calls.
- Production behavior still uses the same Gemini REST endpoint and request body when no test transport is supplied.
- Added direct unit coverage for connection-test success, empty Gemini response, trimmed key/model values passed to transport, missing-key handling, and concise transport/API failure status text.

Targeted Gemini gate:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.dealplanner.ai.GeminiPantryVisionClientTest"
```

Result: `BUILD SUCCESSFUL`.

Full local gate:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`; `231` unit tests, `0` failures/errors/skipped, and lint reported `0` errors with `21` warnings.

Latest helper checkpoint after report sample-manifest evidence work:

Helper checkpoint:

- `scripts\new-phone-test-report.ps1` now includes the latest local sample folder and `SAMPLE_MANIFEST.md` path in the report Source Snapshot when available.
- Generated reports now include a deterministic-text checklist row for verifying the sample manifest before transfer.
- Generated ignored report `phone-test-results\20260716-152859\PHONE_TEST_REPORT.md` confirmed the Source Snapshot includes repo/APK identity, Gemini fields, latest sample folder, and sample manifest path.
- Updated README, PROJECT_SUMMARY, and PHONE_TEST_CHECKLIST to describe report sample-manifest evidence.

Helper checks:

```powershell
powershell -NoProfile -Command "`$null = [scriptblock]::Create((Get-Content -Raw -LiteralPath 'scripts\new-phone-test-report.ps1')); 'new-phone-test-report.ps1 parsed'"
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\new-phone-test-report.ps1 -Help
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\new-phone-test-report.ps1
```

Result: PowerShell parse check passed, help printed successfully, report generation succeeded, and the generated report includes `Phone test sample manifest: C:\Users\adamc\AndroidStudioProjects\Deal_Planner\phone-test-samples\20260716-152321\SAMPLE_MANIFEST.md`.

Full local gate:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`; `231` unit tests, `0` failures/errors/skipped, and lint reported `0` errors with `21` warnings.

Latest helper checkpoint after phone sample manifest verification work:

Helper checkpoint:

- `scripts\new-phone-test-samples.ps1` now verifies required sample files after generation and writes `SAMPLE_MANIFEST.md` with byte counts and SHA-256 hashes.
- Added `-VerifyOnly` and `-SamplesDir` support for local sample-bundle verification before an Android phone is connected.
- Generated and verified local sample folder `phone-test-samples\20260716-152321`, including receipt/flyer TXT/PDF/PNG files, pantry-label TXT/PNG files, UPC-A TXT/PNG files, README, and manifest.
- Updated README, PROJECT_SUMMARY, and PHONE_TEST_CHECKLIST with the manifest and `-VerifyOnly` workflow.

Helper checks:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\new-phone-test-samples.ps1 -Help
powershell -NoProfile -Command "`$null = [scriptblock]::Create((Get-Content -Raw -LiteralPath 'scripts\new-phone-test-samples.ps1')); 'new-phone-test-samples.ps1 parsed'"
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\new-phone-test-samples.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\new-phone-test-samples.ps1 -VerifyOnly
```

Result: help printed successfully, PowerShell parse check passed, sample generation succeeded, and `-VerifyOnly` verified the latest local sample bundle plus manifest.

Full local gate:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`; `231` unit tests, `0` failures/errors/skipped, and lint reported `0` errors with `21` warnings.

Latest helper checkpoint after sample transfer manifest requirement work:

Helper checkpoint:

- `scripts\send-phone-test-samples.ps1` now requires `SAMPLE_MANIFEST.md` in the local sample folder before copying files to a phone.
- The transfer helper help text now states that sample folders must include the manifest generated by `new-phone-test-samples.ps1`.
- README, PROJECT_SUMMARY, and PHONE_TEST_CHECKLIST were updated so phone setup expects manifest-backed sample transfer evidence.

Helper checks:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\send-phone-test-samples.ps1 -Help
powershell -NoProfile -Command "`$null = [scriptblock]::Create((Get-Content -Raw -LiteralPath 'scripts\send-phone-test-samples.ps1')); 'send-phone-test-samples.ps1 parsed'"
.\scripts\send-phone-test-samples.ps1 -SamplesDir phone-test-samples\20260716-152321
```

Result: help printed successfully, PowerShell parse check passed, and the transfer helper accepted the manifest-backed local sample folder before stopping at the expected no connected/authorized Android phone condition.

Full local gate:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`; `231` unit tests, `0` failures/errors/skipped, and lint reported `0` errors with `21` warnings.

Latest helper checkpoint after sample transfer receipt/report work:

Helper checkpoint:

- `scripts\send-phone-test-samples.ps1` now writes ignored `PHONE_SAMPLE_TRANSFER.md` evidence inside the transferred sample folder after remote byte-size verification succeeds.
- The transfer receipt records the device serial, local sample folder, Android destination, remote byte-size verification status, media-scan request status, and each verified file size.
- `scripts\new-phone-test-report.ps1` now includes the expected Android sample destination and latest transfer receipt path/status in the report Source Snapshot.
- Generated phone reports now include a checkbox for confirming `PHONE_SAMPLE_TRANSFER.md` captured destination and byte-size evidence.
- README, PROJECT_SUMMARY, and PHONE_TEST_CHECKLIST were updated so the real-phone run expects transfer receipt evidence when sample transfer is used.

Helper checks:

```powershell
powershell -NoProfile -Command "`$null = [scriptblock]::Create((Get-Content -Raw -LiteralPath 'scripts\send-phone-test-samples.ps1')); 'send-phone-test-samples.ps1 parsed'"
powershell -NoProfile -Command "`$null = [scriptblock]::Create((Get-Content -Raw -LiteralPath 'scripts\new-phone-test-report.ps1')); 'new-phone-test-report.ps1 parsed'"
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\send-phone-test-samples.ps1 -Help
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\new-phone-test-report.ps1
.\scripts\send-phone-test-samples.ps1 -SamplesDir phone-test-samples\20260716-152321
```

Result: PowerShell parse checks passed for both helper scripts, transfer helper help printed successfully, generated ignored report `phone-test-results\20260716-154217\PHONE_TEST_REPORT.md` includes the sample Android destination and missing-transfer-receipt status, and the transfer helper accepted the manifest-backed local sample folder before stopping at the expected no connected/authorized Android phone condition.

Full local gate:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`; `231` unit tests, `0` failures/errors/skipped, and lint reported `0` errors with `21` warnings.

Latest helper checkpoint after phone starter failure-report work:

Helper checkpoint:

- `scripts\start-phone-test-run.ps1` now creates a failure-state phone-test report when setup stops before the normal final report step, unless `-SkipReport` was explicitly used.
- The starter keeps the original failure as the terminating error after attempting the report, so automation still sees setup as failed.
- README, PROJECT_SUMMARY, and PHONE_TEST_CHECKLIST were updated so phone setup expects a report even after partial setup failures.

Helper checks:

```powershell
powershell -NoProfile -Command "`$null = [scriptblock]::Create((Get-Content -Raw -LiteralPath 'scripts\start-phone-test-run.ps1')); 'start-phone-test-run.ps1 parsed'"
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\start-phone-test-run.ps1 -Help
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\start-phone-test-run.ps1 -SkipNetwork
```

Result: PowerShell parse check passed, help output documented failure-state report behavior, and the expected no-phone run failed at required-phone preflight while creating ignored failure-state report `phone-test-results\20260716-154940\PHONE_TEST_REPORT.md` with current repo/APK identity, dirty tracked paths, no selected device, and latest sample destination details.

Full local gate:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`; `231` unit tests, `0` failures/errors/skipped, and lint reported `0` errors with `21` warnings.

Latest helper checkpoint after phone report setup-status work:

Helper checkpoint:

- `scripts\new-phone-test-report.ps1` now includes a `Setup Run Summary` section with setup status and setup failure reason when provided.
- `scripts\start-phone-test-run.ps1` now stamps successful starter-created reports as `Completed` and failure-state reports as `Failed` with the original stopping reason.
- README, PROJECT_SUMMARY, and PHONE_TEST_CHECKLIST were updated so phone setup/report evidence includes setup status.

Helper checks:

```powershell
powershell -NoProfile -Command "`$null = [scriptblock]::Create((Get-Content -Raw -LiteralPath 'scripts\new-phone-test-report.ps1')); 'new-phone-test-report.ps1 parsed'"
powershell -NoProfile -Command "`$null = [scriptblock]::Create((Get-Content -Raw -LiteralPath 'scripts\start-phone-test-run.ps1')); 'start-phone-test-run.ps1 parsed'"
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\new-phone-test-report.ps1 -SetupStatus Manual -SetupFailure "manual verification"
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\start-phone-test-run.ps1 -SkipNetwork
```

Result: PowerShell parse checks passed for both helper scripts, report help documented `SetupStatus`/`SetupFailure`, manual report `phone-test-results\20260716-155539\PHONE_TEST_REPORT.md` included `Setup status: Manual` and `Setup failure: manual verification`, and the expected no-phone starter run created failure report `phone-test-results\20260716-155600\PHONE_TEST_REPORT.md` with `Setup status: Failed` plus `Setup failure: Phone preflight failed with exit code 1.`

Full local gate:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`; `231` unit tests, `0` failures/errors/skipped, and lint reported `0` errors with `21` warnings.

Latest helper checkpoint after sample transfer manifest-hash verification work:

Helper checkpoint:

- `scripts\send-phone-test-samples.ps1` now verifies `SAMPLE_MANIFEST.md` byte counts and SHA-256 hashes before requesting an Android device.
- The transfer helper fails before ADB if the manifest is missing entries, lists missing files, or no longer matches local sample file size/hash values.
- README, PROJECT_SUMMARY, and PHONE_TEST_CHECKLIST were updated so phone sample transfer evidence is manifest-hash backed.

Helper checks:

```powershell
powershell -NoProfile -Command "`$null = [scriptblock]::Create((Get-Content -Raw -LiteralPath 'scripts\send-phone-test-samples.ps1')); 'send-phone-test-samples.ps1 parsed'"
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\send-phone-test-samples.ps1 -Help
.\scripts\send-phone-test-samples.ps1 -SamplesDir phone-test-samples\20260716-152321
```

Result: PowerShell parse check passed, help output documented manifest byte-count/SHA-256 verification, and `send-phone-test-samples.ps1 -SamplesDir phone-test-samples\20260716-152321` printed `Verified sample manifest hashes` before stopping at the expected no connected/authorized Android phone condition.

Full local gate:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`; `231` unit tests, `0` failures/errors/skipped, and lint reported `0` errors with `21` warnings.

Latest app-code checkpoint after pantry label punctuation and AI brand normalization work:

App-code checkpoint:

- `PantryPhraseParser` now trims colon and semicolon punctuation from parsed tokens, so common OCR/manual label cues such as `net wt:`, `best by:`, `opened:`, and `exp:` do not leak cue words into pantry item names.
- `PantryVisionItemMapper` now treats punctuated Gemini brand text such as `Generic:` or `Unknown.` the same as missing/generic brand details, saving `Generic`, requiring VERIFY, and adding `Review brand.`.
- README, PROJECT_SUMMARY, and PHONE_TEST_CHECKLIST were updated to record the punctuation-heavy label behavior and the AI review expectation.

Focused app checks:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.dealplanner.parser.PantryPhraseParserTest" --tests "com.dealplanner.ai.PantryVisionItemMapperTest"
```

Result: `BUILD SUCCESSFUL`; targeted pantry parser and AI pantry mapping tests passed.

Full local gate:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`; `232` unit tests, `0` failures/errors/skipped, and lint reported `0` errors with `21` warnings.

Latest app-code checkpoint after receipt negative-return filtering work:

App-code checkpoint:

- `ReceiptReconciler` now ignores receipt lines with negative amount forms such as `MILK -$1.99`, `EGGS $-2.49`, `APPLES -1,25`, and parenthesized return amounts like `BREAD (3.29)`.
- This prevents returned/refunded grocery rows from being parsed as positive purchases and inflating Budget spending.
- README, PROJECT_SUMMARY, and PHONE_TEST_CHECKLIST were updated with the negative return/refund receipt behavior.

Focused app check:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.dealplanner.domain.ReceiptReconcilerTest"
```

Result: `BUILD SUCCESSFUL`; targeted receipt reconciliation tests passed.

Full local gate:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`; `233` unit tests, `0` failures/errors/skipped, and lint reported `0` errors with `21` warnings.

Latest app-code checkpoint after flexible review date input work:

App-code checkpoint:

- Pantry, Deal, and Receipt review/edit date parsing now uses the shared flexible date parser.
- Pantry best-by, deal valid-until, and receipt date corrections accept common formats such as `12/31/2026`, `12-31-26`, and year-first slash text.
- README, PROJECT_SUMMARY, and PHONE_TEST_CHECKLIST were updated with the flexible review-date behavior for phone testing.

Focused app check:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.dealplanner.ui.state.ReceiptItemInputValidatorTest"
```

Result: `BUILD SUCCESSFUL`; targeted receipt edit validation tests passed.

Full local gate:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`; `233` unit tests, `0` failures/errors/skipped, and lint reported `0` errors with `21` warnings.

Latest app-code checkpoint after pantry/deal review-date validator coverage work:

App-code checkpoint:

- Pantry best-by and Deal valid-until review/edit date checks now live in `PantryItemInputValidator` and `DealItemInputValidator`.
- Pantry and Deal review dialogs use those validators for parsed save values, save enablement, and visible flexible-date error text.
- Added direct unit coverage for blank optional dates, ISO dates, slash dates, two-digit dash dates, year-first slash dates, and invalid text.
- README, PROJECT_SUMMARY, and PHONE_TEST_CHECKLIST were updated with the validator-backed review-date behavior.

Focused app check:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.dealplanner.ui.state.PantryItemInputValidatorTest" --tests "com.dealplanner.ui.state.DealItemInputValidatorTest"
```

Result: `BUILD SUCCESSFUL`; targeted pantry and deal edit validation tests passed.

Full local gate:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`; `237` unit tests, `0` failures/errors/skipped, and lint reported `0` errors with `21` warnings.

Latest helper checkpoint after richer pantry-label phone sample work:

Helper checkpoint:

- `scripts\new-phone-test-samples.ps1` now generates pantry-label TXT/PNG samples with hyphenated package-size rows, punctuated label cues such as `net wt:` and `best by:`, slash dates such as `12/31/2026`, and two-digit dash dates such as `12-31-26`.
- `new-phone-test-samples.ps1 -VerifyOnly` now checks that the generated pantry-label text still contains the phone-test coverage strings before writing the manifest.
- Generated and verified ignored sample folder: `phone-test-samples\20260716-163910`.
- README, PROJECT_SUMMARY, and PHONE_TEST_CHECKLIST were updated so the real phone run expects those richer pantry-label sample cases.

Helper checks:

```powershell
powershell -NoProfile -Command '$null = [scriptblock]::Create((Get-Content -Raw -LiteralPath "scripts\new-phone-test-samples.ps1")); "new-phone-test-samples.ps1 parsed"'
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\new-phone-test-samples.ps1 -Help
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\new-phone-test-samples.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\new-phone-test-samples.ps1 -VerifyOnly -SamplesDir phone-test-samples\20260716-163910
```

Result: parse check passed, help printed, sample generation succeeded, `SAMPLE_MANIFEST.md` was written, the new pantry-label text includes `net wt:`, `best by:`, `12/31/2026`, `12-31-26`, `16-ounce`, and `12-count`, and `-VerifyOnly` accepted the generated folder.

Full local gate:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`; `237` unit tests, `0` failures/errors/skipped, and lint reported `0` errors with `21` warnings.

Latest resource checkpoint after monochrome launcher icon work:

Resource checkpoint:

- Added `app\src\main\res\drawable\ic_launcher_monochrome.xml`.
- Wired the monochrome icon into `ic_launcher.xml` and `ic_launcher_round.xml` so Android launchers that support themed icons have a monochrome asset.
- README, PROJECT_SUMMARY, and PHONE_TEST_CHECKLIST were updated with the current launcher-icon checkpoint.

Focused lint check:

```powershell
.\gradlew.bat lintDebug
```

Result: `BUILD SUCCESSFUL`; lint reported `0` errors with `19` warnings. The previous two `MonochromeLauncherIcon` warnings are gone; remaining warnings are dependency/SDK drift (`GradleDependency`, `OldTargetApi`, `ObsoleteSdkInt`).

Full local gate:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`; `237` unit tests, `0` failures/errors/skipped, and lint reported `0` errors with `19` warnings.

Latest helper checkpoint after phone report lint snapshot work:

Helper checkpoint:

- `scripts\new-phone-test-report.ps1` now reads `app\build\reports\lint-results-debug.xml` when available.
- Generated phone-test reports now include a `Lint snapshot` Source Snapshot line and a dedicated `Lint Snapshot` section with the lint report path, error/warning count, and issue-id counts.
- README, PROJECT_SUMMARY, and PHONE_TEST_CHECKLIST were updated so phone-test evidence expectations include the lint snapshot.

Helper checks:

```powershell
powershell -NoProfile -Command '$null = [scriptblock]::Create((Get-Content -Raw -LiteralPath "scripts\new-phone-test-report.ps1")); "new-phone-test-report.ps1 parsed"'
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\new-phone-test-report.ps1 -Help
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\new-phone-test-report.ps1 -SetupStatus "Preflight only" -SetupFailure "Phone not connected and Gemini key not configured yet"
```

Result: parse check passed, help printed with lint snapshot coverage, and generated ignored report `phone-test-results\20260716-165231\PHONE_TEST_REPORT.md` included `Lint snapshot: 0 error(s), 19 warning(s)` plus a `Lint Snapshot` section listing `GradleDependency: 17`, `ObsoleteSdkInt: 1`, and `OldTargetApi: 1`.

Full local gate:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`; `237` unit tests, `0` failures/errors/skipped, and lint reported `0` errors with `19` warnings.

Latest helper checkpoint after ADB setup guidance work:

Helper checkpoint:

- `scripts\phone-debug-preflight.ps1` now includes the visible `adb devices` state in the Android phone check and gives different recovery guidance for no-device versus unauthorized/offline phone states.
- `scripts\phone-debug-install.ps1` now uses the same concrete recovery wording when install is run directly.
- README, PROJECT_SUMMARY, and PHONE_TEST_CHECKLIST were updated so the next phone run knows what the expected no-phone/unauthorized guidance looks like.

Helper checks:

```powershell
powershell -NoProfile -Command '$null = [scriptblock]::Create((Get-Content -Raw -LiteralPath "scripts\phone-debug-preflight.ps1")); "phone-debug-preflight.ps1 parsed"'
powershell -NoProfile -Command '$null = [scriptblock]::Create((Get-Content -Raw -LiteralPath "scripts\phone-debug-install.ps1")); "phone-debug-install.ps1 parsed"'
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\phone-debug-preflight.ps1 -Help
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\phone-debug-preflight.ps1 -RequirePhone -SkipNetwork
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\phone-debug-install.ps1 -SkipBuild -NoLaunch
```

Result: parse and help checks passed. In the current no-phone state, required-phone preflight failed as expected and now reports: `No connected/authorized phone found. Connect the phone, enable Developer options > USB debugging, choose a data-capable USB mode/cable, confirm adb devices shows device, then rerun. adb devices listed no devices.` Direct install also verified APK identity/source metadata before stopping with the same concrete ADB recovery guidance.

Full local gate:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-20'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`; `237` unit tests, `0` failures/errors/skipped, and lint reported `0` errors with `19` warnings.

Latest recovery checkpoint after feature readiness report work:

Helper checkpoint:

- Added `scripts\new-feature-readiness-report.ps1`.
- The helper creates ignored timestamped `FEATURE_READINESS_REPORT.md` files under `phone-test-results\`.
- The report separates local source/test evidence from remaining external checks for pantry manual/barcode/photo/gallery, flyer text/photo/gallery/PDF, receipt text/photo/gallery/PDF, budget/menu/shopping, Settings, Gemini, phone setup helpers, and deterministic samples.
- README, PROJECT_SUMMARY, and PHONE_TEST_CHECKLIST were updated so the feature readiness report becomes a recovery anchor before real-phone testing.

Helper checks:

```powershell
powershell -NoProfile -Command '$null = [scriptblock]::Create((Get-Content -Raw -LiteralPath "scripts\new-feature-readiness-report.ps1")); "new-feature-readiness-report.ps1 parsed"'
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\new-feature-readiness-report.ps1 -Help
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\new-feature-readiness-report.ps1
```

Result: parse and help checks passed. The helper generated ignored report `phone-test-results\20260716-170930\FEATURE_READINESS_REPORT.md`, showing `237` unit tests, `0` failures/errors/skipped, lint `0` errors with `19` warnings, APK source identity `codex/deal-planner-baseline @ 60a8c23`, APK Gemini configured `False`, and all remaining feature signoff items correctly marked as Android phone or real-Gemini-key checks.

Full local gate:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-20'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result: `BUILD SUCCESSFUL`; `237` unit tests, `0` failures/errors/skipped, and lint reported `0` errors with `19` warnings.

Latest recovery checkpoint after Gemini 3.5 request-defaults work:

AI checkpoint:

- Verified current official Google AI documentation lists `gemini-3.5-flash` as the stable Gemini 3.5 Flash model code with image input and structured output support.
- Removed hardcoded Gemini `temperature` fields from pantry photo extraction and Settings connection-test requests so Gemini 3.x sampling defaults are used.
- Added unit coverage that pantry photo requests still ask for JSON output and connection-test requests still cap output, while neither request overrides `temperature`, `topP`, or `topK`.
- README, PROJECT_SUMMARY, PHONE_TEST_CHECKLIST, and `local.properties.example` were updated with the stable model/default-sampling behavior.

Focused AI gate:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-20'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest --tests "com.dealplanner.ai.GeminiPantryVisionClientTest"
```

Result: `BUILD SUCCESSFUL`.

Full local gate:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-20'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Result before commit: `BUILD SUCCESSFUL`; `239` unit tests, `0` failures/errors/skipped, and lint reported `0` errors with `19` warnings. The dirty-state readiness report `phone-test-results\20260716-172819\FEATURE_READINESS_REPORT.md` correctly refused phone signoff because generated debug `BuildConfig` showed `APK source dirty: true` while tracked changes were still uncommitted.

Latest recovery checkpoint after Deal Planner naming readiness audit work:

Naming checkpoint:

- Added a Deal Planner naming-transition audit to `scripts\new-feature-readiness-report.ps1`.
- The audit verifies the app label, Gradle application ID, Gradle namespace, and root project name are all on Deal Planner / `com.dealplanner`.
- The audit scans active app/docs/scripts for exact old app-name strings while allowing normal SNAP/EBT/WIC benefit wording.
- Added a readiness-matrix row and phone checklist item for confirming no legacy SNAP-era app naming appears on-device.
- README, PROJECT_SUMMARY, PHONE_TEST_CHECKLIST, and this recovery log were updated to make the naming audit part of the recovery baseline.

Helper checks:

```powershell
powershell -NoProfile -Command '$null = [scriptblock]::Create((Get-Content -Raw -LiteralPath "scripts\new-feature-readiness-report.ps1")); "new-feature-readiness-report.ps1 parsed"'
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\new-feature-readiness-report.ps1 -Help
rg -n --hidden -S "SNAP Optimizer|SNAP_Optimizer|snap_optimizer|SNAP SHOPPER" scripts README.md PHONE_TEST_CHECKLIST_2026-07-16.md PROJECT_SUMMARY.md app\src settings.gradle.kts app\build.gradle.kts local.properties.example -g "!app/build"
git diff --check
```

Result: parse and help checks passed. The exact old app-name scan returned no active matches. `git diff --check` passed.

Full readiness gate before commit:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\new-feature-readiness-report.ps1 -RunGate
```

Result before commit: `BUILD SUCCESSFUL`; `239` unit tests, `0` failures/errors/skipped, and lint reported `0` errors with `19` warnings. The dirty-state readiness report `phone-test-results\20260716-173828\FEATURE_READINESS_REPORT.md` showed `Naming audit: OK` and correctly refused phone signoff because generated debug `BuildConfig` showed `APK source dirty: true` while tracked changes were still uncommitted.

Latest recovery checkpoint after feature readiness freshness wording work:

Readiness evidence checkpoint:

- Updated `scripts\new-feature-readiness-report.ps1` so a successful `-RunGate` is treated as current unit-test and lint evidence even when Gradle reuses cached XML/report files whose timestamps did not move.
- The report still shows the raw file freshness for transparency.
- APK freshness and APK source identity remain file/BuildConfig based before phone testing.
- README, PROJECT_SUMMARY, PHONE_TEST_CHECKLIST, and this recovery log were updated so the cached-report behavior is documented.

Helper checks:

```powershell
powershell -NoProfile -Command '$null = [scriptblock]::Create((Get-Content -Raw -LiteralPath "scripts\new-feature-readiness-report.ps1")); "new-feature-readiness-report.ps1 parsed"'
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\new-feature-readiness-report.ps1 -Help
git diff --check
```

Result: parse and help checks passed. `git diff --check` passed.

Full readiness gate before commit:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\new-feature-readiness-report.ps1 -RunGate
```

Result before commit: `BUILD SUCCESSFUL`; `239` unit tests, `0` failures/errors/skipped, and lint reported `0` errors with `19` warnings. The dirty-state readiness report `phone-test-results\20260716-174315\FEATURE_READINESS_REPORT.md` now marks `Unit test freshness` and `Lint freshness` current via the successful `-RunGate`, while still showing raw report-file freshness. It correctly refused phone signoff because generated debug `BuildConfig` showed `APK source dirty: true` while tracked changes were still uncommitted.

Latest recovery checkpoint after strict AI phone setup reporting work:

Phone/AI evidence checkpoint:

- `scripts\start-phone-test-run.ps1` now passes a non-secret setup mode into generated phone-test reports.
- `scripts\new-phone-test-report.ps1` now records `Setup mode` in Setup Run Summary.
- The report AI checklist now expects the final AI pass to use `.\scripts\start-phone-test-run.ps1 -RequireGemini` and to show `RequireGemini=True`.
- README, PROJECT_SUMMARY, PHONE_TEST_CHECKLIST, and this recovery log were updated so the final AI phone pass uses the same one-command path that generates the evidence report.

Helper checks:

```powershell
powershell -NoProfile -Command '$null = [scriptblock]::Create((Get-Content -Raw -LiteralPath "scripts\start-phone-test-run.ps1")); "start-phone-test-run.ps1 parsed"'
powershell -NoProfile -Command '$null = [scriptblock]::Create((Get-Content -Raw -LiteralPath "scripts\new-phone-test-report.ps1")); "new-phone-test-report.ps1 parsed"'
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\start-phone-test-run.ps1 -Help
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\new-phone-test-report.ps1 -Help
git diff --check
```

Result: parse and help checks passed. `git diff --check` passed.

Report rendering check:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\new-phone-test-report.ps1 -SetupStatus "Preflight only" -SetupMode "RequireGemini=True; SkipNetwork=True; SkipBuild=False; NoLaunch=False; SkipSamples=False"
```

Result: generated ignored report `phone-test-results\20260716-174816\PHONE_TEST_REPORT.md`; Setup Run Summary showed the supplied setup mode and AI Verification included the strict starter checklist rows.

Strict starter failure-state check:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\start-phone-test-run.ps1 -RequireGemini -SkipNetwork
```

Result: intentionally failed before phone setup because no Android phone is connected and no real Gemini key is configured. The helper still generated ignored failure-state report `phone-test-results\20260716-174832\PHONE_TEST_REPORT.md` with `Setup status: Failed`, `Setup failure: Phone preflight failed with exit code 1.`, and `Setup mode: RequireGemini=True; SkipNetwork=True; SkipBuild=False; NoLaunch=False; SkipSamples=False`.

Full readiness gate before commit:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\new-feature-readiness-report.ps1 -RunGate
```

Result before commit: `BUILD SUCCESSFUL`; `239` unit tests, `0` failures/errors/skipped, and lint reported `0` errors with `19` warnings. The dirty-state readiness report `phone-test-results\20260716-174940\FEATURE_READINESS_REPORT.md` correctly refused phone signoff because generated debug `BuildConfig` showed `APK source dirty: true` while tracked changes were still uncommitted.

Latest recovery checkpoint after capped PDF status work:

Input-method checkpoint:

- Flyer and receipt PDF imports still render a bounded first page set for phone reliability.
- If a PDF has more than the supported page cap, the final flyer/receipt status now names the processed range, such as `first 12 of 20 PDF pages`, instead of silently implying the whole PDF was scanned.
- Receipt PDF blank/no-line-item statuses also include the capped page range when applicable.
- The phone checklist and generated phone-test report now include explicit capped-PDF status evidence.
- README, PROJECT_SUMMARY, PHONE_TEST_CHECKLIST, and this recovery log were updated with the capped-page behavior.

Full readiness gate before commit:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\new-feature-readiness-report.ps1 -RunGate
```

Result before commit: `BUILD SUCCESSFUL`; `239` unit tests, `0` failures/errors/skipped, and lint reported `0` errors with `19` warnings. The dirty-state readiness report `phone-test-results\20260716-175552\FEATURE_READINESS_REPORT.md` correctly refused phone signoff because generated debug `BuildConfig` showed `APK source dirty: true` while tracked changes were still uncommitted.

Latest recovery checkpoint after shopping list PDF export work:

Shopping checkpoint:

- The Shopping tab now shows `Export PDF` once a generated shopping list exists.
- Export writes a generated shopping-list PDF into app cache through the app FileProvider and opens Android's share sheet without broad storage/media-library permissions.
- The PDF contents include generated date, estimated total, quantity/unit, item name, item cost, store, brand/size, price-per-unit, deal type, coupon, limit, and meal-purpose details.
- `ShoppingListExportFormatterTest` covers the export report contents and defaults for missing optional deal details.
- The feature readiness report and generated phone-test report now include explicit Shopping PDF export/share-sheet evidence.
- README, PROJECT_SUMMARY, PHONE_TEST_CHECKLIST, and this recovery log were updated with the Shopping export behavior.

Focused formatter check:

```powershell
.\gradlew.bat testDebugUnitTest --tests com.dealplanner.domain.ShoppingListExportFormatterTest
```

Result: `BUILD SUCCESSFUL`.

Full readiness gate before commit:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\new-feature-readiness-report.ps1 -RunGate
```

Result before commit: `BUILD SUCCESSFUL`; `241` unit tests, `0` failures/errors/skipped, and lint reported `0` errors with `19` warnings. The dirty-state readiness report `phone-test-results\20260716-180653\FEATURE_READINESS_REPORT.md` correctly refused phone signoff because generated debug `BuildConfig` showed `APK source dirty: true` while tracked changes were still uncommitted.

Latest recovery checkpoint after pantry photo review queue work:

Pantry input checkpoint:

- Pantry Photo/Gallery imports now stage OCR/Gemini items in a `Review Pantry Imports` section instead of immediately merging them into saved Pantry.
- Each pending photo item can be edited with the existing pantry review dialog or removed before saving.
- `Save All` is required before pending photo items upsert/merge into saved Pantry and refresh Shopping.
- Multiple photo/Gallery imports append to the pending review queue instead of overwriting existing pending rows.
- The pending review list is bounded and scrollable so multi-item shelf scans do not take over the whole Pantry screen.
- The feature readiness report and generated phone-test report now include explicit pending review and `Save All` evidence for pantry photo/Gallery checks.
- README, PROJECT_SUMMARY, PHONE_TEST_CHECKLIST, and this recovery log were updated with the edit-before-save behavior.

Focused review queue check:

```powershell
.\gradlew.bat testDebugUnitTest --tests com.dealplanner.ui.state.PantryImportReviewQueueTest
```

Result: `BUILD SUCCESSFUL`.

Full readiness gate before commit:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\new-feature-readiness-report.ps1 -RunGate
```

Result before commit: `BUILD SUCCESSFUL`; `246` unit tests, `0` failures/errors/skipped, and lint reported `0` errors with `19` warnings. The dirty-state readiness report `phone-test-results\20260716-181609\FEATURE_READINESS_REPORT.md` correctly refused phone signoff because generated debug `BuildConfig` showed `APK source dirty: true` while tracked changes were still uncommitted.

Latest recovery checkpoint after custom dietary restrictions work:

Settings and meal-planning checkpoint:

- Settings now has a `Custom Avoid List` field for comma/semicolon/newline-separated terms such as `pork, shellfish, peanuts`.
- Restriction text is normalized before saving, including natural prefixes such as `no pork`, `avoid shellfish`, and `exclude pork`.
- Meal planning filters matching protein and side deals out of generated Menu meals and Shopping.
- Settings save now preserves the existing `Params` row instead of recreating unrelated params fields.
- The phone checklist and generated phone-test report now include a custom avoid-list Menu/Shopping check.
- README, PROJECT_SUMMARY, PHONE_TEST_CHECKLIST, and this recovery log were updated with the custom restriction behavior.

Focused custom restriction checks:

```powershell
.\gradlew.bat testDebugUnitTest --tests com.dealplanner.util.DietaryRestrictionsTest --tests com.dealplanner.ui.state.SettingsInputValidatorTest --tests com.dealplanner.domain.MealPlanningEngineTest
```

Result: `BUILD SUCCESSFUL`.

Full readiness gate before commit:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\new-feature-readiness-report.ps1 -RunGate
```

Result before commit: `BUILD SUCCESSFUL`; `256` unit tests, `0` failures/errors/skipped, and lint reported `0` errors with `19` warnings. The dirty-state readiness report `phone-test-results\20260716-182456\FEATURE_READINESS_REPORT.md` correctly refused phone signoff because generated debug `BuildConfig` showed `APK source dirty: true` while tracked changes were still uncommitted.

Latest recovery checkpoint after live Gemini preflight helper work:

AI setup checkpoint:

- Added `scripts\test-gemini-connection.ps1` for a short local live Gemini API check before the phone AI pass.
- The helper reads `local.properties` or `GEMINI_API_KEY`, prints the model and key source, and never prints the key value.
- Added `-TestGeminiLive` to `scripts\phone-debug-preflight.ps1` and `scripts\start-phone-test-run.ps1` so the final AI setup can require both compiled APK Gemini readiness and a real API response.
- The normal no-key/OCR-fallback phone path still works without the live API check.
- README, PROJECT_SUMMARY, PHONE_TEST_CHECKLIST, generated phone reports, and feature readiness reports were updated so the final AI phone pass is `.\scripts\start-phone-test-run.ps1 -RequireGemini -TestGeminiLive`.

Helper checks:

```powershell
powershell -NoProfile -Command '$content = Get-Content -Raw -LiteralPath "scripts\test-gemini-connection.ps1"; $null = [scriptblock]::Create($content); "test-gemini-connection.ps1 parsed"'
powershell -NoProfile -Command '$content = Get-Content -Raw -LiteralPath "scripts\phone-debug-preflight.ps1"; $null = [scriptblock]::Create($content); "phone-debug-preflight.ps1 parsed"'
powershell -NoProfile -Command '$content = Get-Content -Raw -LiteralPath "scripts\start-phone-test-run.ps1"; $null = [scriptblock]::Create($content); "start-phone-test-run.ps1 parsed"'
powershell -NoProfile -Command '$content = Get-Content -Raw -LiteralPath "scripts\new-phone-test-report.ps1"; $null = [scriptblock]::Create($content); "new-phone-test-report.ps1 parsed"'
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-gemini-connection.ps1 -Help
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\start-phone-test-run.ps1 -Help
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-gemini-connection.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\phone-debug-preflight.ps1 -TestGeminiLive -SkipNetwork
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\phone-debug-preflight.ps1 -TestGeminiLive
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\new-phone-test-report.ps1 -SetupStatus "Preflight only" -SetupMode "RequireGemini=True; TestGeminiLive=True; SkipNetwork=False; SkipBuild=False; NoLaunch=False; SkipSamples=False"
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\new-feature-readiness-report.ps1
git diff --check
```

Result: parse and help checks passed. The no-key local Gemini script intentionally exited `1` with `No non-placeholder Gemini key found` and did not print a key. Preflight with `-TestGeminiLive -SkipNetwork` exited `0` and marked `Gemini live API` as skipped. Preflight with `-TestGeminiLive` intentionally exited `1` in the current no-key state and marked `Gemini live API` as failed before any phone setup. Generated ignored report `phone-test-results\20260716-183451\PHONE_TEST_REPORT.md` included the new `TestGeminiLive=True` setup-mode rows, and generated ignored report `phone-test-results\20260716-183451\FEATURE_READINESS_REPORT.md` included `scripts\test-gemini-connection.ps1` in the Settings/Gemini evidence list. `git diff --check` passed with line-ending warnings only.

Full readiness gate before commit:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\new-feature-readiness-report.ps1 -RunGate
```

Result before commit: `BUILD SUCCESSFUL`; `256` unit tests, `0` failures/errors/skipped, and lint reported `0` errors with `19` warnings. The dirty-state readiness report `phone-test-results\20260716-183536\FEATURE_READINESS_REPORT.md` correctly refused phone signoff because generated debug `BuildConfig` showed `APK source dirty: true` while tracked changes were still uncommitted.

Latest recovery checkpoint after generated Menu and Shopping refresh alignment:

Menu/Shopping checkpoint:

- Added `MealPlanRefreshPolicy` so generated plan refreshes preserve the active generated week start date and length instead of always using a new default range.
- When a generated plan already exists, pantry, deal, receipt, and settings changes now replace the persisted generated Menu rows and the visible Shopping list from the same current meal-planning inputs.
- If all meal-planning inputs disappear after a plan exists, stale generated Menu rows are cleared and Shopping is emptied instead of showing old meals/items.
- Added `MealPlanRefreshPolicyTest` coverage for empty, normal 7-day, and gapped existing plan ranges.
- README, PROJECT_SUMMARY, PHONE_TEST_CHECKLIST, phone report template, and feature readiness report template now describe Menu and Shopping refresh together.

Focused check:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-20'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest --tests com.dealplanner.domain.MealPlanRefreshPolicyTest --tests com.dealplanner.domain.MealPlanningEngineTest
```

Result: `BUILD SUCCESSFUL`.

Full readiness gate before commit:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\new-feature-readiness-report.ps1 -RunGate
```

Result before commit: `BUILD SUCCESSFUL`; `259` unit tests, `0` failures/errors/skipped, and lint reported `0` errors with `19` warnings. The dirty-state readiness report `phone-test-results\20260716-184332\FEATURE_READINESS_REPORT.md` correctly refused phone signoff because generated debug `BuildConfig` showed `APK source dirty: true` while tracked changes were still uncommitted.
