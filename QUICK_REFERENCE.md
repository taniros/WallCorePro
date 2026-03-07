# WallCorePro – Lazy-Safe Setup (Pinned)

## Your minimal flow

### 1. Open everything you need
```powershell
.\scripts\setup.ps1
```
This opens Pixabay, Render, and GitHub in your browser.

### 2. Follow the 5 steps
Open **START_HERE.md** and follow the steps. It's about 10 minutes total.

### 3. Deploy with one command
```powershell
.\scripts\deploy.ps1
```
This safely pushes to GitHub and triggers Render to deploy.

---

**Safety:** The deploy script blocks `.env` and secrets from being committed, and the app only talks to your backend (no direct Pixabay in the app).

**Lazy:** After the first setup, you only run `.\scripts\deploy.ps1` when you change something. Render handles the rest.
