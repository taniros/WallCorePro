# 🚀 WallCorePro – Easiest Safe Deploy (5 Steps)

**TL;DR:** Get Pixabay key → Push to GitHub → Connect Render → Add key in Render → Build app. Done.

**Super lazy?** Run `.\scripts\setup.ps1` first – it opens all the links you need.

---

## Step 1: Get Pixabay Key (2 min)

1. Open: **https://pixabay.com/api/docs/**
2. Sign up (free) → copy your API key
3. Save it somewhere (you'll paste it in Step 3)

---

## Step 2: Push to GitHub (1 min)

Open PowerShell in this folder and run:

```powershell
.\scripts\deploy.ps1
```

Or manually:

```powershell
git init
git add .
git commit -m "Initial"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/WallCorePro.git
git push -u origin main
```
*(Replace YOUR_USERNAME with your GitHub username)*

---

## Step 3: Deploy Backend (3 min)

1. Open: **https://render.com** → Sign up (free)
2. Click **New** → **Blueprint**
3. Connect GitHub → select **WallCorePro**
4. Click **Apply**
5. Click your new service → **Environment** → **Add**
   - Key: `PIXABAY_API_KEY`
   - Value: *(paste your key from Step 1)*
   - *(Optional)* Key: `UNSPLASH_ACCESS_KEY` → Value: your key from [unsplash.com/developers](https://unsplash.com/developers) — adds more wallpaper variety
6. Click **Save** → wait ~2 min for deploy
7. Copy the URL (e.g. `https://wallcorepro-api.onrender.com`)

---

## Step 4: Update App (maybe skip!)

The app already uses `https://wallcorepro-api.onrender.com/v1/`.  
**If** your Render URL is different (e.g. `wallcorepro-api-xyz.onrender.com`), edit `AppConfig.kt` and set `BASE_URL` to your URL + `/v1/`.  
Otherwise → skip this step.

---

## Step 5: Build & Publish

```powershell
.\gradlew assembleRelease
```

Sign the AAB and upload to Play Console.

---

## ✅ Done

- **Safe:** App never touches Pixabay. Backend-only = 0 risk.
- **Automatic:** Every `git push` → Render redeploys backend.
- **Lazy:** You never touch the backend again.

---

**Need help?** Run `.\scripts\setup.ps1` to open all links at once.
