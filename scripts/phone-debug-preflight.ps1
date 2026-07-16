param(
    [switch]$Help,
    [switch]$RequirePhone,
    [switch]$RequireGemini,
    [switch]$SkipNetwork,
    [string]$JavaHome = "C:\Program Files\Java\jdk-20",
    [string]$PackageName = "com.dealplanner",
    [string]$AppLabel = "Deal Planner"
)

$ErrorActionPreference = "Stop"

function Show-Usage {
    Write-Host "Deal Planner phone preflight"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\phone-debug-preflight.ps1"
    Write-Host "  .\scripts\phone-debug-preflight.ps1 -RequirePhone"
    Write-Host "  .\scripts\phone-debug-preflight.ps1 -RequirePhone -RequireGemini"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -RequirePhone    Fail if no connected and authorized Android phone is visible."
    Write-Host "  -RequireGemini   Fail if a real Gemini key/build cannot be verified for AI testing."
    Write-Host "  -SkipNetwork     Skip GitHub and Open Food Facts network checks."
    Write-Host "  -JavaHome PATH   Java home used for readiness checks. Default: C:\Program Files\Java\jdk-20"
    Write-Host "  -PackageName ID  Expected Android package. Default: com.dealplanner"
    Write-Host "  -AppLabel NAME   Expected app label. Default: Deal Planner"
}

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

