param(
    [switch]$Help,
    [string]$OutputDir = "phone-test-results",
    [string]$PackageName = "com.dealplanner"
)

$ErrorActionPreference = "Stop"

function Show-Usage {
    Write-Host "Deal Planner phone test report"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\new-phone-test-report.ps1"
    Write-Host "  .\scripts\new-phone-test-report.ps1 -OutputDir phone-test-results"
    Write-Host ""
    Write-Host "Creates an ignored timestamped Markdown report for recording real-phone pass/fail evidence."
    Write-Host "Set ANDROID_SERIAL when more than one authorized device is connected."
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

function Get-AdbSummary {
    if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
        return [pscustomobject]@{
            Devices = "adb not found on PATH"
            Serial = ""
            Manufacturer = ""
            Model = ""
            AndroidVersion = ""
            AndroidSdk = ""
            PackagePath = ""
        }
    }

    $devices = Get-CommandOutput { adb devices -l }
    $authorizedDevices = @(
        ($devices -split [Environment]::NewLine) |
            Where-Object { $_ -match "^(\S+)\s+device\b" } |
            ForEach-Object { ($_.Trim() -split "\s+")[0] }
    )

    $serial = ""
    if ($env:ANDROID_SERIAL -and $authorizedDevices -contains $env:ANDROID_SERIAL) {
        $serial = $env:ANDROID_SERIAL
    } elseif ($authorizedDevices.Count -eq 1) {
        $serial = $authorizedDevices[0]
    }

    if ([string]::IsNullOrWhiteSpace($serial)) {
        return [pscustomobject]@{
            Devices = $devices
            Serial = ""
            Manufacturer = ""
            Model = ""
            AndroidVersion = ""
            AndroidSdk = ""
            PackagePath = ""
        }
    }

    return [pscustomobject]@{
        Devices = $devices
        Serial = $serial
        Manufacturer = Get-CommandOutput { adb -s $serial shell getprop ro.product.manufacturer }
        Model = Get-CommandOutput { adb -s $serial shell getprop ro.product.model }
        AndroidVersion = Get-CommandOutput { adb -s $serial shell getprop ro.build.version.release }
        AndroidSdk = Get-CommandOutput { adb -s $serial shell getprop ro.build.version.sdk }
        PackagePath = Get-CommandOutput { adb -s $serial shell pm path $PackageName }
    }
}

