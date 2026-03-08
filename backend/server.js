/**
 * WallCorePro Backend API
 * 
 * Fetches wallpapers from Pixabay + Unsplash (server-side only).
 * AI wishes via Gemini (server-side only).
 * App never touches image/AI APIs directly = Play Store compliant.
 * 
 * Required: PIXABAY_API_KEY
 * Optional: UNSPLASH_ACCESS_KEY (adds more variety)
 * Optional: GEMINI_API_KEY (for AI wish generation)
 */

const express = require('express');
const fetch = require('node-fetch');

const app = express();
const PORT = process.env.PORT || 3000;
const PIXABAY_KEY = process.env.PIXABAY_API_KEY;
const UNSPLASH_KEY = process.env.UNSPLASH_ACCESS_KEY;
const GEMINI_KEY = process.env.GEMINI_API_KEY;

if (!PIXABAY_KEY) {
  console.error('FATAL: Set PIXABAY_API_KEY in environment. Get free key at https://pixabay.com/api/docs/');
  process.exit(1);
}
if (UNSPLASH_KEY) {
  console.log('Unsplash enabled – more wallpaper variety');
}
if (GEMINI_KEY) {
  console.log('Gemini AI enabled – wish generation via backend');
}

// JSON body parsing + CORS for app requests
app.use(express.json());
app.use((req, res, next) => {
  res.header('Access-Control-Allow-Origin', '*');
  res.header('Access-Control-Allow-Methods', 'GET, POST');
  res.header('Access-Control-Allow-Headers', 'Content-Type');
  next();
});

// Morning, afternoon, evening & night queries (same as app config)
const MORNING_QUERIES = [
  'good morning flowers roses nature beautiful',
  'good morning sunrise stunning flowers',
  'sunrise pink golden sky flowers nature',
  'morning roses dew garden sunlight'
];
const AFTERNOON_QUERIES = [
  'good afternoon sunshine flowers nature beautiful',
  'afternoon sun golden light garden flowers',
  'sunny afternoon flowers garden peaceful',
  'beautiful afternoon sky clouds nature'
];
const EVENING_QUERIES = [
  'good evening sunset sky beautiful nature',
  'evening golden hour flowers nature',
  'sunset evening flowers garden peaceful',
  'twilight evening nature beautiful sky'
];
const NIGHT_QUERIES = [
  'good night moon stars nature beautiful',
  'good night flowers stunning roses',
  'full moon night sky stars beautiful',
  'night flowers moonlight petals'
];
const CATEGORY_MAP = {
  'Morning Greetings': 'beautiful sunrise flowers morning garden nature',
  'Afternoon Wishes': 'sunny afternoon flowers garden nature bright',
  'Evening Greetings': 'sunset golden hour sky clouds nature beautiful',
  'Night Wishes': 'beautiful moon night stars sky clouds nature',
  'Morning Love': 'red roses morning dew garden flowers',
  'Afternoon Love': 'red roses sunlight garden flowers warm bokeh',
  'Evening Love': 'red roses sunset golden hour flowers romantic',
  'Nightly Love': 'red roses candle night moon flowers',
  'Family Wishes': 'sunflowers morning sunshine garden',
  'Friends Greetings': 'colorful flowers morning garden',
  'Daily Blessings': 'golden sunrise nature peaceful',
  'Spiritual Morning': 'sunrise golden light rays nature',
  'Sweet Night Dreams': 'dreamy moonlit clouds night stars',
  'Romantic Greetings': 'red roses pink flowers morning dew'
};

async function fetchPixabay(query, page = 1, perPage = 20, category = 'nature') {
  const url = `https://pixabay.com/api/?key=${PIXABAY_KEY}&q=${encodeURIComponent(query)}&image_type=photo&orientation=vertical&safesearch=true&category=${category}&page=${page}&per_page=${perPage}`;
  const res = await fetch(url);
  const data = await res.json();
  return data.hits || [];
}

