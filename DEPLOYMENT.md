# WallCorePro – Automatic Deployment Guide

**Platform:** Render.com (chosen for free tier, auto-deploy on push, zero config)

---

## One-Time Setup (≈ 10 minutes)

### 1. Get Pixabay API Key (free)

1. Go to [pixabay.com/api/docs](https://pixabay.com/api/docs/)
2. Sign up → get your free API key
3. Copy it (e.g. `12345678-abcdef1234567890abcdef1234567890`)

### 2. Push Code to GitHub

```bash
cd WallCorePro
git init
git add .
git commit -m "Initial commit"
git remote add origin https://github.com/YOUR_USERNAME/WallCorePro.git
git push -u origin main
```

### 3. Deploy Backend on Render (automatic)

1. Go to [render.com](https://render.com) → Sign up (free)
2. **New** → **Blueprint**
3. Connect your GitHub → select **WallCorePro** repo
4. Render detects `render.yaml` → click **Apply**
5. In the new service → **Environment** → add:
   - **Key:** `PIXABAY_API_KEY`
   - **Value:** your Pixabay key from step 1
6. Click **Save** → Render deploys automatically
7. Copy your service URL (e.g. `https://wallcorepro-api.onrender.com`)

### 4. Update App with Backend URL

Edit `app/src/main/java/com/offline/wallcorepro/config/AppConfig.kt`:

```kotlin
const val BASE_URL = "https://YOUR-SERVICE-URL.onrender.com/v1/"
```

Example: `https://wallcorepro-api.onrender.com/v1/`

### 5. Build & Publish App

```bash
./gradlew assembleRelease
```

Sign the APK/AAB and upload to Play Console.

---

## Automatic Behavior (After Setup)

| Action | Result |
|--------|--------|
| Push to `main` | Render auto-deploys backend |
| Change `backend/` files | Only backend redeploys (monorepo-aware) |
| Change `app/` files | No backend redeploy (efficient) |

---

## Verify Deployment

1. **Backend health:** Open `https://YOUR-SERVICE-URL.onrender.com/health` → should return `{"ok":true}`
2. **Wallpapers:** `https://YOUR-SERVICE-URL.onrender.com/v1/wallpapers?niche=GOOD_MORNING&page=1`
3. **App:** Build, install, open → wallpapers should load

---

## Troubleshooting

| Issue | Fix |
|-------|-----|
| 500 on `/v1/wallpapers` | Check `PIXABAY_API_KEY` is set in Render Environment |
| App shows "No wallpapers" | Verify `BASE_URL` in AppConfig ends with `/v1/` |
| Render free tier sleeps after 15 min | First request may take ~30s; subsequent fast |

---

## Compliance

See [COMPLIANCE.md](COMPLIANCE.md) for Play Store and licensing details.
