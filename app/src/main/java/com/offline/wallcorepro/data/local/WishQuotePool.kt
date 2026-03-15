package com.offline.wallcorepro.data.local

import com.offline.wallcorepro.config.AppConfig
import com.offline.wallcorepro.config.AppConfig.QuoteCategory

/**
 * Local pool of greeting quotes, organised by [QuoteCategory].
 *
 *  • Card quotes    – short (~70 chars), rendered as overlay text on wallpaper cards.
 *  • AI fallbacks   – full wish messages (~150 chars), used when Gemini quota is exceeded.
 *
 * All quotes are niche-pure Good Morning / Good Night greetings tailored to each
 * category (love, for mom, for dad, for husband, for wife, friends, motivation, birthday).
 *
 * ── Non-repeating guarantee ──────────────────────────────────────────────────────────────
 * Each category uses a "shuffle deck": the full pool is shuffled once, then quotes are
 * served in order. When the deck is exhausted it is reshuffled and refilled so the user
 * never sees the same quote twice in a row until the entire pool has been shown.
 */
object WishQuotePool {

    // ── Per-category card overlay quote pools ────────────────────────────────

    private val poolByCategory: Map<QuoteCategory, List<String>> = mapOf(

        QuoteCategory.GOOD_MORNING to listOf(
            "Good morning! Wishing you a day full of joy. ☀️",
            "Good morning! Your smile brightens the whole world. 🌸",
            "Good morning! Today is full of beautiful possibilities. ✨",
            "Good morning! Sending you love and warm hugs. 💕",
            "Good morning! May peace and happiness fill your day. 🌼",
            "Good morning! You are a blessing to everyone around you. 💛",
            "Good morning! Rise up and make this day amazing! 🌅",
            "Good morning! May God's grace guide your every step. 🙏",
            "Good morning! Let your light shine bright today. ⭐",
            "Good morning! Life is beautiful — so are you. 🌸",
            "Good morning! A fresh start, endless blessings. ☀️",
            "Good morning! Sending you sweet smiles and warm wishes. 🌻",
            "Good morning! May your day be as lovely as your heart. 💖",
            "Good morning! Wake up and choose joy! ☀️",
            "Good morning! The best is yet to come — believe it! ✨",
            "Good morning! You deserve all the happiness in the world. 💛",
            "Good morning! Another chance to love, laugh and shine. 🌟",
            "Good morning! Sunshine and smiles coming your way. ☀️🌸",
            "Good morning, sunshine! The world is better with you in it.",
            "Good morning! Wishing you love, peace and beautiful moments. 💕"
        ),

        QuoteCategory.GOOD_AFTERNOON to listOf(
            "Good afternoon! Wishing you a productive and peaceful rest of the day. 🌤️",
            "Good afternoon! May the sun shine bright on your path ahead. ☀️",
            "Good afternoon! Sending you warmth and positive energy. 💛",
            "Good afternoon! Hope your day is going wonderfully. ✨",
            "Good afternoon! Take a moment to breathe and appreciate today. 🌸",
            "Good afternoon! You're doing great — keep that momentum! 💪",
            "Good afternoon! Wishing you clarity and calm for the hours ahead. 🌿",
            "Good afternoon! May your afternoon be as lovely as your smile. 💕",
            "Good afternoon! Sending sunshine and good vibes your way. ☀️",
            "Good afternoon! Hope the rest of your day is simply amazing. 🌟",
            "Good afternoon! You deserve a wonderful afternoon. 🌤️",
            "Good afternoon! May peace and joy fill your afternoon. ✨",
            "Good afternoon! Wishing you strength and serenity. 💛",
            "Good afternoon! Let the afternoon light guide you forward. ☀️",
            "Good afternoon! Beautiful wishes for a beautiful afternoon. 🌸",
            "Good afternoon! May your afternoon be full of blessings. 🙏",
            "Good afternoon! Sending love and light your way. 💕",
            "Good afternoon! Hope you're having a fantastic day so far. 🌟",
            "Good afternoon! Wishing you an afternoon of peace and productivity. 🌤️",
            "Good afternoon! You're making today count — well done! ✨"
        ),

        QuoteCategory.GOOD_EVENING to listOf(
            "Good evening! Wishing you a peaceful and beautiful night ahead. 🌅",
            "Good evening! May the twilight bring you calm and comfort. ✨",
            "Good evening! Sending you warm wishes as the day winds down. 🌙",
            "Good evening! Hope your evening is as lovely as the sunset. 🌅",
            "Good evening! May peace surround you this evening. 💫",
            "Good evening! Wishing you a relaxing and joyful evening. 🌸",
            "Good evening! As the day ends, may your heart be full. 💕",
            "Good evening! Sending love and light for your evening. 🌟",
            "Good evening! May the evening bring you rest and renewal. 🌅",
            "Good evening! Wishing you a serene and beautiful evening. ✨",
            "Good evening! Hope you enjoy a wonderful evening ahead. 🌙",
            "Good evening! May your evening be filled with warmth. 💛",
            "Good evening! Sending peaceful vibes your way. 🌿",
            "Good evening! Wishing you a calm and blessed evening. 🙏",
            "Good evening! May the golden hour bring you joy. 🌅",
            "Good evening! Hope your evening is full of love and light. 💕",
            "Good evening! Wishing you a restful and happy evening. ✨",
            "Good evening! May the evening stars watch over you. ⭐",
            "Good evening! Sending heartfelt wishes for your evening. 🌸",
            "Good evening! Wishing you peace as the day turns to night. 🌙"
        ),

        QuoteCategory.GOOD_NIGHT to listOf(
            "Good night! May your dreams be as sweet as you are. 🌙",
            "Good night! Rest well and wake up refreshed. ⭐",
            "Good night! Sending you warm wishes under the stars. 🌟",
            "Good night! Sweet dreams, beautiful soul. 💫",
            "Good night! You are loved more than you know. 💕",
            "Good night! May peace and calm surround you. 🌙",
            "Good night! Tomorrow holds beautiful things for you. ✨",
            "Good night! Sleep well — you've truly earned it! 🌟",
            "Good night! May the angels watch over you tonight. 🕊️",
            "Good night! Wishing you a peaceful and restful sleep. 🌙",
            "Good night! Let go of today and dream of tomorrow. ⭐",
            "Good night! Stars shine especially for you tonight. 🌟",
            "Good night! May your dreams bring you joy and peace. 🌙",
            "Sweet dreams! Tomorrow is a beautiful new beginning. ✨",
            "Good night! Close your eyes and find sweet peace. 🌙",
            "Good night! Wishing you love and beautiful dreams. 💕",
            "Good night! Let the moon light your way to sweet dreams. 🌙",
            "Good night! May you rest in love and total comfort. 💛",
            "Good night! Sleep blessed and wake up inspired. 🌟",
            "Good night! Counting stars, thinking of you. 🌙⭐"
        ),

        QuoteCategory.LOVE to listOf(
            "Good morning, my love! You are my sunshine. ❤️",
            "Good morning! Wishing a wonderful day to the one I love. 💕",
            "Good night, my love! Dream of us together. 🌙❤️",
            "Good morning! Every day with you is a beautiful blessing. 🌹",
            "Good night, darling! Sleep in my sweetest thoughts. 💙",
            "Good morning! My heart smiles whenever I think of you. 💖",
            "Good night, love! Dream of all our happy moments. 💕",
            "Good morning! You make my whole world brighter. ☀️❤️",
            "Good morning! Sending all my love your way today. 💕",
            "Good night! Missing you in the most beautiful way. 🌙💕",
            "Good morning! Loving you is the best part of my day. 🌸❤️",
            "Good night! My last thought every night is you. 💖🌙",
            "Good morning! Your love is my favourite reason to smile. ☀️",
            "Good night! You are my moon, my stars, my everything. 🌙✨",
            "Good morning! With you, every sunrise is magic. 🌅❤️",
            "Good night! Sleep well, the one who has my whole heart. 💕🌙",
            "Good morning! I fall in love with you every single morning. 🌸",
            "Good night! My dreams are sweeter because of you. 🌙💛",
            "Good morning! You are my favourite hello and hardest goodbye. 💕",
            "Good night! Wrapped in love, sleeping in your thoughts. 🌙❤️"
        ),

        QuoteCategory.FOR_MOM to listOf(
            "Good morning, Mum! Your love lights up every single day. 💐",
            "Good night, Mum! Thank you for everything you do. 🌙💖",
            "Good morning, Mom! Wishing you a day as warm as your heart. ☀️",
            "Good night, Mom! You are the greatest gift in my life. 🌟",
            "Good morning! To the world's best mum — rise and shine! 💐☀️",
            "Good night, Mom! Your love is my forever safe place. 🌙💛",
            "Good morning, Mama! I'm grateful for you every single day. 🌸",
            "Good night! Sleep well, the woman who means the world to me. 🌙",
            "Good morning, Mom! Your smile is the sunshine of my life. ☀️💕",
            "Good night, Mama! Sweet dreams to my greatest hero. 🌟💐",
            "Good morning! Every morning is brighter because of your love, Mom. 🌅",
            "Good night! The world is better because you're in it, Mum. 🌙",
            "Good morning, Mom! Sending you the biggest hug and smile. 💐😊",
            "Good night, Mom! May you rest as peacefully as you deserve. 🌙💖",
            "Good morning! To the woman who gave me everything — have a wonderful day! ☀️",
            "Good night, Mama! Thank you for being my constant strength. 💐🌙",
            "Good morning, Mom! Your kindness and love inspire me always. 🌸",
            "Good night! Dream beautifully, the most wonderful mum ever. 🌙✨",
            "Good morning! Grateful for your endless love and care, Mom. 💐☀️",
            "Good night, Mum! You are loved beyond words. 💛🌙"
        ),

        QuoteCategory.FOR_DAD to listOf(
            "Good morning, Dad! Your strength inspires me every single day. ☀️💪",
            "Good night, Dad! Thank you for being my hero always. 🌙",
            "Good morning, Baba! Wishing you a day full of joy. ☀️",
            "Good night, Dad! Sleep well — you deserve all the rest. 🌟",
            "Good morning! To the greatest dad — rise and shine! ☀️👨",
            "Good night, Dad! Your love and wisdom guide me always. 🌙",
            "Good morning, Dad! Sending you love, respect and gratitude. 💪☀️",
            "Good night, Father! You are my pillar — rest well tonight. 🌙",
            "Good morning, Dad! I'm so proud to be your child. ☀️💛",
            "Good night! Sweet dreams to the man I look up to most. 🌟",
            "Good morning, Dad! Your hard work is my biggest motivation. ☀️💪",
            "Good night, Dad! Thank you for always being there for me. 🌙",
            "Good morning! To my first hero and best mentor — good morning! ☀️",
            "Good night, Dad! May your rest be as deep as your love for us. 🌙",
            "Good morning, Daddy! Your smile is my favourite sunshine. ☀️😊",
            "Good night, Father! Grateful for your endless love and sacrifice. 🌙💛",
            "Good morning, Dad! Wishing you a wonderful, peaceful day ahead. ☀️",
            "Good night! Dream big, amazing Dad — you deserve every blessing. 🌙",
            "Good morning, Dad! The world is better because you lead our family. ☀️",
            "Good night, Dad! You are appreciated, loved and respected. 🌙💪"
        ),

        QuoteCategory.FOR_HUSBAND to listOf(
            "Good morning, husband! You make every day worth waking up to. ☀️💍",
            "Good night, my husband! You are my greatest adventure. 🌙❤️",
            "Good morning! To my partner, my love, my best friend — rise! 💕☀️",
            "Good night! I fall asleep grateful to have you in my life. 🌙💍",
            "Good morning, love! Thank you for being an amazing husband. ☀️❤️",
            "Good night! Dream well, the man who holds my heart. 🌙💕",
            "Good morning! Your love is the best start to every single day. ☀️💍",
            "Good night, husband! You are my peace, my joy and my home. 🌙",
            "Good morning! I love the life we build together every day. 🌅💕",
            "Good night! Sleep close — you are everything to me. 🌙❤️",
            "Good morning, my love! Another day to cherish being yours. ☀️💍",
            "Good night, husband! My heart is happiest when you're near. 🌙💕",
            "Good morning! Thank you for your love, patience and care. ☀️❤️",
            "Good night! May your dreams be as wonderful as you are. 🌙💍",
            "Good morning! Waking up next to you is my favourite blessing. 🌸☀️",
            "Good night, my dearest husband! You are adored and cherished. 🌙",
            "Good morning! Every day with you is a beautiful gift. ☀️💖",
            "Good night! I love you more each day — sleep peacefully. 🌙❤️",
            "Good morning! My heart is full of love and gratitude for you. 💕☀️",
            "Good night, husband! Thank you for making my life so beautiful. 🌙💍"
        ),

        QuoteCategory.FOR_WIFE to listOf(
            "Good morning, wife! You are the sunshine of my life. ☀️🌹",
            "Good night, my love! Dream of how deeply you are adored. 🌙💕",
            "Good morning! To my beautiful wife — today is yours. 🌸☀️",
            "Good night! You are the most precious part of my world. 🌙🌹",
            "Good morning, darling! Your love makes everything brighter. ☀️❤️",
            "Good night, wife! Sleep well — you deserve every blessing. 🌙💖",
            "Good morning! Thank you for your endless love and grace. 🌹☀️",
            "Good night! I am the luckiest because I have you. 🌙💕",
            "Good morning, my queen! Your smile is my favourite sight. ☀️🌸",
            "Good night! May your sleep be as peaceful as your heart. 🌙🌹",
            "Good morning, love! Every day with you is an absolute gift. 💕☀️",
            "Good night, my wife! You are loved beyond every measure. 🌙❤️",
            "Good morning! Your love is the best reason to rise. 🌅🌹",
            "Good night! Grateful every night to call you mine. 🌙💖",
            "Good morning, wife! Thank you for making our home so beautiful. 🌸☀️",
            "Good night, darling! My last thought is always of you. 🌙💕",
            "Good morning! You are my greatest blessing, my dearest wife. ☀️🌹",
            "Good night! Sweet dreams — you deserve every happiness. 🌙",
            "Good morning! Your love gives me strength to face every day. ☀️❤️",
            "Good night, wife! You are my peace, my joy and my everything. 🌙🌹"
        ),

        QuoteCategory.FOR_FRIENDS to listOf(
            "Good morning, bestie! Today is going to be amazing. 🌟",
            "Good morning! Friends like you make every day brighter. ☀️",
            "Good night! Wishing my dearest friend sweet, sweet dreams. 🌸",
            "Good morning! Grateful for your friendship every single day. 💛",
            "Good night! Sleep well, my wonderful, irreplaceable friend. 🌙",
            "Good morning! Sending big hugs to my favourite friend! 🤗☀️",
            "Good morning, friend! I hope your day is as great as you are. 🌟",
            "Good night! You make life so much more fun — rest well. 🌙😊",
            "Good morning! Thinking of you and wishing you a beautiful day. ☀️💛",
            "Good night! Friends like you are life's greatest treasure. 🌙🌟",
            "Good morning! Lucky to have a friend like you in my corner. ☀️🤝",
            "Good night! Sweet dreams, my amazing and loyal friend. 🌙💛",
            "Good morning! Let's make today as wonderful as our friendship. 🌸☀️",
            "Good night! Grateful for your laughter and love — sleep tight. 🌙",
            "Good morning, friend! Your positivity inspires me every day. ✨☀️",
            "Good night! Friends who care like you deserve the best dreams. 🌙💕",
            "Good morning! Rise and shine — great friends deserve great days. ☀️",
            "Good night! Thank you for always being there — rest beautifully. 🌙🌟",
            "Good morning! Sending sunshine, smiles and warm friendship hugs. ☀️🤗",
            "Good night! Sleep peacefully, the friend who means the world. 🌙💛"
        ),

        QuoteCategory.MOTIVATION to listOf(
            "Good morning! Believe in yourself — today you can do anything. 💪☀️",
            "Good night! Rest well — tomorrow you'll rise even stronger. 🌙✨",
            "Good morning! Every great day starts with the right mindset. ☀️🔥",
            "Good night! Let your dreams fuel tomorrow's greatness. 🌙💡",
            "Good morning! You are capable of incredible things — go prove it! 💪🌅",
            "Good night! Recharge fully — champions need quality rest too. 🌙🏆",
            "Good morning! Success begins the moment you decide to start. ☀️✨",
            "Good night! Let go of setbacks — tomorrow is a brand new canvas. 🌙",
            "Good morning! Your only competition is yesterday's version of you. 💪",
            "Good night! Great things are built one restful night at a time. 🌙🌟",
            "Good morning! Rise with purpose — greatness is calling your name. ☀️🔥",
            "Good night! Every dream you chase starts with a good night's rest. 🌙",
            "Good morning! Small steps every day lead to extraordinary results. ✨☀️",
            "Good night! The difference between dreams and reality is called sleep. 🌙💪",
            "Good morning! Choose courage over comfort — today is your day! 🌅💪",
            "Good night! Reset, reflect, and come back stronger tomorrow. 🌙✨",
            "Good morning! Positive mind, positive life — let's go! ☀️💡",
            "Good night! Your future self will thank you for resting well. 🌙",
            "Good morning! Make today so amazing that yesterday gets jealous. ☀️🔥",
            "Good night! Dream boldly — the universe is always listening. 🌙💫"
        ),

        QuoteCategory.BIRTHDAY to listOf(
            "Good morning and Happy Birthday! 🎂 Today is YOUR special day! ☀️🎉",
            "Good night and Happy Birthday! 🎈 May all your birthday wishes come true. 🌙",
            "Good morning! Wishing you the happiest of birthdays and beyond. 🎂☀️",
            "Good night! Another year older, wiser and more wonderful. 🎉🌙",
            "Good morning! Your birthday is a reminder of how loved you are. 🎂💕",
            "Good night! Happy Birthday — may your dreams tonight be magical. 🌙🎈",
            "Good morning! Today we celebrate YOU — shine brighter than ever! 🎂☀️",
            "Good night! Grateful another year brought you into the world. 🌙💛",
            "Good morning! Happy Birthday! May this year be your best yet! 🎉☀️",
            "Good night! Sweet birthday dreams to someone truly special. 🌙🎂",
            "Good morning! Sending birthday hugs, love and endless wishes. 🎈☀️",
            "Good night! Wishing you a year full of joy and beautiful surprises. 🌙🎂",
            "Good morning! Today the world got to keep someone extraordinary. 🎉💕",
            "Good night! Happy Birthday — close your eyes to a world of blessings. 🌙",
            "Good morning! It's your birthday — let the celebration begin! 🎂🌟",
            "Good night! Another beautiful year of life — sleep in gratitude. 🌙🎈",
            "Good morning! You deserve every birthday wish and so much more. ☀️💛",
            "Good night! Wishing you joy that lasts far beyond today. 🌙🎂",
            "Good morning! On your birthday — you are incredibly loved. 🎉☀️💕",
            "Good night! Happy Birthday — the world is better with you in it. 🌙🎈"
        )
    )

