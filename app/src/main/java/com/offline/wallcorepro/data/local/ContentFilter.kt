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
        "bride", "princess", "girlfriend", "wife", "mother", "mom",
        "daughter", "sister", "aunt", "grandmother", "grandma",
        // Men / boys — all variants
        "man", "men", "boy", "boys", "male", "males", "guy", "guys",
        "groom", "boyfriend", "husband", "father", "dad",
        "son", "brother", "uncle", "grandfather", "grandpa",
        // Generic people
        "person", "people", "human", "humans",
        "someone", "somebody",
        // Relationships / couples
        "couple", "couples", "lovers", "lover", "romance with person",
        "wedding", "marriage", "married",
        // Model / portrait / beauty photography
        "model", "models", "portrait", "portraits", "selfie", "selfies",
        "face", "faces", "headshot", "headshots", "closeup face",
        // Fashion / lifestyle
        "fashion", "makeup", "cosmetic", "outfit", "bikini", "lingerie",
        // Family / social groups
        "baby", "babies", "infant", "infants",
        "child", "children", "kid", "kids", "toddler", "teenager",
        "group of people", "crowd of people",
        // Body parts that clearly indicate human subjects
        "skin", "smile", "smiling", "lips",
        "shoulder", "shoulders", "silhouette of person", "shadow of person",
        // Activity descriptors that imply people
        "hugging", "kissing", "embracing", "posing",
        "sitting girl", "standing woman", "walking woman",
        // Photography styles that imply people subjects
        "boudoir", "glamour", "pinup", "pin-up"
    )

    /**
     * Tokens for cross symbols and religious imagery.
     * A wallpaper app focused on nature/sky/flowers must not show crucifixes,
     * church steeples, cemetery crosses, or any other cross-shaped religious art.
     */
    private val CROSS_TOKENS = setOf(
        // The cross symbol itself — use phrases to avoid false positives
        "cross necklace", "cross pendant", "crucifix", "crucifixes", "cruciform",
        "christian cross", "holy cross", "latin cross", "wooden cross",
        "iron cross", "celtic cross",
        // Locations that feature crosses/graves
        "cemetery", "cemeteries", "graveyard", "graveyards",
        "tombstone", "tombstones", "headstone", "headstones",
        // Clearly religious structures (not general scenic)
        "church", "churches", "chapel", "chapels", "cathedral", "cathedrals",
        "monastery", "monasteries", "convent",
        "mosque", "mosques", "minaret", "minarets",
        "religion", "religious", "worship", "praying",
        "holy bible", "scripture",
        "jesus", "christ", "christianity", "catholicism",
        "archangel", "cherub", "cherubs",
        "rosary", "rosaries", "halo", "halos"
    )

    /**
     * Checks whether an image contains people-related OR cross/religious content
     * and should be excluded.
     *
     * Three signals are combined for maximum accuracy:
     * - [tags]     comma-separated tag string from Pixabay (most reliable signal)
     * - [alt]      alt / description text from Pexels
     * - [title]    image title from either API
     *
     * @return true if any blocked keyword is found → image must be excluded
     */
    fun containsPeople(tags: String = "", alt: String = "", title: String = ""): Boolean {
        val combined = "$tags $alt $title".lowercase()
        // Split into words (non-alpha chars are separators) for whole-word matching
        val words = combined.split(Regex("[^a-z]+")).filter { it.length > 1 }.toSet()

        fun matches(tokenSet: Set<String>) = tokenSet.any { token ->
            if (token.contains(' ')) combined.contains(token)
            else words.contains(token)
        }

        return matches(PEOPLE_TOKENS) || matches(CROSS_TOKENS)
    }

    /**
     * Quick check — returns true only if the image is safe (no blocked content).
     * Convenience inverse of [containsPeople] for use in filter chains.
     */
    fun isSafe(tags: String = "", alt: String = "", title: String = ""): Boolean =
        !containsPeople(tags = tags, alt = alt, title = title)
}
