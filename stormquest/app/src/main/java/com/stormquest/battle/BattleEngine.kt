package com.stormquest.battle

import com.stormquest.data.ItemEffect
import com.stormquest.data.Items
import com.stormquest.data.SpellEffect
import com.stormquest.data.SpellTarget
import com.stormquest.data.Spells
import com.stormquest.model.BattleEnemy
import com.stormquest.model.GameCharacter
import com.stormquest.model.StatusEffect
import com.stormquest.model.StatusEffectType
import kotlin.math.max
import kotlin.random.Random

data class BattleAction(
    val type: ActionType,
    val actor: GameCharacter,
    val targetCharacter: GameCharacter? = null,
    val targetEnemy: BattleEnemy? = null,
    val spellId: String? = null,
    val itemId: String? = null
)

enum class ActionType {
    ATTACK, MAGIC, ITEM, DEFEND, FLEE
}

data class ActionResult(
    val messages: List<String>,
    val fled: Boolean = false,
    val fleeSuccess: Boolean = false
)

data class RoundResult(
    val messages: List<String>,
    val fled: Boolean = false,
    val fleeSuccess: Boolean = false,
    val victory: Boolean = false,
    val defeat: Boolean = false
)

class BattleEngine(
    private val party: List<GameCharacter>,
    private val enemies: MutableList<BattleEnemy>
) {
    private val rng = Random.Default

    fun executeRound(playerActions: Map<GameCharacter, BattleAction>): RoundResult {
        val messages = mutableListOf<String>()

        // Check flee first - if any player chose flee, attempt it
        val fleeAction = playerActions.values.firstOrNull { it.type == ActionType.FLEE }
        if (fleeAction != null) {
            val avgPartySpd = party.filter { it.isAlive }.map { it.spd }.average()
            val avgEnemySpd = enemies.filter { it.isAlive }.map { it.data.spd }.average()
            val fleeChance = ((avgPartySpd / (avgPartySpd + avgEnemySpd)) * 80 + 10).toInt()
                .coerceIn(10, 90)
            val roll = rng.nextInt(100)
            return if (roll < fleeChance) {
                RoundResult(listOf("The party successfully fled!"), fled = true, fleeSuccess = true)
            } else {
                RoundResult(listOf("Couldn't escape!"), fled = true, fleeSuccess = false)
            }
        }

        // Build turn order: all alive party members + all alive enemies, sorted by SPD desc
        data class TurnEntry(val isParty: Boolean, val charIdx: Int, val spd: Int)

        val turnOrder = mutableListOf<TurnEntry>()
        party.forEachIndexed { i, c ->
            if (c.isAlive) turnOrder.add(TurnEntry(true, i, c.spd))
        }
        enemies.forEachIndexed { i, e ->
            if (e.isAlive) turnOrder.add(TurnEntry(false, i, e.data.spd))
        }
        turnOrder.sortByDescending { it.spd }

        for (entry in turnOrder) {
            if (entry.isParty) {
                val char = party[entry.charIdx]
                if (!char.isAlive) continue
                val action = playerActions[char] ?: continue
                val result = executePlayerAction(action)
                messages.addAll(result.messages)
            } else {
                val enemy = enemies[entry.charIdx]
                if (!enemy.isAlive) continue
                val aliveParty = party.filter { it.isAlive }
                if (aliveParty.isEmpty()) break
                val enemyMessages = executeEnemyAction(enemy, aliveParty)
                messages.addAll(enemyMessages)
            }

            // Check for early victory/defeat after each action
            if (enemies.none { it.isAlive }) {
                messages.add("All enemies defeated!")
                return RoundResult(messages, victory = true)
            }
            if (party.none { it.isAlive }) {
                messages.add("The party has been defeated...")
                return RoundResult(messages, defeat = true)
            }
        }

        // Tick status effects for party
        party.filter { it.isAlive }.forEach { it.tickStatusEffects() }
        // Clear defending flags
        party.forEach { it.isDefending = false }

        val victory = enemies.none { it.isAlive }
        val defeat = party.none { it.isAlive }

        if (victory) messages.add("All enemies defeated!")
        if (defeat) messages.add("The party has been defeated...")

        return RoundResult(messages, victory = victory, defeat = defeat)
    }

    private fun executePlayerAction(action: BattleAction): ActionResult {
        val messages = mutableListOf<String>()
        val actor = action.actor

        when (action.type) {
            ActionType.ATTACK -> {
                val target = action.targetEnemy ?: return ActionResult(listOf("No target!"))
                if (!target.isAlive) {
                    // Retarget to another alive enemy
                    val alive = enemies.firstOrNull { it.isAlive }
                    if (alive == null) return ActionResult(emptyList())
                    val dmg = calcPhysicalDamage(actor.str, alive.data.def, actor.lck)
                    alive.currentHp = max(0, alive.currentHp - dmg.damage)
                    messages.add("${actor.name} attacks ${alive.displayName} for ${dmg.damage} damage!${if (dmg.isCrit) " CRITICAL!" else ""}")
                    if (!alive.isAlive) messages.add("${alive.displayName} is defeated!")
                } else {
                    val dmg = calcPhysicalDamage(actor.str, target.data.def, actor.lck)
                    target.currentHp = max(0, target.currentHp - dmg.damage)
                    messages.add("${actor.name} attacks ${target.displayName} for ${dmg.damage} damage!${if (dmg.isCrit) " CRITICAL!" else ""}")
                    if (!target.isAlive) messages.add("${target.displayName} is defeated!")
                }
            }

            ActionType.MAGIC -> {
                val spellId = action.spellId ?: return ActionResult(listOf("No spell!"))
                val spell = actor.getAvailableSpells().firstOrNull { it.id == spellId }
                    ?: return ActionResult(listOf("${actor.name} can't cast that spell!"))
                if (actor.currentMp < spell.mpCost) {
                    messages.add("${actor.name} doesn't have enough MP!")
                    return ActionResult(messages)
                }
                actor.currentMp -= spell.mpCost

                when (spell.effect) {
                    SpellEffect.DAMAGE -> {
                        when (spell.target) {
                            SpellTarget.SINGLE_ENEMY -> {
                                val target = action.targetEnemy
                                    ?: enemies.firstOrNull { it.isAlive }
                                    ?: return ActionResult(listOf("No target!"))
                                val eff = target.takeIf { it.isAlive } ?: enemies.firstOrNull { it.isAlive }
                                ?: return ActionResult(listOf("No alive target!"))
                                val dmg = calcMagicDamage(actor.mag, spell.power)
                                eff.currentHp = max(0, eff.currentHp - dmg)
                                messages.add("${actor.name} casts ${spell.displayName} on ${eff.displayName} for $dmg damage!")
                                if (!eff.isAlive) messages.add("${eff.displayName} is defeated!")
                            }
                            SpellTarget.ALL_ENEMIES -> {
                                val aliveEnemies = enemies.filter { it.isAlive }
                                if (aliveEnemies.isEmpty()) {
                                    messages.add("No targets!")
                                } else {
                                    messages.add("${actor.name} casts ${spell.displayName}!")
                                    for (enemy in aliveEnemies) {
                                        val dmg = calcMagicDamage(actor.mag, spell.power)
                                        enemy.currentHp = max(0, enemy.currentHp - dmg)
                                        messages.add("  ${enemy.displayName} takes $dmg damage!")
                                        if (!enemy.isAlive) messages.add("  ${enemy.displayName} is defeated!")
                                    }
                                }
                            }
                            SpellTarget.SINGLE_ALLY -> {
                                // Shouldn't happen for damage spells, treat as single enemy
                                val target = enemies.firstOrNull { it.isAlive }
                                    ?: return ActionResult(listOf("No target!"))
                                val dmg = calcMagicDamage(actor.mag, spell.power)
                                target.currentHp = max(0, target.currentHp - dmg)
                                messages.add("${actor.name} casts ${spell.displayName} on ${target.displayName} for $dmg damage!")
                                if (!target.isAlive) messages.add("${target.displayName} is defeated!")
                            }
                            SpellTarget.ALL_ALLIES -> {
                                // Heal spell misfire - treat same as all enemies
                                messages.add("${actor.name} casts ${spell.displayName}!")
                            }
                        }
                    }
                    SpellEffect.HEAL -> {
                        when (spell.target) {
                            SpellTarget.SINGLE_ALLY -> {
                                val target = action.targetCharacter
                                    ?: actor
                                val heal = calcHeal(actor.mag, spell.power)
                                val before = target.currentHp
                                target.currentHp = minOf(target.maxHp, target.currentHp + heal)
                                val actual = target.currentHp - before
                                messages.add("${actor.name} casts ${spell.displayName} on ${target.name}, restoring $actual HP!")
                            }
                            SpellTarget.ALL_ALLIES -> {
                                messages.add("${actor.name} casts ${spell.displayName}!")
                                for (ally in party.filter { it.isAlive }) {
                                    val heal = calcHeal(actor.mag, spell.power)
                                    val before = ally.currentHp
                                    ally.currentHp = minOf(ally.maxHp, ally.currentHp + heal)
                                    val actual = ally.currentHp - before
                                    messages.add("  ${ally.name} recovers $actual HP!")
                                }
                            }
                            else -> messages.add("${actor.name} casts ${spell.displayName}!")
                        }
                    }
                    SpellEffect.BUFF_DEF -> {
                        val target = action.targetCharacter ?: actor
                        target.statusEffects.add(StatusEffect(StatusEffectType.PROTECT, 3))
                        messages.add("${actor.name} casts ${spell.displayName} on ${target.name}! DEF raised for 3 turns!")
                    }
                }
            }

            ActionType.ITEM -> {
                val itemId = action.itemId ?: return ActionResult(listOf("No item!"))
                val item = Items.ALL.firstOrNull { it.id == itemId }
                    ?: return ActionResult(listOf("Unknown item!"))

                when (item.effect) {
                    ItemEffect.HEAL_HP_SINGLE -> {
                        val target = action.targetCharacter ?: actor
                        val before = target.currentHp
                        target.currentHp = minOf(target.maxHp, target.currentHp + item.power)
                        val actual = target.currentHp - before
                        messages.add("${actor.name} uses ${item.displayName} on ${target.name}, restoring $actual HP!")
                    }
                    ItemEffect.HEAL_HP_ALL -> {
                        messages.add("${actor.name} uses ${item.displayName}!")
                        for (ally in party.filter { it.isAlive }) {
                            val before = ally.currentHp
                            ally.currentHp = minOf(ally.maxHp, ally.currentHp + item.power)
                            val actual = ally.currentHp - before
                            messages.add("  ${ally.name} recovers $actual HP!")
                        }
                    }
                    ItemEffect.HEAL_MP_SINGLE -> {
                        val target = action.targetCharacter ?: actor
                        val before = target.currentMp
                        target.currentMp = minOf(target.maxMp, target.currentMp + item.power)
                        val actual = target.currentMp - before
                        messages.add("${actor.name} uses ${item.displayName} on ${target.name}, restoring $actual MP!")
                    }
                    ItemEffect.REVIVE_SINGLE -> {
                        val target = action.targetCharacter
                            ?: party.firstOrNull { !it.isAlive }
                            ?: return ActionResult(listOf("No one to revive!"))
                        if (target.isAlive) {
                            messages.add("${target.name} is already alive!")
                        } else {
                            target.currentHp = max(1, target.maxHp / 4)
                            messages.add("${actor.name} uses ${item.displayName}! ${target.name} is revived with ${target.currentHp} HP!")
                        }
                    }
                    ItemEffect.DAMAGE_SINGLE -> {
                        val target = action.targetEnemy
                            ?: enemies.firstOrNull { it.isAlive }
                            ?: return ActionResult(listOf("No target!"))
                        val eff = if (target.isAlive) target else enemies.firstOrNull { it.isAlive }
                            ?: return ActionResult(listOf("No alive target!"))
                        eff.currentHp = max(0, eff.currentHp - item.power)
                        messages.add("${actor.name} throws ${item.displayName} at ${eff.displayName} for ${item.power} damage!")
                        if (!eff.isAlive) messages.add("${eff.displayName} is defeated!")
                    }
                }
            }

            ActionType.DEFEND -> {
                actor.isDefending = true
                messages.add("${actor.name} takes a defensive stance!")
            }

            ActionType.FLEE -> {
                // Handled above
            }
        }

        return ActionResult(messages)
    }

    private fun executeEnemyAction(enemy: BattleEnemy, aliveParty: List<GameCharacter>): List<String> {
        val messages = mutableListOf<String>()
        val data = enemy.data

        if (data.isMagicUser && enemy.currentHp > 0) {
            // Magic user AI: cast spells
            val target = selectEnemyTarget(aliveParty)
            val spell = if (data.mag >= 15) Spells.DARK_THUNDER else Spells.DARK_FIRE
            val dmg = calcMagicDamage(data.mag, spell.power)
            val effectiveDmg = if (target.isDefending) dmg / 2 else dmg
            target.currentHp = max(0, target.currentHp - effectiveDmg)
            messages.add("${enemy.displayName} casts ${spell.displayName} on ${target.name} for $effectiveDmg damage!${if (target.isDefending) " (Defending)" else ""}")
            if (!target.isAlive) messages.add("${target.name} has fallen!")
        } else {
            // Physical attack
            val target = selectEnemyTarget(aliveParty)
            val dmg = calcPhysicalDamage(data.str, target.def, 0)
            val effectiveDef = if (target.isDefending) target.def + target.def else target.def
            val baseDmg = max(1, data.str * 2 - effectiveDef + rng.nextInt(6))
            val actualDmg = if (target.hasProtect()) max(1, baseDmg - 3) else baseDmg
            target.currentHp = max(0, target.currentHp - actualDmg)
            messages.add("${enemy.displayName} attacks ${target.name} for $actualDmg damage!${if (target.isDefending) " (Defending)" else ""}")
            if (!target.isAlive) messages.add("${target.name} has fallen!")
        }

        return messages
    }

    private fun selectEnemyTarget(aliveParty: List<GameCharacter>): GameCharacter {
        // Target lowest HP ally if any are below 50%, otherwise random
        val lowHp = aliveParty.filter { it.currentHp.toFloat() / it.maxHp < 0.5f }
        return if (lowHp.isNotEmpty()) {
            lowHp.minByOrNull { it.currentHp } ?: aliveParty.random()
        } else {
            aliveParty.random()
        }
    }

    data class DamageResult(val damage: Int, val isCrit: Boolean)

    private fun calcPhysicalDamage(str: Int, def: Int, lck: Int): DamageResult {
        val critChance = lck * 2
        val isCrit = rng.nextInt(100) < critChance
        var dmg = max(1, str * 2 - def + rng.nextInt(6))
        if (isCrit) dmg = (dmg * 1.5).toInt()
        return DamageResult(max(1, dmg), isCrit)
    }

    private fun calcMagicDamage(mag: Int, power: Int): Int {
        return max(1, mag * 2 + power + rng.nextInt(10))
    }

    private fun calcHeal(mag: Int, power: Int): Int {
        return max(10, mag * 2 + power + rng.nextInt(15))
    }

    fun calculateRewards(): Pair<Int, Int> {
        val expGained = enemies.sumOf { it.data.exp }
        val goldGained = enemies.sumOf { it.data.gold }
        return Pair(expGained, goldGained)
    }
}
