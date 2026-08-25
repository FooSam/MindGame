package com.example

import com.example.ad.AdCounter
import com.example.ad.AdManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdManagerLogicTest {

    @Test
    fun `test ad counter triggers exactly at threshold 3`() {
        val counter = AdCounter(threshold = 3)
        assertEquals(0, counter.currentCount)
        assertEquals(3, counter.gamesUntilNextAd)

        // Game 1
        val trigger1 = counter.incrementAndCheck()
        assertFalse(trigger1)
        assertEquals(1, counter.currentCount)
        assertEquals(2, counter.gamesUntilNextAd)

        // Game 2
        val trigger2 = counter.incrementAndCheck()
        assertFalse(trigger2)
        assertEquals(2, counter.currentCount)
        assertEquals(1, counter.gamesUntilNextAd)

        // Game 3 (Threshold reached)
        val trigger3 = counter.incrementAndCheck()
        assertTrue(trigger3)
        assertEquals(0, counter.currentCount) // Reset to 0
        assertEquals(3, counter.gamesUntilNextAd)

        // Game 4 (Cycle 2 Game 1)
        val trigger4 = counter.incrementAndCheck()
        assertFalse(trigger4)
        assertEquals(1, counter.currentCount)

        // Game 5 (Cycle 2 Game 2)
        val trigger5 = counter.incrementAndCheck()
        assertFalse(trigger5)
        assertEquals(2, counter.currentCount)

        // Game 6 (Cycle 2 Game 3)
        val trigger6 = counter.incrementAndCheck()
        assertTrue(trigger6)
        assertEquals(0, counter.currentCount)
    }

    @Test
    fun `test ad manager unit id configuration`() {
        assertEquals("ca-app-pub-5014630903713895/7605455982", AdManager.PRODUCTION_INTERSTITIAL_AD_UNIT_ID)
        assertEquals("ca-app-pub-3940256099942544/1033173712", AdManager.TEST_INTERSTITIAL_AD_UNIT_ID)
    }

    @Test
    fun `test counter reset function`() {
        val counter = AdCounter(threshold = 3)
        counter.incrementAndCheck()
        counter.incrementAndCheck()
        assertEquals(2, counter.currentCount)

        counter.reset()
        assertEquals(0, counter.currentCount)
        assertEquals(3, counter.gamesUntilNextAd)
    }
}
