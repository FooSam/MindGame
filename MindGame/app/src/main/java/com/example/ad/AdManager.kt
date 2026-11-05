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
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicInteger

/**
 * 通用遊戲局數累計計數器 (每滿特定局數觸發一次事件，如展示通關插頁廣告)
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
 * 全域 Google AdMob 廣告管理器
 * - 頂部橫幅廣告 (Banner): 自動定時刷新
 * - 通關純插頁廣告 (Interstitial): 每累計滿 3 局自動彈出
 * - 提示插頁獎勵廣告 (Rewarded Interstitial): 數獨/貓咪數獨提示、海龜湯次數加值
 */
object AdManager {
    private const val TAG = "AdManager"

    // 1. 通關純插頁廣告單元 ID (每 3 局觸發)
    const val PRODUCTION_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-5014630903713895/3215350450"
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    val adUnitId: String
        get() = if (BuildConfig.DEBUG) TEST_INTERSTITIAL_AD_UNIT_ID else PRODUCTION_INTERSTITIAL_AD_UNIT_ID

    // 2. 提示插頁式獎勵廣告單元 ID (數獨提示、貓咪數獨提示、海龜湯次數補充)
    const val PRODUCTION_REWARDED_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-5014630903713895/7605455982"
    const val TEST_REWARDED_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/5354046379"

    val rewardedAdUnitId: String
        get() = if (BuildConfig.DEBUG) TEST_REWARDED_INTERSTITIAL_AD_UNIT_ID else PRODUCTION_REWARDED_INTERSTITIAL_AD_UNIT_ID

    // 3. 正式橫幅廣告單元 ID
    const val PRODUCTION_BANNER_AD_UNIT_ID = "ca-app-pub-5014630903713895/6390706923"
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"

    val bannerAdUnitId: String
        get() = if (BuildConfig.DEBUG) TEST_BANNER_AD_UNIT_ID else PRODUCTION_BANNER_AD_UNIT_ID

    val counter = AdCounter(threshold = 3)

    // 純插頁廣告狀態
    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false

    // 插頁式獎勵廣告狀態
    private var rewardedInterstitialAd: RewardedInterstitialAd? = null
    private var isRewardedLoading = false

    private var isInitialized = false

    // 橫幅廣告重載觸發事件流水號
    private val _reloadTrigger = MutableStateFlow(0)
    val reloadTrigger: StateFlow<Int> = _reloadTrigger

