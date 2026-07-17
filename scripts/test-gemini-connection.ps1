param(
    [switch]$Help,
    [switch]$TestPantryImage,
    [string]$ImagePath = "",
    [string]$Model = "",
    [string]$Prompt = "Reply with OK to confirm this Deal Planner Gemini setup works.",
    [int]$TimeoutSec = 30
)

$ErrorActionPreference = "Stop"

function Show-Usage {
    Write-Host "Deal Planner Gemini connection test"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\test-gemini-connection.ps1"
    Write-Host "  .\scripts\test-gemini-connection.ps1 -Model gemini-3.5-flash"
    Write-Host "  .\scripts\test-gemini-connection.ps1 -TestPantryImage"
    Write-Host "  .\scripts\test-gemini-connection.ps1 -ImagePath phone-test-samples\20260716-163910\deal-planner-demo-pantry-label.png"
    Write-Host ""
    Write-Host "Reads gemini.api.key and gemini.model from local.properties, or GEMINI_API_KEY and GEMINI_MODEL from the environment."
    Write-Host "Use -TestPantryImage to make a live image-capable Gemini request using the latest generated pantry-label sample."
    Write-Host "The key value is never printed."
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

function Normalize-GeminiModel {
    param([string]$Value)

    $normalized = $Value.Trim() -replace "^models/", ""
    if ([string]::IsNullOrWhiteSpace($normalized)) {
        return "gemini-3.5-flash"
    }

    return $normalized
}

function Get-LatestPantrySampleImage {
    param([string]$Root)

    $sampleRoot = Join-Path $Root "phone-test-samples"
    if (-not (Test-Path $sampleRoot)) {
        return $null
    }

    $latestSampleDirs = Get-ChildItem -LiteralPath $sampleRoot -Directory |
        Sort-Object Name -Descending

    foreach ($sampleDir in $latestSampleDirs) {
        $candidate = Join-Path $sampleDir.FullName "deal-planner-demo-pantry-label.png"
        if (Test-Path $candidate) {
            return $candidate
        }
    }

    return $null
}

function Get-ImageMimeType {
    param([string]$Path)

    switch ([System.IO.Path]::GetExtension($Path).ToLowerInvariant()) {
        ".png" { return "image/png" }
        ".jpg" { return "image/jpeg" }
        ".jpeg" { return "image/jpeg" }
        ".webp" { return "image/webp" }
        default { return "image/jpeg" }
    }
}

function ConvertTo-StatusDetail {
    param(
        [int]$StatusCode,
        [string]$ErrorText
    )

    $detail = $null
    if (-not [string]::IsNullOrWhiteSpace($ErrorText)) {
        try {
            $parsed = $ErrorText | ConvertFrom-Json
            if ($parsed.error) {
                $parts = @()
                if ($parsed.error.status) {
                    $parts += [string]$parsed.error.status
                }
                if ($parsed.error.message) {
                    $parts += [string]$parsed.error.message
                }
                $detail = ($parts -join ": ").Trim()
            }
        } catch {
            $detail = $null
        }
    }

    if ([string]::IsNullOrWhiteSpace($detail)) {
        $detail = ($ErrorText -split "\r?\n" | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }) -join " "
    }
    if ([string]::IsNullOrWhiteSpace($detail)) {
        $detail = "unknown error"
    }

    if ($detail.Length -gt 180) {
        $detail = $detail.Substring(0, 177).TrimEnd() + "..."
    }

    return "HTTP $StatusCode $detail"
}

function Get-WebExceptionBody {
    param($ErrorRecord)

    if ($ErrorRecord.ErrorDetails -and -not [string]::IsNullOrWhiteSpace($ErrorRecord.ErrorDetails.Message)) {
        return $ErrorRecord.ErrorDetails.Message
    }

    $response = $ErrorRecord.Exception.Response
    if ($null -eq $response) {
        return ""
    }

    try {
        $stream = $response.GetResponseStream()
        if ($null -eq $stream) {
            return ""
        }

        $reader = [System.IO.StreamReader]::new($stream)
        try {
            return $reader.ReadToEnd()
        } finally {
            $reader.Dispose()
        }
    } catch {
        return ""
    }
}

function Get-WebExceptionStatusCode {
    param($ErrorRecord)

    $response = $ErrorRecord.Exception.Response
    if ($response -and $response.StatusCode) {
        return [int]$response.StatusCode
    }

    return 0
}

if ($Help) {
    Show-Usage
    exit 0
}

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

$localPropertiesPath = Join-Path $repoRoot "local.properties"
$localGeminiKey = Get-LocalPropertyValue $localPropertiesPath "gemini.api.key"
$envGeminiKey = $env:GEMINI_API_KEY
$localGeminiModel = Get-LocalPropertyValue $localPropertiesPath "gemini.model"
$envGeminiModel = $env:GEMINI_MODEL

