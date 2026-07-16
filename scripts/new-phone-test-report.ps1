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

if ($apkInfo) {
    $apkLine = "- Debug APK: $apkPath ($($apkInfo.Length) bytes, $($apkInfo.LastWriteTime))"
} else {
    $apkLine = "- Debug APK: MISSING at $apkPath"
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
- Package: $PackageName
- Gemini configured: $geminiConfigured
- Gemini model setting: $($geminiModel.Trim())
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
- [ ] Close/relaunch preserves generated Shopping list after a plan exists.
- Notes:

## Deterministic Text Paths

- [ ] Pantry typed input works, including edit/review and comma/leading-decimal quantity corrections.
- [ ] Deals pasted flyer text works, including edit/review and comma/leading-decimal price corrections.
- [ ] Receipts pasted receipt text works, updates Budget, and supports edit/delete corrections.
- [ ] Budget settings save valid comma/leading-decimal values and block invalid text.
- Notes:

## Phone Input Paths

- [ ] Manual barcode/code lookup/fallback works.
- [ ] Barcode scanner works.
- [ ] Pantry photo works or shows a clear recovery message.
- [ ] Pantry gallery works or shows a clear recovery message.
- [ ] Flyer photo works or shows a clear recovery message.
- [ ] Flyer gallery works or shows a clear recovery message.
- [ ] Flyer PDF works or shows a clear recovery message.
- [ ] Receipt photo works or shows a clear recovery message.
- [ ] Receipt gallery works or shows a clear recovery message.
- [ ] Camera denial/cancel, permission-request failure, picker cancel, and external launch-failure states show visible messages.
- Notes:

## AI Verification

- [ ] Without Gemini key, Settings reports OCR fallback and Test AI Connection reports key not configured.
- [ ] With Gemini key rebuilt into APK, Test AI Connection succeeds.
- [ ] Gemini pantry photo recognition creates reviewable items from a real label.
- [ ] Unclear Gemini fields show VERIFY notes explaining what to review.
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
