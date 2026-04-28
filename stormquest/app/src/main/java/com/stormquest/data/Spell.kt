package com.stormquest.data

import java.io.Serializable

enum class SpellTarget {
    SINGLE_ENEMY, ALL_ENEMIES, SINGLE_ALLY, ALL_ALLIES
}

enum class SpellEffect {
    DAMAGE, HEAL, BUFF_DEF
}

data class Spell(
    val id: String,
    val displayName: String,
    val mpCost: Int,
    val power: Int,
    val target: SpellTarget,
    val effect: SpellEffect,
    val description: String
) : Serializable

object Spells {
    // Mage spells
    val FIRE = Spell("fire", "Fire", 5, 20, SpellTarget.SINGLE_ENEMY, SpellEffect.DAMAGE, "Deals fire damage to one enemy")
    val FIRA = Spell("fira", "Fira", 12, 40, SpellTarget.SINGLE_ENEMY, SpellEffect.DAMAGE, "Deals heavy fire damage to one enemy")
    val FIRAGA = Spell("firaga", "Firaga", 25, 35, SpellTarget.ALL_ENEMIES, SpellEffect.DAMAGE, "Deals fire damage to all enemies")
    val ICE = Spell("ice", "Ice", 5, 20, SpellTarget.SINGLE_ENEMY, SpellEffect.DAMAGE, "Deals ice damage to one enemy")
    val BLIZZARA = Spell("blizzara", "Blizzara", 12, 40, SpellTarget.SINGLE_ENEMY, SpellEffect.DAMAGE, "Deals heavy ice damage to one enemy")
    val THUNDER = Spell("thunder", "Thunder", 5, 20, SpellTarget.SINGLE_ENEMY, SpellEffect.DAMAGE, "Deals lightning damage to one enemy")
    val THUNDARA = Spell("thundara", "Thundara", 12, 40, SpellTarget.SINGLE_ENEMY, SpellEffect.DAMAGE, "Deals heavy lightning damage to one enemy")
    val DRAIN = Spell("drain", "Drain", 15, 25, SpellTarget.SINGLE_ENEMY, SpellEffect.DAMAGE, "Drains HP from one enemy")

    // Cleric spells
    val HEAL = Spell("heal", "Heal", 8, 30, SpellTarget.SINGLE_ALLY, SpellEffect.HEAL, "Restores 30+ HP to one ally")
    val HEALARA = Spell("healara", "Healara", 18, 60, SpellTarget.SINGLE_ALLY, SpellEffect.HEAL, "Restores 60+ HP to one ally")
    val CURALL = Spell("curall", "Curall", 30, 40, SpellTarget.ALL_ALLIES, SpellEffect.HEAL, "Restores HP to all allies")
    val HOLY = Spell("holy", "Holy", 25, 55, SpellTarget.SINGLE_ENEMY, SpellEffect.DAMAGE, "Deals holy damage to one enemy")
    val PROTECT = Spell("protect", "Protect", 10, 0, SpellTarget.SINGLE_ALLY, SpellEffect.BUFF_DEF, "Raises defense of one ally")

    // Paladin spells
    val SMITE = Spell("smite", "Smite", 15, 35, SpellTarget.SINGLE_ENEMY, SpellEffect.DAMAGE, "Smites one enemy with holy power")

    // Mage dark spell (enemy use)
    val DARK_FIRE = Spell("dark_fire", "Dark Fire", 5, 20, SpellTarget.SINGLE_ENEMY, SpellEffect.DAMAGE, "Dark fire")
    val DARK_THUNDER = Spell("dark_thunder", "Dark Thunder", 12, 35, SpellTarget.SINGLE_ENEMY, SpellEffect.DAMAGE, "Dark thunder")
}
