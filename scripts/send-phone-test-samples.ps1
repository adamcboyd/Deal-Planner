param(
    [switch]$Help,
    [switch]$Generate,
    [string]$SamplesDir = "",
    [string]$RemoteRoot = "/sdcard/Download/DealPlannerPhoneTestSamples"
)

$ErrorActionPreference = "Stop"

function Show-Usage {
    Write-Host "Deal Planner phone test sample transfer"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\send-phone-test-samples.ps1"
    Write-Host "  .\scripts\send-phone-test-samples.ps1 -Generate"
    Write-Host "  .\scripts\send-phone-test-samples.ps1 -SamplesDir phone-test-samples\20260716-103940"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -Generate        Create a fresh local sample folder before copying."
    Write-Host "  -SamplesDir PATH Copy this local sample folder instead of the latest generated folder."
    Write-Host "  -RemoteRoot PATH Android destination root. Default: /sdcard/Download/DealPlannerPhoneTestSamples"
    Write-Host "                   Sample folders must include SAMPLE_MANIFEST.md from new-phone-test-samples.ps1."
    Write-Host ""
    Write-Host "Set ANDROID_SERIAL when more than one authorized Android device is connected."
}

if ($Help) {
    Show-Usage
    exit 0
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

function Invoke-AdbOutput {
    param(
        [string]$Label,
        [string]$DeviceSerial,
        [string[]]$AdbArgs
    )

    $output = @(& adb -s $DeviceSerial @AdbArgs 2>&1)
    if ($LASTEXITCODE -ne 0) {
        throw "$Label failed with exit code $LASTEXITCODE. $($output -join ' ')"
    }

    return $output
}

function Get-RemoteFileSize {
    param(
        [string]$DeviceSerial,
        [string]$RemotePath
    )

    $output = Invoke-AdbOutput "Read remote file size" $DeviceSerial @("shell", "wc", "-c", $RemotePath)
    $line = ($output -join " ").Trim()
    if ($line -match "^\s*(\d+)") {
        return [int64]$Matches[1]
    }

    throw "Could not parse remote file size for $RemotePath from: $line"
}

function Invoke-MediaScan {
    param(
        [string]$DeviceSerial,
        [string]$RemotePath
    )

    $scanOutput = @(
        & adb -s $DeviceSerial shell am broadcast `
            -a android.intent.action.MEDIA_SCANNER_SCAN_FILE `
            -d "file://$RemotePath" 2>&1
    )
    if ($LASTEXITCODE -ne 0) {
        Write-Warning "Media scan request failed for $RemotePath. The file was copied, but Android's picker may take longer to show it. $($scanOutput -join ' ')"
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

function Get-LatestSampleDir {
    param([string]$Root)

    $sampleRoot = Join-Path $Root "phone-test-samples"
    if (-not (Test-Path $sampleRoot)) {
        return $null
    }

    return Get-ChildItem -LiteralPath $sampleRoot -Directory |
        Sort-Object Name -Descending |
        Select-Object -First 1
}

function Resolve-SampleDir {
    param(
        [string]$Root,
        [string]$Path
    )

    if ([string]::IsNullOrWhiteSpace($Path)) {
        return $null
    }

    if ([System.IO.Path]::IsPathRooted($Path)) {
        return $Path
    }

    return Join-Path $Root $Path
}

function Test-PdfFile {
    param([string]$Path)

    $bytes = [System.IO.File]::ReadAllBytes($Path)
    if ($bytes.Length -lt 12) {
        return $false
    }

    $header = [System.Text.Encoding]::ASCII.GetString($bytes, 0, [Math]::Min(8, $bytes.Length))
    $tailLength = [Math]::Min(256, $bytes.Length)
    $tail = [System.Text.Encoding]::ASCII.GetString($bytes, $bytes.Length - $tailLength, $tailLength)
    return $header.StartsWith("%PDF-") -and $tail.Contains("%%EOF")
}

function Test-PngFile {
    param([string]$Path)

    $bytes = [System.IO.File]::ReadAllBytes($Path)
    if ($bytes.Length -lt 8) {
        return $false
    }

    $signature = ($bytes[0..7] | ForEach-Object { $_.ToString("X2") }) -join ""
    return $signature -eq "89504E470D0A1A0A"
}

function Assert-SampleFolder {
    param([string]$Path)

    if ([string]::IsNullOrWhiteSpace($Path) -or -not (Test-Path $Path)) {
        throw "Phone test sample folder was not found. Run .\scripts\new-phone-test-samples.ps1 first."
    }

    $requiredFiles = @(
        "deal-planner-demo-receipt.txt",
        "deal-planner-demo-receipt.pdf",
        "deal-planner-demo-receipt.png",
        "deal-planner-demo-flyer.txt",
        "deal-planner-demo-flyer.pdf",
        "deal-planner-demo-flyer.png",
        "deal-planner-demo-pantry-label.txt",
        "deal-planner-demo-pantry-label.png",
        "deal-planner-demo-upc-a.txt",
        "deal-planner-demo-upc-a.png",
        "README.md",
        "SAMPLE_MANIFEST.md"
    )

    foreach ($fileName in $requiredFiles) {
        $fullPath = Join-Path $Path $fileName
        if (-not (Test-Path $fullPath)) {
            throw "Sample folder is missing $fileName."
        }
    }

    foreach ($pdfName in @("deal-planner-demo-receipt.pdf", "deal-planner-demo-flyer.pdf")) {
        $pdfPath = Join-Path $Path $pdfName
        if (-not (Test-PdfFile $pdfPath)) {
            throw "$pdfName is not a valid generated PDF sample."
        }
    }

    foreach ($pngName in @("deal-planner-demo-receipt.png", "deal-planner-demo-flyer.png", "deal-planner-demo-pantry-label.png", "deal-planner-demo-upc-a.png")) {
        $pngPath = Join-Path $Path $pngName
        if (-not (Test-PngFile $pngPath)) {
            throw "$pngName is not a valid generated PNG sample."
        }
    }
}

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

if (-not (Test-Path ".\gradlew.bat")) {
    throw "Run this script from the Deal Planner repo, or keep it under scripts\ in that repo."
}

if ($Generate) {
    & (Join-Path $PSScriptRoot "new-phone-test-samples.ps1")
}

$sampleDir = Resolve-SampleDir $repoRoot $SamplesDir
if (-not $sampleDir) {
    $latest = Get-LatestSampleDir $repoRoot
    if (-not $latest) {
        Write-Host "No generated sample folder found; creating one now."
        & (Join-Path $PSScriptRoot "new-phone-test-samples.ps1")
        $latest = Get-LatestSampleDir $repoRoot
    }
    $sampleDir = $latest.FullName
}

Assert-SampleFolder $sampleDir

$deviceSerial = Get-AdbDeviceSerial
$sampleItem = Get-Item -LiteralPath $sampleDir
$remoteRootClean = $RemoteRoot.TrimEnd("/")
$remoteSessionDir = "$remoteRootClean/$($sampleItem.Name)"
$transferReportFileName = "PHONE_SAMPLE_TRANSFER.md"
$filesToCopy = @(
    Get-ChildItem -LiteralPath $sampleDir -File |
        Where-Object { $_.Name -ne $transferReportFileName } |
        Sort-Object Name
)
$verifiedFiles = [System.Collections.Generic.List[object]]::new()

Write-Host "Deal Planner phone test sample transfer"
Write-Host "Repo: $repoRoot"
Write-Host "Device: $deviceSerial"
Write-Host "Local samples: $sampleDir"
Write-Host "Android destination: $remoteSessionDir"
Write-Host ""

Invoke-AdbChecked "Create Android sample folder" $deviceSerial @("shell", "mkdir", "-p", $remoteSessionDir)

foreach ($file in $filesToCopy) {
    $remotePath = "$remoteSessionDir/$($file.Name)"
    Invoke-AdbChecked "Copy $($file.Name)" $deviceSerial @("push", $file.FullName, $remotePath)

    $remoteSize = Get-RemoteFileSize $deviceSerial $remotePath
    if ($remoteSize -ne $file.Length) {
        throw "Remote size mismatch for $($file.Name): local $($file.Length) byte(s), remote $remoteSize byte(s)."
    }
    Write-Host "Verified remote size for $($file.Name): $remoteSize byte(s)."
    $verifiedFiles.Add([pscustomobject]@{
        Name = $file.Name
        LocalBytes = $file.Length
        RemoteBytes = $remoteSize
    }) | Out-Null

    Invoke-MediaScan $deviceSerial $remotePath
}

$verifiedRows = foreach ($verifiedFile in $verifiedFiles) {
    "- $($verifiedFile.Name): local $($verifiedFile.LocalBytes) byte(s), remote $($verifiedFile.RemoteBytes) byte(s)"
}
$transferReportPath = Join-Path $sampleDir $transferReportFileName
$transferReport = @"
# Deal Planner Phone Sample Transfer

- Created: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")
- Device serial: $deviceSerial
- Local samples: $sampleDir
- Android destination: $remoteSessionDir
- Remote file sizes verified: true
- Android media scans requested: true

## Verified Files

$($verifiedRows -join [Environment]::NewLine)
"@
$transferReport | Set-Content -LiteralPath $transferReportPath -Encoding UTF8

Write-Host ""
Write-Host "Copied phone test samples to:"
Write-Host "  $remoteSessionDir"
Write-Host "Remote file sizes were verified, and Android media scan broadcasts were requested for picker visibility."
Write-Host "Wrote local transfer report:"
Write-Host "  $transferReportPath"
Write-Host ""
Write-Host "On the phone, open Files or the Android picker at Downloads > DealPlannerPhoneTestSamples > $($sampleItem.Name)."
