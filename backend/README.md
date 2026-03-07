# WallCorePro Backend API

**Play Store compliant:** Your app fetches wallpapers only from this backend. No Pexels/Pixabay in the app = no license risk.

## Automatic Deploy (Recommended)

See **[DEPLOYMENT.md](../DEPLOYMENT.md)** in the project root for the full automatic flow:

1. Push repo to GitHub
2. Connect to Render (Blueprint) → auto-deploys on every push
3. Add `PIXABAY_API_KEY` in Render dashboard
4. App already points to `https://wallcorepro-api.onrender.com/v1/` (update if your URL differs)

## Local Test

```bash
cd backend
npm install
PIXABAY_API_KEY=your_key npm start
```

Then in AppConfig use: `http://10.0.2.2:3000/v1/` (Android emulator) or your machine's IP.

## Endpoints

| Endpoint | Description |
|----------|-------------|
| GET /v1/wallpapers?niche=X&page=1&per_page=20&category=Y | Wallpapers (category optional) |
| GET /v1/categories?niche=X | Categories list |
| GET /v1/trending?niche=X | Trending wallpapers |
| GET /v1/wallpapers/:id | Single wallpaper |

## Other Hosts

- **Railway**: `railway up` or connect GitHub
- **Fly.io**: `fly launch` then `fly deploy`
- **Vercel**: Use serverless (adapt to Vercel Functions)
