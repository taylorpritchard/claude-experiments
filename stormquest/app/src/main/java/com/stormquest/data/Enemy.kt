package com.stormquest.data

import java.io.Serializable

data class EnemyData(
    val id: String,
    val displayName: String,
    val maxHp: Int,
    val str: Int,
    val def: Int,
    val mag: Int,
    val spd: Int,
    val exp: Int,
    val gold: Int,
    val isMagicUser: Boolean = false,
    val isBoss: Boolean = false,
    val minFloor: Int = 1,
    val maxFloor: Int = 99
) : Serializable

object Enemies {
    val SLIME = EnemyData("slime", "Slime", 30, 5, 2, 0, 3, 15, 5, minFloor = 1, maxFloor = 3)
    val GOBLIN = EnemyData("goblin", "Goblin", 45, 8, 4, 0, 6, 25, 10, minFloor = 1, maxFloor = 4)
    val WOLF = EnemyData("wolf", "Wolf", 55, 10, 3, 0, 9, 30, 8, minFloor = 2, maxFloor = 5)
    val SKELETON = EnemyData("skeleton", "Skeleton", 60, 9, 6, 0, 5, 35, 12, minFloor = 2, maxFloor = 6)
    val ZOMBIE = EnemyData("zombie", "Zombie", 80, 7, 5, 0, 2, 30, 10, minFloor = 3, maxFloor = 7)
    val BANDIT = EnemyData("bandit", "Bandit", 70, 12, 6, 0, 7, 40, 20, minFloor = 3, maxFloor = 8)
    val ORC = EnemyData("orc", "Orc", 100, 14, 8, 0, 4, 50, 18, minFloor = 5, maxFloor = 10)
    val DARK_MAGE = EnemyData("dark_mage", "Dark Mage", 50, 4, 3, 12, 6, 60, 25, isMagicUser = true, minFloor = 4, maxFloor = 10)
    val TROLL = EnemyData("troll", "Troll", 150, 16, 10, 0, 3, 100, 40, minFloor = 6, maxFloor = 12)
    val WYVERN = EnemyData("wyvern", "Wyvern", 200, 18, 12, 0, 7, 120, 60, minFloor = 8, maxFloor = 15)
    val SHADOW_DRAGON = EnemyData("shadow_dragon", "Shadow Dragon", 400, 22, 15, 18, 10, 300, 150, isMagicUser = true, isBoss = true)

    val REGULAR_ENEMIES = listOf(SLIME, GOBLIN, WOLF, SKELETON, ZOMBIE, BANDIT, ORC, DARK_MAGE, TROLL, WYVERN)

    fun getEnemiesForFloor(floor: Int): List<EnemyData> {
        return REGULAR_ENEMIES.filter { floor >= it.minFloor && floor <= it.maxFloor }
            .ifEmpty { listOf(SLIME, GOBLIN) }
    }
}