    // ── Shuffle-deck engine ───────────────────────────────────────────────────
    // Per-category deck: quotes are served in shuffled order with zero repeats
    // until the entire pool has been shown, then the deck is reshuffled.
    private val shuffleDecks = mutableMapOf<String, ArrayDeque<String>>()
    private val deckLock     = Any()

    private fun nextFromDeck(key: String, pool: List<String>): String {
        if (pool.isEmpty()) return "Good morning! ☀️"
        return synchronized(deckLock) {
            val deck = shuffleDecks.getOrPut(key) { ArrayDeque() }
            if (deck.isEmpty()) deck.addAll(pool.shuffled())
            deck.removeFirst()
        }
    }

    // ── Day-of-week card overlay quotes ──────────────────────────────────────

    private val dayOfWeekQuotes = mapOf(
        java.util.Calendar.MONDAY to listOf(
            "Good morning! New week, new blessings — make Monday magnificent! 💪☀️",
            "Good morning! Monday is not just a day, it's a fresh beginning! 🌅",
            "Good morning! Rise and shine — Monday needs your best energy! ⚡☀️",
            "Good night! You owned this Monday — rest now and rise even stronger! 🌙💪",
            "Good morning! Every Monday is a chance to start your best week yet! ✨",
            "Good night! Monday is done — you showed up and that's everything. 🌙"
        ),
        java.util.Calendar.TUESDAY to listOf(
            "Good morning! Tuesday is proof that you survived Monday! Rise & thrive! ☀️😄",
            "Good morning! On this beautiful Tuesday, let your spirit soar! 🌸",
            "Good night! Tuesday was yours — rest deeply and dream boldly! 🌙✨",
            "Good morning! Tuesday brings 24 fresh hours of possibility. Own them! ☀️💛",
            "Good night! You made Tuesday count — sweet dreams beautiful soul! 🌙🌸"
        ),
        java.util.Calendar.WEDNESDAY to listOf(
            "Good morning! Happy Wednesday! You're halfway through — keep shining! 🌟☀️",
            "Good morning! Midweek magic is real — and today it belongs to you! ✨",
            "Good night! Wednesday done and you were wonderful! Rest beautifully. 🌙",
            "Good morning! Wednesday whispers: greatness is just 2 days away! 💫☀️",
            "Good night! Midweek complete — dream of all you'll achieve by Friday! 🌙⭐"
        ),
        java.util.Calendar.THURSDAY to listOf(
            "Good morning! Thursday — almost at the finish line! Push a little more! ☀️🔥",
            "Good morning! Happy Thursday! The weekend is just a heartbeat away! 💛",
            "Good night! Thursday is done — tomorrow is your Friday victory lap! 🌙🎉",
            "Good morning! Thursday means one more day of being absolutely amazing! ✨☀️",
            "Good night! You crushed Thursday! Tomorrow the weekend celebration begins! 🌙💫"
        ),
        java.util.Calendar.FRIDAY to listOf(
            "Good morning! Blessed Friday! May this day be full of joy and light! ✨☀️",
            "Good morning! Friday arrived — and so did your reward for the week! 🎉",
            "Good night! Blessed Friday night — rest well, the weekend is yours! 🌙✨",
            "Good morning! Friday is God's way of saying 'You made it!' 🙏☀️",
            "Good night! Friday nights are made for peace, gratitude and sweet dreams! 🌙🌟",
            "Good morning! Happy Friday! Whatever today brings, you're ready! 💪☀️"
        ),
        java.util.Calendar.SATURDAY to listOf(
            "Good morning! Happy Saturday! Today belongs entirely to you! 🎉☀️",
            "Good morning! Saturday mornings are life's little gift — enjoy every moment! 🌸",
            "Good night! What a beautiful Saturday! Rest well and dream sweetly! 🌙💛",
            "Good morning! Saturday: when the world slows down just for you! ☀️🌺",
            "Good night! Saturday night magic — peace, joy and beautiful rest! 🌙✨"
        ),
        java.util.Calendar.SUNDAY to listOf(
            "Good morning! Blessed Sunday! A day of rest, love and pure gratitude. 🙏☀️",
            "Good morning! Sunday mornings are God's most precious gift! Cherish it! 🌸",
            "Good night! Peaceful Sunday night — rest your soul for a beautiful new week! 🌙",
            "Good morning! Sunday: breathe deeply, love fully, live gratefully! 🌅🙏",
            "Good night! Sunday closes beautifully — tomorrow starts your best week! 🌙🌟",
            "Good morning! Sunday blessings upon you and everyone you love! 💐☀️"
        )
    )