function Get-LocalPropertyValue {
    param(
        [string]$Path,
        [string]$Name
    )

    if (-not (Test-Path $Path)) {
        return $null
    }

    $escapedName = [regex]::Escape($Name)
    $line = Get-Content -Path $Path | Where-Object { $_ -match "^\s*$escapedName\s*=" } | Select-Object -First 1
    if (-not $line) {
        return $null
    }

    return ($line -replace "^\s*$escapedName\s*=\s*", "").Trim()
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

$reportPath = Join-Path $sessionDir "PHONE_TEST_REPORT.md"
$adb = Get-AdbSummary
$apkPath = Join-Path $repoRoot "app\build\outputs\apk\debug\app-debug.apk"
if (Test-Path $apkPath) {
    $apkInfo = Get-Item $apkPath
} else {
    $apkInfo = $null
}
$localPropertiesPath = Join-Path $repoRoot "local.properties"
$localGeminiKey = Get-LocalPropertyValue $localPropertiesPath "gemini.api.key"
$envGeminiKey = $env:GEMINI_API_KEY
$geminiConfigured = (Test-RealGeminiKey $localGeminiKey) -or (Test-RealGeminiKey $envGeminiKey)
$geminiModel = Get-LocalPropertyValue $localPropertiesPath "gemini.model"
if ([string]::IsNullOrWhiteSpace($geminiModel)) {
    if ([string]::IsNullOrWhiteSpace($env:GEMINI_MODEL)) {
        $geminiModel = "gemini-3.5-flash"
    } else {
        $geminiModel = $env:GEMINI_MODEL
    }
}

$branch = Get-CommandOutput { git rev-parse --abbrev-ref HEAD }
$head = Get-CommandOutput { git rev-parse --short HEAD }
$status = Get-CommandOutput { git status --short --branch }
$remote = Get-CommandOutput { git remote get-url origin }
$buildConfigPath = Join-Path $repoRoot "app\build\generated\source\buildConfig\debug\com\dealplanner\BuildConfig.java"
$apkSourceBranch = Get-BuildConfigValue $buildConfigPath "GIT_BRANCH"
$apkSourceSha = Get-BuildConfigValue $buildConfigPath "GIT_SHA"
$apkSourceDirty = Get-BuildConfigValue $buildConfigPath "GIT_DIRTY"
$apkGeminiKey = Get-BuildConfigValue $buildConfigPath "GEMINI_API_KEY"
$apkGeminiModel = Get-BuildConfigValue $buildConfigPath "GEMINI_MODEL"
$apkGeminiConfigured = Test-RealGeminiKey $apkGeminiKey
if ([string]::IsNullOrWhiteSpace($apkSourceBranch)) {
    $apkSourceBranch = "UNKNOWN"
}
if ([string]::IsNullOrWhiteSpace($apkSourceSha)) {
    $apkSourceSha = "UNKNOWN"
}
if ([string]::IsNullOrWhiteSpace($apkSourceDirty)) {
    $apkSourceDirty = "UNKNOWN"
}
if ([string]::IsNullOrWhiteSpace($apkGeminiModel)) {
    $apkGeminiModel = "UNKNOWN"
}

if ($apkInfo) {
    $apkLine = "- Debug APK: $apkPath ($($apkInfo.Length) bytes, $($apkInfo.LastWriteTime))"
} else {
    $apkLine = "- Debug APK: MISSING at $apkPath"
}

$sampleRoot = Join-Path $repoRoot "phone-test-samples"
$latestSampleDir = if (Test-Path $sampleRoot) {
    Get-ChildItem -LiteralPath $sampleRoot -Directory | Sort-Object Name -Descending | Select-Object -First 1
} else {
    $null
}
$sampleRemoteRoot = "/sdcard/Download/DealPlannerPhoneTestSamples"
if ($latestSampleDir) {
    $sampleLine = "- Phone test samples: $($latestSampleDir.FullName)"
    $sampleManifestPath = Join-Path $latestSampleDir.FullName "SAMPLE_MANIFEST.md"
    if (Test-Path $sampleManifestPath) {
        $sampleManifestLine = "- Phone test sample manifest: $sampleManifestPath"
    } else {
        $sampleManifestLine = "- Phone test sample manifest: missing. Run .\scripts\new-phone-test-samples.ps1 -VerifyOnly"
    }
    $sampleTransferReportPath = Join-Path $latestSampleDir.FullName "PHONE_SAMPLE_TRANSFER.md"
    $sampleAndroidDestination = "$sampleRemoteRoot/$($latestSampleDir.Name)"
    if (Test-Path $sampleTransferReportPath) {
        $destinationLine = Get-Content -LiteralPath $sampleTransferReportPath |
            Where-Object { $_ -match "^- Android destination:\s*(.+)$" } |
            Select-Object -First 1
        if ($destinationLine -match "^- Android destination:\s*(.+)$") {
            $sampleAndroidDestination = $Matches[1].Trim()
        }
        $sampleTransferReportLine = "- Phone test sample transfer report: $sampleTransferReportPath"
    } else {
        $sampleTransferReportLine = "- Phone test sample transfer report: not generated yet. Run .\scripts\send-phone-test-samples.ps1 after USB debugging is authorized."
    }
    $sampleDestinationLine = "- Phone test sample Android destination: $sampleAndroidDestination"
} else {
    $sampleLine = "- Phone test samples: not generated yet. Run .\scripts\new-phone-test-samples.ps1"
    $sampleManifestLine = "- Phone test sample manifest: not generated yet."
    $sampleDestinationLine = "- Phone test sample Android destination: not available yet."
    $sampleTransferReportLine = "- Phone test sample transfer report: not generated yet."
}

if ([string]::IsNullOrWhiteSpace($adb.Devices)) {
    $adbBlock = "(no adb output)"
} else {
    $adbBlock = $adb.Devices
}

$report = @"
# Deal Planner Phone Test Report - $stamp

## Source Snapshot

- Repo: $repoRoot
- Branch: $branch
- Commit: $head
- Remote: $remote
$apkLine
- APK source branch: $apkSourceBranch
- APK source commit: $apkSourceSha
- APK source dirty: $apkSourceDirty
- Package: $PackageName
- Gemini configured: $geminiConfigured
- Gemini model setting: $($geminiModel.Trim())
- APK Gemini configured: $apkGeminiConfigured
- APK Gemini model: $($apkGeminiModel.Trim())
$sampleLine
$sampleManifestLine
$sampleDestinationLine
$sampleTransferReportLine
- Checklist: PHONE_TEST_CHECKLIST_2026-07-16.md

## Git Status

~~~text
$status
~~~

## Device Snapshot

- Selected serial: $($adb.Serial)
- Manufacturer: $($adb.Manufacturer)
- Model: $($adb.Model)
- Android version: $($adb.AndroidVersion)
- Android SDK: $($adb.AndroidSdk)
- Installed package path: $($adb.PackagePath)

~~~text
$adbBlock
~~~

## Install And Launch

- [ ] .\scripts\start-phone-test-run.ps1 completed, or the individual setup helpers below were run.
- [ ] .\scripts\phone-debug-preflight.ps1 passed with 0 failures.
- [ ] .\scripts\phone-debug-install.ps1 installed and launched Deal Planner.
- Notes:

## Baseline Smoke

- [ ] App launches.
- [ ] Load Demo works.
- [ ] Pantry tab opens.
- [ ] Deals tab opens.
- [ ] Receipts tab opens.
- [ ] Shopping tab opens.
- [ ] Menu tab opens.
- [ ] Budget tab opens.
- [ ] Settings tab opens.
- [ ] Settings -> About shows version, package, debug build, source branch/commit, and no dirty marker for the expected APK.
- [ ] Close/relaunch preserves generated Shopping list after a plan exists.
- Notes:

## Deterministic Text Paths

- [ ] Pantry typed input works, including edit/review and comma/leading-decimal quantity corrections.
- [ ] Deals pasted flyer text works, including edit/review and comma/leading-decimal price corrections.
- [ ] Receipts pasted receipt text works, updates Budget, and supports edit/delete corrections.
- [ ] Generated sample TXT files were available for pasted flyer/receipt checks, if used.
- [ ] Generated sample folder was copied or otherwise available on the phone, if used.
- [ ] Generated sample manifest was present or verified before transfer, if generated samples were used.
- [ ] Sample transfer helper verified remote byte sizes and requested Android media scans, if used.
- [ ] PHONE_SAMPLE_TRANSFER.md recorded the Android destination and verified byte sizes, if sample transfer was used.
- [ ] Generated sample PNG files were available for pantry/flyer/receipt gallery checks, if used.
- [ ] Budget settings save valid comma/leading-decimal values and block invalid text.
- Notes:

## Phone Input Paths

- [ ] Manual barcode/code lookup/fallback works.
- [ ] Barcode scanner works.
- [ ] Generated UPC-A barcode sample was available for manual code and scanner checks, if used.
- [ ] Pantry photo works or shows a clear recovery message.
- [ ] Pantry gallery works or shows a clear recovery message.
- [ ] Flyer photo works or shows a clear recovery message.
- [ ] Flyer gallery works or shows a clear recovery message.
- [ ] Flyer PDF works or shows a clear recovery message.
- [ ] Receipt photo works or shows a clear recovery message.
- [ ] Receipt gallery works or shows a clear recovery message.
- [ ] Receipt PDF works or shows a clear recovery message.
- [ ] Generated sample PDF files were available for flyer/receipt PDF checks, if used.
- [ ] Camera denial/cancel, permission-request failure, picker cancel, and external launch-failure states show visible messages.
- Notes:

## AI Verification

- [ ] Without Gemini key, Settings reports OCR fallback and Test AI Connection reports key not configured.
- [ ] .\scripts\phone-debug-preflight.ps1 -RequireGemini passed after adding the key and rebuilding the APK.
- [ ] Source Snapshot shows APK Gemini configured is True and APK Gemini model is the expected model.
- [ ] With Gemini key rebuilt into APK, Test AI Connection succeeds.
- [ ] Gemini pantry photo recognition creates reviewable items from a real label.
- [ ] Unclear Gemini fields show VERIFY notes explaining what to review.
- [ ] Zero or negative Gemini amount details fall back to quantity 1.0, show VERIFY, and include Review amount/unit.
- Notes:

## Budget, Menu, Shopping

- [ ] Receipt import/edit/delete updates Budget totals and projections.
- [ ] Menu generation is deterministic and replaces the active generated week.
- [ ] Shopping list refreshes after pantry/deal/receipt/settings changes.
- Notes:

## Failures Or Follow-Ups

| Area | What Happened | Log Folder | Next Action |
| --- | --- | --- | --- |
|  |  |  |  |

## Log Capture

Use this if a phone-only issue appears:

~~~powershell
.\scripts\phone-debug-logs.ps1 -Clear -Launch -DurationSeconds 90
~~~

Log folder:

- phone-test-logs\<timestamp>\logcat-dealplanner-filtered.txt
"@

$report | Set-Content -LiteralPath $reportPath -Encoding UTF8

Write-Host "Created phone test report:"
Write-Host $reportPath
