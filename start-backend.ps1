# Load environment variables from .env file and start the backend
$envFile = Join-Path $PSScriptRoot ".env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^([^#=]+)=(.*)$') {
            $name = $matches[1].Trim()
            $value = $matches[2].Trim()
            [Environment]::SetEnvironmentVariable($name, $value, "Process")
            Write-Host "Set $name" -ForegroundColor Green
        }
    }
}

# Set JAVA_HOME
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot"

# Start the backend
Write-Host "`n🚀 Starting ChatBot Backend with Groq API..." -ForegroundColor Cyan
Write-Host "API URL: $env:LLM_API_URL" -ForegroundColor Yellow
Write-Host "Model: $env:LLM_MODEL" -ForegroundColor Yellow
Write-Host "`n"

mvn spring-boot:run
