param(
    [switch]$Help,
    [switch]$Clear,
    [switch]$Launch,
    [int]$DurationSeconds = 0,
    [string]$OutputDir = "phone-test-logs",
    [string]$PackageName = "com.dealplanner"
)

$ErrorActionPreference = "Stop"

function Show-Usage {
    Write-Host "Deal Planner phone log capture"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\phone-debug-logs.ps1"
    Write-Host "  .\scripts\phone-debug-logs.ps1 -Clear -Launch -DurationSeconds 90"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -Clear              Clear logcat before capture. Use before reproducing a bug."
    Write-Host "  -Launch             Launch Deal Planner before waiting/capturing."
    Write-Host "  -DurationSeconds N  Wait N seconds before dumping logs, so you can reproduce the issue."
    Write-Host "  -OutputDir PATH     Folder for logs. Default: phone-test-logs"
    Write-Host "  -PackageName NAME   Android package. Default: com.dealplanner"
    Write-Host ""
    Write-Host "Set ANDROID_SERIAL when more than one authorized device is connected."
}

function Invoke-AdbChecked {
    param(
        [string]$Label,
        [string]$DeviceSerial,
        [string[]]$AdbArgs
    )

    Write-Host "==> $Label"
    & adb -s $DeviceSerial @AdbArgs
    if ($LASTEXITCODE -ne 0) {
        throw "$Label failed with exit code $LASTEXITCODE."
    }
}

function Get-AdbDeviceSerial {
    $adbCommand = Get-Command adb -ErrorAction SilentlyContinue
    if ($null -eq $adbCommand) {
        throw "adb was not found on PATH. Open Android Studio once, or add Android platform-tools to PATH."
    }

    $adbOutput = @(& adb devices)
    $authorizedDevices = @(
        $adbOutput |
            Where-Object { $_ -match "^(\S+)\s+device$" } |
            ForEach-Object { ($_.Trim() -split "\s+")[0] }
    )
    $problemDevices = @($adbOutput | Where-Object { $_ -match "^(\S+)\s+(unauthorized|offline)$" })

    if ($env:ANDROID_SERIAL) {
        if ($authorizedDevices -notcontains $env:ANDROID_SERIAL) {
            Write-Host ($adbOutput -join [Environment]::NewLine)
            throw "ANDROID_SERIAL is set to '$env:ANDROID_SERIAL', but that device is not connected/authorized."
        }
        return $env:ANDROID_SERIAL
    }

    if ($authorizedDevices.Count -eq 1) {
        return $authorizedDevices[0]
    }

    Write-Host ($adbOutput -join [Environment]::NewLine)
    if ($authorizedDevices.Count -eq 0 -and $problemDevices.Count -gt 0) {
        throw "A phone is visible but not ready. Unlock it and accept the USB debugging prompt, then retry."
    }
    if ($authorizedDevices.Count -eq 0) {
        throw "No connected/authorized Android phone found. Connect the phone with USB debugging enabled, then retry."
    }

    throw "Multiple authorized devices found. Set ANDROID_SERIAL to one device serial, then retry."
}

function Write-AdbOutput {
    param(
        [string]$DeviceSerial,
        [string[]]$AdbArgs,
        [string]$Path
    )

    & adb -s $DeviceSerial @AdbArgs 2>&1 | Set-Content -LiteralPath $Path -Encoding UTF8
}

if ($Help) {
    Show-Usage
    exit 0
}

if ($DurationSeconds -lt 0) {
    throw "DurationSeconds must be zero or greater."
}

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

if (-not (Test-Path ".\gradlew.bat")) {
    throw "Run this script from the Deal Planner repo, or keep it under scripts\ in that repo."
}

$deviceSerial = Get-AdbDeviceSerial
$outputRoot = if ([System.IO.Path]::IsPathRooted($OutputDir)) {
    $OutputDir
} else {
    Join-Path $repoRoot $OutputDir
}

New-Item -ItemType Directory -Force -Path $outputRoot | Out-Null

$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$sessionDir = Join-Path $outputRoot $stamp
New-Item -ItemType Directory -Force -Path $sessionDir | Out-Null

Write-Host "Deal Planner phone log capture"
Write-Host "Repo: $repoRoot"
Write-Host "Device: $deviceSerial"
Write-Host "Output: $sessionDir"
Write-Host ""

Write-AdbOutput $deviceSerial @("devices", "-l") (Join-Path $sessionDir "adb-devices.txt")
Write-AdbOutput $deviceSerial @("shell", "getprop", "ro.product.manufacturer") (Join-Path $sessionDir "device-manufacturer.txt")
Write-AdbOutput $deviceSerial @("shell", "getprop", "ro.product.model") (Join-Path $sessionDir "device-model.txt")
Write-AdbOutput $deviceSerial @("shell", "getprop", "ro.build.version.release") (Join-Path $sessionDir "android-version.txt")
Write-AdbOutput $deviceSerial @("shell", "getprop", "ro.build.version.sdk") (Join-Path $sessionDir "android-sdk.txt")
Write-AdbOutput $deviceSerial @("shell", "dumpsys", "package", $PackageName) (Join-Path $sessionDir "package-dumpsys.txt")

if ($Clear) {
    Invoke-AdbChecked "Clear logcat" $deviceSerial @("logcat", "-c")
}

if ($Launch) {
    Invoke-AdbChecked "Launch Deal Planner" $deviceSerial @(
        "shell",
        "monkey",
        "-p",
        $PackageName,
        "-c",
        "android.intent.category.LAUNCHER",
        "1"
    )
}

if ($DurationSeconds -gt 0) {
    Write-Host "Waiting $DurationSeconds second(s). Reproduce the issue on the phone now."
    Start-Sleep -Seconds $DurationSeconds
}

$fullLogPath = Join-Path $sessionDir "logcat-full.txt"
$filteredLogPath = Join-Path $sessionDir "logcat-dealplanner-filtered.txt"

Write-AdbOutput $deviceSerial @("logcat", "-d", "-v", "time") $fullLogPath

$filterPattern = "dealplanner|DealPlanner|AndroidRuntime|FATAL EXCEPTION|Exception|ANR|ActivityTaskManager|InputDispatcher|Camera|Gemini|OpenFoodFacts|ML Kit|TextRecognition"
Get-Content -LiteralPath $fullLogPath |
    Where-Object { $_ -match $filterPattern } |
    Set-Content -LiteralPath $filteredLogPath -Encoding UTF8

$pid = (& adb -s $deviceSerial shell pidof $PackageName 2>$null)
$pidText = if ($LASTEXITCODE -eq 0) { ($pid -join " ").Trim() } else { "" }
$pidText | Set-Content -LiteralPath (Join-Path $sessionDir "dealplanner-pid.txt") -Encoding UTF8

Write-Host ""
Write-Host "Saved:"
Write-Host "  $fullLogPath"
Write-Host "  $filteredLogPath"
Write-Host ""
Write-Host "Send the filtered log first if a phone test fails; keep the full log for deeper debugging."