    /**
     * 初始化 Google Mobile Ads SDK 並預載入通關插頁與獎勵插頁廣告
     */
    fun initialize(context: Context) {
        AdDiagnosticManager.initialize(context)
        if (isInitialized) return
        try {
            MobileAds.initialize(context) { initStatus ->
                Log.d(TAG, "MobileAds initialized: $initStatus")
                isInitialized = true
                AdDiagnosticManager.recordInitResult(true, initStatus.toString())
                loadInterstitialAd(context.applicationContext)
                loadRewardedInterstitialAd(context.applicationContext)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MobileAds", e)
            AdDiagnosticManager.recordInitResult(false, e.message ?: "Unknown Exception")
        }
    }

    /**
     * 非同步預載入純插頁廣告 (通關休息用)
     */
    fun loadInterstitialAd(context: Context, force: Boolean = false) {
        if (!force && (isInterstitialLoading || interstitialAd != null)) return
        isInterstitialLoading = true
        AdDiagnosticManager.recordInterstitialRequest()

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isInterstitialLoading = false
                    Log.d(TAG, "InterstitialAd loaded successfully")
                    AdDiagnosticManager.recordInterstitialLoaded()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    interstitialAd = null
                    isInterstitialLoading = false
                    Log.w(TAG, "InterstitialAd failed to load: ${loadAdError.message} (code: ${loadAdError.code})")
                    AdDiagnosticManager.recordInterstitialFailed(loadAdError)
                }
            }
        )
    }

    /**
     * 非同步預載入插頁式獎勵廣告 (提示/次數加值用)
     */
    fun loadRewardedInterstitialAd(context: Context, force: Boolean = false) {
        if (!force && (isRewardedLoading || rewardedInterstitialAd != null)) return
        isRewardedLoading = true
        AdDiagnosticManager.recordRewardedRequest()

        val adRequest = AdRequest.Builder().build()
        RewardedInterstitialAd.load(
            context,
            rewardedAdUnitId,
            adRequest,
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedInterstitialAd) {
                    rewardedInterstitialAd = ad
                    isRewardedLoading = false
                    Log.d(TAG, "RewardedInterstitialAd loaded successfully")
                    AdDiagnosticManager.recordRewardedLoaded()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    rewardedInterstitialAd = null
                    isRewardedLoading = false
                    Log.w(TAG, "RewardedInterstitialAd failed to load: ${loadAdError.message} (code: ${loadAdError.code})")
                    AdDiagnosticManager.recordRewardedFailed(loadAdError)
                }
            }
        )
    }

    /**
     * 手動強制重新發送廣告請求 (同時刷新橫幅、通關插頁與提示獎勵廣告)
     */
    fun forceReloadAds(context: Context) {
        isInterstitialLoading = false
        interstitialAd = null
        loadInterstitialAd(context.applicationContext, force = true)

        isRewardedLoading = false
        rewardedInterstitialAd = null
        loadRewardedInterstitialAd(context.applicationContext, force = true)

        _reloadTrigger.value = _reloadTrigger.value + 1
    }

    /**
     * 玩家完成任意一局遊戲或中途中斷/重置退出時調用。
     * 每累計 3 局展示純插頁廣告。
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

    fun recordGameInterrupted(activity: Activity?, onAdClosed: () -> Unit = {}) {
        recordGameFinished(activity, onAdClosed)
    }

    /**
     * 展示純插頁廣告 (通關冷卻用)
     */
    fun showAdNow(activity: Activity?, onAdClosed: () -> Unit = {}) {
        if (activity != null && interstitialAd != null) {
            val ad = interstitialAd
            ad?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "InterstitialAd dismissed")
                    interstitialAd = null
                    AdDiagnosticManager.recordInterstitialDismissed()
                    loadInterstitialAd(activity.applicationContext)
                    onAdClosed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.w(TAG, "InterstitialAd failed to show: ${adError.message}")
                    interstitialAd = null
                    AdDiagnosticManager.recordInterstitialShowFailed(adError.message)
                    loadInterstitialAd(activity.applicationContext)
                    onAdClosed()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "InterstitialAd showed fullscreen content")
                    AdDiagnosticManager.recordInterstitialShow()
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

    /**
     * 展示插頁式獎勵廣告 (數獨提示、貓咪數獨提示、海龜湯次數加值專用)
     * @param activity 當前 Activity
     * @param onUserEarnedReward 玩家觀看完畢獲取獎勵之回呼；若廣告未就緒亦優雅降級執行，確保遊戲不卡關
     */
    fun showRewardedAd(activity: Activity?, onUserEarnedReward: () -> Unit = {}) {
        if (activity != null && rewardedInterstitialAd != null) {
            val ad = rewardedInterstitialAd
            var rewardGranted = false

            ad?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "RewardedInterstitialAd dismissed")
                    rewardedInterstitialAd = null
                    AdDiagnosticManager.recordRewardedDismissed()
                    loadRewardedInterstitialAd(activity.applicationContext)
                    if (rewardGranted) {
                        onUserEarnedReward()
                    }
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.w(TAG, "RewardedInterstitialAd failed to show: ${adError.message}")
                    rewardedInterstitialAd = null
                    AdDiagnosticManager.recordRewardedShowFailed(adError.message)
                    loadRewardedInterstitialAd(activity.applicationContext)
                    // 展示失敗時優雅降級給予獎勵
                    onUserEarnedReward()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "RewardedInterstitialAd showed fullscreen content")
                    AdDiagnosticManager.recordRewardedShow()
                }
            }

            ad?.show(activity) { rewardItem: RewardItem ->
                Log.d(TAG, "User earned reward: ${rewardItem.type} (${rewardItem.amount})")
                rewardGranted = true
                AdDiagnosticManager.recordRewardEarned(rewardItem.type, rewardItem.amount)
            }
        } else {
            // 廣告尚未載入或處於審核期未填充，優雅降級直接給予提示獎勵，並非同步觸發背景載入
            if (activity != null && rewardedInterstitialAd == null) {
                loadRewardedInterstitialAd(activity.applicationContext)
            }
            onUserEarnedReward()
        }
    }
}
