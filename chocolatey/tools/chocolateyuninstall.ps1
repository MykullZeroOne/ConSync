# ConSync Chocolatey Uninstall Script

$ErrorActionPreference = 'Stop'

$packageName = 'consync'

# Remove batch file
$binDir = Join-Path $env:ChocolateyInstall "bin"
$batchFile = Join-Path $binDir "consync.bat"

if (Test-Path $batchFile) {
    Remove-Item $batchFile -Force
    Write-Host "Removed consync command" -ForegroundColor Green
}

Write-Host ""
Write-Host "ConSync has been uninstalled" -ForegroundColor Green
Write-Host ""
Write-Host "Note: Configuration files in %USERPROFILE%\.consync-app were not removed" -ForegroundColor Yellow
Write-Host "      Delete manually if desired" -ForegroundColor Yellow
Write-Host ""
