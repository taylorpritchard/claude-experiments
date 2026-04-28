package com.stormquest.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.stormquest.R

object CharacterArt {
    // Spritesheet: 1408 x 768
    // Row 0 (top):    Human · Dwarf · Elf · Halfling · Half-Orc · Gnome
    // Row 1 (bottom): Paladin · Warrior · Mage · Rogue · Ranger · Cleric

    private const val SHEET_W = 1408
    private const val SHEET_H = 768
    private const val COLS = 6
    private const val CELL_W = SHEET_W / COLS   // 234 px
    private const val HEADER_H = 115            // parchment title banner
    private const val ROW_H = (SHEET_H - HEADER_H) / 2  // 326 px
    private const val LABEL_H = 58             // name label at bottom of each portrait
    private const val PAD_X = 6
    private const val PAD_Y = 6

    private val raceCol = mapOf(
        "human" to 0, "dwarf" to 1, "elf" to 2,
        "halfling" to 3, "half_orc" to 4, "gnome" to 5
    )
    private val classCol = mapOf(
        "paladin" to 0, "warrior" to 1, "mage" to 2,
        "rogue" to 3, "ranger" to 4, "cleric" to 5
    )

    private var sheet: Bitmap? = null

    private fun getSheet(context: Context): Bitmap? {
        if (sheet == null || sheet!!.isRecycled) {
            sheet = BitmapFactory.decodeResource(context.resources, R.drawable.character_sheet)
        }
        return sheet
    }

    fun getRace(context: Context, raceId: String): Bitmap? =
        raceCol[raceId]?.let { col -> crop(context, col, 0) }

    fun getCharClass(context: Context, classId: String): Bitmap? =
        classCol[classId]?.let { col -> crop(context, col, 1) }

    private fun crop(context: Context, col: Int, row: Int): Bitmap? {
        val src = getSheet(context) ?: return null
        val x = col * CELL_W + PAD_X
        val y = HEADER_H + row * ROW_H + PAD_Y
        val w = (CELL_W - PAD_X * 2).coerceAtMost(src.width - x)
        val h = (ROW_H - LABEL_H - PAD_Y).coerceAtMost(src.height - y)
        if (w <= 0 || h <= 0) return null
        return try {
            Bitmap.createBitmap(src, x, y, w, h)
        } catch (_: Exception) {
            null
        }
    }
}
