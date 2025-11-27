# Start Backend
Write-Host "Starting Java Backend..." -ForegroundColor Green
$backendCommand = "Set-Location -Path '$PSScriptRoot'; mvn spring-boot:run"
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
