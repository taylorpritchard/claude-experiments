package com.stormquest.data

import java.io.Serializable

data class CharacterClass(
    val id: String,
    val displayName: String,
    val description: String,
    val baseHp: Int,
    val baseMp: Int,
    val baseStr: Int,
    val baseDef: Int,
    val baseMag: Int,
    val baseSpd: Int,
    val baseLck: Int,
    // Growth values per level (base; random bonus added separately)
    val hpGrowth: Int,
    val mpGrowth: Int,
    val strGrowth: Int,
    val defGrowth: Int,
    val magGrowth: Int,
    val spdGrowth: Int,
    val lckGrowth: Int,
    val spellsByLevel: Map<Int, List<Spell>>  // level -> spells unlocked at that level
) : Serializable

object CharacterClasses {
    val WARRIOR = CharacterClass(
        id = "warrior",
        displayName = "Warrior",
        description = "Powerful front-line fighter",
        baseHp = 45, baseMp = 0,
        baseStr = 9, baseDef = 8, baseMag = 1, baseSpd = 5, baseLck = 3,
        hpGrowth = 12, mpGrowth = 0,
        strGrowth = 4, defGrowth = 3, magGrowth = 0, spdGrowth = 1, lckGrowth = 1,
        spellsByLevel = emptyMap()
    )

    val MAGE = CharacterClass(
        id = "mage",
        displayName = "Mage",
        description = "Master of arcane spells",
        baseHp = 20, baseMp = 45,
        baseStr = 2, baseDef = 2, baseMag = 10, baseSpd = 7, baseLck = 4,
        hpGrowth = 4, mpGrowth = 8,
        strGrowth = 1, defGrowth = 1, magGrowth = 4, spdGrowth = 2, lckGrowth = 1,
        spellsByLevel = mapOf(
            1 to listOf(Spells.FIRE),
            3 to listOf(Spells.ICE, Spells.THUNDER),
            6 to listOf(Spells.FIRA, Spells.BLIZZARA),
            8 to listOf(Spells.THUNDARA, Spells.DRAIN),
            10 to listOf(Spells.FIRAGA)
        )
    )

    val CLERIC = CharacterClass(
        id = "cleric",
        displayName = "Cleric",
        description = "Holy healer and support",
        baseHp = 30, baseMp = 35,
        baseStr = 4, baseDef = 5, baseMag = 7, baseSpd = 4, baseLck = 5,
        hpGrowth = 6, mpGrowth = 6,
        strGrowth = 1, defGrowth = 2, magGrowth = 3, spdGrowth = 1, lckGrowth = 2,
        spellsByLevel = mapOf(
            1 to listOf(Spells.HEAL),
            3 to listOf(Spells.PROTECT, Spells.HOLY),
            6 to listOf(Spells.HEALARA),
            10 to listOf(Spells.CURALL)
        )
    )

    val ROGUE = CharacterClass(
        id = "rogue",
        displayName = "Rogue",
        description = "Swift and cunning striker",
        baseHp = 28, baseMp = 12,
        baseStr = 7, baseDef = 4, baseMag = 2, baseSpd = 11, baseLck = 8,
        hpGrowth = 5, mpGrowth = 1,
        strGrowth = 3, defGrowth = 1, magGrowth = 0, spdGrowth = 3, lckGrowth = 3,
        spellsByLevel = emptyMap()
    )

    val PALADIN = CharacterClass(
        id = "paladin",
        displayName = "Paladin",
        description = "Holy warrior and defender",
        baseHp = 38, baseMp = 22,
        baseStr = 7, baseDef = 9, baseMag = 5, baseSpd = 3, baseLck = 4,
        hpGrowth = 8, mpGrowth = 4,
        strGrowth = 2, defGrowth = 3, magGrowth = 2, spdGrowth = 1, lckGrowth = 1,
        spellsByLevel = mapOf(
            1 to listOf(Spells.HEAL),
            3 to listOf(Spells.SMITE),
            6 to listOf(Spells.HEALARA),
            10 to listOf(Spells.HOLY)
        )
    )

    val RANGER = CharacterClass(
        id = "ranger",
        displayName = "Ranger",
        description = "Versatile archer and tracker",
        baseHp = 33, baseMp = 18,
        baseStr = 7, baseDef = 5, baseMag = 3, baseSpd = 9, baseLck = 6,
        hpGrowth = 6, mpGrowth = 3,
        strGrowth = 2, defGrowth = 2, magGrowth = 1, spdGrowth = 2, lckGrowth = 2,
        spellsByLevel = mapOf(
            1 to listOf(Spells.FIRE),
            3 to listOf(Spells.ICE),
            6 to listOf(Spells.THUNDER)
        )
    )

    val ALL = listOf(WARRIOR, MAGE, CLERIC, ROGUE, PALADIN, RANGER)

    fun getById(id: String): CharacterClass = ALL.first { it.id == id }
}
