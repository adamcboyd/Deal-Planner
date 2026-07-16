param(
    [switch]$RequirePhone,
    [switch]$SkipNetwork,
    [string]$JavaHome = "C:\Program Files\Java\jdk-20",
    [string]$PackageName = "com.dealplanner"
)

$ErrorActionPreference = "Stop"

function Add-Check {
    param(
        [System.Collections.Generic.List[object]]$Results,
        [string]$Name,
        [ValidateSet("OK", "WARN", "FAIL")]
        [string]$Status,
        [string]$Detail
    )

    $Results.Add([pscustomobject]@{
        Name = $Name
        Status = $Status
        Detail = $Detail
    }) | Out-Null

    Write-Host ("[{0}] {1} - {2}" -f $Status, $Name, $Detail)
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

function Test-RealKey {
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

$results = [System.Collections.Generic.List[object]]::new()
$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

Write-Host "Deal Planner phone preflight"
Write-Host "Repo: $repoRoot"
Write-Host ""

if (Test-Path ".\gradlew.bat") {
    Add-Check $results "Repo root" "OK" "Gradle wrapper found."
} else {
    Add-Check $results "Repo root" "FAIL" "gradlew.bat not found. Keep this script under scripts\ in the Deal Planner repo."
}

if (Get-Command git -ErrorAction SilentlyContinue) {
    $branch = (& git rev-parse --abbrev-ref HEAD 2>$null)
    $shortStatus = @(& git status --short 2>$null)
    if ($LASTEXITCODE -eq 0) {
        if ($shortStatus.Count -eq 0) {
            Add-Check $results "Git branch" "OK" "$branch is clean."
        } else {
            Add-Check $results "Git branch" "WARN" "$branch has $($shortStatus.Count) uncommitted path(s)."
        }
    } else {
        Add-Check $results "Git branch" "WARN" "Could not read git status."
    }
} else {
    Add-Check $results "Git branch" "WARN" "git was not found on PATH."
}

if (Test-Path $JavaHome) {
    Add-Check $results "Java" "OK" "Configured JavaHome exists: $JavaHome"
} elseif ($env:JAVA_HOME -and (Test-Path $env:JAVA_HOME)) {
    Add-Check $results "Java" "OK" "JAVA_HOME exists: $env:JAVA_HOME"
} else {
    Add-Check $results "Java" "WARN" "Configured JavaHome was not found. Build commands may still work if java is on PATH."
}

$apkInfo = $null
$apkPath = Join-Path $repoRoot "app\build\outputs\apk\debug\app-debug.apk"
if (Test-Path $apkPath) {
    $apkInfo = Get-Item $apkPath
    Add-Check $results "Debug APK" "OK" ("{0} bytes, last written {1}" -f $apkInfo.Length, $apkInfo.LastWriteTime)
} else {
    Add-Check $results "Debug APK" "WARN" "Debug APK not found. Run .\gradlew.bat assembleDebug or .\scripts\phone-debug-install.ps1."
}

$adbCommand = Get-Command adb -ErrorAction SilentlyContinue
if ($null -eq $adbCommand) {
    Add-Check $results "ADB" "FAIL" "adb was not found on PATH. Add Android platform-tools or open Android Studio."
} else {
    Add-Check $results "ADB" "OK" "adb found at $($adbCommand.Source)"
    $adbOutput = @(& adb devices)
    $authorizedDevices = @($adbOutput | Where-Object { $_ -match "^(\S+)\s+device$" } | ForEach-Object { ($_.Trim() -split "\s+")[0] })
    $problemDevices = @($adbOutput | Where-Object { $_ -match "^(\S+)\s+(unauthorized|offline)$" })

    if ($env:ANDROID_SERIAL) {
        if ($authorizedDevices -contains $env:ANDROID_SERIAL) {
            Add-Check $results "Android phone" "OK" "ANDROID_SERIAL device is connected and authorized."
        } else {
            $status = if ($RequirePhone) { "FAIL" } else { "WARN" }
            Add-Check $results "Android phone" $status "ANDROID_SERIAL is set to '$env:ANDROID_SERIAL', but that device is not connected/authorized."
        }
    } elseif ($authorizedDevices.Count -eq 1) {
        Add-Check $results "Android phone" "OK" "One authorized device found: $($authorizedDevices[0])"
    } elseif ($authorizedDevices.Count -gt 1) {
        $status = if ($RequirePhone) { "FAIL" } else { "WARN" }
        Add-Check $results "Android phone" $status "Multiple authorized devices found. Set ANDROID_SERIAL before install."
    } elseif ($problemDevices.Count -gt 0) {
        $status = if ($RequirePhone) { "FAIL" } else { "WARN" }
        Add-Check $results "Android phone" $status "Phone is visible but unauthorized/offline. Unlock it and accept USB debugging."
    } else {
        $status = if ($RequirePhone) { "FAIL" } else { "WARN" }
        Add-Check $results "Android phone" $status "No connected/authorized phone found yet."
    }
}

$localPropertiesPath = Join-Path $repoRoot "local.properties"
$localGeminiKey = Get-LocalPropertyValue $localPropertiesPath "gemini.api.key"
$localGeminiModel = Get-LocalPropertyValue $localPropertiesPath "gemini.model"
$envGeminiKey = $env:GEMINI_API_KEY
$envGeminiModel = $env:GEMINI_MODEL

if (Test-RealKey $localGeminiKey) {
    Add-Check $results "Gemini key" "OK" "local.properties contains a non-placeholder Gemini key. Key value was not printed."
} elseif (Test-RealKey $envGeminiKey) {
    Add-Check $results "Gemini key" "OK" "GEMINI_API_KEY is set. Key value was not printed."
} else {
    Add-Check $results "Gemini key" "WARN" "No non-placeholder Gemini key found. Pantry photos will use OCR fallback."
}

$model = if ($localGeminiModel) { $localGeminiModel } elseif ($envGeminiModel) { $envGeminiModel } else { "gemini-3.5-flash" }
Add-Check $results "Gemini model" "OK" "Build model setting resolves to $($model.Trim())."

if ($apkInfo -and (Test-Path $localPropertiesPath)) {
    $localPropertiesInfo = Get-Item $localPropertiesPath
    if ($localPropertiesInfo.LastWriteTime -gt $apkInfo.LastWriteTime) {
        Add-Check $results "Gemini APK freshness" "WARN" "local.properties is newer than app-debug.apk. Rebuild before AI phone testing so BuildConfig has the current key/model."
    } else {
        Add-Check $results "Gemini APK freshness" "OK" "app-debug.apk is newer than local.properties."
    }
} elseif ($apkInfo -and ((Test-RealKey $envGeminiKey) -or -not [string]::IsNullOrWhiteSpace($envGeminiModel))) {
    Add-Check $results "Gemini APK freshness" "WARN" "GEMINI_* environment values cannot be timestamp-checked against app-debug.apk. Rebuild before AI phone testing if they changed."
}

if ($SkipNetwork) {
    Add-Check $results "Open Food Facts" "WARN" "Network check skipped."
} else {
    try {
        $headers = @{
            "User-Agent" = "DealPlanner/1.0 (https://github.com/adamcboyd/Deal-Planner)"
        }
        $uri = "https://world.openfoodfacts.org/api/v3/product/3017624010701.json?fields=code,status,result,product_name"
        $response = Invoke-WebRequest -Uri $uri -Headers $headers -UseBasicParsing -TimeoutSec 20
        if ($response.Content -match '"product_found"') {
            Add-Check $results "Open Food Facts" "OK" "Barcode lookup endpoint reachable."
        } else {
            Add-Check $results "Open Food Facts" "WARN" "Endpoint returned a response, but not the expected product_found marker."
        }
    } catch {
        Add-Check $results "Open Food Facts" "WARN" "Barcode lookup network check failed: $($_.Exception.Message)"
    }
}

if (Test-Path ".\PHONE_TEST_CHECKLIST_2026-07-16.md") {
    Add-Check $results "Phone checklist" "OK" "PHONE_TEST_CHECKLIST_2026-07-16.md is present."
} else {
    Add-Check $results "Phone checklist" "WARN" "Phone checklist file not found."
}

Write-Host ""
$failCount = @($results | Where-Object { $_.Status -eq "FAIL" }).Count
$warnCount = @($results | Where-Object { $_.Status -eq "WARN" }).Count
Write-Host ("Summary: {0} failure(s), {1} warning(s)." -f $failCount, $warnCount)

if ($failCount -gt 0) {
    exit 1
}

Write-Host "Preflight complete. For install/launch, run .\scripts\phone-debug-install.ps1"
