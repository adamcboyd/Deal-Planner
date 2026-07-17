param(
    [switch]$Help,
    [switch]$SkipBuild,
    [switch]$NoLaunch,
    [string]$PackageName = "com.dealplanner",
    [string]$AppLabel = "Deal Planner",
    [string]$JavaHome = "C:\Program Files\Java\jdk-20"
)

$ErrorActionPreference = "Stop"

function Show-Usage {
    Write-Host "Deal Planner debug APK phone install"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\phone-debug-install.ps1"
    Write-Host "  .\scripts\phone-debug-install.ps1 -SkipBuild"
    Write-Host "  .\scripts\phone-debug-install.ps1 -SkipBuild -NoLaunch"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -SkipBuild       Reuse the existing debug APK after freshness checks."
    Write-Host "  -NoLaunch        Install but do not launch the app with adb monkey."
    Write-Host "  -PackageName ID  Expected Android package. Default: com.dealplanner"
    Write-Host "  -AppLabel NAME   Expected app label. Default: Deal Planner"
    Write-Host "  -JavaHome PATH   Java home used for Gradle builds. Default: C:\Program Files\Java\jdk-20"
    Write-Host ""
    Write-Host "Set ANDROID_SERIAL when more than one authorized Android device is connected."
}

if ($Help) {
    Show-Usage
    exit 0
}

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

function Write-BuildConfigSummary {
    param([string]$Root)

    $buildConfigPath = Join-Path $Root "app\build\generated\source\buildConfig\debug\com\dealplanner\BuildConfig.java"
    $sourceBranch = Get-BuildConfigValue $buildConfigPath "GIT_BRANCH"
    $sourceSha = Get-BuildConfigValue $buildConfigPath "GIT_SHA"
    $sourceDirty = Get-BuildConfigValue $buildConfigPath "GIT_DIRTY"
    $geminiModel = Get-BuildConfigValue $buildConfigPath "GEMINI_MODEL"
    $geminiKey = Get-BuildConfigValue $buildConfigPath "GEMINI_API_KEY"

    if ($sourceBranch -and $sourceSha) {
        $dirtyLabel = if ($sourceDirty -eq "true") { "dirty" } else { "clean" }
        Write-Host "Generated APK source identity: $sourceBranch @ $sourceSha ($dirtyLabel BuildConfig)"
    } else {
        Write-Warning "Generated BuildConfig source identity was not found. Rebuild before phone identity checks."
    }

    if ([string]::IsNullOrWhiteSpace($geminiModel)) {
        Write-Warning "Generated BuildConfig Gemini model was not found."
    } else {
        Write-Host "Generated APK Gemini model: $($geminiModel.Trim())"
    }

    $geminiConfigured = Test-RealGeminiKey $geminiKey
    Write-Host "Generated APK Gemini configured: $geminiConfigured (key value not printed)"
}

