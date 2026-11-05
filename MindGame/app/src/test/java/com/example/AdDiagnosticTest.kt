package com.example

import com.example.ad.AdDiagnosticManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AdDiagnosticTest {

    @Before
    fun setUp() {
        AdDiagnosticManager.clearLogs()
    }

    @Test
    fun `test error code descriptions`() {
        assertTrue(AdDiagnosticManager.parseErrorCodeDescription(0).contains("ERROR_CODE_INTERNAL_ERROR"))
        assertTrue(AdDiagnosticManager.parseErrorCodeDescription(1).contains("ERROR_CODE_INVALID_REQUEST"))
        assertTrue(AdDiagnosticManager.parseErrorCodeDescription(2).contains("ERROR_CODE_NETWORK_ERROR"))
        
        // 測試純 Code 3 (無特別訊息)
        val defaultCode3 = AdDiagnosticManager.parseErrorCodeDescription(3)
        assertTrue(defaultCode3.contains("ERROR_CODE_NO_FILL"))
        assertTrue(defaultCode3.contains("無廣告庫存"))

        // 測試 Code 3 伴隨 Account not approved yet
        val accountPending = AdDiagnosticManager.parseErrorCodeDescription(3, "Account not approved yet. <https://support.google.com/admob/answer/9905175#1>")
        assertTrue(accountPending.contains("審查中未放行"))
        assertTrue(accountPending.contains("不計入後台報表"))

        // 測試 Code 1 格式不符
        val formatMismatch = AdDiagnosticManager.parseErrorCodeDescription(1, "The ad unit doesn't match format")
        assertTrue(formatMismatch.contains("格式不符"))

        // 測試 parseShortStatus
        assertEquals("帳戶審查中 (Code 3) (22:11:34)", AdDiagnosticManager.parseShortStatus(3, "Account not approved yet", "22:11:34"))
        assertEquals("無庫存 (Code 3) (22:11:34)", AdDiagnosticManager.parseShortStatus(3, "No fill", "22:11:34"))
        assertEquals("格式不符 (Code 1) (22:11:34)", AdDiagnosticManager.parseShortStatus(1, "The ad unit doesn't match format", "22:11:34"))
    }

    @Test
    fun `test banner and interstitial request counters`() {
        val initialSnapshot = AdDiagnosticManager.snapshot.value
        assertEquals(0, initialSnapshot.bannerRequests)
        assertEquals(0, initialSnapshot.bannerSuccesses)
        assertEquals(0, initialSnapshot.interstitialRequests)

        // 發送 3 次橫幅請求，2 次成功
        AdDiagnosticManager.recordBannerRequest()
        AdDiagnosticManager.recordBannerRequest()
        AdDiagnosticManager.recordBannerLoaded()
        AdDiagnosticManager.recordBannerRequest()
        AdDiagnosticManager.recordBannerLoaded()

        val afterBanner = AdDiagnosticManager.snapshot.value
        assertEquals(3, afterBanner.bannerRequests)
        assertEquals(2, afterBanner.bannerSuccesses)
        assertEquals(0, afterBanner.bannerFailures)

        // 發送插頁請求
        AdDiagnosticManager.recordInterstitialRequest()
        AdDiagnosticManager.recordInterstitialLoaded()
        val afterInterstitial = AdDiagnosticManager.snapshot.value
        assertEquals(1, afterInterstitial.interstitialRequests)
        assertEquals(1, afterInterstitial.interstitialSuccesses)
        assertTrue(afterInterstitial.isInterstitialReady)

        // 發送插頁式獎勵請求
        AdDiagnosticManager.recordRewardedRequest()
        AdDiagnosticManager.recordRewardedLoaded()
        val afterRewarded = AdDiagnosticManager.snapshot.value
        assertEquals(1, afterRewarded.rewardedRequests)
        assertEquals(1, afterRewarded.rewardedSuccesses)
        assertTrue(afterRewarded.isRewardedReady)
    }

    @Test
    fun `test exportDiagnosticReport content`() {
        AdDiagnosticManager.recordInitResult(true, "Adapter status: READY")
        AdDiagnosticManager.recordBannerRequest()
        AdDiagnosticManager.recordBannerLoaded()
        AdDiagnosticManager.recordRewardedRequest()
        AdDiagnosticManager.recordRewardedLoaded()

        val report = AdDiagnosticManager.exportDiagnosticReport()
        assertTrue(report.contains("MindGame AdMob 廣告連線診斷報告"))
        assertTrue(report.contains("SDK 初始化"))
        assertTrue(report.contains("頂部橫幅單元 ID"))
        assertTrue(report.contains("通關插頁單元 ID"))
        assertTrue(report.contains("提示獎勵單元 ID"))
        assertTrue(report.contains("橫幅廣告統計: 請求 1 次 / 成功 1 次"))
        assertTrue(report.contains("獎勵廣告統計: 請求 1 次 / 成功 1 次"))
    }

    @Test
    fun `test log ring buffer capped at 50`() {
        for (i in 1..70) {
            AdDiagnosticManager.recordBannerRequest()
        }
        val logs = AdDiagnosticManager.getLogs()
        assertEquals(50, logs.size)
    }
}
