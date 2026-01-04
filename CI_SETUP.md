# CI Setup and Auto-Deploy Configuration

## ✅ CI Workflow Created

A GitHub Actions workflow has been created at [.github/workflows/ci.yml](.github/workflows/ci.yml) with:

- **Backend Tests**: Runs Maven tests with Java 21
- **Frontend Build**: Builds Vue.js app with Node 22, checks for vulnerabilities
- **Docker Build**: Verifies the multi-stage Dockerfile builds successfully

## 🔧 Push the Workflow

The commit is ready but needs `workflow` scope to push. Options:

### Option 1: Push from Git Bash/Terminal
```bash
git push origin main
```

### Option 2: Update VS Code GitHub Token
1. Open Command Palette (Ctrl+Shift+P)
2. Run: `GitHub: Sign Out`
3. Run: `GitHub: Sign In`
4. When authorizing, ensure `workflow` scope is checked
5. Push again

## 🚀 Enable Safe Auto-Deploy (Deploy Only After CI Passes)

### Step 1: Enable Branch Protection (Recommended)
1. Go to: https://github.com/dsokol3/AI-Study-Guide/settings/branches
2. Click **Add branch protection rule**
3. Branch name pattern: `main`
4. Enable:
   - ✅ **Require status checks to pass before merging**
   - Select: `Backend Tests (Java 21 + Maven)`, `Frontend Build (Node 22 + Vite)`, `Docker Build Test`
   - ✅ **Require branches to be up to date before merging** (optional but recommended)
5. Click **Create**

### Step 2: Configure Render Auto-Deploy
1. Go to your Render dashboard: https://dashboard.render.com
2. Select your Web Service
3. Go to **Settings** tab
4. Under **Build & Deploy**:
   - Set **Auto-Deploy**: `Yes`
   - **Branch**: `main`
   
Now pushes to `main` will auto-deploy, but branch protection ensures only code that passes CI can be merged to `main`.

### Step 3: Verify Everything Works
1. Create a test branch:
   ```bash
   git checkout -b test-ci
   ```

2. Make a small change (e.g., update README)

3. Push and create PR:
   ```bash
   git push origin test-ci
   ```

4. Go to GitHub → Pull Requests → Create PR
5. Watch CI run automatically
6. If CI passes, merge to `main` → Render will auto-deploy
7. If CI fails, you can't merge (protected by branch rules)

## 🔍 Monitor CI Status

Check CI runs at: https://github.com/dsokol3/AI-Study-Guide/actions

Each push/PR will show:
- ✅ Green check = All tests passed
- ❌ Red X = Tests failed (can't merge)
- 🟡 Yellow dot = CI running

## 🎯 Workflow Behavior

**On Push to `main`:**
- Runs all 3 CI jobs (backend tests, frontend build, Docker build)
- If passes → Render auto-deploys (when enabled)
- You'll get email notifications for failures

**On Pull Request:**
- Runs all 3 CI jobs
- Shows status in PR (can't merge if fails and branch protection enabled)
- No deployment until merged to `main`

## 🔐 Environment Variables

CI uses test values for API keys. Your production keys on Render are separate and secure.

**CI Test Values** (in workflow):
```yaml
DB_HOST: localhost
GEMINI_API_KEY: test-key
LLM_API_KEY: test-key
```

**Production Values** (on Render):
- Set in Render Dashboard → Environment tab
- Never committed to Git
- Used only in production

## 📝 Next Steps

1. **Push the workflow file** (fix OAuth scope issue)
2. **Watch first CI run** at GitHub Actions page
3. **Enable branch protection** (optional but recommended)
4. **Enable Render auto-deploy** on `main` branch
5. **Test with a PR** to verify CI blocks bad code

## 🆘 Troubleshooting

### CI Fails on First Run
- Check logs at: https://github.com/dsokol3/AI-Study-Guide/actions
- Most common: missing `package-lock.json` in frontend (run `npm install` to generate)
- Backend tests might need actual test database (currently using mock values)

### Auto-Deploy Not Working
- Verify **Auto-Deploy** is set to `Yes` on Render
- Check **Branch** is set to `main`
- Redeploy manually once to initialize

### Can't Merge PR
- If branch protection enabled, all CI checks must pass
- Click "Details" next to failed check to see logs
- Fix the issue, push again, CI reruns automatically
