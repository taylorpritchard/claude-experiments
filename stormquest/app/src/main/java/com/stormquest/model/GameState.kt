package com.stormquest.model

import com.stormquest.data.Items

object GameState {
    var party: MutableList<GameCharacter> = mutableListOf()
    var gold: Int = 100
    var currentFloor: Int = 1
    var inventory: MutableMap<String, Int> = mutableMapOf()
    var exploreLog: MutableList<String> = mutableListOf()
    var isInitialized: Boolean = false

    fun initialize() {
        gold = 100
        currentFloor = 1
        exploreLog = mutableListOf("Your adventure begins...")
        // Starting inventory
        inventory = mutableMapOf(
            Items.POTION.id to 5,
            Items.ETHER.id to 3,
            Items.PHOENIX_DOWN.id to 2
        )
        isInitialized = true
    }

    fun addItem(itemId: String, qty: Int = 1) {
        inventory[itemId] = (inventory[itemId] ?: 0) + qty
    }

    fun removeItem(itemId: String, qty: Int = 1): Boolean {
        val current = inventory[itemId] ?: 0
        if (current < qty) return false
        val newQty = current - qty
        if (newQty <= 0) {
            inventory.remove(itemId)
        } else {
            inventory[itemId] = newQty
        }
        return true
    }

    fun getItemCount(itemId: String): Int = inventory[itemId] ?: 0

    fun addLog(message: String) {
        exploreLog.add(message)
        if (exploreLog.size > 50) {
            exploreLog.removeAt(0)
        }
    }

    fun getLastLogs(n: Int): List<String> {
        return if (exploreLog.size <= n) exploreLog.toList()
        else exploreLog.takeLast(n)
    }

    fun isPartyAlive(): Boolean = party.any { it.isAlive }

    fun advanceFloor() {
        currentFloor++
    }

    fun isBossFloor(): Boolean = currentFloor % 5 == 0
}
