param(
    [switch]$Help,
    [switch]$RunGate,
    [string]$JavaHome = "C:\Program Files\Java\jdk-20",
    [string]$OutputDir = "phone-test-results"
)

$ErrorActionPreference = "Stop"

function Show-Usage {
    Write-Host "Deal Planner feature readiness report"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\new-feature-readiness-report.ps1"
    Write-Host "  .\scripts\new-feature-readiness-report.ps1 -RunGate"
    Write-Host "  .\scripts\new-feature-readiness-report.ps1 -OutputDir phone-test-results"
    Write-Host ""
    Write-Host "Creates an ignored timestamped Markdown report that separates local build/test evidence from phone-only checks."
    Write-Host "Use -RunGate to run .\gradlew.bat testDebugUnitTest assembleDebug lintDebug before the snapshot."
    Write-Host "Without -RunGate, the report inspects existing outputs and flags stale/missing evidence where possible."
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

function Format-ReadinessTimestamp {
    param($Value)

    if ($null -eq $Value) {
        return "not available"
    }

    return $Value.ToLocalTime().ToString("yyyy-MM-dd HH:mm:ss K")
}

function Get-LatestInputTimestamp {
    param([string[]]$Paths)

    $items = @(
        foreach ($path in $Paths) {
            $fullPath = Join-Path $repoRoot $path
            if (Test-Path -LiteralPath $fullPath -PathType Container) {
                Get-ChildItem -LiteralPath $fullPath -Recurse -File -Force
            } elseif (Test-Path -LiteralPath $fullPath -PathType Leaf) {
                Get-Item -LiteralPath $fullPath
            }
        }
    )

    if ($items.Count -eq 0) {
        return $null
    }

    return ($items | Sort-Object LastWriteTimeUtc -Descending | Select-Object -First 1).LastWriteTimeUtc
}

function Get-FreshnessSnapshot {
    param(
        [string]$Label,
        $LatestInputUtc,
        [System.IO.FileInfo[]]$Outputs
    )

    if ($Outputs.Count -eq 0) {
        return [pscustomobject]@{
            Summary = "missing; rerun the matching Gradle/helper command."
            Detail = "$Label output was not found."
            IsFresh = $false
        }
    }

    if ($null -eq $LatestInputUtc) {
        return [pscustomobject]@{
            Summary = "unknown; no input timestamp was found."
            Detail = "$Label output exists, but source input timestamps could not be calculated."
            IsFresh = $false
        }
    }

    $oldestOutputUtc = ($Outputs | Sort-Object LastWriteTimeUtc | Select-Object -First 1).LastWriteTimeUtc
    $isFresh = $oldestOutputUtc -ge $LatestInputUtc
    $summaryPrefix = if ($isFresh) { "current" } else { "stale" }

    return [pscustomobject]@{
        Summary = "$summaryPrefix; oldest output $(Format-ReadinessTimestamp $oldestOutputUtc), latest input $(Format-ReadinessTimestamp $LatestInputUtc)."
        Detail = "$Label oldest output: $(Format-ReadinessTimestamp $oldestOutputUtc); latest input: $(Format-ReadinessTimestamp $LatestInputUtc)."
        IsFresh = $isFresh
    }
}

function Get-EffectiveFreshnessSnapshot {
    param(
        [string]$Label,
        $Freshness,
        [bool]$GateExecuted
    )

    if (-not $GateExecuted) {
        return $Freshness
    }

    return [pscustomobject]@{
        Summary = "current; successful -RunGate executed $Label during this report. Raw file freshness: $($Freshness.Summary)"
        Detail = "$Label gate: current because -RunGate completed successfully. Raw file freshness: $($Freshness.Detail)"
        IsFresh = $true
    }
}

function Get-NamingAudit {
    $expectedChecks = @(
        [pscustomobject]@{
            Label = "App label"
            Path = "app\src\main\res\values\strings.xml"
            Expected = '<string name="app_name">Deal Planner</string>'
        },
        [pscustomobject]@{
            Label = "Application ID"
            Path = "app\build.gradle.kts"
            Expected = 'applicationId = "com.dealplanner"'
        },
        [pscustomobject]@{
            Label = "Package namespace"
            Path = "app\build.gradle.kts"
            Expected = 'namespace = "com.dealplanner"'
        },
        [pscustomobject]@{
            Label = "Root project name"
            Path = "settings.gradle.kts"
            Expected = 'rootProject.name = "Deal Planner"'
        }
    )

    $details = New-Object System.Collections.Generic.List[string]
    $failedChecks = New-Object System.Collections.Generic.List[string]

    foreach ($check in $expectedChecks) {
        $fullPath = Join-Path $repoRoot $check.Path
        if (-not (Test-Path -LiteralPath $fullPath -PathType Leaf)) {
            $failedChecks.Add($check.Label)
            $details.Add("MISSING - $($check.Label): $($check.Path)")
            continue
        }

        $content = Get-Content -Raw -LiteralPath $fullPath
        if ($content.Contains($check.Expected)) {
            $details.Add("OK - $($check.Label): $($check.Expected)")
        } else {
            $failedChecks.Add($check.Label)
            $details.Add("MISMATCH - $($check.Label): expected $($check.Expected) in $($check.Path)")
        }
    }

    $oldAppNames = @(
        ("SNAP " + "Optimizer"),
        ("SNAP" + "_Optimizer"),
        ("snap" + "_optimizer"),
        ("SNAP " + "SHOPPER")
    )
    $scanPaths = @(
        "app\src",
        "scripts",
        "README.md",
        "PHONE_TEST_CHECKLIST_2026-07-16.md",
        "PROJECT_SUMMARY.md",
        "local.properties.example",
        "settings.gradle.kts",
        "app\build.gradle.kts"
    )
    $oldNameHits = New-Object System.Collections.Generic.List[string]

    foreach ($scanPath in $scanPaths) {
        $fullPath = Join-Path $repoRoot $scanPath
        if (Test-Path -LiteralPath $fullPath -PathType Container) {
            $files = Get-ChildItem -LiteralPath $fullPath -Recurse -File -Force -ErrorAction SilentlyContinue |
                Where-Object { $_.FullName -notmatch "\\app\\build\\" -and $_.FullName -notmatch "\\.gradle\\" }
        } elseif (Test-Path -LiteralPath $fullPath -PathType Leaf) {
            $files = @(Get-Item -LiteralPath $fullPath)
        } else {
            $files = @()
        }

        foreach ($file in $files) {
            $matches = @(Select-String -LiteralPath $file.FullName -Pattern $oldAppNames -SimpleMatch -ErrorAction SilentlyContinue)
            foreach ($match in $matches) {
                $relativePath = $file.FullName.Substring($repoRoot.Length).TrimStart([char[]]@('\', '/'))
                $oldNameHits.Add("${relativePath}:$($match.LineNumber): $($match.Pattern)")
            }
        }
    }

    if ($oldNameHits.Count -eq 0) {
        $details.Add("OK - Old app-name scan: no active legacy app naming found in app/docs/scripts.")
    } else {
        $failedChecks.Add("Old app-name scan")
        $details.Add("MISMATCH - Old app-name scan found active old-name references:")
        foreach ($hit in $oldNameHits) {
            $details.Add("  $hit")
        }
    }

    $isOk = $failedChecks.Count -eq 0
    $summary = if ($isOk) {
        "OK; active app identity is Deal Planner / com.dealplanner and no old app-name strings were found in active app/docs/scripts."
    } else {
        "needs attention; failed checks: $($failedChecks -join ', ')."
    }

    return [pscustomobject]@{
        Summary = $summary
        Details = ($details -join [Environment]::NewLine)
        IsOk = $isOk
    }
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
        "Local evidence present; build/test/lint/APK evidence green and current."
    } elseif ($missing.Count -eq 0) {
        "Local evidence present; rerun or fix local gate/freshness checks before phone signoff."
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

$gateRunSummary = "Not run by this report; existing outputs were inspected."
$gateExecuted = $false
if ($RunGate) {
    if (-not (Test-Path $JavaHome)) {
        throw "JavaHome does not exist: $JavaHome"
    }

    $env:JAVA_HOME = $JavaHome
    $env:Path = "$JavaHome\bin;$env:Path"
    & .\gradlew.bat testDebugUnitTest assembleDebug lintDebug
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle readiness gate failed with exit code $LASTEXITCODE."
    }
    $gateRunSummary = "Ran .\gradlew.bat testDebugUnitTest assembleDebug lintDebug successfully before this snapshot."
    $gateExecuted = $true
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
$testResultDir = Join-Path $repoRoot "app\build\test-results\testDebugUnitTest"
$lintReportPath = Join-Path $repoRoot "app\build\reports\lint-results-debug.xml"
$apkPath = Join-Path $repoRoot "app\build\outputs\apk\debug\app-debug.apk"
$appInputPaths = @(
    "app\src",
    "app\build.gradle.kts",
    "build.gradle.kts",
    "settings.gradle.kts",
    "gradle.properties"
)
$apkInputPaths = @($appInputPaths + @("local.properties"))
$latestAppInputUtc = Get-LatestInputTimestamp $appInputPaths
$latestApkInputUtc = Get-LatestInputTimestamp $apkInputPaths
$testOutputFiles = @(Get-ChildItem -LiteralPath $testResultDir -Filter "TEST-*.xml" -File -ErrorAction SilentlyContinue)
$lintOutputFiles = @(Get-Item -LiteralPath $lintReportPath -ErrorAction SilentlyContinue)
$apkOutputFiles = @(Get-Item -LiteralPath $apkPath -ErrorAction SilentlyContinue)
$testSnapshot = Get-UnitTestSnapshot $testResultDir
$lintSnapshot = Get-LintSnapshot $lintReportPath
$testFreshness = Get-FreshnessSnapshot "Unit test" $latestAppInputUtc $testOutputFiles
$lintFreshness = Get-FreshnessSnapshot "Lint" $latestAppInputUtc $lintOutputFiles
$apkFreshness = Get-FreshnessSnapshot "Debug APK" $latestApkInputUtc $apkOutputFiles
$testEvidenceFreshness = Get-EffectiveFreshnessSnapshot "unit tests" $testFreshness $gateExecuted
$lintEvidenceFreshness = Get-EffectiveFreshnessSnapshot "lint" $lintFreshness $gateExecuted
$namingAudit = Get-NamingAudit
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

$apkIdentityMatchesHead = $apkSourceSha -eq $head -and $apkSourceDirty.ToString().ToLowerInvariant() -eq "false"
$apkIdentitySummary = if ($apkIdentityMatchesHead) {
    "matches current HEAD $head and clean BuildConfig."
} else {
    "does not match current clean HEAD. Rebuild with .\gradlew.bat assembleDebug before phone testing."
}
$testAndLintEvidenceCurrent = $testEvidenceFreshness.IsFresh -and $lintEvidenceFreshness.IsFresh
$gateGreen = $testSnapshot.IsGreen -and
    $lintSnapshot.IsGreen -and
    $testAndLintEvidenceCurrent -and
    $apkFreshness.IsFresh -and
    $apkIdentityMatchesHead -and
    $namingAudit.IsOk

$features = @(
    New-FeatureRow `
        -Area "Current source, APK, and repo identity" `
        -Evidence @("gradlew.bat", "settings.gradle.kts", "app\build.gradle.kts", "scripts\phone-debug-preflight.ps1", "scripts\phone-debug-install.ps1") `
        -LocalChecks @("Git branch/remote", "APK source BuildConfig", "preflight helper") `
        -PhoneCheck "Run .\scripts\start-phone-test-run.ps1 with an authorized Android phone." `
        -GateGreen $gateGreen
    New-FeatureRow `
        -Area "Deal Planner naming transition" `
        -Evidence @("app\src\main\res\values\strings.xml", "app\build.gradle.kts", "settings.gradle.kts", "README.md", "PROJECT_SUMMARY.md") `
        -LocalChecks @("App label", "Application ID", "Package namespace", "Root project name", "Old app-name scan") `
        -PhoneCheck "Confirm Settings -> About Deal Planner shows package com.dealplanner and no legacy SNAP-era app naming appears on-device." `
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
        -Evidence @("app\src\main\java\com\dealplanner\ui\screens\PantryScreen.kt", "app\src\main\java\com\dealplanner\ui\viewmodel\AppViewModel.kt", "app\src\main\java\com\dealplanner\ui\state\PantryImportReviewQueue.kt", "app\src\main\java\com\dealplanner\ocr\PantryOcrCandidateExtractor.kt", "app\src\main\java\com\dealplanner\ai\GeminiPantryVisionClient.kt", "app\src\test\java\com\dealplanner\ui\state\PantryImportReviewQueueTest.kt", "app\src\test\java\com\dealplanner\ocr\PantryOcrCandidateExtractorTest.kt", "app\src\test\java\com\dealplanner\ai\GeminiPantryVisionClientTest.kt", "app\src\test\java\com\dealplanner\ai\PantryVisionItemMapperTest.kt") `
        -LocalChecks @("Photo import review queue tests", "OCR candidate tests", "Gemini response/client parsing tests", "AI-to-pantry VERIFY mapping tests") `
        -PhoneCheck "Test pantry Photo/Gallery pending review and Save All without key for OCR fallback, then with real Gemini key for AI vision." `
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
        -Evidence @("app\src\main\java\com\dealplanner\domain\MealPlanningEngine.kt", "app\src\main\java\com\dealplanner\domain\BudgetEngine.kt", "app\src\main\java\com\dealplanner\domain\ShoppingListExportFormatter.kt", "app\src\main\java\com\dealplanner\ui\export\ShoppingListPdfExporter.kt", "app\src\main\java\com\dealplanner\ui\screens\MenuScreen.kt", "app\src\main\java\com\dealplanner\ui\screens\ShoppingListScreen.kt", "app\src\main\java\com\dealplanner\ui\screens\BudgetScreen.kt", "app\src\test\java\com\dealplanner\domain\MealPlanningEngineTest.kt", "app\src\test\java\com\dealplanner\domain\BudgetEngineTest.kt", "app\src\test\java\com\dealplanner\domain\ShoppingListExportFormatterTest.kt") `
        -LocalChecks @("Meal planning engine tests", "budget engine tests", "receipt budget delta tests", "shopping list export formatter tests") `
        -PhoneCheck "Load Demo, generate menu, verify shopping list, export PDF/share sheet, edit budget, and relaunch to confirm persistence." `
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
- Gradle gate run by report: $gateRunSummary
- Unit tests: $($testSnapshot.Summary)
- Unit test freshness: $($testEvidenceFreshness.Summary)
- Lint: $($lintSnapshot.Summary)
- Lint freshness: $($lintEvidenceFreshness.Summary)
- Debug APK freshness: $($apkFreshness.Summary)
- APK source branch: $apkSourceBranch
- APK source commit: $apkSourceSha
- APK source dirty: $apkSourceDirty
- APK identity: $apkIdentitySummary
- Naming audit: $($namingAudit.Summary)
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

## Evidence Freshness

- Gradle gate run by report: $gateRunSummary
- Unit test evidence: $($testEvidenceFreshness.Detail)
- Lint evidence: $($lintEvidenceFreshness.Detail)
- Debug APK evidence: $($apkFreshness.Detail)
- APK identity: $apkIdentitySummary

When `-RunGate` is used, the successful Gradle run is treated as stronger local evidence than report-file timestamps for unit test and lint outputs. APK freshness and APK source identity still have to match the current source and Git HEAD before phone testing.

## Naming Audit

~~~text
$($namingAudit.Details)
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