    // ── Occasion-specific card overlay quotes ─────────────────────────────────

    private val occasionQuotes = mapOf(
        "new_year"     to listOf(
            "Happy New Year! 🎊 May this year bring everything your heart deserves!",
            "Happy New Year! 🥂 New year, new joy, new blessings, new beginnings!",
            "Happy New Year! 🎉 May 2025 be your most beautiful chapter yet!",
            "Happy New Year! 🌟 May every day of this year be filled with love and light!"
        ),
        "valentine"    to listOf(
            "Happy Valentine's Day! 💝 You are my favourite everything!",
            "Happy Valentine's Day! 🌹 Love is the most beautiful language — and I speak yours!",
            "Happy Valentine's Day! ❤️ Every day with you is Valentine's Day!",
            "Happy Valentine's Day! 💕 My heart smiles loudest when I think of you!"
        ),
        "womens_day"   to listOf(
            "Happy Women's Day! 🌹 To every woman — you are strength, grace and pure beauty!",
            "Happy Women's Day! 💜 Celebrate the incredible woman that you are today!",
            "Happy Women's Day! 🌸 You inspire the world simply by being yourself!"
        ),
        "mothers_day"  to listOf(
            "Happy Mother's Day! 💐 The world is better because of your endless love!",
            "Happy Mother's Day! 🌸 You are the heart of everything — thank you, Mom!",
            "Happy Mother's Day! 💖 Every good thing in me began with your love, Mum!"
        ),
        "fathers_day"  to listOf(
            "Happy Father's Day! 👨 Your love and strength are my greatest treasure!",
            "Happy Father's Day! ☀️ A father's love is the most powerful force I know!",
            "Happy Father's Day! 💪 You are my hero, my guide and my biggest blessing!"
        ),
        "christmas"    to listOf(
            "Merry Christmas! 🎄 May your home be filled with warmth, joy and love!",
            "Merry Christmas! ⭐ May the magic of this season touch your heart completely!",
            "Merry Christmas! 🎁 Wishing you peace, love and all the blessings of this season!"
        ),
        "new_year_eve" to listOf(
            "Happy New Year's Eve! 🥂 Goodbye old year — hello to a beautiful new chapter!",
            "Happy New Year's Eve! 🎊 Count your blessings tonight — they are more than you know!"
        ),
        "friday"       to listOf(
            "Blessed Friday! ✨ May this special day bring you peace and abundant blessings!",
            "Blessed Friday! 🌙 The most beautiful day of the week — may it shine for you!",
            "Happy Friday! 🎉 You made it through the week — now let the weekend magic begin!"
        ),
        "earth_day"    to listOf(
            "Happy Earth Day! 🌍 Let's cherish and protect this beautiful home we share!",
            "Happy Earth Day! 🌿 One planet, one home, one precious sunrise every morning!"
        )
    )