async function fetchUnsplash(query, page = 1, perPage = 10) {
  if (!UNSPLASH_KEY) return [];
  try {
    const url = `https://api.unsplash.com/search/photos?query=${encodeURIComponent(query)}&page=${page}&per_page=${perPage}&orientation=portrait&content_filter=low`;
    const res = await fetch(url, {
      headers: { 'Authorization': `Client-ID ${UNSPLASH_KEY}` }
    });
    const data = await res.json();
    return data.results || [];
  } catch (e) {
    console.warn('Unsplash fetch failed:', e.message);
    return [];
  }
}

function toWallpaperDtoFromUnsplash(photo, category = 'General', niche = 'GOOD_MORNING') {
  const id = `unsplash_${photo.id}`;
  const imageUrl = photo.urls?.regular || photo.urls?.full || photo.urls?.small;
  const thumb = photo.urls?.small || photo.urls?.thumb;
  const title = (photo.alt_description || photo.description || 'Greeting').split(' ').slice(0, 3).join(' ').replace(/^\w/, c => c.toUpperCase()) || 'Greeting';
  const user = photo.user || {};
  return {
    id,
    title,
    imageUrl,
    thumbnailUrl: thumb,
    category,
    niche,
    dominantColor: photo.color || '#FF6F00',
    isTrending: false,
    isPremium: false,
    createdAt: Date.now(),
    downloadsCount: 0,
    photographer: user.name || user.username || 'Unsplash',
    photographerUrl: user.links?.html || `https://unsplash.com/@${user.username || 'unknown'}`,
    alt: photo.alt_description || ''
  };
}

function toWallpaperDto(hit, category = 'General', niche = 'GOOD_MORNING') {
  const id = `pixabay_${hit.id}`;
  const imageUrl = hit.largeImageURL || hit.webformatURL || hit.previewURL;
  const tags = (hit.tags || '').split(',');
  const title = tags[0] ? tags[0].trim().replace(/^\w/, c => c.toUpperCase()) : 'Greeting';
  return {
    id,
    title,
    imageUrl,
    thumbnailUrl: hit.webformatURL || hit.previewURL,
    category,
    niche,
    dominantColor: hit.dominantColor || '#FF6F00',
    isTrending: false,
    isPremium: false,
    createdAt: Date.now(),
    downloadsCount: 0,
    photographer: hit.user,
    photographerUrl: `https://pixabay.com/users/${hit.user}-${hit.id}/`,
    alt: ''
  };
}

