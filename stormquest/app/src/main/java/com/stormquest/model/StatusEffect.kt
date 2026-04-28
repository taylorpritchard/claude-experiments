package com.stormquest.model

import java.io.Serializable

enum class StatusEffectType {
    PROTECT  // DEF buff
}

data class StatusEffect(
    val type: StatusEffectType,
    var turnsRemaining: Int
) : Serializable
