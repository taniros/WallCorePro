package com.offline.wallcorepro.data.local

/**
 * Filters out wallpapers that contain people — specifically women, girls,
 * men, couples or any identifiable human subjects — to keep the content
 * strictly nature / flower / sky / night-sky themed for this niche.
 *
 * Used as a post-fetch guard after every Pexels and Pixabay API call.
 */
object ContentFilter {

    /**
     * Exact word tokens that indicate human subjects.
     * Checked as whole words to avoid false positives
     * (e.g. "landscape" must not match "man" inside "romance").
     */
    private val PEOPLE_TOKENS = setOf(
        // Women / girls — all variants
        "woman", "women", "girl", "girls", "lady", "ladies", "female", "females",
        "bride", "princess", "queen", "girlfriend", "wife", "mother", "mom",
        "daughter", "sister", "aunt", "grandmother", "grandma",
        "ms", "mrs", "miss",
        // Men / boys — all variants
        "man", "men", "boy", "boys", "male", "males", "guy", "guys",
        "groom", "king", "boyfriend", "husband", "father", "dad",
        "son", "brother", "uncle", "grandfather", "grandpa",
        // Generic people
        "person", "people", "human", "humans", "individual", "individuals",
        "someone", "somebody",
        // Relationships / couples
        "couple", "couples", "lovers", "lover", "romance with person",
        "together", "wedding", "marriage", "married",
        // Model / portrait / beauty photography
        "model", "models", "portrait", "portraits", "selfie", "selfies",
        "face", "faces", "headshot", "headshots", "closeup face",
        // Fashion / lifestyle
        "fashion", "beauty", "makeup", "cosmetic", "outfit", "dress", "dressed",
        "wearing", "bikini", "lingerie",
        // Family / social groups
        "family", "families", "baby", "babies", "infant", "infants",
        "child", "children", "kid", "kids", "toddler", "teenager", "teen", "teens",
        "friend", "friends", "group of people", "crowd",
        // Body parts (indicate human subjects)
        "skin", "hair", "smile", "smiling", "eyes", "eye", "lips",
        "hand", "hands", "arm", "arms", "shoulder", "shoulders",
        "body", "figure", "silhouette", "shadow of person",
        // Activity descriptors that imply people
        "holding", "hugging", "kissing", "embracing", "posing",
        "sitting girl", "standing woman", "walking woman",
        // Photography styles that imply people subjects
        "boudoir", "glamour", "pinup", "pin-up"
    )

    /**
     * Checks whether an image contains people-related content and should be excluded.
     *
     * Three signals are combined for maximum accuracy:
     * - [tags]     comma-separated tag string from Pixabay (most reliable signal)
     * - [alt]      alt / description text from Pexels
     * - [title]    image title from either API
     *
     * @return true if any people keyword is found → image must be excluded
     */
    fun containsPeople(tags: String = "", alt: String = "", title: String = ""): Boolean {
        val combined = "$tags $alt $title".lowercase()
        // Split into words (non-alpha chars are separators) for whole-word matching
        val words = combined.split(Regex("[^a-z]+")).filter { it.length > 1 }.toSet()
        return PEOPLE_TOKENS.any { token ->
            // Multi-word phrases use substring match; single words use exact set membership
            if (token.contains(' ')) combined.contains(token)
            else words.contains(token)
        }
    }

    /**
     * Quick check — returns true only if the image is safe (no people detected).
     * Convenience inverse of [containsPeople] for use in filter chains.
     */
    fun isSafe(tags: String = "", alt: String = "", title: String = ""): Boolean =
        !containsPeople(tags = tags, alt = alt, title = title)
}
