# 🚀 Quick Start Guide - Google Gemini Embeddings

## Getting Your API Key

1. Visit [Google AI Studio](https://aistudio.google.com/app/apikey)
2. Sign in with your Google account
3. Click "Create API Key"
4. Copy your new API key

## Setup (Choose One Method)

### Method 1: Environment Variable (Recommended for Production)

**Windows PowerShell:**
```powershell
$env:GEMINI_API_KEY="your_api_key_here"
mvn spring-boot:run
```

**Linux/Mac:**
```bash
export GEMINI_API_KEY="your_api_key_here"
mvn spring-boot:run
```

### Method 2: .env File (Recommended for Development)

```bash
# Copy template
cp .env.template .env

# Edit .env file
nano .env  # or your favorite editor

# Add this line with your actual key:
GEMINI_API_KEY=your_actual_api_key_here

# Run application
mvn spring-boot:run
```

## Verifying It Works

### Check Logs on Startup
You should see:
```
╔══════════════════════════════════════════════════════════════╗
║  🚀 Initializing Google Gemini Embedding Service             ║
║  Model: text-embedding-004 (768 dimensions)                  ║
║  Cache: Enabled                                              ║
║  Max Retries: 3                                              ║
╚══════════════════════════════════════════════════════════════╝
✅ Gemini API key configured (length: XX)
```

### Test the Embedding Service

**Upload a document** through the UI, then check logs:
```
🔄 Generating Gemini embedding for text of length XXXX chars
📡 Calling Gemini API: https://generativelanguage.googleapis.com/...
✅ Received embedding with 768 dimensions
✅ Gemini embedding generated in XXX ms
```

## Common Issues

### ⚠️ "Gemini API key is not configured"
- **Solution**: Set the GEMINI_API_KEY environment variable
- **Check**: Restart your application after setting it

### ⚠️ "API call failed: 403 Forbidden"
- **Solution**: Your API key is invalid or expired
- **Action**: Generate a new key at https://aistudio.google.com/app/apikey

### ⚠️ "API call failed: 429 Too Many Requests"
- **Solution**: You've hit rate limits
- **Action**: Wait a few minutes or upgrade your API quota

## API Usage Statistics

Check your cache performance in logs:
```
✅ Stats: 150 embeddings generated, 95 cache hits (63% hit rate)
✅ API Stats: 55 API calls, 0 errors (0% error rate)
```

## Database Update Required

⚠️ **IMPORTANT**: The embedding dimension changed from 384 to 768!

If you have existing data:
1. Backup your database
2. Drop and recreate document tables (or alter column)
3. Re-upload all documents to generate new 768-dimensional embeddings

## Cost Monitoring

- Monitor usage: https://console.cloud.google.com/
- Free tier: 1,500 requests per day
- Caching reduces API calls significantly

## Need Help?

- 📖 Full documentation: [MIGRATION_SUMMARY.md](MIGRATION_SUMMARY.md)
- 🐛 Issues: Check application logs
- 💬 Google AI: https://ai.google.dev/

---

**Status**: ✅ Migration Complete - Ready to Use!
