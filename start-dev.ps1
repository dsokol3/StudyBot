# Load environment variables from .env file
$envFile = Join-Path $PSScriptRoot ".env"
if (Test-Path $envFile) {
    $envVars = Get-Content $envFile | ForEach-Object {
        if ($_ -match '^([^#=]+)=(.*)$') {
            "$($matches[1].Trim())=$($matches[2].Trim())"
        }
    }
    $envSetup = $envVars -join "; `$env:"
    if ($envSetup) { $envSetup = "`$env:$envSetup" }
}

# Start Backend
Write-Host "Starting Java Backend..." -ForegroundColor Green
$javaHome = "C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot"
$backendCommand = "Set-Location -Path '$PSScriptRoot'; $envSetup; `$env:JAVA_HOME = '$javaHome'; mvn spring-boot:run"
Start-Process powershell -ArgumentList "-NoExit", "-Command", $backendCommand

# Wait a moment for backend to initialize
Start-Sleep -Seconds 3

# Start Frontend
Write-Host "Starting Vue Frontend..." -ForegroundColor Green
$frontendPath = Join-Path $PSScriptRoot "frontend"
$frontendCommand = "Set-Location -Path '$frontendPath'; npm run dev"
Start-Process powershell -ArgumentList "-NoExit", "-Command", $frontendCommand

Write-Host "`nBoth servers are starting!" -ForegroundColor Cyan
Write-Host "Backend: http://localhost:8080" -ForegroundColor Yellow
Write-Host "Frontend: http://localhost:5173" -ForegroundColor Yellow
Write-Host "`nPress Ctrl+C in each window to stop the servers." -ForegroundColor Gray
