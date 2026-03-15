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
const PIXABAY_KEY  = process.env.PIXABAY_API_KEY;
const UNSPLASH_KEY = process.env.UNSPLASH_ACCESS_KEY;
const PEXELS_KEY   = process.env.PEXELS_API_KEY;
const GEMINI_KEY   = process.env.GEMINI_API_KEY;
const ADMIN_KEY    = process.env.ADMIN_SECRET_KEY;  // For GitHub Actions sync trigger

if (!PIXABAY_KEY) {
  console.error('FATAL: Set PIXABAY_API_KEY in environment. Get free key at https://pixabay.com/api/docs/');
  process.exit(1);
}
if (UNSPLASH_KEY) console.log('Unsplash enabled – more wallpaper variety');
if (PEXELS_KEY)   console.log('Pexels enabled – premium wallpaper quality');
if (GEMINI_KEY)   console.log('Gemini AI enabled – wish generation via backend');

// JSON body parsing + CORS for app requests
app.use(express.json());
app.use((req, res, next) => {
  res.header('Access-Control-Allow-Origin', '*');
  res.header('Access-Control-Allow-Methods', 'GET, POST');
  res.header('Access-Control-Allow-Headers', 'Content-Type');
  next();
});

// ── All search queries flat-listed for the infinite-wheel algorithm ─────────
// 40 queries total (20 morning + 20 night) — diverse mix of landscapes, skies,
// beaches, mountains, wildlife, seasonal, and flowers.
// Each session uses a random seed that offsets BOTH which query is picked AND
// which Pixabay page is fetched, so every session starts at a unique position.
const ALL_QUERIES = [
  // Morning (20 queries) — indices 0-19
  'sunrise pink golden sky clouds dramatic beautiful landscape',
  'rainbow after rain sky colorful morning nature stunning',
  'hot air balloon sunrise sky colorful dreamy landscape',
  'golden hour dramatic sky clouds orange pink sunrise beautiful',
  'snow mountain peak sunrise golden light landscape dramatic',
  'misty mountain valley morning fog ethereal landscape beautiful',
  'autumn forest mountain lake reflection golden morning',
  'grand canyon sunrise orange rock landscape dramatic beautiful',
  'tropical beach turquoise water morning golden light paradise',
  'waterfall morning mist sunbeam tropical forest beautiful',
  'lake reflection mountains morning calm peaceful beautiful',
  'ocean waves sunrise morning golden light peaceful beach',
  'cherry blossom sunrise golden hour pink trees nature aesthetic',
  'autumn maple trees golden orange red leaves morning light',
  'bamboo forest green morning light peaceful nature beautiful',
  'peacock feathers iridescent colorful beautiful macro nature',
  'butterfly flower macro colorful bokeh morning garden nature',
  'lavender field sunrise purple golden light nature peaceful',
  'tulip field spring colorful flowers sunrise nature beautiful',
  'sunflower field golden morning sunrise blue sky nature beautiful',
  // Night (20 queries) — indices 20-39
  'milky way stars purple blue night sky mountains stunning',
  'northern lights aurora borealis night sky green mountains',
  'nebula colorful space cosmic galaxy stars beautiful',
  'astrophotography stars milky way desert landscape night',
  'meteor shower night sky stars long exposure beautiful',
  'full moon night sky clouds dramatic beautiful nature',
  'bioluminescent ocean waves glowing blue night beach beautiful',
  'moonlit lake reflection calm water night peaceful beautiful',
  'waterfall night long exposure moonlight nature beautiful',
  'snow mountain night stars landscape dramatic beautiful cold',
  'fireflies night forest magical glowing trees nature dreamy',
  'autumn night moon silhouette tree orange leaves beautiful',
  'lightning storm night sky dramatic clouds nature powerful',
  'crystal cave glowing blue stalactites underground beautiful',
  'deep ocean underwater glowing bioluminescent blue beautiful',
  'city lights night skyline reflection water beautiful',
  'abstract dark colorful bokeh night purple blue beautiful',
  'roses candlelight bokeh dark night flowers warm romantic',
  'crescent moon night sky clouds stars peaceful nature lovely',
  'purple galaxy stars cosmos beautiful night nature gorgeous'
];

