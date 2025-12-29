# ============================================
# ChatBot Application Starter
# Starts both Backend (Spring Boot) and Frontend (Vue)
# ============================================

Write-Host @"
╔══════════════════════════════════════════════════════════════╗
║  🚀 ChatBot Application Starter                              ║
╚══════════════════════════════════════════════════════════════╝
"@ -ForegroundColor Cyan

# Clear any existing Spring profile (force PostgreSQL usage)
$env:SPRING_PROFILES_ACTIVE = $null
[Environment]::SetEnvironmentVariable("SPRING_PROFILES_ACTIVE", $null, "Process")

# Load environment variables from .env file
$envFile = Join-Path $PSScriptRoot ".env"
if (Test-Path $envFile) {
    Write-Host "Loading environment variables..." -ForegroundColor Gray
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^([^#=]+)=(.*)$') {
            $name = $matches[1].Trim()
            $value = $matches[2].Trim()
            # Skip SPRING_PROFILES_ACTIVE to force PostgreSQL
            if ($name -ne "SPRING_PROFILES_ACTIVE") {
                [Environment]::SetEnvironmentVariable($name, $value, "Process")
            }
        }
    }
}

# Set Java Home
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot"

# Kill any existing processes on our ports
Write-Host "Checking for existing processes..." -ForegroundColor Gray
@(8080, 5173) | ForEach-Object {
    $port = $_
    $connections = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
    if ($connections) {
        $connections | ForEach-Object {
            try {
                Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue
                Write-Host "  Stopped process on port $port" -ForegroundColor Yellow
            } catch {}
        }
    }
}
Start-Sleep -Seconds 1

# Check Docker container
Write-Host "`nChecking PostgreSQL container..." -ForegroundColor Gray
$container = docker ps --filter "name=chatbot-postgres" --format "{{.Status}}" 2>$null
if (-not $container) {
    Write-Host "  Starting PostgreSQL container..." -ForegroundColor Yellow
    docker-compose up -d
    Start-Sleep -Seconds 3
} else {
    Write-Host "  PostgreSQL is running" -ForegroundColor Green
}

# Build environment command for backend (explicitly clear SPRING_PROFILES_ACTIVE)
$envVars = @(
    "`$env:SPRING_PROFILES_ACTIVE = `$null",
    "`$env:JAVA_HOME = '$env:JAVA_HOME'",
    "`$env:LLM_API_URL = '$env:LLM_API_URL'",
    "`$env:LLM_API_KEY = '$env:LLM_API_KEY'",
    "`$env:LLM_MODEL = '$env:LLM_MODEL'",
    "`$env:GEMINI_API_KEY = '$env:GEMINI_API_KEY'"
) -join "; "

# Start Backend
Write-Host "`n[1/2] Starting Java Backend..." -ForegroundColor Green
$backendCommand = "$envVars; Set-Location '$PSScriptRoot'; mvn spring-boot:run"
Start-Process powershell -ArgumentList "-NoExit", "-Command", $backendCommand

# Wait for backend to initialize
Write-Host "  Waiting for backend to start..." -ForegroundColor Gray
$maxWait = 30
$waited = 0
while ($waited -lt $maxWait) {
    Start-Sleep -Seconds 2
    $waited += 2
    $connection = Test-NetConnection -ComputerName localhost -Port 8080 -InformationLevel Quiet -WarningAction SilentlyContinue
    if ($connection) {
        Write-Host "  Backend started successfully!" -ForegroundColor Green
        break
    }
    Write-Host "  Still waiting... ($waited/$maxWait seconds)" -ForegroundColor Gray
}

# Start Frontend
Write-Host "`n[2/2] Starting Vue Frontend..." -ForegroundColor Green
$frontendPath = Join-Path $PSScriptRoot "frontend"
$frontendCommand = "Set-Location '$frontendPath'; npm run dev"
Start-Process powershell -ArgumentList "-NoExit", "-Command", $frontendCommand

# Final message
Write-Host @"

╔══════════════════════════════════════════════════════════════╗
║  ✅ ChatBot is starting!                                     ║
╠══════════════════════════════════════════════════════════════╣
║  Backend:  http://localhost:8080                             ║
║  Frontend: http://localhost:5173                             ║
╠══════════════════════════════════════════════════════════════╣
║  LLM: Groq API ($env:LLM_MODEL)
╚══════════════════════════════════════════════════════════════╝

Press Ctrl+C in each terminal window to stop the servers.
"@ -ForegroundColor Cyan

# Open browser after a short delay
Start-Sleep -Seconds 3
Start-Process "http://localhost:5173"
