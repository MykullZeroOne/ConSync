# ConSync Chocolatey Install Script

$ErrorActionPreference = 'Stop'

$packageName = 'consync'
$version = '1.0.0'
$url = "https://github.com/yourusername/consync/releases/download/v$version/consync-$version-windows.zip"
$checksum = 'REPLACE_WITH_ACTUAL_SHA256'
$checksumType = 'sha256'

$installDir = Join-Path $env:ChocolateyInstall "lib\$packageName\tools"

# Download and extract
Install-ChocolateyZipPackage `
    -PackageName $packageName `
    -Url $url `
    -UnzipLocation $installDir `
    -Checksum $checksum `
    -ChecksumType $checksumType

# Create batch file in chocolatey bin
$binDir = Join-Path $env:ChocolateyInstall "bin"
$batchFile = Join-Path $binDir "consync.bat"

$batchContent = @"
@echo off
setlocal

REM ConSync Launcher
set "CONSYNC_JAR=$installDir\consync.jar"

if not exist "%CONSYNC_JAR%" (
    echo Error: ConSync JAR not found
    exit /b 1
)

REM Load credentials if they exist
set "CREDS_FILE=%USERPROFILE%\.consync-app\credentials.bat"
if exist "%CREDS_FILE%" (
    call "%CREDS_FILE%"
)

java -jar "%CONSYNC_JAR%" %*
"@

Set-Content -Path $batchFile -Value $batchContent

Write-Host ""
Write-Host "ConSync $version has been installed!" -ForegroundColor Green
Write-Host ""
Write-Host "Configure authentication:" -ForegroundColor Yellow
Write-Host "  For Confluence Cloud:" -ForegroundColor Cyan
Write-Host "    `$env:CONFLUENCE_USERNAME = 'your-email@example.com'"
Write-Host "    `$env:CONFLUENCE_API_TOKEN = 'your-api-token'"
Write-Host ""
Write-Host "  For Confluence Data Center/Server:" -ForegroundColor Cyan
Write-Host "    `$env:CONFLUENCE_PAT = 'your-personal-access-token'"
Write-Host ""
Write-Host "Get started: consync --help" -ForegroundColor Green
Write-Host ""
