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
    fun testSoundManagerSfxSafety() {
        SoundManager.isSfxEnabled = true
        SoundManager.playClick()
        SoundManager.playSwitchSnap()
        SoundManager.playSuccess()
        SoundManager.playWin()
        SoundManager.playError()
        SoundManager.playCatMeow()

        SoundManager.isSfxEnabled = false
        SoundManager.playClick()
        SoundManager.playSwitchSnap()
        SoundManager.playSuccess()
        SoundManager.playWin()
        SoundManager.playError()
        SoundManager.playCatMeow()
    }
}