    // ── AI-style fallback wishes (used when Gemini is unavailable) ────────────

    private val morningAiWishes = mapOf(
        "Inspirational" to listOf(
            "Good morning! Rise and shine — today is a fresh canvas. Paint it with passion, purpose and a smile that lights up the world. ☀️",
            "Good morning! The universe has aligned just for you today. Step out with confidence — your best chapter is still being written. ✨",
            "Good morning! Wake up and believe! Today holds incredible possibilities. Step forward boldly and make it extraordinary. 🌅"
        ),
        "Romantic" to listOf(
            "Good morning, my love! Waking up knowing you exist makes every sunrise more beautiful. You are my forever favourite hello. 💕",
            "Good morning, darling! My heart whispers your name with every morning breeze. You are my most beautiful thought. 🌸❤️",
            "Good morning! If love were a sunrise, it would look exactly like you — warm, radiant and impossible to look away from. 💖"
        ),
        "Funny" to listOf(
            "Good morning! I had grand plans for productivity today. Then coffee happened. So… same mission, slightly caffeinated! ☕😄",
            "Good morning! Great news: coffee exists. Even better news: so do you! Have a magnificent day, you wonderful human! 😂",
            "Good morning! Today's forecast: 100% chance of awesome with a slight chance of afternoon napping. Plan accordingly! 😴☀️"
        ),
        "Heartfelt" to listOf(
            "Good morning! You are stronger than you know, braver than you feel, and loved more than you can ever realise. Blessed day! 🌸",
            "Good morning! Wishing you a day as beautiful as your heart — filled with joy, peace and endless reasons to smile. ✨",
            "Good morning! Whatever yesterday held, today is a brand new gift. Open it with hope and share it generously with love. 💛"
        ),
        "Spiritual" to listOf(
            "Good morning! May God's grace light your path today. Walk in faith, lead with love, and trust His beautiful plan. 🙏",
            "Good morning! Rise with gratitude — every morning is a miracle, a chance to love more and live fully. Blessed morning! ✨",
            "Good morning! May the peace that surpasses all understanding guard your heart as you begin this blessed new day. 🌅🙏"
        ),
        "For Mom" to listOf(
            "Good morning, Mum! Every sunrise reminds me how grateful I am for your love, sacrifice and beautiful heart. Have a blessed day! 💐☀️",
            "Good morning, Mom! You are the reason I believe in unconditional love. Wishing you a day as wonderful as you are! 🌸",
            "Good morning, Mama! Your warmth and kindness are the best gifts I've ever received. Sending you all my love today! 💐"
        ),
        "For Dad" to listOf(
            "Good morning, Dad! Your strength, wisdom and quiet love have shaped everything I am. Have an incredible day, my hero! ☀️💪",
            "Good morning, Father! Your example inspires me every single day. Wishing you a peaceful and fulfilling morning! ☀️",
            "Good morning, Dad! I am grateful every day for your guidance and unconditional love. Have a wonderful day! 💪☀️"
        ),
        "For Husband" to listOf(
            "Good morning, my love! Waking up knowing I get to spend another day with you is the greatest blessing. Have an amazing day! 💍☀️",
            "Good morning, husband! You are my home, my safe place and my greatest adventure. Wishing you a beautiful day! 💕",
            "Good morning, darling! Thank you for your love, your patience and for making every ordinary day feel extraordinary. 💍🌅"
        ),
        "For Wife" to listOf(
            "Good morning, my queen! Your love is the sunshine that makes my whole life bloom. Have a wonderful, beautiful day! 🌹☀️",
            "Good morning, darling! Watching you sleep is my favourite reminder of how blessed I truly am. Rise and shine! 💕",
            "Good morning, wife! Thank you for your grace, your love and for making our life together so beautiful every day. 🌹🌅"
        ),
        "For Friends" to listOf(
            "Good morning, bestie! Life is so much richer with a friend like you in it. Wishing you the most brilliant day! 🤝☀️",
            "Good morning, friend! Your friendship is one of the greatest gifts in my life. Go out there and shine today! 🌟",
            "Good morning! Just wanted you to know I'm thinking of you and wishing you an incredibly wonderful day ahead! ☀️💛"
        ),
        "Motivation" to listOf(
            "Good morning! Today is a blank page in the story of your greatness. Pick up the pen and write something extraordinary! 💪☀️",
            "Good morning! Your potential is limitless and today is your proof. Step out with confidence and conquer what's ahead! 🔥🌅",
            "Good morning! Champions don't wait for perfect conditions — they create them. Go out and own your day! 💪✨"
        ),
        "Birthday" to listOf(
            "Good morning and Happy Birthday! 🎂 Today the world celebrates YOU — and you deserve every single moment of joy it brings! ☀️🎉",
            "Good morning! On your birthday, I want you to know just how loved and appreciated you truly are. Have an amazing day! 🎈💕",
            "Good morning! Another year of life, love and blessings! May this birthday be the beginning of your most beautiful year yet! 🎂☀️"
        )
    )