function Get-RelativeRepoPath {
    param(
        [string]$Root,
        [string]$Path
    )

    if ($Path.StartsWith($Root, [System.StringComparison]::OrdinalIgnoreCase)) {
        return $Path.Substring($Root.Length).TrimStart('\', '/')
    }

    return $Path
}

function Get-LatestBuildInput {
    param([string]$Root)

    $candidatePaths = @(
        "app\src\main",
        "app\build.gradle.kts",
        "build.gradle.kts",
        "settings.gradle.kts",
        "gradle.properties"
    )

    $items = foreach ($candidatePath in $candidatePaths) {
        $fullPath = Join-Path $Root $candidatePath
        if (-not (Test-Path $fullPath)) {
            continue
        }

        $item = Get-Item $fullPath
        if ($item.PSIsContainer) {
            Get-ChildItem -LiteralPath $fullPath -Recurse -File -Force
        } else {
            $item
        }
    }

    return @($items | Sort-Object LastWriteTime -Descending | Select-Object -First 1)
}

function Get-AndroidBuildTool {
    param([string]$ToolName)

    $pathCommand = Get-Command $ToolName -ErrorAction SilentlyContinue
    if ($pathCommand) {
        return $pathCommand.Source
    }

    $sdkRoots = @(
        $env:ANDROID_HOME,
        $env:ANDROID_SDK_ROOT,
        (Join-Path $env:LOCALAPPDATA "Android\Sdk")
    ) | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -Unique

    foreach ($sdkRoot in $sdkRoots) {
        $buildToolsRoot = Join-Path $sdkRoot "build-tools"
        if (-not (Test-Path $buildToolsRoot)) {
            continue
        }

        $tool = Get-ChildItem -LiteralPath $buildToolsRoot -Recurse -Filter $ToolName -ErrorAction SilentlyContinue |
            Sort-Object FullName -Descending |
            Select-Object -First 1

        if ($tool) {
            return $tool.FullName
        }
    }

    return $null
}

function Assert-ApkIdentity {
    param(
        [string]$ApkPath,
        [string]$ExpectedPackageName,
        [string]$ExpectedAppLabel
    )

    $aaptPath = Get-AndroidBuildTool "aapt.exe"
    if (-not $aaptPath) {
        Write-Warning "aapt.exe was not found; package/permission inspection skipped."
        return
    }

    $badging = @(& $aaptPath dump badging $ApkPath 2>$null)
    if ($LASTEXITCODE -ne 0) {
        throw "Could not inspect app-debug.apk with aapt dump badging."
    }

    $permissions = @(& $aaptPath dump permissions $ApkPath 2>$null)
    if ($LASTEXITCODE -ne 0) {
        throw "Could not inspect app-debug.apk with aapt dump permissions."
    }

    $packageLine = $badging | Where-Object { $_ -match "^package:" } | Select-Object -First 1
    $labelLine = $badging | Where-Object { $_ -match "^application-label:" } | Select-Object -First 1
    $packageOk = $packageLine -match "name='$([regex]::Escape($ExpectedPackageName))'"
    $labelOk = $labelLine -match "application-label:'$([regex]::Escape($ExpectedAppLabel))'"

    if (-not ($packageOk -and $labelOk)) {
        throw "Expected APK identity $ExpectedPackageName / $ExpectedAppLabel, but app-debug.apk reported: $packageLine $labelLine"
    }

    $requiredPermissions = @(
        "android.permission.INTERNET",
        "android.permission.CAMERA"
    )
    $missingPermissions = @(
        foreach ($permission in $requiredPermissions) {
            if (-not ($permissions | Where-Object { $_ -match "name='$([regex]::Escape($permission))'" })) {
                $permission
            }
        }
    )

    if ($missingPermissions.Count -gt 0) {
        throw "app-debug.apk is missing required permission(s): $($missingPermissions -join ', ')"
    }

    $unneededStoragePermissions = @(
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.MANAGE_EXTERNAL_STORAGE",
        "android.permission.READ_MEDIA_IMAGES",
        "android.permission.READ_MEDIA_VIDEO",
        "android.permission.READ_MEDIA_AUDIO"
    )
    $presentStoragePermissions = @(
        foreach ($permission in $unneededStoragePermissions) {
            if ($permissions | Where-Object { $_ -match "name='$([regex]::Escape($permission))'" }) {
                $permission
            }
        }
    )

    if ($presentStoragePermissions.Count -gt 0) {
        throw "app-debug.apk unexpectedly requests broad storage/media permission(s): $($presentStoragePermissions -join ', ')"
    }

    Write-Host "Verified APK identity and scoped permissions: $ExpectedPackageName / $ExpectedAppLabel"
}

function Assert-ApkFreshForBuildInputs {
    param(
        [string]$ApkPath,
        [bool]$SkipBuildRequested
    )

    if (-not $SkipBuildRequested) {
        return
    }

    $apkInfo = Get-Item $ApkPath
    $latestBuildInput = Get-LatestBuildInput $repoRoot
    if ($latestBuildInput.Count -eq 0) {
        return
    }

    if ($latestBuildInput[0].LastWriteTime -gt $apkInfo.LastWriteTime) {
        $relativePath = Get-RelativeRepoPath $repoRoot $latestBuildInput[0].FullName
        throw "$relativePath is newer than app-debug.apk. Run .\scripts\phone-debug-install.ps1 without -SkipBuild so the phone gets the current app code/resources."
    }
}

function Assert-ApkFreshForGeminiConfig {
    param(
        [string]$ApkPath,
        [bool]$SkipBuildRequested
    )

    if (-not $SkipBuildRequested) {
        return
    }

    $localPropertiesPath = Join-Path $repoRoot "local.properties"
    if (Test-Path $localPropertiesPath) {
        $apkInfo = Get-Item $ApkPath
        $localPropertiesInfo = Get-Item $localPropertiesPath
        if ($localPropertiesInfo.LastWriteTime -gt $apkInfo.LastWriteTime) {
            throw "local.properties is newer than app-debug.apk. Run .\scripts\phone-debug-install.ps1 without -SkipBuild so Gemini key/model values are rebuilt into BuildConfig."
        }
    }

    if ((Test-RealGeminiKey $env:GEMINI_API_KEY) -or -not [string]::IsNullOrWhiteSpace($env:GEMINI_MODEL)) {
        Write-Warning "SkipBuild reuses the existing APK and cannot verify whether current GEMINI_* environment values are compiled into BuildConfig. Run without -SkipBuild after changing Gemini environment values."
    }
}

function Assert-BuildConfigMatchesGitHead {
    param([string]$Root)

    if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
        Write-Warning "git was not found on PATH; BuildConfig source identity could not be compared with current HEAD."
        return
    }

    $buildConfigPath = Join-Path $Root "app\build\generated\source\buildConfig\debug\com\dealplanner\BuildConfig.java"
    $sourceBranch = Get-BuildConfigValue $buildConfigPath "GIT_BRANCH"
    $sourceSha = Get-BuildConfigValue $buildConfigPath "GIT_SHA"
    if ([string]::IsNullOrWhiteSpace($sourceBranch) -or [string]::IsNullOrWhiteSpace($sourceSha)) {
        throw "Generated BuildConfig source identity was not found. Run .\scripts\phone-debug-install.ps1 without -SkipBuild so Settings -> About can identify the installed APK."
    }

    $currentBranch = (& git rev-parse --abbrev-ref HEAD 2>$null)
    $currentSha = (& git rev-parse --short HEAD 2>$null)
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($currentSha)) {
        Write-Warning "Could not read current Git HEAD; BuildConfig source identity could not be compared."
        return
    }

    if ($sourceBranch -ne $currentBranch -or $sourceSha -ne $currentSha) {
        throw "Generated APK source identity is $sourceBranch @ $sourceSha, but current Git identity is $currentBranch @ $currentSha. Run .\scripts\phone-debug-install.ps1 without -SkipBuild, or run .\gradlew.bat assembleDebug before installing."
    }
}

