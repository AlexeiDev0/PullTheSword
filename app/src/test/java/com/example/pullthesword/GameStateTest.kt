package com.example.pullthesword

import org.junit.Assert.*
import org.junit.Test

class GameStateTest {
    @Test fun fireBonusRequiresSetAndEveryTenthManualStrike() {
        val game=GameState().apply {index=12;claimed.addAll(8..11);style=1}
        repeat(9) {game.manualHit(it*1000L,1.0);assertFalse(game.lastExplosion)}
        val before=game.damage
        game.manualHit(10000L,1.0)
        assertTrue(game.lastExplosion)
        assertEquals(game.power*4,game.damage-before,.0001)
        val missing=GameState().apply {index=12;claimed.addAll(8..10);manualHits=9}
        missing.manualHit(10000L,1.0)
        assertFalse(missing.lastExplosion)
    }
    @Test fun comboExpiresAndIceSetIncreasesDamage() {
        val game=GameState().apply {index=8;style=1;claimed.addAll(4..7)}
        game.manualHit(1000L,1.0)
        assertEquals(game.power*1.08,game.manualHit(1500L,1.0),.0001)
        game.manualHit(2500L,1.0)
        assertEquals(1,game.combo)
        game.style=0
        assertEquals(game.power*1.25,game.manualHit(2600L,1.0),.0001)
    }
    @Test fun automationClaimsOnlyInSmithStyle() {
        val game=GameState().apply {auto=1;damage=99.0;style=2}
        assertTrue(game.autoTick())
        assertEquals(1,game.index)
        assertEquals(setOf(0),game.claimed)
        assertEquals(0.0,game.damage,.0001)
        val manual=GameState().apply {auto=1;damage=99.0;style=0}
        assertFalse(manual.autoTick())
        assertTrue(manual.ready)
        assertEquals(0,manual.index)
        manual.style=2
        assertTrue(manual.autoTick())
        assertEquals(1,manual.index)
    }
    @Test fun cycleUnlocksPermanentPowerAndResetsLocalDifficulty() {
        val game=GameState().apply {index=23;damage=required}
        game.claim()
        assertEquals(1,game.cycle)
        assertEquals(200.0,game.required,.0001)
        assertEquals(5.75,game.power,.0001)
        assertEquals(Relics.all[0],game.relic)
    }
    @Test fun purchaseRejectsInsufficientCoinsAndCriticalCap() {
        val game=GameState()
        assertFalse(game.buyUpgrade(3))
        game.coins=10000
        val cost=game.comboCost
        assertTrue(game.buyUpgrade(3))
        assertEquals(10000-cost,game.coins)
        assertEquals(1,game.comboLevel)
        game.critical=10
        assertFalse(game.buyUpgrade(2))
    }
    @Test fun extractionRequiresProgressAndClaimIsIdempotent() {
        val game = GameState()
        game.claim()
        assertEquals(0,game.index)
        repeat(20) { game.hit() }
        assertTrue(game.ready)
        assertEquals(1f,game.progress)
        val coins = game.coins
        game.hit()
        assertEquals(coins,game.coins)
        game.claim()
        assertEquals(setOf(0),game.claimed)
        assertEquals(1,game.index)
        assertEquals(0f,game.progress)
        game.claim()
        assertEquals(1,game.index)
    }
    @Test fun collectionBonusAndDamageCap() {
        val game = GameState()
        game.claimed.addAll(0..4)
        assertEquals(5.1,game.power,0.0001)
        game.hit(100000.0)
        assertEquals(game.required,game.damage,0.0001)
        game.claimed.addAll(5..9)
        assertEquals(5.25,game.power,0.0001)
    }
}