    private val afternoonAiWishes = mapOf(
        "Inspirational" to listOf(
            "Good afternoon! The day is still full of possibility — make the most of every hour ahead. 🌤️",
            "Good afternoon! You've already accomplished so much today. Keep that energy flowing! ✨",
            "Good afternoon! Midday is the perfect time to refocus and finish strong. You've got this! 💪"
        ),
        "Romantic" to listOf(
            "Good afternoon, my love! Thinking of you in the golden light of this beautiful day. 💕",
            "Good afternoon, darling! Every hour without you feels long — sending you all my love. 🌸",
            "Good afternoon! You're the sunshine that makes my afternoon glow. Missing you! ❤️"
        ),
        "Funny" to listOf(
            "Good afternoon! If the morning was a warm-up, consider this the main event. Caffeine optional! ☕😄",
            "Good afternoon! The day is half done — time to decide: power through or power nap? Both valid! 😴",
            "Good afternoon! Your afternoon coffee and I have something in common: we both think you're essential! ☀️"
        ),
        "Heartfelt" to listOf(
            "Good afternoon! May the rest of your day be as warm and wonderful as you are. ✨",
            "Good afternoon! Sending you peace, clarity and a gentle reminder that you're doing great. 💛",
            "Good afternoon! Whatever this afternoon holds, you have the strength to handle it beautifully. 🌿"
        ),
        "Spiritual" to listOf(
            "Good afternoon! May God's grace carry you through the rest of this blessed day. 🙏",
            "Good afternoon! A moment to pause, breathe and thank the One who holds every hour. ✨",
            "Good afternoon! May divine peace fill your afternoon and guide your steps. 🌤️🙏"
        ),
        "For Mom" to listOf(
            "Good afternoon, Mum! Hope your day is going wonderfully. You deserve every good thing! 💐",
            "Good afternoon, Mom! Sending you love and gratitude for all you do. Rest when you can! 🌸",
            "Good afternoon, Mama! You're the heart of our family — wishing you a peaceful afternoon! 💐☀️"
        ),
        "For Dad" to listOf(
            "Good afternoon, Dad! Hope your day is productive and peaceful. You're our hero! ☀️💪",
            "Good afternoon, Father! Wishing you strength and clarity for the rest of the day. 🙏",
            "Good afternoon, Dad! Thank you for everything — hope this afternoon treats you well! 💪"
        ),
        "For Husband" to listOf(
            "Good afternoon, my love! Missing you and counting the hours until we're together again. 💍",
            "Good afternoon, husband! Hope your day is going great. Sending you all my love! ❤️",
            "Good afternoon, darling! You're on my mind this afternoon — have a wonderful rest of the day! 💕"
        ),
        "For Wife" to listOf(
            "Good afternoon, my queen! Hope your day is as beautiful as you are. Thinking of you! 🌹",
            "Good afternoon, darling! Sending you love and light for the rest of this wonderful day. 💕",
            "Good afternoon, wife! You make every hour brighter. Wishing you a peaceful afternoon! 🌹☀️"
        ),
        "For Friends" to listOf(
            "Good afternoon, friend! Hope your day is going amazing. You're one of the best! 🤝",
            "Good afternoon, bestie! Sending good vibes your way for the rest of the day! 🌟",
            "Good afternoon! Just wanted to say I'm thinking of you. Have a great afternoon! ☀️💛"
        ),
        "Motivation" to listOf(
            "Good afternoon! The best part of the day is still ahead. Make it count! 💪🔥",
            "Good afternoon! You're halfway there — push through and finish strong! ✨",
            "Good afternoon! Champions don't quit at midday. Keep going — you've got this! 🌤️💪"
        ),
        "Birthday" to listOf(
            "Good afternoon and Happy Birthday! 🎂 Hope your special day is going wonderfully! 🎉",
            "Good afternoon! On your birthday — wishing you joy for the rest of this beautiful day! 🎈",
            "Good afternoon! Another year of you in the world — hope your birthday afternoon is magical! 🎂✨"
        )
    )

