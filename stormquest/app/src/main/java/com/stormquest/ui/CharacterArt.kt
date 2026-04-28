package com.stormquest.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.stormquest.R

object CharacterArt {
    // Spritesheet: 1408 x 768
    // Row 0 (top):    Human · Dwarf · Elf · Halfling · Half-Orc · Gnome
    // Row 1 (bottom): Paladin · Warrior · Mage · Rogue · Ranger · Cleric

    // Measured via pixel analysis — 235px column spacing, first center at x=110
    private val X_CENTERS = intArrayOf(110, 345, 580, 815, 1050, 1285)
    private const val HALF_W = 95   // ±95px from center keeps neighbours out

    private const val RACE_Y1  = 145
    private const val RACE_Y2  = 415

    private const val CLASS_Y1 = 480
    private const val CLASS_Y2 = 708  // stops before the icon name labels

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
        if (sheet == null || sheet!!.isRecycled)
            sheet = BitmapFactory.decodeResource(context.resources, R.drawable.character_sheet)
        return sheet
    }

    fun getRace(context: Context, raceId: String): Bitmap? =
        raceCol[raceId]?.let { crop(context, it, isRace = true) }

    fun getCharClass(context: Context, classId: String): Bitmap? =
        classCol[classId]?.let { crop(context, it, isRace = false) }

    private fun crop(context: Context, col: Int, isRace: Boolean): Bitmap? {
        val src = getSheet(context) ?: return null
        val cx = X_CENTERS[col]
        val x  = (cx - HALF_W).coerceAtLeast(0)
        val y  = if (isRace) RACE_Y1 else CLASS_Y1
        val w  = (HALF_W * 2).coerceAtMost(src.width - x)
        val h  = (if (isRace) RACE_Y2 - RACE_Y1 else CLASS_Y2 - CLASS_Y1)
                     .coerceAtMost(src.height - y)
        if (w <= 0 || h <= 0) return null
        return try {
            Bitmap.createBitmap(src, x, y, w, h)
        } catch (_: Exception) {
            null
        }
    }
}
