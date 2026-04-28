package com.stormquest.model

import com.stormquest.data.CharacterClass
import com.stormquest.data.CharacterClasses
import com.stormquest.data.Race
import com.stormquest.data.Spell
import java.io.Serializable

data class GameCharacter(
    val name: String,
    val race: Race,
    val characterClass: CharacterClass,
    var level: Int = 1,
    var exp: Int = 0,
    var maxHp: Int,
    var currentHp: Int,
    var maxMp: Int,
    var currentMp: Int,
    var str: Int,
    var def: Int,
    var mag: Int,
    var spd: Int,
    var lck: Int,
    var isDefending: Boolean = false,
    val statusEffects: MutableList<StatusEffect> = mutableListOf()
) : Serializable {

    val isAlive: Boolean get() = currentHp > 0

    val expToNextLevel: Int get() = level * 100

    fun getAvailableSpells(): List<Spell> {
        val spells = mutableListOf<Spell>()
        for ((reqLevel, spellList) in characterClass.spellsByLevel) {
            if (level >= reqLevel) {
                spells.addAll(spellList)
            }
        }
        return spells.distinctBy { it.id }
    }

    fun hasProtect(): Boolean = statusEffects.any { it.type == StatusEffectType.PROTECT && it.turnsRemaining > 0 }

    fun addExp(amount: Int): List<String> {
        val messages = mutableListOf<String>()
        exp += amount
        while (exp >= expToNextLevel) {
            exp -= expToNextLevel
            levelUp(messages)
        }
        return messages
    }

    private fun levelUp(messages: MutableList<String>) {
        level++
        val cls = characterClass
        val hpGain = cls.hpGrowth + (0..3).random()
        val mpGain = cls.mpGrowth + (0..2).random()
        val strGain = cls.strGrowth + (0..1).random()
        val defGain = cls.defGrowth + (0..1).random()
        val magGain = cls.magGrowth + (0..1).random()
        val spdGain = cls.spdGrowth + (0..1).random()
        val lckGain = cls.lckGrowth + (0..1).random()

        maxHp += hpGain
        maxMp += mpGain
        str += strGain
        def += defGain
        mag += magGain
        spd += spdGain
        lck += lckGain

        // Restore gained portion
        currentHp = minOf(currentHp + hpGain, maxHp)
        currentMp = minOf(currentMp + mpGain, maxMp)

        messages.add("$name reached Level $level! HP+$hpGain MP+$mpGain STR+$strGain DEF+$defGain")
    }

    fun tickStatusEffects() {
        val iter = statusEffects.iterator()
        while (iter.hasNext()) {
            val effect = iter.next()
            effect.turnsRemaining--
            if (effect.turnsRemaining <= 0) {
                iter.remove()
            }
        }
    }

    companion object {
        fun create(name: String, race: Race, cls: CharacterClass): GameCharacter {
            val maxHp = cls.baseHp + race.hpBonus
            val maxMp = cls.baseMp + race.mpBonus
            return GameCharacter(
                name = name,
                race = race,
                characterClass = cls,
                maxHp = maxHp,
                currentHp = maxHp,
                maxMp = maxMp,
                currentMp = maxMp,
                str = cls.baseStr + race.strBonus,
                def = cls.baseDef + race.defBonus,
                mag = cls.baseMag + race.magBonus,
                spd = cls.baseSpd + race.spdBonus,
                lck = cls.baseLck + race.lckBonus
            )
        }
    }
}
