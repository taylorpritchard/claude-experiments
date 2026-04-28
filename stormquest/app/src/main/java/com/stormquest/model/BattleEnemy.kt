package com.stormquest.model

import com.stormquest.data.EnemyData
import java.io.Serializable

data class BattleEnemy(
    val data: EnemyData,
    val instanceId: Int,
    var currentHp: Int,
    var isDefending: Boolean = false
) : Serializable {
    val isAlive: Boolean get() = currentHp > 0
    val displayName: String get() = if (instanceId > 0) "${data.displayName} ${instanceId + 1}" else data.displayName

    companion object {
        fun fromData(data: EnemyData, instanceId: Int = 0): BattleEnemy {
            return BattleEnemy(data, instanceId, data.maxHp)
        }
    }
}