    private val eveningAiWishes = mapOf(
        "Inspirational" to listOf(
            "Good evening! As the day winds down, may you find peace and gratitude for all you've done. 🌅",
            "Good evening! The evening is a gift — a chance to reflect, rest and prepare for tomorrow. ✨",
            "Good evening! You showed up today. Now let the evening wrap you in calm and renewal. 🌙"
        ),
        "Romantic" to listOf(
            "Good evening, my love! As the sun sets, my thoughts turn to you. Dream of us tonight. 💕",
            "Good evening, darling! The twilight hour is made for missing you. Sending all my love. 🌅❤️",
            "Good evening! You're the most beautiful part of my evening. Thinking of you always. 🌸"
        ),
        "Funny" to listOf(
            "Good evening! The day has spoken. The verdict: you survived. Reward: a well-deserved evening of rest! 😄",
            "Good evening! Dinner, relaxation, or early bed? All valid. You've earned your evening choices! 🌙",
            "Good evening! The sun has clocked out. Your turn to do the same when you're ready! ☀️😴"
        ),
        "Heartfelt" to listOf(
            "Good evening! May this evening bring you peace, warmth and the comfort of knowing you're loved. 🌙💛",
            "Good evening! Release the day's weight and let the evening hold you gently. You deserve rest. ✨",
            "Good evening! As darkness falls, may your heart be full of today's blessings. Sweet evening! 💫"
        ),
        "Spiritual" to listOf(
            "Good evening! May God's peace fill your heart as the day closes. Rest in His grace. 🙏",
            "Good evening! Lay down the day's burdens. The Lord watches over you through the night. 🌟",
            "Good evening! Blessed evening — may divine rest and renewal be yours tonight. 🙏🌅"
        ),
        "For Mom" to listOf(
            "Good evening, Mum! Hope your day was wonderful. Rest well — you deserve it! 💐",
            "Good evening, Mom! Thank you for everything. May your evening be peaceful and blessed. 🌙",
            "Good evening, Mama! You're the best. Wishing you a calm and beautiful evening! 💐✨"
        ),
        "For Dad" to listOf(
            "Good evening, Dad! Hope your day went well. Rest up — you've earned it! ☀️",
            "Good evening, Father! Wishing you a peaceful evening and a restful night ahead. 🌙",
            "Good evening, Dad! Thank you for all you do. Have a blessed evening! 💪🙏"
        ),
        "For Husband" to listOf(
            "Good evening, my love! Can't wait to be with you. Have a peaceful evening! 💍",
            "Good evening, husband! You're my favourite person to spend evenings with. ❤️",
            "Good evening, darling! Sending you love as the day ends. Rest well! 💕🌙"
        ),
        "For Wife" to listOf(
            "Good evening, my queen! Hope your day was beautiful. You make every evening special! 🌹",
            "Good evening, darling! Wishing you a calm and lovely evening. You're so loved! 💕",
            "Good evening, wife! Thank you for another day together. Rest peacefully! 🌹✨"
        ),
        "For Friends" to listOf(
            "Good evening, friend! Hope your day was great. Catch you soon! 🤝",
            "Good evening, bestie! Sending good vibes for a relaxing evening. You're the best! 🌟",
            "Good evening! Hope you're having a wonderful evening. Thinking of you! ☀️💛"
        ),
        "Motivation" to listOf(
            "Good evening! Today you showed up. Tomorrow you'll rise again. Rest well in between! 💪",
            "Good evening! The day is done — recharge for tomorrow's victories. Sleep well! 🌙",
            "Good evening! Champions rest too. Get some quality sleep — tomorrow needs you! ✨"
        ),
        "Birthday" to listOf(
            "Good evening and Happy Birthday! 🎂 Hope your special day was magical! 🎉",
            "Good evening! As your birthday winds down, know you're loved and celebrated! 🎈",
            "Good evening! What a birthday! Rest well — the best of the year is still ahead! 🎂🌙"
        )
    )

