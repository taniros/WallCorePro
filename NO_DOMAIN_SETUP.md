# Publish Without a Domain

You don't need wallcorepro.com or any domain. Use free options:

---

## 1. Privacy Policy (required by Play Store)

**Option A – Google Docs (easiest)**

1. Create a Google Doc with your privacy policy text
2. File → Share → change to "Anyone with the link can view"
3. Copy link, change `/edit` to `/pub` at the end
4. In `AppConfig.kt` set:
   ```kotlin
   const val CUSTOM_PRIVACY_POLICY_URL = "https://docs.google.com/document/d/YOUR_DOC_ID/pub"
   ```

**Option B – GitHub Pages (free)**

1. Create a repo, add `privacy.html` with your policy
2. Enable GitHub Pages in repo settings
3. Use: `https://yourusername.github.io/repo-name/privacy`

---

## 2. Support Email (required)

Use your real Gmail. In `AppConfig.kt`:

```kotlin
const val CUSTOM_FEEDBACK_EMAIL = "youremail@gmail.com"
```

---

## 3. Terms (optional)

Same as privacy – use the same Google Doc URL, or leave empty.

```kotlin
const val CUSTOM_TERMS_URL = ""  // or same as CUSTOM_PRIVACY_POLICY_URL
```

---

## Summary

In `AppConfig.kt` set these before publishing:

| Setting | Example |
|---------|---------|
| `CUSTOM_PRIVACY_POLICY_URL` | `https://docs.google.com/document/d/xxx/pub` |
| `CUSTOM_FEEDBACK_EMAIL` | `youremail@gmail.com` |

No domain needed.
