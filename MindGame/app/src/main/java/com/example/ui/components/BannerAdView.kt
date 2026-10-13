package com.example.ui.components

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ad.AdManager
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

private const val TAG = "BannerAdView"

/**
 * 頂部 AdMob 橫幅廣告 (Banner Ad) 元件
 * - 自動依據 Debug / Release 模式選擇測試 ID 或正式 ID
 * - 自動監聽 Activity 生命週期 (pause / resume / destroy) 杜絕資源洩漏
 * - 固定標準 320x50 高度 (50.dp)，水平置中對齊，防止版面跳動
 */
@Composable
fun BannerAdView(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val adView = remember {
        AdView(context).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = AdManager.bannerAdUnitId
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.d(TAG, "Banner ad loaded successfully ($adUnitId)")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.w(
                        TAG,
                        "Banner ad failed to load: ${loadAdError.message} (code: ${loadAdError.code})"
                    )
                }

                override fun onAdOpened() {
                    Log.d(TAG, "Banner ad opened")
                }

                override fun onAdClosed() {
                    Log.d(TAG, "Banner ad closed")
                }
            }
            try {
                loadAd(AdRequest.Builder().build())
            } catch (e: Exception) {
                Log.e(TAG, "Error loading banner ad", e)
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    try {
                        adView.pause()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error pausing adView", e)
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    try {
                        adView.resume()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error resuming adView", e)
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    try {
                        adView.destroy()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error destroying adView", e)
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            try {
                adView.destroy()
            } catch (e: Exception) {
                Log.e(TAG, "Error destroying adView on dispose", e)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.wrapContentSize(),
            factory = { adView }
        )
    }
}
