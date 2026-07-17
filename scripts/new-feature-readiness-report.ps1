param(
    [switch]$Help,
    [string]$OutputDir = "phone-test-results"
)

$ErrorActionPreference = "Stop"

function Show-Usage {
    Write-Host "Deal Planner feature readiness report"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\new-feature-readiness-report.ps1"
    Write-Host "  .\scripts\new-feature-readiness-report.ps1 -OutputDir phone-test-results"
    Write-Host ""
    Write-Host "Creates an ignored timestamped Markdown report that separates local build/test evidence from phone-only checks."
    Write-Host "Run after .\gradlew.bat testDebugUnitTest assembleDebug lintDebug for the strongest local snapshot."
}

function Get-CommandOutput {
    param([scriptblock]$Command)

    try {
        $output = @(& $Command 2>&1)
        return ($output -join [Environment]::NewLine).Trim()
    } catch {
        return $_.Exception.Message
    }
}

function Get-BuildConfigValue {
    param(
        [string]$Path,
        [string]$Name
    )

    if (-not (Test-Path $Path)) {
        return $null
    }

    $escapedName = [regex]::Escape($Name)
    $line = Get-Content -LiteralPath $Path |
        Where-Object { $_ -match "^\s+public static final .* $escapedName = " } |
        Select-Object -First 1
    if (-not $line) {
        return $null
    }

    if ($line -match '=\s+"([^"]*)";') {
        return $Matches[1]
    }

    if ($line -match '=\s+(true|false);') {
        return $Matches[1]
    }

    return $null
}

function Test-RealGeminiKey {
    param([string]$Value)

    if ([string]::IsNullOrWhiteSpace($Value)) {
        return $false
    }

    $trimmed = $Value.Trim()
    return -not (
        $trimmed.Equals("YOUR_GEMINI_API_KEY", [System.StringComparison]::OrdinalIgnoreCase) -or
        $trimmed.StartsWith("YOUR_", [System.StringComparison]::OrdinalIgnoreCase)
    )
}

function Get-UnitTestSnapshot {
    param([string]$ResultDir)

    if (-not (Test-Path $ResultDir)) {
        return [pscustomobject]@{
            Summary = "not available. Run .\gradlew.bat testDebugUnitTest."
            Tests = 0
            Failures = 0
            Errors = 0
            Skipped = 0
            IsGreen = $false
        }
    }

    $files = @(Get-ChildItem -LiteralPath $ResultDir -Filter "TEST-*.xml" -File)
    if ($files.Count -eq 0) {
        return [pscustomobject]@{
            Summary = "not available. No TEST-*.xml files found under $ResultDir."
            Tests = 0
            Failures = 0
            Errors = 0
            Skipped = 0
            IsGreen = $false
        }
    }

    [xml[]]$xml = $files | ForEach-Object { [xml](Get-Content -LiteralPath $_.FullName) }
    $tests = ($xml | ForEach-Object { [int]$_.testsuite.tests } | Measure-Object -Sum).Sum
    $failures = ($xml | ForEach-Object { [int]$_.testsuite.failures } | Measure-Object -Sum).Sum
    $errors = ($xml | ForEach-Object { [int]$_.testsuite.errors } | Measure-Object -Sum).Sum
    $skipped = ($xml | ForEach-Object { [int]$_.testsuite.skipped } | Measure-Object -Sum).Sum

    return [pscustomobject]@{
        Summary = "$tests test(s), $failures failure(s), $errors error(s), $skipped skipped"
        Tests = $tests
        Failures = $failures
        Errors = $errors
        Skipped = $skipped
        IsGreen = ($tests -gt 0 -and $failures -eq 0 -and $errors -eq 0)
    }
}

function Get-LintSnapshot {
    param([string]$Path)

    if (-not (Test-Path $Path)) {
        return [pscustomobject]@{
            Summary = "not available. Run .\gradlew.bat lintDebug."
            ErrorCount = 0
            WarningCount = 0
            Groups = "not available"
            IsGreen = $false
        }
    }

    try {
        [xml]$lint = Get-Content -LiteralPath $Path
        $issues = @($lint.issues.issue)
        $errors = @($issues | Where-Object { $_.severity -eq "Error" }).Count
        $warnings = @($issues | Where-Object { $_.severity -eq "Warning" }).Count
        $groups = @(
            $issues |
                Group-Object id |
                Sort-Object Name |
                ForEach-Object { "$($_.Name): $($_.Count)" }
        )
        if ($groups.Count -eq 0) {
            $groups = @("none")
        }

        return [pscustomobject]@{
            Summary = "$errors error(s), $warnings warning(s)"
            ErrorCount = $errors
            WarningCount = $warnings
            Groups = ($groups -join [Environment]::NewLine)
            IsGreen = ($errors -eq 0)
        }
    } catch {
        return [pscustomobject]@{
            Summary = "could not read lint report: $($_.Exception.Message)"
            ErrorCount = 0
            WarningCount = 0
            Groups = "not available"
            IsGreen = $false
        }
    }
}

