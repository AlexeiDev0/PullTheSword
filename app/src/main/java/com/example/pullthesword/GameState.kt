package com.example.pullthesword

import kotlin.math.pow

data class Relic(val name: String, val world: Int, val kind: Int, val rarity: Int, val fire: Boolean = false)

object Relics {
    val all = listOf(
        Relic("Old Sword",0,0,0), Relic("Iron Sword",0,0,0), Relic("Valley Spear",0,2,1), Relic("Royal Blade",0,0,2),
        Relic("Frozen Dagger",1,1,1), Relic("Ice Axe",1,3,1), Relic("Glacier Spear",1,2,2), Relic("Winter Crown",1,4,3),
        Relic("Flame Sword",2,0,2,true), Relic("Lava Axe",2,3,2,true), Relic("Burning Spear",2,2,2,true), Relic("Fire Hammer",2,5,3,true),
        Relic("Root Blade",3,0,1), Relic("Forest Axe",3,3,2), Relic("Emerald Staff",3,2,2), Relic("Sylvan Crown",3,4,3),
        Relic("Ancient Sword",4,0,2), Relic("King's Hammer",4,5,2), Relic("Golden Crown",4,4,3), Relic("Sun Relic",4,1,3),
        Relic("Star Blade",5,0,3), Relic("Void Spear",5,2,3), Relic("Alien Tool",5,3,4), Relic("Cosmic Relic",5,4,4)
    )
}

class GameState {
    var style = 0
    var comboLevel = 0
    var automationLevel = 0
    var manualHits = 0
    var combo = 0
    var lastManualHit = 0L
    var lastExplosion = false
    var lastCritical = false
    val cycle get() = index / Relics.all.size
    val permanentBonus get() = 1.0 + cycle * .15
    val variant get() = cycle % 4
    fun setComplete(world: Int) = (world * 4 until world * 4 + 4).all { it in claimed }
    val comboCost get() = (45 * 1.45.pow(comboLevel)).toLong()
    val automationCost get() = (90 * 1.55.pow(automationLevel)).toLong()
    fun buyUpgrade(type: Int): Boolean {
        val cost = when(type) { 0 -> powerCost; 1 -> autoCost; 2 -> critCost; 3 -> comboCost; 4 -> automationCost; else -> return false }
        if (coins < cost || (type == 2 && critical >= 10)) return false
        coins -= cost
        when(type) { 0 -> level++; 1 -> auto++; 2 -> critical++; 3 -> comboLevel++; 4 -> automationLevel++ }
        return true
    }
    fun manualHit(now: Long, criticalRoll: Double): Double {
        if (ready) return 0.0
        combo = if (now >= lastManualHit && now - lastManualHit <= 900) (combo + 1).coerceAtMost(10) else 1
        lastManualHit = now
        manualHits++
        lastExplosion = setComplete(2) && manualHits % 10 == 0
        lastCritical = criticalRoll < critical * .03
        val series = if (style == 1) 1.0 + (combo - 1) * (.04 + comboLevel * .01 + if(setComplete(1)) .04 else 0.0) else 1.0
        val force = if (style == 0) 1.25 else 1.0
        return hit(force * series * (if(lastCritical) 3.0 else 1.0) * (if(lastExplosion) 4.0 else 1.0))
    }
    fun autoTick(): Boolean {
        if (auto <= 0) return false
        if(!ready) hit(auto * (1.0 + automationLevel * .15) * if(style == 2) 1.5 else 1.0)
        if (ready && style == 2) { claim(); return true }
        return false
    }
    var coins = 0L
    var level = 0
    var auto = 0
    var critical = 0
    var index = 0
    var damage = 0.0
    var claimed = mutableSetOf<Int>()
    val relic get() = Relics.all[index % Relics.all.size]
    val required get() = 100.0 * 1.27.pow(index % Relics.all.size) * 2.0.pow(cycle)
    val bonus get() = when { claimed.size >= 10 -> 1.05; claimed.size >= 5 -> 1.02; else -> 1.0 }
    val power get() = (5 + level * 3) * bonus * permanentBonus
    val progress get() = (damage / required).coerceIn(0.0, 1.0).toFloat()
    val ready get() = damage >= required
    val powerCost get() = (20 * 1.35.pow(level)).toLong()
    val autoCost get() = (75 * 1.6.pow(auto)).toLong()
    val critCost get() = (100 * 1.7.pow(critical)).toLong()
    fun hit(multiplier: Double = 1.0): Double {
        if (ready) return 0.0
        val dealt = (power * multiplier).coerceAtMost(required - damage)
        damage = (damage + dealt).coerceAtMost(required)
        coins += 2 + level
        return dealt
    }
    fun claim() {
        if (!ready) return
        claimed.add(index % Relics.all.size)
        coins += (required / 3).toLong()
        index++
        damage = 0.0
        combo = 0
        lastManualHit = 0L
    }
}
