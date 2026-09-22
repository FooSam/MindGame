package com.example.ad

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.util.concurrent.atomic.AtomicInteger

/**
 * 通用遊戲局數累計計數器 (每滿特定局數觸發一次事件，如展示廣告)
 */
class AdCounter(val threshold: Int = 3) {
    private val count = AtomicInteger(0)

    val currentCount: Int
        get() = count.get()

    val gamesUntilNextAd: Int
        get() {
            val rem = threshold - (count.get() % threshold)
            return if (rem == 0) threshold else rem
        }

    /**
     * 記錄完成一局。
     * @return 若達到門檻值（如 3 局）返回 true 並自動重置計數，否則返回 false。
     */
    fun incrementAndCheck(): Boolean {
        val current = count.incrementAndGet()
        return if (current >= threshold) {
            count.set(0)
            true
        } else {
            false
        }
    }

    fun reset() {
        count.set(0)
    }
}

/**
 * 全域 Google AdMob 插頁式廣告 (Interstitial Ad) 管理器
 */
object AdManager {
    private const val TAG = "AdManager"

    // 正式廣告單元 ID (由使用者提供)
    const val PRODUCTION_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-5014630903713895/7605455982"
    // Google 官方標準測試廣告單元 ID (用於 Debug 模式防誤點封號)
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    val adUnitId: String
        get() = if (BuildConfig.DEBUG) TEST_INTERSTITIAL_AD_UNIT_ID else PRODUCTION_INTERSTITIAL_AD_UNIT_ID

    val counter = AdCounter(threshold = 3)

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false
    private var isInitialized = false

    /**
     * 初始化 Google Mobile Ads SDK 並預載入第一則插頁廣告
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            MobileAds.initialize(context) { initStatus ->
                Log.d(TAG, "MobileAds initialized: $initStatus")
                isInitialized = true
                loadInterstitialAd(context.applicationContext)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MobileAds", e)
        }
    }

    /**
     * 非同步預載入插頁廣告
     */
    fun loadInterstitialAd(context: Context) {
        if (isLoading || interstitialAd != null) return
        isLoading = true

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoading = false
                    Log.d(TAG, "InterstitialAd loaded successfully")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    interstitialAd = null
                    isLoading = false
                    Log.w(TAG, "InterstitialAd failed to load: ${loadAdError.message} (code: ${loadAdError.code})")
                }
            }
        )
    }

    /**
     * 玩家完成任意一局遊戲或中途中斷/重置退出時調用。
     * 每累計 3 局且廣告已就緒時展示廣告，並在關閉後執行回呼；若無廣告或未達 3 局則直接執行回呼。
     */
    fun recordGameFinished(activity: Activity?, onAdClosed: () -> Unit = {}) {
        val shouldShowAd = counter.incrementAndCheck()
        Log.d(TAG, "Game completed/interrupted. Total count in cycle: ${counter.currentCount}, Trigger ad: $shouldShowAd")

        if (shouldShowAd) {
            showAdNow(activity, onAdClosed)
        } else {
            if (interstitialAd == null && activity != null) {
                loadInterstitialAd(activity.applicationContext)
            }
            onAdClosed()
        }
    }

    /**
     * 玩家在遊戲進行中中途中斷（如返回上一頁、主動重置、放棄）時調用。
     */
    fun recordGameInterrupted(activity: Activity?, onAdClosed: () -> Unit = {}) {
        recordGameFinished(activity, onAdClosed)
    }

    /**
     * 直接展示廣告（例如獎勵回補或看廣告提問），關閉後執行回調
     */
    fun showAdNow(activity: Activity?, onAdClosed: () -> Unit = {}) {
        if (activity != null && interstitialAd != null) {
            val ad = interstitialAd
            ad?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "InterstitialAd dismissed")
                    interstitialAd = null
                    loadInterstitialAd(activity.applicationContext)
                    onAdClosed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.w(TAG, "InterstitialAd failed to show: ${adError.message}")
                    interstitialAd = null
                    loadInterstitialAd(activity.applicationContext)
                    onAdClosed()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "InterstitialAd showed fullscreen content")
                }
            }
            ad?.show(activity)
        } else {
            if (interstitialAd == null && activity != null) {
                loadInterstitialAd(activity.applicationContext)
            }
            onAdClosed()
        }
    }
}