function New-FeatureRow {
    param(
        [string]$Area,
        [string[]]$Evidence,
        [string[]]$LocalChecks,
        [string]$PhoneCheck,
        [bool]$GateGreen
    )

    $missing = @(
        $Evidence |
            Where-Object { -not (Test-Path (Join-Path $repoRoot $_)) }
    )
    $localStatus = if ($missing.Count -eq 0 -and $GateGreen) {
        "Local evidence present; build/test/lint gate green."
    } elseif ($missing.Count -eq 0) {
        "Local evidence present; rerun or fix local gate before phone signoff."
    } else {
        "Missing local evidence: $($missing -join ', ')"
    }

    return [pscustomobject]@{
        Area = $Area
        Evidence = ($Evidence -join "<br>")
        LocalChecks = ($LocalChecks -join "<br>")
        LocalStatus = $localStatus
        PhoneCheck = $PhoneCheck
    }
}

if ($Help) {
    Show-Usage
    exit 0
}

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

if (-not (Test-Path ".\gradlew.bat")) {
    throw "Run this script from the Deal Planner repo, or keep it under scripts\ in that repo."
}

if ([System.IO.Path]::IsPathRooted($OutputDir)) {
    $outputRoot = $OutputDir
} else {
    $outputRoot = Join-Path $repoRoot $OutputDir
}

New-Item -ItemType Directory -Force -Path $outputRoot | Out-Null

$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$sessionDir = Join-Path $outputRoot $stamp
New-Item -ItemType Directory -Force -Path $sessionDir | Out-Null

$reportPath = Join-Path $sessionDir "FEATURE_READINESS_REPORT.md"
$branch = Get-CommandOutput { git rev-parse --abbrev-ref HEAD }
$head = Get-CommandOutput { git rev-parse --short HEAD }
$status = Get-CommandOutput { git status --short --branch }
$remote = Get-CommandOutput { git remote get-url origin }
$testSnapshot = Get-UnitTestSnapshot (Join-Path $repoRoot "app\build\test-results\testDebugUnitTest")
$lintSnapshot = Get-LintSnapshot (Join-Path $repoRoot "app\build\reports\lint-results-debug.xml")
$gateGreen = $testSnapshot.IsGreen -and $lintSnapshot.IsGreen
$buildConfigPath = Join-Path $repoRoot "app\build\generated\source\buildConfig\debug\com\dealplanner\BuildConfig.java"
$apkSourceBranch = Get-BuildConfigValue $buildConfigPath "GIT_BRANCH"
$apkSourceSha = Get-BuildConfigValue $buildConfigPath "GIT_SHA"
$apkSourceDirty = Get-BuildConfigValue $buildConfigPath "GIT_DIRTY"
$apkGeminiKey = Get-BuildConfigValue $buildConfigPath "GEMINI_API_KEY"
$apkGeminiConfigured = Test-RealGeminiKey $apkGeminiKey
$apkGeminiModel = Get-BuildConfigValue $buildConfigPath "GEMINI_MODEL"

if ([string]::IsNullOrWhiteSpace($apkSourceBranch)) { $apkSourceBranch = "UNKNOWN" }
if ([string]::IsNullOrWhiteSpace($apkSourceSha)) { $apkSourceSha = "UNKNOWN" }
if ([string]::IsNullOrWhiteSpace($apkSourceDirty)) { $apkSourceDirty = "UNKNOWN" }
if ([string]::IsNullOrWhiteSpace($apkGeminiConfigured)) { $apkGeminiConfigured = "UNKNOWN" }
if ([string]::IsNullOrWhiteSpace($apkGeminiModel)) { $apkGeminiModel = "UNKNOWN" }

