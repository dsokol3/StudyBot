# Render Environment Variable Setup Guide

## Critical Steps for Gemini API Key on Render

### 1. Add Environment Variable
In your Render Dashboard:
1. Go to your **Web Service**
2. Click **Environment** tab
3. Add a new environment variable:
   - **Key**: `GEMINI_API_KEY` (exact case-sensitive match required)
   - **Value**: Your actual Gemini API key (starts with `AIza...`)

### 2. Verify Variable Name
✅ **CORRECT**: `GEMINI_API_KEY`  
❌ **WRONG**: `gemini.api.key`, `Gemini_API_Key`, `GEMINI_KEY`

Spring Boot reads the environment variable `GEMINI_API_KEY` and maps it to the property `gemini.api.key`.

### 3. Restart Service
**CRITICAL**: After adding the environment variable:
1. Click **Manual Deploy** → **Deploy latest commit**
2. OR trigger a new deployment by pushing a commit

Environment variables are only loaded at **startup**, not while the service is running.

### 4. Check Logs
After deployment, check the Render logs for these lines:

```
✅ CORRECT OUTPUT:
🔍 Debug: GEMINI_API_KEY env var = present (length: 39)
🔍 Debug: Injected apiKey = present (length: 39)
✅ Gemini API key configured (length: 39)

❌ PROBLEM OUTPUT:
🔍 Debug: GEMINI_API_KEY env var = null
🔍 Debug: Injected apiKey = blank
⚠️  WARNING: Gemini API key is not configured!
```

### 5. Common Issues

#### Issue 1: Variable Not Found
**Symptom**: Logs show `GEMINI_API_KEY env var = null`  
**Solution**: 
- Double-check spelling: `GEMINI_API_KEY` (all caps, underscore)
- Redeploy the service after adding the variable
- Ensure you added it to the correct service (not a different Render service)

#### Issue 2: Variable is Blank
**Symptom**: Logs show `GEMINI_API_KEY env var = blank`  
**Solution**: 
- Check the value in Render dashboard isn't empty
- Remove any leading/trailing spaces
- Ensure you clicked "Save Changes" after adding the variable

#### Issue 3: Wrong API Key Format
**Symptom**: API calls fail with authentication errors  
**Solution**: 
- Gemini API keys start with `AIza`
- Get a valid key from: https://aistudio.google.com/app/apikey
- Do NOT use quotes around the key value in Render

### 6. Test Without Deployment
To test locally before deploying:

```powershell
# Windows PowerShell
$env:GEMINI_API_KEY="your-api-key-here"
mvn spring-boot:run
```

```bash
# Linux/Mac
export GEMINI_API_KEY="your-api-key-here"
mvn spring-boot:run
```

### 7. Alternative: Use application.properties (NOT RECOMMENDED)
If environment variables absolutely don't work, you can hardcode it:

```properties
# src/main/resources/application.properties
gemini.api.key=AIzaSy...your-actual-key...
```

⚠️ **WARNING**: Never commit API keys to Git! Use `.gitignore` to exclude files with secrets.

### 8. Render-Specific Notes

#### Docker Environment
Render runs your Dockerfile, which uses `ENTRYPOINT ["java","-jar","/app/app.jar"]`.  
Environment variables are automatically passed to the Java process by Render.

#### No Additional Configuration Needed
You do NOT need to:
- Modify the Dockerfile to pass environment variables
- Use Docker `ENV` statements
- Create a `.env` file in the repository

Render injects environment variables at runtime automatically.

### 9. Verification Checklist

Before deploying:
- [ ] Environment variable name is exactly `GEMINI_API_KEY`
- [ ] Value is your actual Gemini API key (39 characters, starts with `AIza`)
- [ ] You clicked "Save Changes" in Render dashboard
- [ ] You triggered a new deployment (Manual Deploy or Git push)
- [ ] You checked the logs for the debug output

### 10. Get Your Gemini API Key

If you don't have a Gemini API key:
1. Go to: https://aistudio.google.com/app/apikey
2. Click **"Create API key"**
3. Choose **"Create API key in new project"** or select existing project
4. Copy the key (starts with `AIza`)
5. Paste it into Render's `GEMINI_API_KEY` environment variable

### Support

If you still have issues after following this guide:
1. Check Render logs for the debug output (search for "🔍 Debug:")
2. Verify the key works by testing locally first
3. Ensure your Render service is using the latest deployment
4. Try removing and re-adding the environment variable
5. Contact Render support if the variable isn't being passed to the container