// Max Pixabay page to use (Pixabay reliably returns results up to page 20).
const MAX_PIXABAY_PAGE = 20;

// People-related tags — any Pixabay photo whose tags contain these is dropped.
const PEOPLE_TAGS = new Set([
  'woman','women','girl','girls','man','men','person','people','model',
  'portrait','couple','wedding','bride','groom','lady','boy','child',
  'children','baby','human','female','male','face','smile','hair','student'
]);

function containsPeople(tags = '') {
  const lower = tags.toLowerCase();
  return [...PEOPLE_TAGS].some(t => lower.includes(t));
}
// Each category has 5 diverse queries. The server rotates through them using
// (page + seed) % 5 so every request to the same category yields different images.
const CATEGORY_MAP = {
  'Morning Greetings': [
    'sunrise pink golden sky clouds beautiful landscape',
    'cherry blossom morning sunrise golden hour trees nature',
    'mountain lake reflection morning calm peaceful beautiful',
    'rainbow morning sky colorful nature stunning beautiful',
    'waterfall morning mist sunbeam forest golden beautiful'
  ],
  'Afternoon Wishes': [
    'tropical beach turquoise water golden afternoon paradise',
    'sunflower field blue sky bright afternoon sunshine beautiful',
    'butterfly garden colorful flowers afternoon macro nature',
    'mountain valley green afternoon golden landscape beautiful',
    'autumn park golden leaves afternoon sunshine peaceful'
  ],
  'Evening Greetings': [
    'sunset sky dramatic orange clouds beautiful landscape',
    'golden hour mountain silhouette sunset beautiful landscape',
    'ocean sunset waves evening beautiful golden light',
    'desert dunes golden sunset evening dramatic beautiful',
    'lavender field sunset purple sky evening peaceful'
  ],
  'Night Wishes': [
    'milky way stars night sky mountains beautiful stunning',
    'northern lights aurora borealis night sky colorful beautiful',
    'moonlit lake reflection calm water night peaceful',
    'fireflies forest magical glowing night trees dreamy',
    'full moon night sky clouds dramatic beautiful nature'
  ],
  'Morning Love': [
    'red roses morning dew garden soft bokeh romantic',
    'pink flowers heart bokeh morning garden beautiful soft',
    'cherry blossom petals pink morning romantic nature bokeh',
    'rose garden golden morning light beautiful soft romantic',
    'tulips red pink morning garden romantic beautiful nature'
  ],
  'Afternoon Love': [
    'red roses heart sunlight garden warm bokeh romantic',
    'pink flowers golden afternoon garden beautiful soft',
    'tropical hibiscus flower colorful afternoon warm beautiful',
    'rose garden afternoon warm golden light romantic bokeh',
    'magnolia blossom pink afternoon garden romantic beautiful'
  ],
  'Evening Love': [
    'red roses heart sunset golden hour romantic bokeh',
    'pink flowers sunset garden evening romantic beautiful bokeh',
    'rose petals candlelight evening warm bokeh soft romantic',
    'golden sunset flowers garden evening beautiful soft bokeh',
    'heart bokeh flowers evening warm romantic light beautiful'
  ],
  'Nightly Love': [
    'red roses candle night moon flowers bokeh dark romantic',
    'pink roses moonlight soft bokeh night beautiful romantic',
    'heart bokeh lights night dark flowers warm romantic',
    'roses candlelight dark bokeh night beautiful soft glow',
    'moonlit flowers garden night romantic peaceful bokeh'
  ],
  'Family Wishes': [
    'sunflower field golden sunshine warm morning beautiful nature',
    'autumn maple trees golden orange leaves morning beautiful',
    'tropical paradise beach turquoise water golden beautiful',
    'meadow wildflowers colorful sunshine warm peaceful nature',
    'cherry blossom park golden morning beautiful nature peaceful'
  ],
  'Friends Greetings': [
    'colorful wildflowers meadow sunshine beautiful vibrant nature',
    'butterfly garden colorful flowers macro beautiful stunning',
    'rainbow sky colorful morning nature stunning beautiful',
    'tropical flowers vibrant colorful paradise beautiful nature',
    'peacock feathers iridescent colorful beautiful macro nature'
  ],
  'Daily Blessings': [
    'golden sunrise mountains peaceful beautiful light rays',
    'waterfall sunbeam morning mist golden beautiful nature',
    'lake reflection mountain sunrise peaceful calm beautiful',
    'rainbow golden sky beautiful peaceful nature stunning',
    'misty forest golden light rays morning ethereal beautiful'
  ],
  'Spiritual Morning': [
    'sunrise golden light rays sky peaceful beautiful nature',
    'mountain top sunrise golden light dramatic beautiful',
    'ocean sunrise golden rays morning peaceful beautiful',
    'misty valley golden morning light ethereal beautiful nature',
    'dawn golden hour sky clouds light rays beautiful peaceful'
  ],
  'Sweet Night Dreams': [
    'dreamy moonlit clouds night stars soft beautiful nature',
    'milky way stars dreamy night sky purple blue beautiful',
    'aurora borealis night sky dreamy colorful beautiful nature',
    'fireflies forest magical glowing night dreamy beautiful',
    'bioluminescent ocean glowing blue night beautiful dreamy'
  ],
  'Romantic Greetings': [
    'red roses pink flowers heart bokeh romantic beautiful',
    'cherry blossom petals pink bokeh romantic nature beautiful',
    'rose garden soft bokeh morning romantic beautiful nature',
    'heart shaped roses pink bokeh flowers romantic beautiful',
    'tulips pink red romantic garden beautiful nature soft'
  ]
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

async function fetchPexels(query, page = 1, perPage = 15) {
  if (!PEXELS_KEY) return [];
  try {
    const url = `https://api.pexels.com/v1/search?query=${encodeURIComponent(query)}&page=${page}&per_page=${perPage}&orientation=portrait`;
    const res = await fetch(url, { headers: { 'Authorization': PEXELS_KEY } });
    if (!res.ok) return [];
    const data = await res.json();
    return data.photos || [];
  } catch (e) {
    console.warn('Pexels fetch failed:', e.message);
    return [];
  }
}

function toWallpaperDtoFromPexels(photo, category = 'General', niche = 'GOOD_MORNING') {
  const id = `pexels_${photo.id}`;
  const imageUrl = photo.src?.large2x || photo.src?.large || photo.src?.original;
  const thumb    = photo.src?.medium  || photo.src?.small;
  const title    = (photo.alt || 'Beautiful Wallpaper').split(' ').slice(0, 4).join(' ').replace(/^\w/, c => c.toUpperCase());
  return {
    id, title, imageUrl, thumbnailUrl: thumb, category, niche,
    dominantColor:    photo.avg_color || '#FF6F00',
    isTrending:       false,
    isPremium:        false,
    createdAt:        Date.now(),
    downloadsCount:   0,
    photographer:     photo.photographer,
    photographerUrl:  photo.photographer_url,
    alt:              (photo.alt || '').toLowerCase()
  };
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
  const tagParts = (hit.tags || '').split(',');
  const title = tagParts[0] ? tagParts[0].trim().replace(/^\w/, c => c.toUpperCase()) : 'Greeting';
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
    // Full comma-separated tags — app uses this for accurate people-content filtering
    alt: (hit.tags || '').toLowerCase()
  };
}

