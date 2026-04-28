package com.stormquest.data

import java.io.Serializable

enum class ItemEffect {
    HEAL_HP_SINGLE,
    HEAL_HP_ALL,
    HEAL_MP_SINGLE,
    REVIVE_SINGLE,
    DAMAGE_SINGLE
}

data class Item(
    val id: String,
    val displayName: String,
    val description: String,
    val effect: ItemEffect,
    val power: Int,
    val buyPrice: Int,
    val sellable: Boolean = true
) : Serializable

object Items {
    val POTION = Item(
        id = "potion",
        displayName = "Potion",
        description = "Restores 50 HP",
        effect = ItemEffect.HEAL_HP_SINGLE,
        power = 50,
        buyPrice = 30
    )
    val HI_POTION = Item(
        id = "hi_potion",
        displayName = "Hi-Potion",
        description = "Restores 150 HP",
        effect = ItemEffect.HEAL_HP_SINGLE,
        power = 150,
        buyPrice = 100
    )
    val ETHER = Item(
        id = "ether",
        displayName = "Ether",
        description = "Restores 30 MP",
        effect = ItemEffect.HEAL_MP_SINGLE,
        power = 30,
        buyPrice = 50
    )
    val ELIXIR = Item(
        id = "elixir",
        displayName = "Elixir",
        description = "Restores 100 HP to all allies",
        effect = ItemEffect.HEAL_HP_ALL,
        power = 100,
        buyPrice = 200
    )
    val PHOENIX_DOWN = Item(
        id = "phoenix_down",
        displayName = "Phoenix Down",
        description = "Revives an ally to 25% HP",
        effect = ItemEffect.REVIVE_SINGLE,
        power = 0,
        buyPrice = 150
    )
    val GRENADE = Item(
        id = "grenade",
        displayName = "Grenade",
        description = "Deals 80 damage to one enemy",
        effect = ItemEffect.DAMAGE_SINGLE,
        power = 80,
        buyPrice = 80
    )

    val ALL = listOf(POTION, HI_POTION, ETHER, ELIXIR, PHOENIX_DOWN, GRENADE)
    val SHOP_ITEMS = listOf(POTION, HI_POTION, ETHER, PHOENIX_DOWN)

    fun getById(id: String): Item = ALL.first { it.id == id }
}
