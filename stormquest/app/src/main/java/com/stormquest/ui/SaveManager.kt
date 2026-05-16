package com.stormquest.ui

import android.content.Context
import com.stormquest.data.CharacterClasses
import com.stormquest.data.Races
import com.stormquest.model.GameCharacter
import com.stormquest.model.GameState
import org.json.JSONArray
import org.json.JSONObject

object SaveManager {

    private const val PREFS_NAME = "stormquest_save"
    private const val KEY_DATA   = "save_v1"

    fun hasSave(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).contains(KEY_DATA)

    fun deleteSave(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_DATA).apply()

    fun save(context: Context) {
        if (GameState.party.isEmpty()) return
        try {
            val root = JSONObject()
            root.put("gold",  GameState.gold)
            root.put("floor", GameState.currentFloor)

            val inv = JSONObject()
            for ((id, qty) in GameState.inventory) inv.put(id, qty)
            root.put("inventory", inv)

            val log = JSONArray()
            for (entry in GameState.exploreLog) log.put(entry)
            root.put("log", log)

            val partyArr = JSONArray()
            for (c in GameState.party) {
                val o = JSONObject()
                o.put("name",      c.name)
                o.put("raceId",    c.race.id)
                o.put("classId",   c.characterClass.id)
                o.put("level",     c.level)
                o.put("exp",       c.exp)
                o.put("maxHp",     c.maxHp)
                o.put("currentHp", c.currentHp)
                o.put("maxMp",     c.maxMp)
                o.put("currentMp", c.currentMp)
                o.put("str",       c.str)
                o.put("def",       c.def)
                o.put("mag",       c.mag)
                o.put("spd",       c.spd)
                o.put("lck",       c.lck)
                partyArr.put(o)
            }
            root.put("party", partyArr)

            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_DATA, root.toString()).apply()
        } catch (_: Exception) {}
    }

    fun load(context: Context): Boolean {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_DATA, null) ?: return false
        return try {
            val root = JSONObject(raw)

            GameState.gold         = root.getInt("gold")
            GameState.currentFloor = root.getInt("floor")

            GameState.inventory.clear()
            val inv = root.getJSONObject("inventory")
            for (key in inv.keys()) GameState.inventory[key] = inv.getInt(key)

            GameState.exploreLog.clear()
            val log = root.getJSONArray("log")
            for (i in 0 until log.length()) GameState.exploreLog.add(log.getString(i))

            GameState.party.clear()
            val partyArr = root.getJSONArray("party")
            for (i in 0 until partyArr.length()) {
                val o = partyArr.getJSONObject(i)
                val race = Races.ALL.firstOrNull { it.id == o.getString("raceId") } ?: continue
                val cls  = CharacterClasses.ALL.firstOrNull { it.id == o.getString("classId") } ?: continue
                GameState.party.add(
                    GameCharacter(
                        name            = o.getString("name"),
                        race            = race,
                        characterClass  = cls,
                        level           = o.getInt("level"),
                        exp             = o.getInt("exp"),
                        maxHp           = o.getInt("maxHp"),
                        currentHp       = o.getInt("currentHp"),
                        maxMp           = o.getInt("maxMp"),
                        currentMp       = o.getInt("currentMp"),
                        str             = o.getInt("str"),
                        def             = o.getInt("def"),
                        mag             = o.getInt("mag"),
                        spd             = o.getInt("spd"),
                        lck             = o.getInt("lck")
                    )
                )
            }

            GameState.isInitialized = GameState.party.isNotEmpty()
            GameState.isInitialized
        } catch (_: Exception) {
            false
        }
    }
}