if ($Help) {
    Show-Usage
    exit 0
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

function Read-ApkAaptInfo {
    param(
        [string]$AaptPath,
        [string]$ApkPath
    )

    $badging = @(& $AaptPath dump badging $ApkPath 2>$null)
    if ($LASTEXITCODE -ne 0) {
        throw "aapt dump badging failed."
    }

    $permissions = @(& $AaptPath dump permissions $ApkPath 2>$null)
    if ($LASTEXITCODE -ne 0) {
        throw "aapt dump permissions failed."
    }

    return [pscustomobject]@{
        Badging = $badging
        Permissions = $permissions
    }
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
    $headSha = (& git rev-parse HEAD 2>$null)
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

    $originUrl = (& git remote get-url origin 2>$null)
    if ($LASTEXITCODE -eq 0 -and $originUrl) {
        if ($originUrl -match "github\.com[:/]adamcboyd/Deal-Planner(\.git)?$") {
            Add-Check $results "Git remote" "OK" "origin points to adamcboyd/Deal-Planner."
        } else {
            Add-Check $results "Git remote" "WARN" "origin points to $originUrl, not adamcboyd/Deal-Planner."
        }
    } else {
        Add-Check $results "Git remote" "WARN" "Could not read origin remote."
    }

    $upstream = (& git rev-parse --abbrev-ref --symbolic-full-name '@{u}' 2>$null)
    if ($LASTEXITCODE -eq 0 -and $upstream) {
        $aheadBehind = (& git rev-list --left-right --count "HEAD...@{u}" 2>$null)
        if ($LASTEXITCODE -eq 0 -and $aheadBehind) {
            $counts = $aheadBehind.Trim() -split "\s+"
            $ahead = [int]$counts[0]
            $behind = [int]$counts[1]
            if ($ahead -eq 0 -and $behind -eq 0) {
                Add-Check $results "Git upstream" "OK" "$branch is synced with $upstream."
            } elseif ($ahead -gt 0 -and $behind -eq 0) {
                Add-Check $results "Git upstream" "WARN" "$branch is $ahead commit(s) ahead of $upstream. Push before phone testing."
            } elseif ($ahead -eq 0 -and $behind -gt 0) {
                Add-Check $results "Git upstream" "WARN" "$branch is $behind commit(s) behind $upstream. Pull/rebase before phone testing."
            } else {
                Add-Check $results "Git upstream" "WARN" "$branch diverged from $upstream ($ahead ahead, $behind behind)."
            }
        } else {
            Add-Check $results "Git upstream" "WARN" "Could not compare $branch with $upstream."
        }
    } else {
        Add-Check $results "Git upstream" "WARN" "$branch has no configured upstream branch."
    }

    if (-not $SkipNetwork -and $originUrl -and $headSha) {
        $remoteRef = @(& git ls-remote origin "refs/heads/$branch" 2>$null)
        if ($LASTEXITCODE -eq 0 -and $remoteRef.Count -gt 0) {
            $remoteSha = ($remoteRef[0] -split "\s+")[0]
            if ($remoteSha -eq $headSha) {
                Add-Check $results "GitHub branch" "OK" "GitHub origin/$branch matches local HEAD."
            } else {
                Add-Check $results "GitHub branch" "WARN" "GitHub origin/$branch differs from local HEAD. Push or pull before phone testing."
            }
        } else {
            Add-Check $results "GitHub branch" "WARN" "Could not verify GitHub origin/$branch with ls-remote."
        }
    } elseif ($SkipNetwork) {
        Add-Check $results "GitHub branch" "WARN" "Network check skipped; GitHub branch SHA not verified."
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

if ($apkInfo) {
    $aaptPath = Get-AndroidBuildTool "aapt.exe"
    if ($aaptPath) {
        try {
            $apkAaptInfo = Read-ApkAaptInfo $aaptPath $apkPath
            $packageLine = $apkAaptInfo.Badging | Where-Object { $_ -match "^package:" } | Select-Object -First 1
            $labelLine = $apkAaptInfo.Badging | Where-Object { $_ -match "^application-label:" } | Select-Object -First 1
            $packageOk = $packageLine -match "name='$([regex]::Escape($PackageName))'"
            $labelOk = $labelLine -match "application-label:'$([regex]::Escape($AppLabel))'"

            if ($packageOk -and $labelOk) {
                Add-Check $results "APK identity" "OK" "$PackageName / $AppLabel verified in app-debug.apk."
            } else {
                Add-Check $results "APK identity" "FAIL" "Expected $PackageName / $AppLabel, but APK reported: $packageLine $labelLine"
            }

            $requiredPermissions = @(
                "android.permission.INTERNET",
                "android.permission.CAMERA"
            )
            $missingPermissions = @(
                foreach ($permission in $requiredPermissions) {
                    if (-not ($apkAaptInfo.Permissions | Where-Object { $_ -match "name='$([regex]::Escape($permission))'" })) {
                        $permission
                    }
                }
            )

            if ($missingPermissions.Count -eq 0) {
                Add-Check $results "APK permissions" "OK" "Required network and camera permissions are present."
            } else {
                Add-Check $results "APK permissions" "FAIL" "Missing required permission(s): $($missingPermissions -join ', ')"
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
                    if ($apkAaptInfo.Permissions | Where-Object { $_ -match "name='$([regex]::Escape($permission))'" }) {
                        $permission
                    }
                }
            )

            if ($presentStoragePermissions.Count -eq 0) {
                Add-Check $results "APK storage permissions" "OK" "No broad storage/media permissions requested; picker imports use scoped URI grants."
            } else {
                Add-Check $results "APK storage permissions" "WARN" "Unexpected broad storage/media permission(s): $($presentStoragePermissions -join ', ')"
            }
        } catch {
            Add-Check $results "APK identity" "WARN" "Could not inspect app-debug.apk with aapt: $($_.Exception.Message)"
        }
    } else {
        Add-Check $results "APK identity" "WARN" "aapt.exe was not found; package/permission inspection skipped."
    }
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
    $status = if ($RequireGemini) { "FAIL" } else { "WARN" }
    Add-Check $results "Gemini key" $status "No non-placeholder Gemini key found. Pantry photos will use OCR fallback."
}

$model = if ($localGeminiModel) { $localGeminiModel } elseif ($envGeminiModel) { $envGeminiModel } else { "gemini-3.5-flash" }
Add-Check $results "Gemini model" "OK" "Build model setting resolves to $($model.Trim())."

if ($apkInfo -and (Test-Path $localPropertiesPath)) {
    $localPropertiesInfo = Get-Item $localPropertiesPath
    if ($localPropertiesInfo.LastWriteTime -gt $apkInfo.LastWriteTime) {
        $status = if ($RequireGemini) { "FAIL" } else { "WARN" }
        Add-Check $results "Gemini APK freshness" $status "local.properties is newer than app-debug.apk. Rebuild before AI phone testing so BuildConfig has the current key/model."
    } else {
        Add-Check $results "Gemini APK freshness" "OK" "app-debug.apk is newer than local.properties."
    }
} elseif ($apkInfo -and ((Test-RealKey $envGeminiKey) -or -not [string]::IsNullOrWhiteSpace($envGeminiModel))) {
    $status = if ($RequireGemini) { "FAIL" } else { "WARN" }
    Add-Check $results "Gemini APK freshness" $status "GEMINI_* environment values cannot be timestamp-checked against app-debug.apk. Rebuild before AI phone testing if they changed."
} elseif ($RequireGemini -and $apkInfo) {
    Add-Check $results "Gemini APK freshness" "FAIL" "Gemini was required, but no local.properties or GEMINI_* configuration was available to verify against app-debug.apk."
}

if ($apkInfo) {
    $latestBuildInput = Get-LatestBuildInput $repoRoot
    if ($latestBuildInput.Count -gt 0 -and $latestBuildInput[0].LastWriteTime -gt $apkInfo.LastWriteTime) {
        $relativePath = Get-RelativeRepoPath $repoRoot $latestBuildInput[0].FullName
        Add-Check $results "APK source freshness" "WARN" "$relativePath is newer than app-debug.apk. Rebuild before phone testing."
    } else {
        Add-Check $results "APK source freshness" "OK" "app-debug.apk is newer than app source/resources/build config."
    }
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

if (Test-Path ".\scripts\phone-debug-logs.ps1") {
    Add-Check $results "Phone log helper" "OK" "scripts\phone-debug-logs.ps1 is present for crash/log capture."
} else {
    Add-Check $results "Phone log helper" "WARN" "Log helper not found; phone failures may be harder to diagnose."
}

if (Test-Path ".\scripts\new-phone-test-report.ps1") {
    Add-Check $results "Phone test report helper" "OK" "scripts\new-phone-test-report.ps1 is present for timestamped pass/fail evidence capture."
} else {
    Add-Check $results "Phone test report helper" "WARN" "Phone test report helper not found; test evidence may end up only in chat."
}

Write-Host ""
$failCount = @($results | Where-Object { $_.Status -eq "FAIL" }).Count
$warnCount = @($results | Where-Object { $_.Status -eq "WARN" }).Count
Write-Host ("Summary: {0} failure(s), {1} warning(s)." -f $failCount, $warnCount)

if ($failCount -gt 0) {
    exit 1
}

Write-Host "Preflight complete. For install/launch, run .\scripts\phone-debug-install.ps1"
