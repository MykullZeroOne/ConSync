# ConSync Update Script for Windows
$ErrorActionPreference = "Stop"

Write-Host "ConSync Updater" -ForegroundColor Blue
Write-Host ""

# Check if ConSync is installed
try {
    $currentVersionOutput = & consync --version 2>&1 | Out-String
    $currentVersion = if ($currentVersionOutput -match '(\d+\.\d+\.\d+)') { $matches[1] } else { "unknown" }
    Write-Host "Current version: " -NoNewline
    Write-Host $currentVersion -ForegroundColor Yellow
} catch {
    Write-Host "Error: ConSync is not installed" -ForegroundColor Red
    Write-Host "Install it first from: https://github.com/MykullZeroOne/ConSync/releases"
    exit 1
}

# Get latest release version from GitHub
Write-Host "Checking for updates..."
try {
    $latestRelease = Invoke-RestMethod -Uri "https://api.github.com/repos/MykullZeroOne/ConSync/releases/latest"
    $latestVersion = $latestRelease.tag_name -replace '^v', ''
    Write-Host "Latest version: " -NoNewline
    Write-Host $latestVersion -ForegroundColor Green
    Write-Host ""
} catch {
    Write-Host "Error: Could not fetch latest version" -ForegroundColor Red
    exit 1
}

# Compare versions
if ($currentVersion -eq $latestVersion) {
    Write-Host "✓ You're already running the latest version!" -ForegroundColor Green
    exit 0
}

# Download and install update
Write-Host "Updating ConSync..." -ForegroundColor Yellow
Write-Host ""

# Create temp directory
$tempDir = Join-Path $env:TEMP "consync-update-$(Get-Random)"
New-Item -ItemType Directory -Force -Path $tempDir | Out-Null
Push-Location $tempDir

try {
    # Download latest release
    Write-Host "Downloading ConSync v$latestVersion..."
    $zipUrl = "https://github.com/MykullZeroOne/ConSync/releases/download/v$latestVersion/consync-$latestVersion-windows.zip"
    $zipFile = "consync-$latestVersion-windows.zip"
    Invoke-WebRequest -Uri $zipUrl -OutFile $zipFile

    # Download and verify checksum
    Write-Host "Verifying checksum..."
    $checksumUrl = "$zipUrl.sha256"
    $checksumFile = "$zipFile.sha256"
    Invoke-WebRequest -Uri $checksumUrl -OutFile $checksumFile

    $expectedHash = (Get-Content $checksumFile).Split()[0]
    $actualHash = (Get-FileHash $zipFile -Algorithm SHA256).Hash

    if ($expectedHash -eq $actualHash) {
        Write-Host "✓ Checksum verified" -ForegroundColor Green
    } else {
        Write-Host "✗ Checksum verification failed" -ForegroundColor Red
        exit 1
    }

    # Extract
    Write-Host "Extracting..."
    Expand-Archive -Path $zipFile -DestinationPath . -Force
    Set-Location "consync-$latestVersion-windows"

    # Run installer
    Write-Host "Installing..."
    & .\install.ps1

    Write-Host ""
    Write-Host "✓ ConSync updated successfully to v$latestVersion!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Verify: consync --version" -ForegroundColor Cyan

} finally {
    # Cleanup
    Pop-Location
    Remove-Item -Path $tempDir -Recurse -Force -ErrorAction SilentlyContinue
}
