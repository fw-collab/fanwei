package com.example.wordmemory.ui.wrongbook

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import com.example.wordmemory.data.local.model.WrongWordItem
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * 错题卡片位图生成器。
 *
 * 纯 Canvas 绘制，不依赖布局 XML，方便直接生成分享图片。
 * 视觉使用主 App 的蓝橙配色。
 */
object WrongCardGenerator {

    private const val WIDTH = 1080
    private const val HEIGHT = 1350

    private val COLOR_BG = 0xFFF5F7FC.toInt()
    private val COLOR_SURFACE = 0xFFFFFFFF.toInt()
    private val COLOR_BLUE = 0xFF1F5FBF.toInt()
    private val COLOR_BLUE_SOFT = 0xFFE8F0FF.toInt()
    private val COLOR_ORANGE = 0xFFF28A2E.toInt()
    private val COLOR_TEXT = 0xFF172033.toInt()
    private val COLOR_SECONDARY = 0xFF5F6B7A.toInt()
    private val COLOR_DIVIDER = 0xFFE3E8F0.toInt()

    fun generate(item: WrongWordItem): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(COLOR_BG)

        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_SURFACE
            setShadowLayer(20f, 0f, 8f, 0x22000000)
        }
        val cardRect = RectF(60f, 60f, 1020f, 1290f)
        canvas.drawRoundRect(cardRect, 44f, 44f, cardPaint)

        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_ORANGE
        }
        canvas.drawRoundRect(RectF(120f, 138f, 208f, 148f), 5f, 5f, accentPaint)

        val eyebrowPaint = textPaint(COLOR_ORANGE, 26f, true)
        canvas.drawText("MISTAKE CARD", 120f, 196f, eyebrowPaint)

        val titlePaint = textPaint(COLOR_TEXT, 54f, true)
        canvas.drawText("错题复盘", 120f, 276f, titlePaint)

        val wordPaint = textPaint(COLOR_BLUE, 92f, true)
        fitTextSize(wordPaint, item.spelling, 840f, 92f, 54f)
        canvas.drawText(item.spelling, 120f, 410f, wordPaint)

        val phoneticPaint = textPaint(COLOR_SECONDARY, 36f, false)
        fitTextSize(phoneticPaint, item.phonetic.ifBlank { "—" }, 760f, 36f, 24f)
        canvas.drawText(
            item.phonetic.ifBlank { "—" },
            120f,
            468f,
            phoneticPaint,
        )

        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DIVIDER
            strokeWidth = 2f
        }
        canvas.drawLine(120f, 526f, 960f, 526f, dividerPaint)

        val labelPaint = textPaint(COLOR_ORANGE, 28f, true)
        canvas.drawText("释义", 120f, 586f, labelPaint)

        val meaningPaint = textPaint(COLOR_TEXT, 46f, false)
        val meaningLayout = StaticLayout.Builder.obtain(
            item.meaning.ifBlank { "暂无释义" },
            0,
            item.meaning.ifBlank { "暂无释义" }.length,
            meaningPaint,
            840,
        )
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(10f, 1f)
            .setMaxLines(4)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()
        canvas.save()
        canvas.translate(120f, 620f)
        meaningLayout.draw(canvas)
        canvas.restore()

        val statsRect = RectF(120f, 1010f, 960f, 1150f)
        val statsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BLUE_SOFT
        }
        canvas.drawRoundRect(statsRect, 28f, 28f, statsPaint)

        val statsLabelPaint = textPaint(COLOR_SECONDARY, 28f, false)
        canvas.drawText("累计答错", 160f, 1060f, statsLabelPaint)

        val countPaint = textPaint(COLOR_ORANGE, 54f, true)
        canvas.drawText("${item.wrongCount} 次", 160f, 1122f, countPaint)

        val fromPaint = textPaint(COLOR_SECONDARY, 26f, false)
        canvas.drawText("来自错题本", 710f, 1096f, fromPaint)

        val footerPaint = textPaint(COLOR_SECONDARY, 25f, false)
        canvas.drawText("单词记忆 · 把错题变成下一次的记忆", 120f, 1238f, footerPaint)

        return bitmap
    }

    /** 保存到 cache/wrong_cards，并通过 FileProvider 返回可分享 URI。 */
    fun saveToCache(context: Context, bitmap: Bitmap, wordId: Long): Uri {
        val dir = File(context.cacheDir, "wrong_cards")
        if (!dir.exists() && !dir.mkdirs()) {
            throw IOException("无法创建错题卡片目录")
        }

        val file = File(dir, "wrong_card_${wordId}_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { output ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                throw IOException("错题卡片编码失败")
            }
        }

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
    }

    private fun textPaint(color: Int, size: Float, bold: Boolean): TextPaint {
        return TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
    }

    private fun fitTextSize(paint: Paint, text: String, maxWidth: Float, start: Float, min: Float) {
        var size = start
        paint.textSize = size
        while (size > min && paint.measureText(text) > maxWidth) {
            size -= 2f
            paint.textSize = size
        }
    }
}
