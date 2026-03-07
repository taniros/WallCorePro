# WallCorePro – Step-by-Step Setup (Explained Simply)

This guide explains every step in plain language. Each step tells you **what you're doing**, **why it matters**, and **what you should see**.

---

## Before You Start

**What you need:**
- This project on your computer
- A Google account (for Pixabay, GitHub, Render)
- About 15–20 minutes

**How it works (simple version):**
- Your app does **not** talk to Pixabay or Unsplash directly
- A small **backend server** (hosted free on Render) fetches wallpapers from those sites
- Your app only talks to your backend
- This keeps API keys secret and satisfies Play Store rules

---

## Step 1: Get Your Pixabay API Key

**What you're doing:** Getting a free key that lets your backend request wallpaper images from Pixabay.

**Why:** Without this key, your backend cannot fetch any wallpapers.

**How:**

1. Open: **https://pixabay.com/api/docs/**
2. Click **Sign up** (or log in) and create an account
3. After logging in, you'll see your **API key** on the page
4. It looks like: `12345678-abcdef1234567890abcdef1234567890`
5. **Copy the whole key** and save it in Notepad – you'll paste it in Step 4

**What you should see:** A long string of letters and numbers. That's your key.

---

## Step 2: (Optional) Get Unsplash API Key

**What you're doing:** Adding a second image source for more wallpaper variety.

**Why:** More sources = more wallpapers. You can skip this – the app works with only Pixabay.

**How:**

1. Go to: **https://unsplash.com/developers**
2. Sign up and create a new application
3. Copy your **Access Key**
4. Save it for Step 4

---

## Step 3: Push Your Code to GitHub

**What you're doing:** Putting your project on GitHub so Render can host your backend and auto-update when you change code.

**Why:** Render needs your code on GitHub to deploy it. No GitHub = no backend.

**How:**

1. Open **PowerShell** in your project folder: `c:\Users\ghaza\OneDrive\Desktop\Project2025\WallCorePro`
2. Run these commands one by one:
   ```powershell
   git init
   git add .
   git commit -m "Initial commit"
   git branch -M main
   ```
3. Go to **https://github.com/new** in your browser
4. Create a new repository named **WallCorePro** (or any name)
5. Leave it empty – no README, no .gitignore
6. Click **Create repository**
7. Copy the repository URL (e.g. `https://github.com/YOUR_USERNAME/WallCorePro.git`)
8. Back in PowerShell, add the remote (replace with YOUR URL):
   ```powershell
   git remote add origin https://github.com/YOUR_USERNAME/WallCorePro.git
   git push -u origin main
   ```

**What you should see:** Files being uploaded. If it asks you to log in to GitHub, do that first.

**Tip:** If `git` is not recognized, install Git from https://git-scm.com/download/win

---

## Step 4: Deploy the Backend on Render

**What you're doing:** Creating a free cloud server that fetches wallpapers from Pixabay and sends them to your app.

**Why:** Your app needs this server to get wallpapers. Render hosts it for free.

**How:**

1. Go to: **https://render.com**
2. Click **Get Started** and sign up (you can use GitHub to sign up quickly)
3. Click **New +** → **Blueprint**
4. Connect your GitHub account if asked
5. Select your **WallCorePro** repository
6. Render will detect `render.yaml` – click **Apply**
7. Wait 1–2 minutes for the service to be created
8. Click on your new service (e.g. **wallcorepro-api**)
9. Go to the **Environment** tab (left sidebar)
10. Click **Add Environment Variable**
11. Add your Pixabay key: **Key:** `PIXABAY_API_KEY`, **Value:** paste key from Step 1
12. If you have Unsplash, add: **Key:** `UNSPLASH_ACCESS_KEY`, **Value:** your Unsplash key
13. Click **Save Changes**
14. Wait 2–3 minutes for the deploy to finish
15. Copy your service URL from the top (e.g. `https://wallcorepro-api.onrender.com`)

**What you should see:** Status changes to **Live** (green). Your backend is running.

**Note:** On the free tier, Render sleeps after 15 minutes of no use. The first request after that may take 30–60 seconds. That's normal.

---

## Step 5: Tell Your App Where the Backend Is

**What you're doing:** Updating one line in your app so it knows your backend's URL.

**Why:** If the URL is wrong, the app won't load wallpapers.

