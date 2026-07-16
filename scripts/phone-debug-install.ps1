param(
    [switch]$SkipBuild,
    [switch]$NoLaunch,
    [string]$PackageName = "com.dealplanner",
    [string]$JavaHome = "C:\Program Files\Java\jdk-20"
)

$ErrorActionPreference = "Stop"

function Invoke-Checked {
    param(
        [string]$Label,
        [scriptblock]$Command
    )

    Write-Host ""
    Write-Host "==> $Label"
    & $Command
    if ($LASTEXITCODE -ne 0) {
        throw "$Label failed with exit code $LASTEXITCODE."
    }
}

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

if (-not (Test-Path ".\gradlew.bat")) {
    throw "Run this script from the Deal Planner repo, or keep it under scripts\ in that repo."
}

if (-not $SkipBuild) {
    if (Test-Path $JavaHome) {
        $env:JAVA_HOME = $JavaHome
        $env:Path = "$env:JAVA_HOME\bin;$env:Path"
    } else {
        Write-Warning "Configured JavaHome was not found: $JavaHome. Using the current JAVA_HOME/PATH."
    }

    Invoke-Checked "Gradle verification and debug APK build" {
        .\gradlew.bat testDebugUnitTest assembleDebug lintDebug
    }
}

$apkPath = Join-Path $repoRoot "app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $apkPath)) {
    throw "Debug APK not found at $apkPath. Run without -SkipBuild first."
}

$adbCommand = Get-Command adb -ErrorAction SilentlyContinue
if ($null -eq $adbCommand) {
    throw "adb was not found on PATH. Open Android Studio once, or add Android platform-tools to PATH."
}

$adbOutput = & adb devices
$authorizedDevices = @($adbOutput | Where-Object { $_ -match "^(\S+)\s+device$" } | ForEach-Object { ($_.Trim() -split "\s+")[0] })
$problemDevices = @($adbOutput | Where-Object { $_ -match "^(\S+)\s+(unauthorized|offline)$" })

if ($env:ANDROID_SERIAL) {
    if ($authorizedDevices -notcontains $env:ANDROID_SERIAL) {
        Write-Host ($adbOutput -join [Environment]::NewLine)
        throw "ANDROID_SERIAL is set to '$env:ANDROID_SERIAL', but that device is not connected/authorized."
    }
    $deviceSerial = $env:ANDROID_SERIAL
} elseif ($authorizedDevices.Count -eq 1) {
    $deviceSerial = $authorizedDevices[0]
} elseif ($authorizedDevices.Count -eq 0) {
    Write-Host ($adbOutput -join [Environment]::NewLine)
    if ($problemDevices.Count -gt 0) {
        throw "A phone is visible but not ready. Unlock it and accept the USB debugging prompt, then retry."
    }
    throw "No connected/authorized Android phone found. Connect the phone with USB debugging enabled, then retry."
} else {
    Write-Host ($adbOutput -join [Environment]::NewLine)
    throw "Multiple authorized devices found. Set ANDROID_SERIAL to one device serial, then retry."
}

Invoke-Checked "Install Deal Planner debug APK on $deviceSerial" {
    adb -s $deviceSerial install -r $apkPath
}

if (-not $NoLaunch) {
    Invoke-Checked "Launch Deal Planner on $deviceSerial" {
        adb -s $deviceSerial shell monkey -p $PackageName -c android.intent.category.LAUNCHER 1
    }
}

Write-Host ""
Write-Host "Deal Planner is installed. On the phone, tap Load Demo and follow RECOVERY_STATUS_2026-07-16.md."