$keySource = ""
$apiKey = ""
if (Test-RealGeminiKey $localGeminiKey) {
    $apiKey = $localGeminiKey.Trim()
    $keySource = "local.properties"
} elseif (Test-RealGeminiKey $envGeminiKey) {
    $apiKey = $envGeminiKey.Trim()
    $keySource = "GEMINI_API_KEY"
}

$modelSource = if (-not [string]::IsNullOrWhiteSpace($Model)) {
    $Model
} elseif (-not [string]::IsNullOrWhiteSpace($localGeminiModel)) {
    $localGeminiModel
} elseif (-not [string]::IsNullOrWhiteSpace($envGeminiModel)) {
    $envGeminiModel
} else {
    "gemini-3.5-flash"
}
$modelName = Normalize-GeminiModel $modelSource

Write-Host "Deal Planner Gemini connection test"
Write-Host "Repo: $repoRoot"
Write-Host "Model: $modelName"

if ([string]::IsNullOrWhiteSpace($apiKey)) {
    Write-Host "[FAIL] No non-placeholder Gemini key found. Add gemini.api.key to local.properties or set GEMINI_API_KEY."
    exit 1
}

Write-Host "Key source: $keySource (value not printed)"

$resolvedImagePath = ""
if (-not [string]::IsNullOrWhiteSpace($ImagePath)) {
    $resolvedImagePath = (Resolve-Path -LiteralPath $ImagePath -ErrorAction Stop).Path
} elseif ($TestPantryImage) {
    $latestPantrySample = Get-LatestPantrySampleImage $repoRoot
    if ($latestPantrySample) {
        $resolvedImagePath = $latestPantrySample
    }
}

$useImage = -not [string]::IsNullOrWhiteSpace($resolvedImagePath)
if ($TestPantryImage -and -not $useImage) {
    Write-Host "[FAIL] Pantry image sample not found. Run .\scripts\new-phone-test-samples.ps1 or pass -ImagePath."
    exit 1
}

$effectivePrompt = $Prompt
if ($useImage -and $Prompt -eq "Reply with OK to confirm this Deal Planner Gemini setup works.") {
    $effectivePrompt = "Reply with OK if you can process this Deal Planner pantry label image."
}

$parts = @()
if ($useImage) {
    $imageInfo = Get-Item -LiteralPath $resolvedImagePath
    if ($imageInfo.Length -gt 15MB) {
        Write-Host "[FAIL] Image is too large for the inline Gemini test. Use a smaller image under 15 MB."
        exit 1
    }

    $imageBase64 = [Convert]::ToBase64String([System.IO.File]::ReadAllBytes($resolvedImagePath))
    $parts += @{
        inline_data = @{
            mime_type = Get-ImageMimeType $resolvedImagePath
            data = $imageBase64
        }
    }
    Write-Host "Image test: $resolvedImagePath"
}

$parts += @{
    text = $effectivePrompt
}

$body = @{
    contents = @(
        @{
            parts = $parts
        }
    )
    generationConfig = @{
        maxOutputTokens = 16
    }
} | ConvertTo-Json -Depth 10

$uri = "https://generativelanguage.googleapis.com/v1beta/models/$modelName`:generateContent"
$headers = @{
    "x-goog-api-key" = $apiKey
}

try {
    $response = Invoke-RestMethod -Method Post -Uri $uri -Headers $headers -ContentType "application/json" -Body $body -TimeoutSec $TimeoutSec
    $candidates = @($response.candidates)
    $parts = if ($candidates.Count -gt 0 -and $candidates[0].content) {
        @($candidates[0].content.parts)
    } else {
        @()
    }
    $answer = (
        $parts |
            ForEach-Object { $_.text } |
            Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
    ) -join "`n"

    if ([string]::IsNullOrWhiteSpace($answer)) {
        Write-Host "[FAIL] Gemini responded, but returned an empty test response."
        exit 1
    }

    if ($useImage) {
        Write-Host "[OK] Gemini image connection OK using $modelName."
    } else {
        Write-Host "[OK] Gemini connection OK using $modelName."
    }
    exit 0
} catch {
    $statusCode = Get-WebExceptionStatusCode $_
    $errorText = Get-WebExceptionBody $_
    if ($statusCode -gt 0) {
        Write-Host "[FAIL] Gemini request failed: $(ConvertTo-StatusDetail -StatusCode $statusCode -ErrorText $errorText)"
    } else {
        Write-Host "[FAIL] Gemini request failed: $($_.Exception.Message)"
    }
    exit 1
}