// ─── In-memory wallpaper cache ─────────────────────────────────────────────
// Pre-fetches all 40 queries from Pixabay + Unsplash + Pexels on startup so
// every /v1/wallpapers request is served in < 50 ms (no live API round-trip).
// Auto-refreshes every CACHE_TTL_MS milliseconds via setTimeout chain.
// ──────────────────────────────────────────────────────────────────────────────
const CACHE = {
  items:      [],        // All pre-fetched & deduped wallpapers (main feed)
  byCategory: {},        // category name → wallpaper[]
  trending:   [],        // Pre-fetched trending wallpapers
  lastBuilt:  0,         // Timestamp of last successful build
  building:   false      // Guard against concurrent builds
};
const CACHE_TTL_MS    = 6 * 60 * 60 * 1000;  // Rebuild every 6 hours
const CACHE_MIN_ITEMS = 200;                   // Min items before serving from cache
const CACHE_PAGES_PER_QUERY = 3;               // Pages fetched per query per source

async function buildCache() {
  if (CACHE.building) return;
  CACHE.building = true;
  const t0 = Date.now();
  console.log('⟳ Building wallpaper cache from all sources...');

  const collected = [];

  // ── Main feed: all 40 queries × 3 pages × 3 sources ──────────────────────
  // Batched in groups of 4 to avoid hammering Pixabay rate limits (100 req/min).
  const BATCH_SIZE = 4;
  for (let b = 0; b < ALL_QUERIES.length; b += BATCH_SIZE) {
    const batch = ALL_QUERIES.slice(b, b + BATCH_SIZE);
    await Promise.all(batch.map(async (query, bi) => {
      const qIdx     = b + bi;
      const category = qIdx < 20 ? 'Morning Greetings' : 'Night Wishes';
      for (let page = 1; page <= CACHE_PAGES_PER_QUERY; page++) {
        try {
          const perPx = PEXELS_KEY ? 15 : (UNSPLASH_KEY ? 20 : 30);
          const perUs = UNSPLASH_KEY ? 8  : 0;
          const perPe = PEXELS_KEY   ? 12 : 0;
          const [px, us, pe] = await Promise.all([
            fetchPixabay(query, page, perPx),
            fetchUnsplash(query, page, perUs),
            fetchPexels(query, page, perPe)
          ]);
          collected.push(
            ...px.filter(h => !containsPeople(h.tags)).map(h => toWallpaperDto(h, category, 'GOOD_MORNING')),
            ...us.filter(p => !containsPeople(p.alt_description || '')).map(p => toWallpaperDtoFromUnsplash(p, category, 'GOOD_MORNING')),
            ...pe.filter(p => !containsPeople(p.alt || '')).map(p => toWallpaperDtoFromPexels(p, category, 'GOOD_MORNING'))
          );
        } catch (e) { /* skip individual failures silently */ }
      }
    }));
    // Small delay between batches to respect rate limits
    if (b + BATCH_SIZE < ALL_QUERIES.length) await new Promise(r => setTimeout(r, 300));
  }

  // ── Per-category cache: CATEGORY_MAP queries, 2 pages each ───────────────
  const newByCategory = {};
  for (const [cat, queries] of Object.entries(CATEGORY_MAP)) {
    const catItems = [];
    for (let qi = 0; qi < queries.length; qi++) {
      const q = queries[qi];
      try {
        const [px, us, pe] = await Promise.all([
          fetchPixabay(q, 1, PEXELS_KEY ? 15 : 25),
          fetchUnsplash(q, 1, UNSPLASH_KEY ? 8 : 0),
          fetchPexels(q, 1, PEXELS_KEY ? 12 : 0)
        ]);
        catItems.push(
          ...px.filter(h => !containsPeople(h.tags)).map(h => toWallpaperDto(h, cat, 'GOOD_MORNING')),
          ...us.filter(p => !containsPeople(p.alt_description || '')).map(p => toWallpaperDtoFromUnsplash(p, cat, 'GOOD_MORNING')),
          ...pe.filter(p => !containsPeople(p.alt || '')).map(p => toWallpaperDtoFromPexels(p, cat, 'GOOD_MORNING'))
        );
      } catch (e) { /* skip */ }
    }
    newByCategory[cat] = catItems;
  }

  // ── Trending cache ────────────────────────────────────────────────────────
  const TRENDING_QUERIES = [
    'beautiful landscape nature sunrise mountains stunning',
    'aurora borealis night sky colorful beautiful nature',
    'waterfall tropical forest misty golden beautiful',
    'cherry blossom sakura pink spring beautiful nature'
  ];
  const trendingItems = [];
  for (const q of TRENDING_QUERIES) {
    try {
      const [px, us, pe] = await Promise.all([
        fetchPixabay(q, 1, 10),
        fetchUnsplash(q, 1, 5),
        fetchPexels(q, 1, 8)
      ]);
      trendingItems.push(
        ...px.map(h => { const w = toWallpaperDto(h, 'Trending', 'GOOD_MORNING'); w.isTrending = true; return w; }),
        ...us.map(p => { const w = toWallpaperDtoFromUnsplash(p, 'Trending', 'GOOD_MORNING'); w.isTrending = true; return w; }),
        ...pe.map(p => { const w = toWallpaperDtoFromPexels(p, 'Trending', 'GOOD_MORNING'); w.isTrending = true; return w; })
      );
    } catch (e) { /* skip */ }
  }

  // ── Deduplicate and commit ─────────────────────────────────────────────────
  const seen = new Set();
  CACHE.items      = collected.filter(w => { if (seen.has(w.id)) return false; seen.add(w.id); return true; });
  CACHE.byCategory = newByCategory;
  CACHE.trending   = trendingItems;
  CACHE.lastBuilt  = Date.now();
  CACHE.building   = false;

  console.log(`✓ Cache built: ${CACHE.items.length} wallpapers, ${Object.keys(newByCategory).length} categories, ${trendingItems.length} trending — ${((Date.now()-t0)/1000).toFixed(1)}s`);

  // Schedule next rebuild after TTL
  setTimeout(() => buildCache().catch(console.error), CACHE_TTL_MS);
}