    private val nightAiWishes = mapOf(
        "Inspirational" to listOf(
            "Good night! You showed up and gave your best today. Now rest deeply — tomorrow the world needs your strength again. 🌙✨",
            "Good night! As the stars emerge, let your worries fade. You've done enough today. Rest, dream big, rise stronger. ⭐",
            "Good night! The greatest chapters aren't written in daylight alone. Dream boldly tonight and wake up ready to live them. 🌟"
        ),
        "Romantic" to listOf(
            "Good night, my love! Close your eyes knowing someone thinks of you with a heart full of love. Dream beautifully. 🌙💕",
            "Good night, darling! As night falls, my love for you only grows deeper. Meet me in the most beautiful corner of your dreams. ❤️🌙",
            "Good night! The moon knows exactly what I feel for you — endlessly present, quietly radiant, always watching over you. 💙🌙"
        ),
        "Funny" to listOf(
            "Good night! Your pillow has been waiting all day for this reunion. Don't keep it waiting any longer — it misses you! 😂🛏️",
            "Good night! After careful review, your day scores 10/10. Your pillow now officially awards you 8 hours of premium rest! 😄",
            "Good night! Warning: dreams may contain plot twists, flying, and celebrity cameos. Standard wish terms apply! 😂✨"
        ),
        "Heartfelt" to listOf(
            "Good night! Tonight, release what tired you, keep what inspired you, and sleep knowing tomorrow is a beautiful new gift. 🌙💛",
            "Good night! As you close your eyes, know you are deeply loved, endlessly valued, and wonderfully made. Sweet dreams! ❤️🌟",
            "Good night! May you find in sleep the peace you truly deserve — rest easy, beautiful soul. Tomorrow you'll shine again. 🌙✨"
        ),
        "Spiritual" to listOf(
            "Good night! Lay your burdens down and rest in God's grace. He who watches over you neither slumbers nor sleeps. 🙏🌙",
            "Good night! As the night wraps around you, may divine peace fill your heart. Sleep blessed, loved and fully restored. 🌟🙏",
            "Good night! Hand your worries to the Lord and rest in His promises. Tomorrow, His mercies are brand new. 🌙✨"
        ),
        "For Mom" to listOf(
            "Good night, Mum! As you close your eyes, know you are the most cherished person in my world. Sleep beautifully! 💐🌙",
            "Good night, Mom! Thank you for your endless love and sacrifices. Rest peacefully — you deserve every moment of it. 🌙",
            "Good night, Mama! You are my heart's greatest treasure. Sleep well and dream of all the love you so richly deserve. 💐🌟"
        ),
        "For Dad" to listOf(
            "Good night, Dad! Thank you for your strength and steadfast love. Rest well — you have earned every moment of peace. 🌙",
            "Good night, Father! Your guidance is my greatest gift. Sleep peacefully knowing how deeply you are respected and loved. 🌙",
            "Good night, Dad! The world is a better place because of you. Dream peacefully, my hero and my greatest inspiration. 🌙💪"
        ),
        "For Husband" to listOf(
            "Good night, husband! In a world full of people, you are my favourite. Sleep peacefully — you are loved beyond measure. 🌙💍",
            "Good night, my love! As you drift to sleep, know you are cherished, adored and my greatest blessing. Sweet dreams! 🌙❤️",
            "Good night, darling! I am grateful every night that you are mine. Sleep well — tomorrow I'll love you even more. 💍🌙"
        ),
        "For Wife" to listOf(
            "Good night, my queen! Your love is my calmest, happiest thought as I close my eyes. Sleep beautifully tonight. 🌹🌙",
            "Good night, darling! You are my favourite chapter in this story called life. Rest peacefully, my dearest love. 🌙💕",
            "Good night, wife! Grateful every single night for your grace, your love and the beautiful life we share. 🌹🌙"
        ),
        "For Friends" to listOf(
            "Good night, friend! Thank you for your light, your laughter and your love. Sleep well — you are truly appreciated. 🌙💛",
            "Good night! Life would be so much less colourful without a friend like you. Rest well and dream of happy times. 🌙",
            "Good night, bestie! Wishing you a peaceful sleep full of the warmest, happiest dreams. You deserve all good things! 🌙🌟"
        ),
        "Motivation" to listOf(
            "Good night! Recharge fully — the extraordinary version of you has an important day tomorrow. Rest is part of the plan. 🌙💪",
            "Good night! Great achievements begin with great rest. Sleep well, dream big, and wake up ready to change the world! 🌙🔥",
            "Good night! Champions sleep on time. Your body and mind deserve this rest — tomorrow you'll rise and conquer. 🌙✨"
        ),
        "Birthday" to listOf(
            "Good night and Happy Birthday! 🎂 May your birthday dreams be magical and tomorrow bring everything your heart desires. 🌙🎈",
            "Good night! As your birthday ends, know that every candle blown and every wish made is already on its way to you. 🌙🎂",
            "Good night! What a wonderful birthday you've had. Rest well — the best of this year is only just beginning! 🌙🎉"
        )
    )

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns a time-aware "Quote of the Day" for home screen, share card, favorites.
     * Uses the larger local pool (40+ quotes) for better variety.
     */
    fun getQuoteOfDay(): AppConfig.Quote {
        val tod = AppConfig.TimeOfDay.current()
        val dayOfYear = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
        val (pool, labels) = when (tod) {
            AppConfig.TimeOfDay.MORNING   -> poolByCategory[QuoteCategory.GOOD_MORNING]!! to listOf(
                "Morning Wishes", "Morning Blessing", "Daily Grace", "Heartfelt",
                "Morning Energy", "Morning Joy", "Warm Wishes", "Grateful Heart"
            )
            AppConfig.TimeOfDay.AFTERNOON -> poolByCategory[QuoteCategory.GOOD_AFTERNOON]!! to listOf(
                "Afternoon Wishes", "Afternoon Blessing", "Midday Grace", "Productive Vibes",
                "Afternoon Energy", "Sunny Wishes", "Warm Afternoon", "Peaceful Midday"
            )
            AppConfig.TimeOfDay.EVENING   -> poolByCategory[QuoteCategory.GOOD_EVENING]!! to listOf(
                "Evening Wishes", "Twilight Blessing", "Sunset Grace", "Evening Vibes",
                "Golden Hour", "Evening Peace", "Warm Evening", "Serene Twilight"
            )
            AppConfig.TimeOfDay.NIGHT     -> poolByCategory[QuoteCategory.GOOD_NIGHT]!! to listOf(
                "Night Wishes", "Sweet Dreams", "Peaceful Night", "Night Vibes",
                "Spiritual Bedtime", "Night Blessing", "Calm Night", "Divine Night"
            )
        }
        val idx = dayOfYear % pool.size
        val label = labels[idx % labels.size]
        return AppConfig.Quote(pool[idx], label)
    }

