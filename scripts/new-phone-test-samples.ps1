param(
    [switch]$Help,
    [string]$OutputDir = "phone-test-samples"
)

$ErrorActionPreference = "Stop"

function Show-Usage {
    Write-Host "Deal Planner phone test sample generator"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\new-phone-test-samples.ps1"
    Write-Host "  .\scripts\new-phone-test-samples.ps1 -OutputDir phone-test-samples"
    Write-Host ""
    Write-Host "Creates ignored timestamped TXT and PDF sample files from the bundled demo receipt/flyer assets."
}

if ($Help) {
    Show-Usage
    exit 0
}

function Escape-PdfText {
    param([string]$Value)

    return $Value.Replace("\", "\\").Replace("(", "\(").Replace(")", "\)")
}

function Split-DisplayLine {
    param(
        [string]$Line,
        [int]$MaxChars
    )

    $clean = $Line.Replace("`t", "    ")
    if ($clean.Length -le $MaxChars) {
        return @($clean)
    }

    $chunks = New-Object System.Collections.Generic.List[string]
    $remaining = $clean
    while ($remaining.Length -gt $MaxChars) {
        $breakAt = $remaining.LastIndexOf(" ", [Math]::Min($MaxChars, $remaining.Length - 1))
        if ($breakAt -lt 24) {
            $breakAt = $MaxChars
        }
        $chunks.Add($remaining.Substring(0, $breakAt).TrimEnd()) | Out-Null
        $remaining = $remaining.Substring($breakAt).TrimStart()
    }

    if ($remaining.Length -gt 0) {
        $chunks.Add($remaining) | Out-Null
    }

    return @($chunks)
}

function New-SimplePdf {
    param(
        [string]$SourceTextPath,
        [string]$OutputPath,
        [string]$Title
    )

    $rawLines = Get-Content -LiteralPath $SourceTextPath
    $displayLines = New-Object System.Collections.Generic.List[string]
    foreach ($line in $rawLines) {
        foreach ($chunk in (Split-DisplayLine -Line $line -MaxChars 86)) {
            $displayLines.Add($chunk) | Out-Null
        }
    }

    $maxLinesPerPage = 54
    $pages = New-Object System.Collections.Generic.List[object]
    for ($start = 0; $start -lt $displayLines.Count; $start += $maxLinesPerPage) {
        $count = [Math]::Min($maxLinesPerPage, $displayLines.Count - $start)
        $pages.Add(@($displayLines | Select-Object -Skip $start -First $count)) | Out-Null
    }
    if ($pages.Count -eq 0) {
        $pages.Add(@("")) | Out-Null
    }

    $objects = New-Object System.Collections.Generic.List[string]
    $objects.Add("<< /Type /Catalog /Pages 2 0 R >>") | Out-Null
    $objects.Add("__PAGES__") | Out-Null
    $objects.Add("<< /Type /Font /Subtype /Type1 /BaseFont /Courier >>") | Out-Null

    $pageObjectIds = New-Object System.Collections.Generic.List[int]
    foreach ($pageLines in $pages) {
        $pageObjectId = $objects.Count + 1
        $contentObjectId = $objects.Count + 2
        $pageObjectIds.Add($pageObjectId) | Out-Null

        $contentBuilder = [System.Text.StringBuilder]::new()
        [void]$contentBuilder.AppendLine("BT")
        [void]$contentBuilder.AppendLine("/F1 10 Tf")
        [void]$contentBuilder.AppendLine("36 756 Td")
        [void]$contentBuilder.AppendLine("12 TL")
        [void]$contentBuilder.AppendLine(("({0}) Tj" -f (Escape-PdfText $Title)))
        [void]$contentBuilder.AppendLine("T*")
        [void]$contentBuilder.AppendLine("T*")
        foreach ($line in $pageLines) {
            if ([string]::IsNullOrWhiteSpace($line)) {
                [void]$contentBuilder.AppendLine("T*")
            } else {
                [void]$contentBuilder.AppendLine(("({0}) Tj" -f (Escape-PdfText $line)))
                [void]$contentBuilder.AppendLine("T*")
            }
        }
        [void]$contentBuilder.AppendLine("ET")

        $content = $contentBuilder.ToString()
        $contentByteLength = [System.Text.Encoding]::ASCII.GetByteCount($content)
        $objects.Add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 3 0 R >> >> /Contents $contentObjectId 0 R >>") | Out-Null
        $objects.Add("<< /Length $contentByteLength >>`nstream`n$content`nendstream") | Out-Null
    }

    $kids = ($pageObjectIds | ForEach-Object { "$_ 0 R" }) -join " "
    $objects[1] = "<< /Type /Pages /Kids [ $kids ] /Count $($pageObjectIds.Count) >>"

    $encoding = [System.Text.Encoding]::ASCII
    $builder = [System.Text.StringBuilder]::new()
    [void]$builder.Append("%PDF-1.4`n% Deal Planner phone test sample`n")

    $offsets = New-Object System.Collections.Generic.List[int]
    for ($index = 0; $index -lt $objects.Count; $index++) {
        $offsets.Add($encoding.GetByteCount($builder.ToString())) | Out-Null
        [void]$builder.Append(("{0} 0 obj`n{1}`nendobj`n" -f ($index + 1), $objects[$index]))
    }

    $xrefOffset = $encoding.GetByteCount($builder.ToString())
    [void]$builder.AppendLine("xref")
    [void]$builder.AppendLine(("0 {0}" -f ($objects.Count + 1)))
    [void]$builder.AppendLine("0000000000 65535 f ")
    foreach ($offset in $offsets) {
        [void]$builder.AppendLine(("{0:D10} 00000 n " -f $offset))
    }
    [void]$builder.AppendLine("trailer")
    [void]$builder.AppendLine(("<< /Size {0} /Root 1 0 R >>" -f ($objects.Count + 1)))
    [void]$builder.AppendLine("startxref")
    [void]$builder.AppendLine($xrefOffset.ToString())
    [void]$builder.AppendLine("%%EOF")

    [System.IO.File]::WriteAllBytes($OutputPath, $encoding.GetBytes($builder.ToString()))
}

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

$assetsRoot = Join-Path $repoRoot "app\src\main\assets"
$receiptAsset = Join-Path $assetsRoot "demo_receipt.txt"
$flyerAsset = Join-Path $assetsRoot "demo_flyer.txt"

if (-not (Test-Path $receiptAsset)) {
    throw "Missing bundled demo receipt asset: $receiptAsset"
}
if (-not (Test-Path $flyerAsset)) {
    throw "Missing bundled demo flyer asset: $flyerAsset"
}

if ([System.IO.Path]::IsPathRooted($OutputDir)) {
    $outputRoot = $OutputDir
} else {
    $outputRoot = Join-Path $repoRoot $OutputDir
}

$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$sessionDir = Join-Path $outputRoot $stamp
New-Item -ItemType Directory -Force -Path $sessionDir | Out-Null

$receiptText = Join-Path $sessionDir "deal-planner-demo-receipt.txt"
$flyerText = Join-Path $sessionDir "deal-planner-demo-flyer.txt"
$receiptPdf = Join-Path $sessionDir "deal-planner-demo-receipt.pdf"
$flyerPdf = Join-Path $sessionDir "deal-planner-demo-flyer.pdf"
$readmePath = Join-Path $sessionDir "README.md"

Copy-Item -LiteralPath $receiptAsset -Destination $receiptText -Force
Copy-Item -LiteralPath $flyerAsset -Destination $flyerText -Force
New-SimplePdf -SourceTextPath $receiptAsset -OutputPath $receiptPdf -Title "Deal Planner Demo Receipt"
New-SimplePdf -SourceTextPath $flyerAsset -OutputPath $flyerPdf -Title "Deal Planner Demo Flyer"

$readme = @"
# Deal Planner Phone Test Samples - $stamp

Copy this folder to the Android phone or upload it to a location the phone can open.

- deal-planner-demo-receipt.txt: paste into Receipts -> Paste receipt OCR text.
- deal-planner-demo-receipt.pdf: choose from Receipts -> PDF.
- deal-planner-demo-flyer.txt: paste into Deals -> Paste flyer OCR text.
- deal-planner-demo-flyer.pdf: choose from Deals -> Choose Flyer PDF.

Expected receipt result: the bundled demo receipt imports grocery line items, ignores total/tender lines, and updates Budget.
Expected flyer result: the bundled demo flyer imports multiple Kroger deals with prices, limits, coupons, and deal scores.
"@

Set-Content -LiteralPath $readmePath -Value $readme -Encoding UTF8

Write-Host "Created phone test samples:"
Write-Host $sessionDir
Get-ChildItem -LiteralPath $sessionDir | Select-Object Name, Length
