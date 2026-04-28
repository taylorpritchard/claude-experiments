package com.stormquest.data

import java.io.Serializable

data class Race(
    val id: String,
    val displayName: String,
    val description: String,
    val hpBonus: Int = 0,
    val mpBonus: Int = 0,
    val strBonus: Int = 0,
    val defBonus: Int = 0,
    val magBonus: Int = 0,
    val spdBonus: Int = 0,
    val lckBonus: Int = 0
) : Serializable

object Races {
    val HUMAN = Race(
        id = "human",
        displayName = "Human",
        description = "Versatile and adaptable",
        hpBonus = 5, mpBonus = 5,
        strBonus = 1, defBonus = 1, magBonus = 1, spdBonus = 1,
        lckBonus = 2
    )
    val ELF = Race(
        id = "elf",
        displayName = "Elf",
        description = "Graceful and magically gifted",
        mpBonus = 15, magBonus = 3, spdBonus = 2, lckBonus = 1
    )
    val DWARF = Race(
        id = "dwarf",
        displayName = "Dwarf",
        description = "Stout and resilient",
        hpBonus = 20, strBonus = 2, defBonus = 3, spdBonus = -1
    )
    val HALFLING = Race(
        id = "halfling",
        displayName = "Halfling",
        description = "Quick and fortunate",
        mpBonus = 5, strBonus = -1, spdBonus = 3, lckBonus = 4
    )
    val HALF_ORC = Race(
        id = "half_orc",
        displayName = "Half-Orc",
        description = "Fierce and powerful",
        hpBonus = 25, strBonus = 4, defBonus = 2, magBonus = -2, spdBonus = -1
    )
    val GNOME = Race(
        id = "gnome",
        displayName = "Gnome",
        description = "Clever and magically attuned",
        hpBonus = -5, mpBonus = 20, strBonus = -1, magBonus = 4, spdBonus = 2, lckBonus = 2
    )

    val ALL = listOf(HUMAN, ELF, DWARF, HALFLING, HALF_ORC, GNOME)
}