function Assert-InstalledPackage {
    param(
        [string]$DeviceSerial,
        [string]$ExpectedPackageName
    )

    $packagePath = @(& adb -s $DeviceSerial shell pm path $ExpectedPackageName 2>$null)
    if ($LASTEXITCODE -ne 0 -or -not ($packagePath | Where-Object { $_ -match "^package:" })) {
        throw "Install finished, but $ExpectedPackageName was not found on device $DeviceSerial."
    }

    Write-Host "Verified installed package on device: $ExpectedPackageName"
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

Assert-ApkIdentity -ApkPath $apkPath -ExpectedPackageName $PackageName -ExpectedAppLabel $AppLabel
Assert-ApkFreshForBuildInputs -ApkPath $apkPath -SkipBuildRequested $SkipBuild.IsPresent
Assert-ApkFreshForGeminiConfig -ApkPath $apkPath -SkipBuildRequested $SkipBuild.IsPresent
Assert-BuildConfigMatchesGitHead -Root $repoRoot
Write-BuildConfigSummary -Root $repoRoot

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
        throw "A phone is visible but not ready. Unlock the phone, accept the USB debugging prompt, confirm adb devices shows device, then retry."
    }
    throw "No connected/authorized Android phone found. Connect the phone, enable Developer options > USB debugging, choose a data-capable USB mode/cable, confirm adb devices shows device, then retry."
} else {
    Write-Host ($adbOutput -join [Environment]::NewLine)
    throw "Multiple authorized devices found. Set ANDROID_SERIAL to one device serial, then retry."
}

Invoke-Checked "Install Deal Planner debug APK on $deviceSerial" {
    adb -s $deviceSerial install -r $apkPath
}

Assert-InstalledPackage -DeviceSerial $deviceSerial -ExpectedPackageName $PackageName

if (-not $NoLaunch) {
    Invoke-Checked "Launch Deal Planner on $deviceSerial" {
        adb -s $deviceSerial shell monkey -p $PackageName -c android.intent.category.LAUNCHER 1
    }
}

Write-Host ""
Write-Host "Deal Planner is installed. On the phone, tap Load Demo and follow RECOVERY_STATUS_2026-07-16.md."