// GET /v1/wallpapers
//
// Infinite-wheel algorithm:
//   seed  (0–999) — per-session random value sent by the app.
//   page  (1+)    — ever-increasing scroll position.
//
// Effective Pixabay page = ((page - 1 + seed) % MAX_PIXABAY_PAGE) + 1
//   → cycles Pixabay pages 1–20 based on both page and seed.
//
// Two queries are selected per request (offset by seed so different sessions
// see different query combinations on the same page number):
//   q1 = ALL_QUERIES[(page - 1 + seed)         % ALL_QUERIES.length]
//   q2 = ALL_QUERIES[(page - 1 + seed + 12)    % ALL_QUERIES.length]
//
// hasNext is ALWAYS true — the app never sees end-of-pagination.
// People-tagged photos are filtered out before the response is sent.
app.get('/v1/wallpapers', async (req, res) => {
  try {
    const { niche = 'GOOD_MORNING', page = 1, per_page = 20, category, seed = 0 } = req.query;
    const pageNum    = Math.max(1, parseInt(page)    || 1);
    const seedNum    = Math.abs(parseInt(seed)    || 0) % 10000;
    const perPageNum = Math.min(50, Math.max(10, parseInt(per_page) || 20));

    // ── Serve from in-memory cache (< 50 ms, no external API call) ───────────
    if (category && CATEGORY_MAP[category]) {
      const pool = CACHE.byCategory[category];
      if (pool && pool.length > 0) {
        const start = ((pageNum - 1) * perPageNum + seedNum * 7) % pool.length;
        const wallpapers = Array.from({ length: perPageNum }, (_, i) => pool[(start + i) % pool.length]);
        return res.json({ wallpapers, total: wallpapers.length, page: pageNum, hasNext: true });
      }
    } else if (CACHE.items.length >= CACHE_MIN_ITEMS) {
      // stride spreads requests across the whole cache so seed variety is maximised
      const stride = Math.max(1, Math.floor(CACHE.items.length / 200));
      const start  = ((pageNum - 1) * perPageNum + seedNum * stride) % CACHE.items.length;
      const wallpapers = Array.from({ length: perPageNum }, (_, i) => CACHE.items[(start + i) % CACHE.items.length]);
      if (pageNum === 1) wallpapers.slice(0, 5).forEach(w => { w.isTrending = true; });
      return res.json({ wallpapers, total: wallpapers.length, page: pageNum, hasNext: true });
    }

    // ── Cache not warm yet — fall back to live fetch (only on first cold start) ─
    let wallpapers = [];

    if (category && CATEGORY_MAP[category]) {
      const queries = CATEGORY_MAP[category];
      const query = queries[(pageNum + seedNum) % queries.length];
      const effectivePage = ((pageNum - 1 + seedNum) % MAX_PIXABAY_PAGE) + 1;
      const [px, us, pe] = await Promise.all([
        fetchPixabay(query, effectivePage, PEXELS_KEY ? 15 : (UNSPLASH_KEY ? 20 : perPageNum)),
        fetchUnsplash(query, effectivePage, UNSPLASH_KEY ? 8 : 0),
        fetchPexels(query, effectivePage, PEXELS_KEY ? 12 : 0)
      ]);
      wallpapers = [
        ...px.filter(h => !containsPeople(h.tags)).map(h => toWallpaperDto(h, category, niche)),
        ...us.filter(p => !containsPeople(p.alt_description || '')).map(p => toWallpaperDtoFromUnsplash(p, category, niche)),
        ...pe.filter(p => !containsPeople(p.alt || '')).map(p => toWallpaperDtoFromPexels(p, category, niche))
      ];
    } else {
      const q1Idx = (pageNum - 1 + seedNum)      % ALL_QUERIES.length;
      const q2Idx = (pageNum - 1 + seedNum + 20) % ALL_QUERIES.length;
      const effectivePage = ((pageNum - 1 + seedNum) % MAX_PIXABAY_PAGE) + 1;
      const perQ = Math.ceil(perPageNum / (PEXELS_KEY ? 3 : (UNSPLASH_KEY ? 3 : 2)));
      const [px1, px2, us1, pe1] = await Promise.all([
        fetchPixabay(ALL_QUERIES[q1Idx], effectivePage, perQ),
        fetchPixabay(ALL_QUERIES[q2Idx], effectivePage, perQ),
        fetchUnsplash(ALL_QUERIES[q1Idx], effectivePage, UNSPLASH_KEY ? perQ : 0),
        fetchPexels(ALL_QUERIES[q1Idx], effectivePage, PEXELS_KEY ? perQ : 0)
      ]);
      const catQ1 = q1Idx < 20 ? 'Morning Greetings' : 'Night Wishes';
      const catQ2 = q2Idx < 20 ? 'Morning Greetings' : 'Night Wishes';
      wallpapers = [
        ...px1.filter(h => !containsPeople(h.tags)).map(h => toWallpaperDto(h, catQ1, niche)),
        ...px2.filter(h => !containsPeople(h.tags)).map(h => toWallpaperDto(h, catQ2, niche)),
        ...us1.filter(p => !containsPeople(p.alt_description || '')).map(p => toWallpaperDtoFromUnsplash(p, catQ1, niche)),
        ...pe1.filter(p => !containsPeople(p.alt || '')).map(p => toWallpaperDtoFromPexels(p, catQ1, niche))
      ];
      wallpapers.sort((a, b) => String(a.id).localeCompare(String(b.id)));
    }

    if (pageNum === 1) wallpapers.slice(0, 5).forEach(w => { w.isTrending = true; });
    res.json({ wallpapers, total: wallpapers.length, page: pageNum, hasNext: true });

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

    // Serve from cache if available
    if (CACHE.trending.length > 0) {
      const pageN = parseInt(page) || 1;
      const start = ((pageN - 1) * per) % CACHE.trending.length;
      const wallpapers = Array.from({ length: per }, (_, i) => CACHE.trending[(start + i) % CACHE.trending.length]);
      return res.json({ wallpapers, total: wallpapers.length, page: pageN, hasNext: true });
    }

    // Live fallback
    const TRENDING_QUERIES = [
      'beautiful landscape nature sunrise mountains stunning',
      'aurora borealis night sky colorful beautiful nature',
      'waterfall tropical forest misty golden beautiful',
      'cherry blossom sakura pink spring beautiful nature'
    ];
    const trendingQ = TRENDING_QUERIES[(parseInt(page) || 1) % TRENDING_QUERIES.length];
    const [px, us, pe] = await Promise.all([
      fetchPixabay(trendingQ, parseInt(page) || 1, PEXELS_KEY ? 12 : (UNSPLASH_KEY ? 14 : per)),
      fetchUnsplash(trendingQ, parseInt(page) || 1, UNSPLASH_KEY ? 8 : 0),
      fetchPexels(trendingQ, parseInt(page) || 1, PEXELS_KEY ? 10 : 0)
    ]);
    const wallpapers = [
      ...px.map(h => toWallpaperDto(h, 'Trending', niche)),
      ...us.map(p => toWallpaperDtoFromUnsplash(p, 'Trending', niche)),
      ...pe.map(p => toWallpaperDtoFromPexels(p, 'Trending', niche))
    ];
    wallpapers.sort((a, b) => String(a.id).localeCompare(String(b.id)));
    wallpapers.forEach(w => { w.isTrending = true; });
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

// POST /v1/admin/sync — triggered by GitHub Actions to rebuild the wallpaper cache.
// Secured with ADMIN_SECRET_KEY env var (set the same value in GitHub Actions Secret).
app.post('/v1/admin/sync', (req, res) => {
  const key = req.headers['x-admin-key'] || req.query.key;
  if (ADMIN_KEY && key !== ADMIN_KEY) {
    return res.status(401).json({ error: 'Unauthorized — set ADMIN_SECRET_KEY env var' });
  }
  if (CACHE.building) {
    return res.json({ ok: true, message: 'Cache build already in progress' });
  }
  buildCache().catch(err => console.error('Admin sync failed:', err));
  res.json({ ok: true, message: 'Cache rebuild started in background', cacheAge: CACHE.lastBuilt ? Math.round((Date.now() - CACHE.lastBuilt) / 60000) + ' min' : 'never built' });
});

// GET /health — enhanced to report cache status (useful for GitHub Actions)
app.get('/health', (req, res) => res.json({
  ok:         true,
  cacheItems: CACHE.items.length,
  cacheAge:   CACHE.lastBuilt ? Math.round((Date.now() - CACHE.lastBuilt) / 60000) + ' min ago' : 'not built yet',
  building:   CACHE.building,
  sources:    { pixabay: !!PIXABAY_KEY, unsplash: !!UNSPLASH_KEY, pexels: !!PEXELS_KEY, gemini: !!GEMINI_KEY }
}));

app.listen(PORT, () => {
  console.log(`WallCorePro API running on port ${PORT}`);
  console.log(`Endpoints: /v1/wallpapers, /v1/categories, /v1/trending, /v1/ai/*, /v1/admin/sync`);
  console.log(`Sources: Pixabay=${!!PIXABAY_KEY} Unsplash=${!!UNSPLASH_KEY} Pexels=${!!PEXELS_KEY} Gemini=${!!GEMINI_KEY}`);

  // Pre-fill cache in background immediately — takes 30-120 s depending on sources.
  // All /v1/wallpapers requests during this window fall back to live Pixabay fetch.
  // Once built, all subsequent requests are served from memory in < 50 ms.
  setTimeout(() => buildCache().catch(console.error), 500);
});
