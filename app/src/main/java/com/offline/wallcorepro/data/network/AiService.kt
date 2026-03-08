package com.offline.wallcorepro.data.network

import com.offline.wallcorepro.config.AppConfig
import com.offline.wallcorepro.config.AppConfig.QuoteCategory
import com.offline.wallcorepro.data.local.WishQuotePool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class AiService @Inject constructor(
    private val aiApiService: AiApiService
) {

    /**
     * Generates a personalised wish based on niche (Morning / Night) and mood.
     *
     * This function always returns [Result.success].
     * If Gemini is unavailable (quota exceeded, network error, etc.) the
     * response is silently sourced from [WishQuotePool] — the caller never
     * sees a raw API error message.
     */
    /**
     * Generates a beautiful, unique greeting wish.
     *
     * [variationSeed] is incremented by the caller on each request.  It drives
     * two forcing mechanisms that guarantee Gemini produces a different message
     * every single time — even when the same [mood] is requested again:
     *   1. A rotating metaphor "universe" (ocean, forest, stars, garden…)
     *   2. A rotating sentence-structure rule (single lyrical line, two-part, etc.)
     * Result is always [Result.success] — failures fall back to the local pool.
     */
    suspend fun generateWish(
        niche: String,
        mood: String,
        userName: String = "",
        selectedCategoryKeys: Set<String> = emptySet(),
        variationSeed: Int = 0,
        tone: String = AppConfig.EmotionalTone.default.key  // NEW: emotional tone adapter
    ): Result<String> = withContext(Dispatchers.IO) {
        val isMorning        = niche.contains("morning", ignoreCase = true)
        val timeWord         = if (isMorning) "morning" else "night"
        val nameClause       = if (userName.isNotBlank()) ", ${userName.trim()}" else ""
        val audience         = buildAudienceHint(selectedCategoryKeys)
        val styleGuide       = buildStyleGuide(mood)
        val toneGuide        = buildToneGuide(tone)
        val metaphorUniverse = METAPHOR_UNIVERSES[variationSeed % METAPHOR_UNIVERSES.size]
        val structureRule    = STRUCTURE_RULES[variationSeed % STRUCTURE_RULES.size]

        return@withContext try {
            val prompt = """
                You are a poet-greeting master. Write ONE unique "Good $timeWord" message. Never repeat what you've written before.

                ── Hard requirements ────────────────────────────────────────────────────────
                • Opening:   MUST start with exactly "Good $timeWord$nameClause!"
                • Audience:  $audience
                • Mood:      $mood — $styleGuide
                • Tone:      $tone — $toneGuide
                • Length:    130–165 characters total (including opening + emojis)
                • Emojis:    2–3 emojis placed naturally inside the text, not just at the end
                • No clichés: never write "may your day be filled with", "wishing you a wonderful",
                              "have a great day", "hope you have a", or any overused phrases

                ── Creative lens for THIS generation ────────────────────────────────────────
                • Draw imagery ONLY from: $metaphorUniverse
                • Sentence structure:     $structureRule
                • Make the reader feel the specific emotion of $mood within the first 5 words

                ── Format ───────────────────────────────────────────────────────────────────
                Output the message text ONLY. No quotes around it. No explanation. No title.
                ── End ──────────────────────────────────────────────────────────────────────
            """.trimIndent()

            val response = aiApiService.generateWish(AiPromptRequest(prompt))
            if (!response.isSuccessful) throw Exception("API ${response.code()}")
            val raw  = response.body()?.text?.trim() ?: throw Exception("Empty AI response")
            val text = raw.trimStart('"', '"', '\'', '«').trimEnd('"', '"', '\'', '»').trim()
            Result.success(text)

        } catch (e: Exception) {
            Timber.w("AI backend unavailable (${e.message?.take(60)}) — serving local fallback")
            var fallback = WishQuotePool.getFallbackWish(niche, mood)
            if (userName.isNotBlank()) {
                fallback = fallback.replaceFirst(
                    Regex("Good (morning|night)!", RegexOption.IGNORE_CASE),
                    "Good $timeWord, ${userName.trim()}!"
                )
            }
            Result.success(fallback)
        }
    }

    /**
     * Rephrases an existing wish with a fresh voice while preserving its meaning.
     * Always succeeds — falls back to [original] if Gemini is unavailable.
     */
    suspend fun rephraseWish(
        original: String,
        tone: String = AppConfig.EmotionalTone.default.key
    ): Result<String> = withContext(Dispatchers.IO) {
        val toneGuide = buildToneGuide(tone)
        return@withContext try {
            val prompt = """
                You are a master wish-writer. Rephrase the following greeting message.

                ── Rules ────────────────────────────────────────────────────────────────────
                • Keep EXACTLY the same greeting opening word (Good morning / Good night)
                • Preserve the core meaning and emotional intent
                • Use the tone: $tone — $toneGuide
                • Same approximate length as the original
                • Use 2–3 fresh emojis placed naturally
                • Do NOT copy any phrase from the original — every word must be new
                • No quotes, no explanation, output the rephrased message ONLY

                ── Original message ──────────────────────────────────────────────────────────
                $original
                ── End ──────────────────────────────────────────────────────────────────────
            """.trimIndent()

            val response = aiApiService.rephraseWish(AiPromptRequest(prompt))
            if (!response.isSuccessful) throw Exception("API ${response.code()}")
            val raw  = response.body()?.text?.trim() ?: throw Exception("Empty AI response")
            val text = raw.trimStart('"', '"', '\'', '«').trimEnd('"', '"', '\'', '»').trim()
            Result.success(text)
        } catch (e: Exception) {
            Timber.w("rephraseWish failed — returning original (${e.message?.take(40)})")
            Result.success(original) // graceful fallback: keep the original
        }
    }

    // ── Variety pools (rotate with variationSeed) ─────────────────────────────

    private val METAPHOR_UNIVERSES = listOf(
        "sunrise, golden light, morning dew, blossoming flowers, warm rays",
        "ocean waves, sea breeze, horizon, tides, the sound of water",
        "moonlight, stars, night sky, silver glow, constellations",
        "forest, trees, birdsong, green light through leaves, roots",
        "fire, candle flame, warmth, ember, the hearth",
        "mountain peaks, fresh air, heights, wide open sky, stillness",
        "garden, petals, fragrance, butterflies, soft earth after rain",
        "clouds, wind, drifting, open sky, freedom",
        "rivers, flow, current, clear water, reflection",
        "seasons changing, autumn leaves, first snow, spring blossoms",
        "desert, endless sand, starry silence, vast space",
        "city lights at night, neon reflections, the hum of the world waking"
    )

    private val STRUCTURE_RULES = listOf(
        "One vivid opening statement, then one tender closing wish",
        "Two short punchy lines — one observation, one gift",
        "Start with a nature metaphor, end with a personal declaration",
        "A single lyrical sentence that flows like a river",
        "Question or exclamation first, then a warm conclusion",
        "Three compact beats: image → emotion → blessing",
        "Begin mid-thought as if continuing a beautiful conversation",
        "Paint a scene in the first half, reveal the feeling in the second",
        "Lead with the emotion, close with a vivid sensory image",
        "An intimate whisper tone — as if written only for this one person",
        "Energetic and bold — short, sharp, unforgettable",
        "Gentle and slow — like honey poured over the words"
    )

    private fun buildAudienceHint(keys: Set<String>): String {
        if (keys.isEmpty()) return "a loved one, friend or family member"
        val labels = keys.mapNotNull { QuoteCategory.fromKey(it)?.displayName }
        return when {
            labels.isEmpty() -> "a loved one, friend or family member"
            labels.size == 1 -> labels.first()
            else             -> labels.dropLast(1).joinToString(", ") + " or " + labels.last()
        }
    }

    /** Maps the user's persistent [EmotionalTone] into a writing instruction. */
    private fun buildToneGuide(tone: String): String = when (tone.lowercase()) {
        "soft"      -> "gentle pacing, nurturing language, soft imagery like petals and morning light — comfort above all"
        "deep"      -> "philosophical depth, rich metaphor, words that linger — make the reader pause and feel"
        "romantic"  -> "passionate warmth, longing beauty, the feeling of being truly loved — intimate and devoted"
        "energetic" -> "bold rhythm, dynamic verbs, fire-like energy — ignite the reader's spirit from the first word"
        "funny"     -> "playful wit, gentle humour, a light smile — never sarcastic, always warm and joyful"
        else        -> "warm, balanced and beautifully human"
    }

    private fun buildStyleGuide(mood: String): String = when (mood.lowercase()) {
        "inspirational" -> "bold, uplifting — paint a vivid image of possibility and inner strength"
        "romantic"      -> "tender, intimate, heartfelt — like a whisper meant only for them"
        "funny"         -> "playful and clever — a genuine smile, not a forced joke"
        "heartfelt"     -> "deeply warm, sincere — the kind of message that makes someone feel truly seen"
        "spiritual"     -> "faith-filled and serene — grounded in grace, not in religion alone"
        "formal"        -> "elegant and refined — respectful warmth with a touch of class"
        "cute"          -> "soft, sweet, adorable — like a sunny hug in words"
        "for mom"       -> "overflowing with love and gratitude — as deep as a mother's own love"
        "for dad"       -> "proud, respectful, deeply grateful — honouring his quiet, steady strength"
        "for husband"   -> "romantic and devoted — celebrating your chosen partner in life"
        "for wife"      -> "adoring and tender — she is your world and this message should show it"
        "for friends"   -> "genuine and joyful — the way only a true friend can make you feel"
        "motivation"    -> "fired up and purposeful — make them feel unstoppable"
        "birthday"      -> "celebratory and loving — today is all about them, make it shine"
        else            -> "warm, genuine and beautifully human"
    }

    /**
     * Generates aesthetic search keywords for a given category.
     * Falls back to [AppConfig.CATEGORY_QUERY_MAP] data via the repository
     * on failure — returns [Result.failure] so the caller can handle it.
     */
    suspend fun generateSearchKeywords(category: String, niche: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)
            val monthName    = java.text.DateFormatSymbols().months[currentMonth]

            val prompt = """
                Given the category '$category' for a '${niche.lowercase()}' wallpaper app.
                Current month: $monthName.
                Generate 5 high-quality, aesthetic-focused search keywords for Pexels/Pixabay.
                Focus on: Colors, mood, lighting (e.g., bokeh, golden hour), and seasonal relevance.
                Format: comma separated list of phrases.
                Example: 'ethereal morning mist, golden hour forest, minimal pastel sunrise'
                Rules: NO card text, NO greeting card, ONLY background descriptions.
            """.trimIndent()

            val response = aiApiService.generateKeywords(AiPromptRequest(prompt))
            if (!response.isSuccessful) throw Exception("API ${response.code()}")
            val keywords = response.body()?.keywords?.filter { it.isNotEmpty() } ?: throw Exception("Empty AI response")
            Result.success(keywords)

        } catch (e: Exception) {
            Timber.e(e, "AI keyword generation failed")
            Result.failure(e)
        }
    }
}