// GET /v1/wallpapers
app.get('/v1/wallpapers', async (req, res) => {
  try {
    const { niche = 'GOOD_MORNING', page = 1, per_page = 20, category } = req.query;
    const pageNum = Math.max(1, parseInt(page) || 1);
    const perPageNum = Math.min(40, Math.max(5, parseInt(per_page) || 20));
    const half = Math.ceil(perPageNum / 2);

    let hits = [];
    if (category && CATEGORY_MAP[category]) {
      const query = CATEGORY_MAP[category];
      const [px, us] = await Promise.all([
        fetchPixabay(query, pageNum, UNSPLASH_KEY ? Math.ceil(perPageNum / 2) : perPageNum),
        UNSPLASH_KEY ? fetchUnsplash(query, pageNum, Math.floor(perPageNum / 2)) : Promise.resolve([])
      ]);
      hits = [
        ...px.map(h => toWallpaperDto(h, category, niche)),
        ...us.map(p => toWallpaperDtoFromUnsplash(p, category, niche))
      ];
      // Deterministic sort by id — stable order for paging (no random shuffle)
      hits.sort((a, b) => String(a.id).localeCompare(String(b.id)));
    } else {
      const mq = MORNING_QUERIES[pageNum % MORNING_QUERIES.length];
      const aq = AFTERNOON_QUERIES[pageNum % AFTERNOON_QUERIES.length];
      const eq = EVENING_QUERIES[pageNum % EVENING_QUERIES.length];
      const nq = NIGHT_QUERIES[pageNum % NIGHT_QUERIES.length];
      const pixabayPer = UNSPLASH_KEY ? Math.ceil(half / 4) : Math.ceil(perPageNum / 4);
      const unsplashPer = UNSPLASH_KEY ? Math.floor(half / 4) : 0;
      const [morningPx, afternoonPx, eveningPx, nightPx, morningUs, afternoonUs, eveningUs, nightUs] = await Promise.all([
        fetchPixabay(mq, pageNum, pixabayPer),
        fetchPixabay(aq, pageNum, pixabayPer),
        fetchPixabay(eq, pageNum, pixabayPer),
        fetchPixabay(nq, pageNum, pixabayPer),
        unsplashPer ? fetchUnsplash(mq, pageNum, unsplashPer) : Promise.resolve([]),
        unsplashPer ? fetchUnsplash(aq, pageNum, unsplashPer) : Promise.resolve([]),
        unsplashPer ? fetchUnsplash(eq, pageNum, unsplashPer) : Promise.resolve([]),
        unsplashPer ? fetchUnsplash(nq, pageNum, unsplashPer) : Promise.resolve([])
      ]);
      hits = [
        ...morningPx.map(h => toWallpaperDto(h, 'Morning Greetings', niche)),
        ...afternoonPx.map(h => toWallpaperDto(h, 'Afternoon Wishes', niche)),
        ...eveningPx.map(h => toWallpaperDto(h, 'Evening Greetings', niche)),
        ...nightPx.map(h => toWallpaperDto(h, 'Night Wishes', niche)),
        ...morningUs.map(p => toWallpaperDtoFromUnsplash(p, 'Morning Greetings', niche)),
        ...afternoonUs.map(p => toWallpaperDtoFromUnsplash(p, 'Afternoon Wishes', niche)),
        ...eveningUs.map(p => toWallpaperDtoFromUnsplash(p, 'Evening Greetings', niche)),
        ...nightUs.map(p => toWallpaperDtoFromUnsplash(p, 'Night Wishes', niche))
      ];
      // Deterministic sort by id — stable order for paging (no random shuffle)
      hits.sort((a, b) => String(a.id).localeCompare(String(b.id)));
    }

    res.json({
      wallpapers: hits,
      total: hits.length,
      page: pageNum,
      hasNext: hits.length >= perPageNum
    });
  } catch (err) {
    console.error('Wallpapers error:', err);
    res.status(500).json({ wallpapers: [], total: 0, page: 1, hasNext: false });
  }
});

// GET /v1/categories
app.get('/v1/categories', (req, res) => {
  const { niche = 'GOOD_MORNING' } = req.query;
  const categories = Object.keys(CATEGORY_MAP).map((name, i) => ({
    id: `${niche}_${name.toLowerCase().replace(/\s+/g, '_').replace(/&/g, 'and')}`,
    name,
    imageUrl: '',
    count: 0,
    niche
  }));
  res.json({ categories });
});

// GET /v1/trending
app.get('/v1/trending', async (req, res) => {
  try {
    const { niche = 'GOOD_MORNING', page = 1, per_page = 20 } = req.query;
    const per = parseInt(per_page) || 20;
    const [px, us] = await Promise.all([
      fetchPixabay('beautiful nature flowers', parseInt(page) || 1, UNSPLASH_KEY ? Math.ceil(per / 2) : per),
      UNSPLASH_KEY ? fetchUnsplash('beautiful nature flowers', parseInt(page) || 1, Math.floor(per / 2)) : Promise.resolve([])
    ]);
    const wallpapers = [
      ...px.map(h => toWallpaperDto(h, 'Trending', niche)),
      ...us.map(p => toWallpaperDtoFromUnsplash(p, 'Trending', niche))
    ];
    wallpapers.sort((a, b) => String(a.id).localeCompare(String(b.id)));
    res.json({ wallpapers, total: wallpapers.length, page: 1, hasNext: false });
  } catch (err) {
    console.error('Trending error:', err);
    res.json({ wallpapers: [], total: 0, page: 1, hasNext: false });
  }
});

