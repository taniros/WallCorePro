# WallCorePro – 0-Risk Compliance

This document explains how the app stays compliant with Play Store policies and image licensing.

---

## Strategy: Backend-Only

```
┌─────────────┐      ┌─────────────────┐      ┌─────────────┐
│  Your App   │ ───► │  Your Backend   │ ───► │   Pixabay   │
│  (Play)     │      │  (Render, etc.) │      │   API       │
└─────────────┘      └─────────────────┘      └─────────────┘
       │                       │
       │                       │ Only backend has
       │                       │ Pixabay API key
       │                       │
       └── App never talks     └── Images fetched
           to Pixabay              server-side only
```

**Result:** Google Play cannot see Pixabay, Pexels, or any third-party image API in your app. The app only calls your own backend.

---

## Licensing

| Source | Status | Notes |
|--------|--------|-------|
| **Pixabay** | ✅ Used | Free for commercial use. No attribution required. |
| **Pexels** | ❌ Not used | Prohibits wallpaper apps. Disabled. |

Pixabay license: [pixabay.com/service/license](https://pixabay.com/service/license/)  
- Content can be used for free for commercial and non-commercial use  
- No attribution required (optional)  
- Quote overlays = creative use, fully allowed  

---

## Play Store Compliance

| Requirement | How We Comply |
|-------------|---------------|
| No direct third-party API keys in app | `USE_BACKEND_ONLY = true` – app has no Pexels/Pixabay keys |
| No prohibited content sources | Only Pixabay (allowed) via backend |
| Privacy | Backend does not collect user data |
| Content policy | Nature/flower/sky images only; no people/portraits |

---

## Configuration Checklist

In `AppConfig.kt`:

- [x] `USE_BACKEND_ONLY = true`
- [x] `USE_PEXELS_API = false`
- [x] `USE_PIXABAY_API = false`
- [ ] `BASE_URL` = your deployed backend URL + `/v1/`

---

## Risk Summary

| Risk | Mitigation |
|------|------------|
| Play Store rejects for third-party APIs | App only calls your backend |
| Pixabay ToS violation | Backend fetches; no re-hosting; images from Pixabay CDN |
| Pexels violation | Pexels fully disabled |
| API key exposure | Key only in backend env, never in app |

**Conclusion:** With backend-only mode and Pixabay as the sole source, the app is designed for 0 licensing and Play Store risk.