    /**
     * Returns a **stable, deterministic** quote for a given wallpaper.
     *
     * The same [wallpaperId] + [wallpaperCategory] + [selectedCategoryKeys] triple
     * always returns the exact same quote — guaranteed.  This means the quote shown
     * on the grid card and the quote shown inside the detail screen are always identical.
     *
     * How it works:
     *  1. Build the active category pool from the user's selection.
     *  2. Use [wallpaperCategory] to pick the best-matching pool (context-aware).
     *  3. Use `abs(wallpaperId.hashCode())` as a deterministic index into that pool.
     */
    fun getStableQuote(
        wallpaperId: String,
        wallpaperCategory: String,
        selectedCategoryKeys: Set<String> = QuoteCategory.defaults
    ): String {
        val activeCats = buildActiveCategoryList(selectedCategoryKeys)
        val primaryCat = mapWallpaperCategoryToQuoteCategory(wallpaperCategory)
        val idHash     = kotlin.math.abs(wallpaperId.hashCode())
        // Category-context always wins: use the wallpaper's mapped category pool even if
        // the user hasn't enabled that quote category. User preference only applies when
        // no category-specific mapping exists (primaryCat == null).
        val chosen = primaryCat ?: activeCats[idHash % activeCats.size]
        val pool   = poolByCategory[chosen] ?: poolByCategory[QuoteCategory.GOOD_MORNING]!!
        return pool[idHash % pool.size]
    }

    /**
     * Returns the next non-repeating card quote for the shuffle deck
     * (used for things like daily/notification contexts where variety over
     * time matters more than per-item stability).
     */
    fun getNextQuote(
        selectedCategoryKeys: Set<String>,
        wallpaperCategory: String
    ): String {
        val activeCats = buildActiveCategoryList(selectedCategoryKeys)
        val primaryCat = mapWallpaperCategoryToQuoteCategory(wallpaperCategory)
        val chosen     = primaryCat ?: activeCats.randomOrNull() ?: QuoteCategory.GOOD_MORNING
        val pool       = poolByCategory[chosen] ?: poolByCategory[QuoteCategory.GOOD_MORNING]!!
        return nextFromDeck(chosen.key, pool)
    }

    /**
     * Legacy helper — uses default enabled categories with stable hash.
     */
    fun getCardQuote(wallpaperId: String, category: String): String =
        getStableQuote(wallpaperId, category)

    /**
     * Returns a day-of-week specific quote (e.g. Monday motivation, Friday blessing).
     * Falls back to [getStableQuote] when the current day has no special pool.
     */
    fun getDayOfWeekQuote(wallpaperId: String): String {
        val dow  = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
        val pool = dayOfWeekQuotes[dow]
        return if (pool != null) {
            nextFromDeck("dow_$dow", pool)
        } else {
            getStableQuote(wallpaperId, "")
        }
    }

    /**
     * Returns a special occasion quote for the given [occasionKey] (e.g. "valentine").
     * Falls back to a morning/night quote when the key is unknown.
     */
    fun getOccasionQuote(occasionKey: String): String {
        val pool = occasionQuotes[occasionKey]
        return if (!pool.isNullOrEmpty()) {
            nextFromDeck("occasion_$occasionKey", pool)
        } else {
            nextFromDeck("morning_default", poolByCategory[QuoteCategory.GOOD_MORNING]!!)
        }
    }

    /**
     * Returns a short quote for push notification body text.
     * Supports all 4 time periods: Morning, Afternoon, Evening, Night.
     */
    fun getNotificationQuote(timeOfDay: AppConfig.TimeOfDay): String = when (timeOfDay) {
        AppConfig.TimeOfDay.MORNING   -> nextFromDeck("notif_morning",   poolByCategory[QuoteCategory.GOOD_MORNING]!!)
        AppConfig.TimeOfDay.AFTERNOON -> nextFromDeck("notif_afternoon", poolByCategory[QuoteCategory.GOOD_AFTERNOON]!!)
        AppConfig.TimeOfDay.EVENING   -> nextFromDeck("notif_evening",   poolByCategory[QuoteCategory.GOOD_EVENING]!!)
        AppConfig.TimeOfDay.NIGHT     -> nextFromDeck("notif_night",     poolByCategory[QuoteCategory.GOOD_NIGHT]!!)
    }

    /** Backward compatibility: isMorning = true → Morning, false → Night */
    fun getNotificationQuote(isMorning: Boolean): String =
        getNotificationQuote(if (isMorning) AppConfig.TimeOfDay.MORNING else AppConfig.TimeOfDay.NIGHT)

    /**
     * Returns a full-length AI-fallback wish when Gemini is unavailable.
     * Supports Morning, Afternoon, Evening, Night niches.
     */
    fun getFallbackWish(niche: String, mood: String): String {
        val map = when {
            niche.contains("morning",   ignoreCase = true) -> morningAiWishes
            niche.contains("afternoon", ignoreCase = true) -> afternoonAiWishes
            niche.contains("evening",   ignoreCase = true) -> eveningAiWishes
            else -> nightAiWishes
        }
        val list = map[mood] ?: map["Inspirational"] ?: listOf("Good morning! Wishing you a truly beautiful day! ✨")
        return list.random()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildActiveCategoryList(keys: Set<String>): List<QuoteCategory> {
        val list = keys.mapNotNull { QuoteCategory.fromKey(it) }
        return list.ifEmpty { QuoteCategory.defaults.mapNotNull { QuoteCategory.fromKey(it) } }
    }

    private fun mapWallpaperCategoryToQuoteCategory(wallpaperCategory: String): QuoteCategory? = when {
        wallpaperCategory.contains("Love",     ignoreCase = true) ||
        wallpaperCategory.contains("Romantic", ignoreCase = true) -> QuoteCategory.LOVE
        wallpaperCategory.contains("Family",   ignoreCase = true) -> QuoteCategory.FOR_MOM
        wallpaperCategory.contains("Friend",   ignoreCase = true) -> QuoteCategory.FOR_FRIENDS
        wallpaperCategory.contains("Night",    ignoreCase = true) ||
        wallpaperCategory.contains("Dream",    ignoreCase = true) ||
        wallpaperCategory.contains("Nightly",  ignoreCase = true) -> QuoteCategory.GOOD_NIGHT
        wallpaperCategory.contains("Morning",  ignoreCase = true) -> QuoteCategory.GOOD_MORNING
        wallpaperCategory.contains("Afternoon", ignoreCase = true) -> QuoteCategory.GOOD_AFTERNOON
        wallpaperCategory.contains("Evening",  ignoreCase = true) -> QuoteCategory.GOOD_EVENING
        wallpaperCategory.contains("Blessing", ignoreCase = true) ||
        wallpaperCategory.contains("Spiritual", ignoreCase = true) -> QuoteCategory.MOTIVATION
        else -> null
    }
}