**How:**

1. Open: `app\src\main\java\com\offline\wallcorepro\config\AppConfig.kt`
2. Find the line with `BASE_URL` (around line 180)
3. Replace with your Render URL – it **must** end with `/v1/`
4. Example: `const val BASE_URL = "https://YOUR-URL.onrender.com/v1/"`
5. Save the file

---

## Step 6: Set Privacy Policy URL and Support Email (Required for Play Store)

**What you're doing:** Giving Google Play a link to your privacy policy and an email for support.

**Why:** Google rejects apps without a privacy policy URL and contact email.

**How – Part A (Create privacy policy with Google Docs):**

1. Create a new Google Doc
2. Paste or write your privacy policy (copy from the in-app Privacy Policy screen)
3. **File** → **Share** → **"Anyone with the link can view"**
4. Copy the link and change `/edit` to `/pub` at the end

**How – Part B (Update AppConfig):**

1. Open `AppConfig.kt`
2. Set: `CUSTOM_PRIVACY_POLICY_URL = "https://docs.google.com/document/d/YOUR_DOC_ID/pub"`
3. Set: `CUSTOM_FEEDBACK_EMAIL = "youremail@gmail.com"`
4. Save the file

**See also:** `NO_DOMAIN_SETUP.md` for more options.

---

## Step 7: Verify Important Settings

**What you're doing:** Making sure the app uses only your backend (not direct API calls).

**Why:** This keeps API keys safe and satisfies Play Store rules.

**How:** Open `AppConfig.kt` and check: `USE_BACKEND_ONLY = true`, `USE_PEXELS_API = false`, `USE_PIXABAY_API = false`

---

## Step 8: Test the Backend

**What you're doing:** Checking that your backend is running and returning wallpapers.

**How:**

1. Open: `https://YOUR-RENDER-URL.onrender.com/health` → should show `{"ok":true}`
2. Open: `https://YOUR-RENDER-URL.onrender.com/v1/wallpapers?niche=GOOD_MORNING&page=1` → should show JSON with wallpapers

If you see errors, check that `PIXABAY_API_KEY` is set in Render (Step 4).

---

## Step 9: Build and Test the App

**What you're doing:** Building the app and installing it on your phone.

**How:**

1. In PowerShell: `.\gradlew assembleDebug` (or `.\gradlew --stop` then `.\gradlew assembleDebug --no-configuration-cache --no-daemon` if you have Java issues)
2. Install: `.\gradlew installDebug` or copy `app\build\outputs\apk\debug\app-debug.apk` to your phone
3. Open the app and check that wallpapers load

---

## Step 10: Publish to Google Play

**What you're doing:** Building a release version and submitting to the Play Store.

**How:**

1. Build: `.\gradlew assembleRelease`
2. Sign the AAB/APK (see Android docs)
3. Go to **Google Play Console** → Create/select app
4. Fill in store listing, screenshots
5. In **App content**: add Privacy policy URL and Contact email (same as in AppConfig)
6. Upload signed AAB and submit for review

---

## Summary Checklist

- [ ] Step 1: Pixabay API key obtained
- [ ] Step 2: (Optional) Unsplash API key obtained
- [ ] Step 3: Code pushed to GitHub
- [ ] Step 4: Backend deployed on Render with API keys
- [ ] Step 5: `BASE_URL` updated in AppConfig.kt
- [ ] Step 6: Privacy policy URL and support email set
- [ ] Step 7: `USE_BACKEND_ONLY = true` verified
- [ ] Step 8: Backend health & wallpapers tested
- [ ] Step 9: App built and wallpapers load
- [ ] Step 10: Submitted to Play Store

---

## Why This Is Safe & Compliant

| Concern | How It's Handled |
|---------|------------------|
| Play Store sees third-party APIs | App only calls YOUR backend – no Pixabay/Unsplash in the app |
| Pexels forbids wallpaper apps | Pexels is disabled – we use Pixabay only |
| API keys exposed | Keys stay in Render Environment only – never in the app |
| Privacy policy | App has in-app policy + you provide a URL for Play Store |

---

## Need Help?

- **Quick version:** `QUICK_REFERENCE.md`
- **No domain?** `NO_DOMAIN_SETUP.md` – use Google Docs for privacy policy
- **Compliance details:** `COMPLIANCE.md`