// GET /v1/wallpapers/:id
app.get('/v1/wallpapers/:id', async (req, res) => {
  const { id } = req.params;
  try {
    if (id.startsWith('pixabay_')) {
      const pixabayId = id.replace('pixabay_', '');
      const url = `https://pixabay.com/api/?key=${PIXABAY_KEY}&id=${pixabayId}`;
      const r = await fetch(url);
      const data = await r.json();
      const hit = data.hits?.[0];
      if (!hit) return res.status(404).json({ error: 'Not found' });
      return res.json(toWallpaperDto(hit));
    }
    if (id.startsWith('unsplash_') && UNSPLASH_KEY) {
      const unsplashId = id.replace('unsplash_', '');
      const url = `https://api.unsplash.com/photos/${unsplashId}`;
      const r = await fetch(url, { headers: { 'Authorization': `Client-ID ${UNSPLASH_KEY}` } });
      if (!r.ok) return res.status(404).json({ error: 'Not found' });
      const photo = await r.json();
      return res.json(toWallpaperDtoFromUnsplash(photo));
    }
    return res.status(404).json({ error: 'Not found' });
  } catch (err) {
    res.status(500).json({ error: 'Server error' });
  }
});

// ─── Gemini AI Proxy (key stays on server, never in app) ────────────────────
const GEMINI_MODEL = 'gemini-2.0-flash';

async function callGemini(prompt) {
  if (!GEMINI_KEY) return null;
  try {
    const url = `https://generativelanguage.googleapis.com/v1beta/models/${GEMINI_MODEL}:generateContent?key=${GEMINI_KEY}`;
    const res = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        contents: [{ parts: [{ text: prompt }] }],
        generationConfig: { temperature: 0.9, maxOutputTokens: 256 }
      })
    });
    const data = await res.json();
    const text = data.candidates?.[0]?.content?.parts?.[0]?.text;
    return text ? text.trim() : null;
  } catch (e) {
    console.warn('Gemini API error:', e.message);
    return null;
  }
}

// POST /v1/ai/generate-wish
app.post('/v1/ai/generate-wish', async (req, res) => {
  try {
    const { niche, mood, userName = '', selectedCategoryKeys = [], variationSeed = 0, tone = 'soft' } = req.body || {};
    const prompt = req.body.prompt;
    if (prompt) {
      const text = await callGemini(prompt);
      if (text) return res.json({ text });
    }
    return res.status(503).json({ error: 'AI unavailable', fallback: true });
  } catch (err) {
    res.status(500).json({ error: 'Server error', fallback: true });
  }
});

// POST /v1/ai/rephrase-wish
app.post('/v1/ai/rephrase-wish', async (req, res) => {
  try {
    const { original, tone = 'soft' } = req.body || {};
    const prompt = req.body.prompt;
    if (prompt) {
      const text = await callGemini(prompt);
      if (text) return res.json({ text });
    }
    return res.status(503).json({ error: 'AI unavailable', fallback: true });
  } catch (err) {
    res.status(500).json({ error: 'Server error', fallback: true });
  }
});

// POST /v1/ai/generate-keywords
app.post('/v1/ai/generate-keywords', async (req, res) => {
  try {
    const { category, niche } = req.body || {};
    const prompt = req.body.prompt;
    if (prompt) {
      const text = await callGemini(prompt);
      if (text) {
        const keywords = text.split(',').map(s => s.trim()).filter(Boolean);
        return res.json({ keywords });
      }
    }
    return res.status(503).json({ error: 'AI unavailable', keywords: [] });
  } catch (err) {
    res.status(500).json({ error: 'Server error', keywords: [] });
  }
});

// Health check
app.get('/health', (req, res) => res.json({ ok: true }));

app.listen(PORT, () => {
  console.log(`WallCorePro API running on port ${PORT}`);
  console.log(`Endpoints: /v1/wallpapers, /v1/categories, /v1/trending, /v1/ai/*`);
});
