# Wallpaper Source Strategy – License-Safe & Automated

This document describes how the app sources wallpapers to stay compliant with image providers' terms and Google Play policies.

---

## Current Configuration (License-Safe)

| Source  | Enabled | Risk       | Notes                                          |
|---------|---------|------------|------------------------------------------------|
| **Pexels**  | `false` | High (prohibited) | Explicitly forbids wallpaper apps. Keep off.   |
| **Pixabay** | `true`  | Low        | No wallpaper restriction. Quote overlay = creative use. |

---

## How It Works

1. **`USE_PEXELS_API = false`** – Pexels is never called. No terms risk.

2. **`USE_PIXABAY_API = true`** – Pixabay supplies all images:
   - Searches use morning/night illustration queries.
   - Content is filtered (no people).
   - Every wallpaper gets a **quote overlay** → creative modification → helps avoid “standalone” distribution.

3. **Fallback chain**: Pixabay → Backend (if both APIs disabled).

4. **Automatic behavior**:
   - Sync and paging use Pixabay only when Pexels is off.
   - Category sync uses Pixabay only.
   - No manual steps needed.

---

## AppConfig Flags

```kotlin
// app/config/AppConfig.kt
const val USE_PEXELS_API  = false   // OFF for compliance
const val USE_PIXABAY_API = true    // Primary source
```

---

## Future Options (Even Lower Risk)

1. **NASA API**
   - Public domain.
   - Good for space/earth imagery.
   - Can be added as an extra source.

2. **Your own backend**
   - `api.wallcorepro.com` can host pre-licensed images.
   - Curate from CC0 or explicitly licensed sources.
   - Full control and no third‑party terms.

3. **CC0 / public-domain sources**
   - e.g. Openverse, Wikimedia Commons.
   - Search for “wallpaper”, “nature”, “sunrise”, etc.

---

## Attribution

- **Pixabay**: Attribution not required but recommended (e.g. in About/Settings).
- **Photographer credits**: Stored and shown in detail view when available.

---

*Last updated with Pixabay-only, Pexels-disabled configuration.*
