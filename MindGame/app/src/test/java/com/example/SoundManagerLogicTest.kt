package com.example

import com.example.audio.SoundManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundManagerLogicTest {

    @Test
    fun testBgmTracksCount() {
        assertTrue("Should have at least 10 BGM tracks for variety", SoundManager.bgmTracks.size >= 10)
        assertEquals(11, SoundManager.bgmTracks.size)
    }

    @Test
    fun testSoundManagerBgmStateToggle() {
        SoundManager.setBgmState(false)
        assertFalse(SoundManager.isBgmEnabled)

        SoundManager.setBgmState(true)
        assertTrue(SoundManager.isBgmEnabled)

        SoundManager.setBgmState(false)
        assertFalse(SoundManager.isBgmEnabled)
    }

    @Test
    fun testSoundManagerSafetyWithoutContext() {
        // Ensuring no uncaught exceptions are thrown when called without Android Context
        SoundManager.isBgmEnabled = true
        SoundManager.onGameSwitched()
        SoundManager.playRandomBgm()
        SoundManager.pauseBgm()
        SoundManager.resumeBgm()
        SoundManager.onAppFocusChanged(false)
        SoundManager.onAppFocusChanged(true)
        SoundManager.stopBgm()
    }

    @Test
    fun testSoundManagerSfxSafetyAndCoverage() {
        // 啟用 SFX 測試全套 24 款音效呼叫安全性（無 Context / 未初始化時皆安全無崩潰）
        SoundManager.isSfxEnabled = true
        SoundManager.playClick()
        SoundManager.playSwitchSnap()
        SoundManager.playTapConfirm()
        SoundManager.playTapBack()
        SoundManager.playModalOpen()
        SoundManager.playModalClose()
        SoundManager.playToggle()
        SoundManager.playSuccess()
        SoundManager.playWin()
        SoundManager.playError()

        // 動作組
        SoundManager.playStep()
        SoundManager.playFruitSliceSwish()
        SoundManager.playImpactHit(combo = 1)
        SoundManager.playImpactHit(combo = 5)
        SoundManager.playCriticalHit()
        SoundManager.playSkillCast()
        SoundManager.playMetalClang()
        SoundManager.playDamageMiss()

        // 環境組
        SoundManager.playClayDrop()
        SoundManager.playWoodblockChop()
        SoundManager.playMirrorRotateSnap()
        SoundManager.playRotate()
        SoundManager.playCrystalActivate()
        SoundManager.playLaserConnect()
        SoundManager.playLaserMazeClear()

        // 特效組與五聲音階
        SoundManager.playCoinOrSausage()
        for (combo in 1..8) {
            SoundManager.playComboChime(combo)
        }
        SoundManager.playFruitJuiceSplash()
        SoundManager.playBombExplosion()
        SoundManager.playCrossBoom()
        SoundManager.playFreezeEffect()
        SoundManager.playRainbowExplosion()
        SoundManager.playFeverFanfare()
        SoundManager.playBlenderWhir()
        SoundManager.playPinballBounce()
        SoundManager.playFlipperSnap()
        SoundManager.playSpringLaunch()
        SoundManager.playPinDrop()
        SoundManager.playCatMeow()
        SoundManager.playCentipedeEscape()
        SoundManager.playHeartbeatPulse(1.0f)

        // 關閉 SFX 測試
        SoundManager.isSfxEnabled = false
        SoundManager.playClick()
        SoundManager.playSwitchSnap()
        SoundManager.playSuccess()
        SoundManager.playWin()
        SoundManager.playError()
        SoundManager.playComboChime(3)
        SoundManager.playBombExplosion()

        // 恢復預設
        SoundManager.isSfxEnabled = true
    }
}
