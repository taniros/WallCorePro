package com.offline.wallcorepro.promotion

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.offline.wallcorepro.config.AppConfig
import timber.log.Timber
import java.io.File

/**
 * Generates a beautiful branded invite card and launches the system share sheet.
 *
 * WHY A VISUAL CARD BEATS PLAIN TEXT:
 *   • A plain text message says "hey download this app"
 *   • A beautiful image with a wish quote says "look how beautiful this is — I want it"
 *   • Images get 3–5× more clicks in WhatsApp, Telegram, Instagram DMs
 *
 * The generated card:
 *   ┌─────────────────────────────────────────┐
 *   │  ✨  Morning & Night Wishes             │  ← gradient background
 *   │                                         │
 *   │  "Good morning! May your day be as      │  ← sample wish (today's quote)
 *   │   bright as your beautiful smile ☀️"    │
 *   │                                         │
 *   │  ─────────────────────────────────────  │
 *   │  Download FREE on Google Play           │  ← CTA footer
 *   │  play.google.com/store/apps/...         │
 *   └─────────────────────────────────────────┘
 */
object ShareCardGenerator {

    private const val CARD_WIDTH  = 1080
    private const val CARD_HEIGHT = 1080
    private const val CORNER_RADIUS = 48f

    /**
     * Generates the invite card bitmap, saves it to cache, and launches
     * the system share sheet with the image + text invite.
     */
    fun shareInviteCard(context: Context) {
        try {
            val bitmap = generateCard(context)
            val file   = saveBitmapToCache(context, bitmap)
            val uri    = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareText = "${AppConfig.INVITE_MESSAGE}${AppConfig.PLAY_STORE_URL}"

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(intent, "Invite friends to ${AppConfig.APP_NAME_SHORT}")
            )
        } catch (e: Exception) {
            Timber.e(e, "ShareCardGenerator: failed to generate/share invite card")
            // Fallback to plain text share
            val fallback = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(
                    Intent.EXTRA_TEXT,
                    "${AppConfig.INVITE_MESSAGE}${AppConfig.PLAY_STORE_URL}"
                )
            }
            context.startActivity(Intent.createChooser(fallback, "Share ${AppConfig.APP_NAME}"))
        }
    }

    // ─── Bitmap Generation ────────────────────────────────────────────────────

    private fun generateCard(context: Context): Bitmap {
        val bmp    = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        drawBackground(canvas)
        drawTopDecoration(canvas)
        drawAppName(canvas)
        drawWish(canvas)
        drawDivider(canvas)
        drawCta(canvas)
        drawBottomUrl(canvas)

        return bmp
    }

    private fun drawBackground(canvas: Canvas) {
        // Deep warm-night gradient: dark indigo → deep burgundy → dark night
        val gradient = LinearGradient(
            0f, 0f, CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat(),
            intArrayOf(0xFF0D0A1A.toInt(), 0xFF1A0A2E.toInt(), 0xFF2A0A1E.toInt()),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = gradient }
        canvas.drawRect(0f, 0f, CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat(), paint)
    }

    private fun drawTopDecoration(canvas: Canvas) {
        // Soft glowing circle at the top (star/moon decoration)
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.RadialGradient(
                CARD_WIDTH / 2f, 180f, 200f,
                intArrayOf(0x55FF6F00.toInt(), 0x00000000.toInt()),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(CARD_WIDTH / 2f, 180f, 200f, glowPaint)

        // Moon emoji rendered as text
        val moonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize  = 120f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("🌅", CARD_WIDTH / 2f, 200f, moonPaint)
    }

    private fun drawAppName(canvas: Canvas) {
        // App name with golden glow
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color     = 0xFFFFD54F.toInt()
            textSize  = 72f
            textAlign = Paint.Align.CENTER
            typeface  = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            setShadowLayer(24f, 0f, 0f, 0xAAFF9800.toInt())
        }
        canvas.drawText(AppConfig.APP_NAME_SHORT, CARD_WIDTH / 2f, 340f, namePaint)

        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color     = 0xCCFFFFFF.toInt()
            textSize  = 36f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Beautiful Morning & Night Wishes", CARD_WIDTH / 2f, 400f, tagPaint)
    }

    private fun drawWish(canvas: Canvas) {
        // Frosted glass quote card
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x33FFFFFF.toInt()
        }
        val cardRect = RectF(60f, 450f, CARD_WIDTH - 60f, 760f)
        canvas.drawRoundRect(cardRect, 32f, 32f, cardPaint)

        // Quote marks
        val quotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color     = 0x88FF9800.toInt()
            textSize  = 100f
            textAlign = Paint.Align.LEFT
            typeface  = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        canvas.drawText("\u201C", 80f, 530f, quotePaint)

        // Wish text (today's quote)
        val wishPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color     = 0xFFFFFFFF.toInt()
            textSize  = 40f
            textAlign = Paint.Align.LEFT
            typeface  = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        }
        val wishText = AppConfig.getQuoteOfDay().text
        drawWrappedText(canvas, wishText, wishPaint, 100f, 560f, CARD_WIDTH - 120f, 52f)

        // Closing quote
        canvas.drawText("\u201D", CARD_WIDTH - 100f, 750f, quotePaint.apply { textAlign = Paint.Align.RIGHT })
    }

    private fun drawDivider(canvas: Canvas) {
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                120f, 0f, CARD_WIDTH - 120f, 0f,
                intArrayOf(0x00FFFFFF.toInt(), 0x88FF9800.toInt(), 0x00FFFFFF.toInt()),
                null,
                Shader.TileMode.CLAMP
            )
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(120f, 810f, CARD_WIDTH - 120f, 810f, dividerPaint)
    }

    private fun drawCta(canvas: Canvas) {
        // CTA badge
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                200f, 850f, CARD_WIDTH - 200f, 950f,
                intArrayOf(0xFFFF6F00.toInt(), 0xFFFF9800.toInt()),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(RectF(200f, 855f, CARD_WIDTH - 200f, 945f), 45f, 45f, badgePaint)

        val ctaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color     = 0xFFFFFFFF.toInt()
            textSize  = 44f
            textAlign = Paint.Align.CENTER
            typeface  = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("⬇ Download FREE on Google Play", CARD_WIDTH / 2f, 912f, ctaPaint)
    }

    private fun drawBottomUrl(canvas: Canvas) {
        val urlPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color     = 0x88FFFFFF.toInt()
            textSize  = 28f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(AppConfig.PLAY_STORE_URL, CARD_WIDTH / 2f, 990f, urlPaint)
    }

    private fun drawWrappedText(
        canvas: Canvas, text: String, paint: Paint,
        x: Float, startY: Float, maxWidth: Float, lineHeight: Float
    ) {
        val words = text.split(" ")
        var line  = ""
        var y     = startY
        for (word in words) {
            val test = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(test) > maxWidth - x && line.isNotEmpty()) {
                canvas.drawText(line, x, y, paint)
                line = word
                y   += lineHeight
                if (y > 740f) { canvas.drawText("...", x, y, paint); return }
            } else {
                line = test
            }
        }
        if (line.isNotEmpty()) canvas.drawText(line, x, y, paint)
    }

    // ─── File Operations ──────────────────────────────────────────────────────

    private fun saveBitmapToCache(context: Context, bitmap: Bitmap): File {
        val cacheDir = File(context.cacheDir, "share").also { it.mkdirs() }
        val file     = File(cacheDir, "invite_card.jpg")
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        return file
    }
}