$features = @(
    New-FeatureRow `
        -Area "Current source, APK, and repo identity" `
        -Evidence @("gradlew.bat", "settings.gradle.kts", "app\build.gradle.kts", "scripts\phone-debug-preflight.ps1", "scripts\phone-debug-install.ps1") `
        -LocalChecks @("Git branch/remote", "APK source BuildConfig", "preflight helper") `
        -PhoneCheck "Run .\scripts\start-phone-test-run.ps1 with an authorized Android phone." `
        -GateGreen $gateGreen
    New-FeatureRow `
        -Area "Pantry manual text and edit/review" `
        -Evidence @("app\src\main\java\com\dealplanner\ui\screens\PantryScreen.kt", "app\src\main\java\com\dealplanner\parser\PantryPhraseParser.kt", "app\src\test\java\com\dealplanner\parser\PantryPhraseParserTest.kt", "app\src\test\java\com\dealplanner\ui\state\PantryItemInputValidatorTest.kt") `
        -LocalChecks @("Parser tests", "edit dialog validation tests", "manual clear/retain policy tests") `
        -PhoneCheck "Type pantry items, edit VERIFY rows, and confirm shopping list refresh on the phone." `
        -GateGreen $gateGreen
    New-FeatureRow `
        -Area "Pantry barcode and manual UPC intake" `
        -Evidence @("app\src\main\java\com\dealplanner\ui\screens\PantryScreen.kt", "app\src\main\java\com\dealplanner\lookup\OpenFoodFactsBarcodeClient.kt", "app\src\main\java\com\dealplanner\lookup\BarcodePantryMapper.kt", "app\src\test\java\com\dealplanner\lookup\OpenFoodFactsBarcodeClientTest.kt", "app\src\test\java\com\dealplanner\lookup\BarcodePantryMapperTest.kt") `
        -LocalChecks @("Barcode normalization tests", "Open Food Facts response parsing tests", "fallback pantry-row mapping tests") `
        -PhoneCheck "Type generated UPC-A, scan generated barcode image, and confirm found/not-found fallback messaging." `
        -GateGreen $gateGreen
    New-FeatureRow `
        -Area "Pantry photo/gallery, OCR fallback, and Gemini vision" `
        -Evidence @("app\src\main\java\com\dealplanner\ui\screens\PantryScreen.kt", "app\src\main\java\com\dealplanner\ui\viewmodel\AppViewModel.kt", "app\src\main\java\com\dealplanner\ocr\PantryOcrCandidateExtractor.kt", "app\src\main\java\com\dealplanner\ai\GeminiPantryVisionClient.kt", "app\src\test\java\com\dealplanner\ocr\PantryOcrCandidateExtractorTest.kt", "app\src\test\java\com\dealplanner\ai\GeminiPantryVisionClientTest.kt", "app\src\test\java\com\dealplanner\ai\PantryVisionItemMapperTest.kt") `
        -LocalChecks @("OCR candidate tests", "Gemini response/client parsing tests", "AI-to-pantry VERIFY mapping tests") `
        -PhoneCheck "Test pantry Photo/Gallery without key for OCR fallback, then with real Gemini key for AI vision." `
        -GateGreen $gateGreen
    New-FeatureRow `
        -Area "Flyer deals text/photo/gallery/PDF" `
        -Evidence @("app\src\main\java\com\dealplanner\ui\screens\DealsScreen.kt", "app\src\main\java\com\dealplanner\parser\DealsParser.kt", "app\src\test\java\com\dealplanner\parser\DealsParserTest.kt", "app\src\test\java\com\dealplanner\ui\state\DealItemInputValidatorTest.kt") `
        -LocalChecks @("Flyer parser tests", "deal review validation tests", "manual text clear/retain policy tests") `
        -PhoneCheck "Paste flyer text, choose generated flyer PNG/PDF, and take a flyer photo on the phone." `
        -GateGreen $gateGreen
    New-FeatureRow `
        -Area "Receipt text/photo/gallery/PDF and budget updates" `
        -Evidence @("app\src\main\java\com\dealplanner\ui\screens\ReceiptsScreen.kt", "app\src\main\java\com\dealplanner\domain\ReceiptReconciler.kt", "app\src\main\java\com\dealplanner\domain\ReceiptAdjustmentCalculator.kt", "app\src\test\java\com\dealplanner\domain\ReceiptReconcilerTest.kt", "app\src\test\java\com\dealplanner\domain\ReceiptAdjustmentCalculatorTest.kt", "app\src\test\java\com\dealplanner\ui\state\ReceiptItemInputValidatorTest.kt") `
        -LocalChecks @("Receipt parser/reconciliation tests", "receipt edit/delete delta tests", "receipt review validation tests") `
        -PhoneCheck "Paste receipt text, choose generated receipt PNG/PDF, take a receipt photo, and confirm Budget changes." `
        -GateGreen $gateGreen
    New-FeatureRow `
        -Area "Meal plan, shopping list, and budget core" `
        -Evidence @("app\src\main\java\com\dealplanner\domain\MealPlanningEngine.kt", "app\src\main\java\com\dealplanner\domain\BudgetEngine.kt", "app\src\main\java\com\dealplanner\ui\screens\MenuScreen.kt", "app\src\main\java\com\dealplanner\ui\screens\ShoppingListScreen.kt", "app\src\main\java\com\dealplanner\ui\screens\BudgetScreen.kt", "app\src\test\java\com\dealplanner\domain\MealPlanningEngineTest.kt", "app\src\test\java\com\dealplanner\domain\BudgetEngineTest.kt") `
        -LocalChecks @("Meal planning engine tests", "budget engine tests", "receipt budget delta tests") `
        -PhoneCheck "Load Demo, generate menu, verify shopping list, edit budget, and relaunch to confirm persistence." `
        -GateGreen $gateGreen
    New-FeatureRow `
        -Area "Settings and Gemini connection test" `
        -Evidence @("app\src\main\java\com\dealplanner\ui\screens\ParamsScreen.kt", "app\src\main\java\com\dealplanner\ai\GeminiPantryVisionClient.kt", "app\src\test\java\com\dealplanner\ai\GeminiPantryVisionClientTest.kt", "app\src\test\java\com\dealplanner\ui\state\SettingsInputValidatorTest.kt", "local.properties.example") `
        -LocalChecks @("Gemini missing-key/error summary tests", "settings numeric validation tests", "BuildConfig Gemini snapshot") `
        -PhoneCheck "Verify no-key fallback, then rebuild with real key and run Test AI Connection plus pantry AI photo." `
        -GateGreen $gateGreen
    New-FeatureRow `
        -Area "Phone test helpers and deterministic samples" `
        -Evidence @("scripts\start-phone-test-run.ps1", "scripts\new-phone-test-samples.ps1", "scripts\send-phone-test-samples.ps1", "scripts\new-phone-test-report.ps1", "scripts\phone-debug-logs.ps1", "PHONE_TEST_CHECKLIST_2026-07-16.md") `
        -LocalChecks @("sample generator verify mode", "preflight/install/report helper parse checks", "checklist coverage") `
        -PhoneCheck "Run one-command phone setup, complete checklist, and attach generated report/logs for failures." `
        -GateGreen $gateGreen
)

$featureRows = $features | ForEach-Object {
    "| $($_.Area) | $($_.LocalStatus) | $($_.LocalChecks) | $($_.PhoneCheck) |"
}

$report = @"
# Deal Planner Feature Readiness Report - $stamp

## Snapshot

- Repo: $repoRoot
- Branch: $branch
- Commit: $head
- Remote: $remote
- Unit tests: $($testSnapshot.Summary)
- Lint: $($lintSnapshot.Summary)
- APK source branch: $apkSourceBranch
- APK source commit: $apkSourceSha
- APK source dirty: $apkSourceDirty
- APK Gemini configured: $apkGeminiConfigured
- APK Gemini model: $apkGeminiModel

## Git Status

~~~text
$status
~~~

## Readiness Matrix

| Area | Local Status | Local Evidence Checks | Remaining Phone Check |
| --- | --- | --- | --- |
$($featureRows -join [Environment]::NewLine)

## Evidence Files By Area

$(
    ($features | ForEach-Object {
        "### $($_.Area)`n`n$($_.Evidence -replace '<br>', [Environment]::NewLine)"
    }) -join "`n`n"
)

## Lint Issue Groups

~~~text
$($lintSnapshot.Groups)
~~~

## Next Required External Evidence

- Authorized Android phone visible in adb devices as device.
- `.\scripts\start-phone-test-run.ps1` completed and generated a phone report with `Setup status: Completed`.
- Manual checklist results recorded for every input path in `PHONE_TEST_CHECKLIST_2026-07-16.md`.
- Real Gemini key configured locally, APK rebuilt, `.\scripts\phone-debug-preflight.ps1 -RequireGemini` passed, and Settings -> Test AI Connection succeeded.
- Pantry Gemini photo import verified on the phone with reviewable VERIFY notes.
"@

$report | Set-Content -LiteralPath $reportPath -Encoding UTF8

Write-Host "Created feature readiness report:"
Write-Host $reportPath
