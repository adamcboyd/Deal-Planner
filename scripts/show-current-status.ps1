param(
    [switch]$Help,
    [string]$PackageName = "com.dealplanner"
)

$ErrorActionPreference = "Stop"

function Show-Usage {
    Write-Host "Deal Planner current status"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\show-current-status.ps1"
    Write-Host ""
    Write-Host "Prints a read-only recovery snapshot: repo state, debug APK identity, Gemini key/model readiness,"
    Write-Host "ADB phone visibility, latest generated reports/samples, and the next phone-test commands."
    Write-Host "No build, install, network, or live Gemini request is run."
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

function Get-LocalPropertyValue {
    param(
        [string]$Path,
        [string]$Name
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        return $null
    }

    $escapedName = [regex]::Escape($Name)
    $line = Get-Content -LiteralPath $Path |
        Where-Object { $_ -match "^\s*$escapedName\s*=" } |
        Select-Object -First 1
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

    if (-not (Test-Path -LiteralPath $Path)) {
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

function Format-Boolean {
    param([bool]$Value)

    if ($Value) {
        return "yes"
    }

    return "no"
}

function Get-LatestReport {
    param(
        [string]$Root,
        [string]$ReportName
    )

    if (-not (Test-Path -LiteralPath $Root)) {
        return $null
    }

    return Get-ChildItem -LiteralPath $Root -Directory |
        Sort-Object Name -Descending |
        ForEach-Object {
            $candidate = Join-Path $_.FullName $ReportName
            if (Test-Path -LiteralPath $candidate) {
                Get-Item -LiteralPath $candidate
            }
        } |
        Select-Object -First 1
}

function Get-LatestSampleDir {
    param([string]$Root)

    if (-not (Test-Path -LiteralPath $Root)) {
        return $null
    }

    return Get-ChildItem -LiteralPath $Root -Directory |
        Sort-Object Name -Descending |
        Select-Object -First 1
}

function Get-AdbStatus {
    if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
        return [pscustomobject]@{
            Status = "not ready"
            Detail = "adb was not found on PATH."
            Devices = "adb not found on PATH"
        }
    }

    $devices = Get-CommandOutput { adb devices -l }
    $lines = @(
        ($devices -split "\r?\n") |
            Where-Object { $_ -match "^\S+\s+(device|unauthorized|offline|recovery|sideload|rescue|no permissions)\b" }
    )
    $authorized = @($lines | Where-Object { $_ -match "^\S+\s+device\b" })
    $problem = @($lines | Where-Object { $_ -notmatch "^\S+\s+device\b" })

    if ($authorized.Count -eq 1 -and $problem.Count -eq 0) {
        return [pscustomobject]@{
            Status = "ready"
            Detail = "one authorized phone is visible: $($authorized[0])"
            Devices = $devices
        }
    }

    if ($authorized.Count -gt 1) {
        return [pscustomobject]@{
            Status = "needs selection"
            Detail = "multiple authorized phones are visible; set ANDROID_SERIAL before install/test."
            Devices = $devices
        }
    }

    if ($problem.Count -gt 0) {
        return [pscustomobject]@{
            Status = "not ready"
            Detail = "phone is visible but not authorized/online; unlock it, accept USB debugging, then rerun."
            Devices = $devices
        }
    }

    return [pscustomobject]@{
        Status = "not ready"
        Detail = "no authorized Android phone is visible."
        Devices = $devices
    }
}

function Write-Section {
    param([string]$Title)

    Write-Host ""
    Write-Host $Title
    Write-Host ("-" * $Title.Length)
}

if ($Help) {
    Show-Usage
    exit 0
}

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

if (-not (Test-Path -LiteralPath ".\gradlew.bat")) {
    throw "Run this script from the Deal Planner repo, or keep it under scripts\ in that repo."
}

$branch = Get-CommandOutput { git branch --show-current }
$commit = Get-CommandOutput { git rev-parse --short HEAD }
$remote = Get-CommandOutput { git remote get-url origin }
$status = Get-CommandOutput { git status --short --branch }
$upstream = Get-CommandOutput { git rev-parse --abbrev-ref --symbolic-full-name "@{u}" }
$aheadBehind = "no upstream"
if (-not [string]::IsNullOrWhiteSpace($upstream) -and -not $upstream.StartsWith("fatal:", [System.StringComparison]::OrdinalIgnoreCase)) {
    $counts = Get-CommandOutput { git rev-list --left-right --count "$upstream...HEAD" }
    if ($counts -match "^\s*(\d+)\s+(\d+)\s*$") {
        $aheadBehind = "behind $($Matches[1]), ahead $($Matches[2]) relative to $upstream"
    }
}

$apkPath = Join-Path $repoRoot "app\build\outputs\apk\debug\app-debug.apk"
$buildConfigPath = Join-Path $repoRoot "app\build\generated\source\buildConfig\debug\com\dealplanner\BuildConfig.java"
$apk = if (Test-Path -LiteralPath $apkPath) { Get-Item -LiteralPath $apkPath } else { $null }
$buildBranch = Get-BuildConfigValue $buildConfigPath "GIT_BRANCH"
$buildCommit = Get-BuildConfigValue $buildConfigPath "GIT_SHA"
$buildDirty = Get-BuildConfigValue $buildConfigPath "GIT_DIRTY"
$buildGeminiKey = Get-BuildConfigValue $buildConfigPath "GEMINI_API_KEY"
$buildGeminiModel = Get-BuildConfigValue $buildConfigPath "GEMINI_MODEL"
$apkIdentity = if ($buildCommit -eq $commit -and $buildDirty -eq "false") {
    "matches current clean HEAD"
} elseif ([string]::IsNullOrWhiteSpace($buildCommit)) {
    "not available; build the debug APK"
} else {
    "does not match current clean HEAD; rebuild before phone testing"
}

$localPropertiesPath = Join-Path $repoRoot "local.properties"
$localGeminiKey = Get-LocalPropertyValue $localPropertiesPath "gemini.api.key"
$localGeminiModel = Get-LocalPropertyValue $localPropertiesPath "gemini.model"
$envGeminiKey = $env:GEMINI_API_KEY
$envGeminiModel = $env:GEMINI_MODEL
$localGeminiReady = Test-RealGeminiKey $localGeminiKey
$envGeminiReady = Test-RealGeminiKey $envGeminiKey
$buildGeminiReady = Test-RealGeminiKey $buildGeminiKey
$configuredModel = if (-not [string]::IsNullOrWhiteSpace($localGeminiModel)) {
    $localGeminiModel
} elseif (-not [string]::IsNullOrWhiteSpace($envGeminiModel)) {
    $envGeminiModel
} elseif (-not [string]::IsNullOrWhiteSpace($buildGeminiModel)) {
    $buildGeminiModel
} else {
    "gemini-3.5-flash"
}

$adb = Get-AdbStatus
$latestPhoneReport = Get-LatestReport (Join-Path $repoRoot "phone-test-results") "PHONE_TEST_REPORT.md"
$latestFeatureReport = Get-LatestReport (Join-Path $repoRoot "phone-test-results") "FEATURE_READINESS_REPORT.md"
$latestSamples = Get-LatestSampleDir (Join-Path $repoRoot "phone-test-samples")

Write-Host "Deal Planner current status"
Write-Host "Repo: $repoRoot"

Write-Section "Source"
Write-Host "Branch: $branch"
Write-Host "Commit: $commit"
Write-Host "Remote: $remote"
Write-Host "Sync: $aheadBehind"
Write-Host "Git status:"
Write-Host $status

Write-Section "Debug APK"
if ($apk) {
    Write-Host "APK: $($apk.FullName)"
    Write-Host "APK updated: $($apk.LastWriteTime)"
    Write-Host "APK size: $($apk.Length) bytes"
} else {
    Write-Host "APK: not built"
}
Write-Host "APK source branch: $buildBranch"
Write-Host "APK source commit: $buildCommit"
Write-Host "APK source dirty: $buildDirty"
Write-Host "APK identity: $apkIdentity"

Write-Section "Gemini"
Write-Host "Local key present: $(Format-Boolean $localGeminiReady)"
Write-Host "Environment key present: $(Format-Boolean $envGeminiReady)"
Write-Host "APK key compiled: $(Format-Boolean $buildGeminiReady)"
Write-Host "Model: $configuredModel"

Write-Section "Phone"
Write-Host "ADB status: $($adb.Status)"
Write-Host "Detail: $($adb.Detail)"
Write-Host "adb devices:"
Write-Host $adb.Devices

Write-Section "Latest Evidence"
Write-Host "Phone report: $(if ($latestPhoneReport) { $latestPhoneReport.FullName } else { 'not generated yet' })"
Write-Host "Feature readiness report: $(if ($latestFeatureReport) { $latestFeatureReport.FullName } else { 'not generated yet' })"
Write-Host "Sample folder: $(if ($latestSamples) { $latestSamples.FullName } else { 'not generated yet' })"

Write-Section "Next Commands"
Write-Host "Normal phone setup:"
Write-Host "  .\scripts\start-phone-test-run.ps1 -WaitForPhone"
Write-Host "Final AI phone pass after adding a real Gemini key:"
Write-Host "  .\scripts\start-phone-test-run.ps1 -WaitForPhone -RequireGemini -TestGeminiLive -TestGeminiImage"
Write-Host "Fresh local readiness snapshot:"
Write-Host "  .\scripts\new-feature-readiness-report.ps1 -RunGate"

if ($adb.Status -ne "ready") {
    Write-Host ""
    Write-Host "Phone note: connect the Android phone, enable USB debugging, accept the prompt, and wait for adb devices to show device."
}

if (-not ($localGeminiReady -or $envGeminiReady)) {
    Write-Host "Gemini note: no real local/env key is configured yet; no-key OCR fallback can still be phone-tested."
} elseif (-not $buildGeminiReady) {
    Write-Host "Gemini note: a key is available to the build environment, but the current APK does not include it. Rebuild/reinstall before AI phone testing."
}
