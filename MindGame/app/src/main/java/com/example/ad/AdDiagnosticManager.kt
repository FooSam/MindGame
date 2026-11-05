package com.example.ad

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.google.android.gms.ads.LoadAdError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale

/**
 * 廣告事件日誌資料模型
 */
data class AdLogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val tag: String,
    val event: String,
    val message: String,
    val errorCode: Int? = null,
    val responseInfo: String? = null
) {
    fun toFormattedString(): String {
        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
        val errStr = if (errorCode != null) " [Code $errorCode]" else ""
        return "[$timeStr][$tag][$event]$errStr $message"
    }
}

/**
 * 廣告整體狀態快照
 */
data class AdDiagnosticSnapshot(
    val initStatus: String = "未初始化",
    val isDebugMode: Boolean = BuildConfig.DEBUG,
    val bannerAdUnitId: String = "",
    val interstitialAdUnitId: String = "",
    val rewardedAdUnitId: String = "",
    val bannerRequests: Int = 0,
    val bannerSuccesses: Int = 0,
    val bannerFailures: Int = 0,
    val lastBannerStatus: String = "尚未請求",
    val interstitialRequests: Int = 0,
    val interstitialSuccesses: Int = 0,
    val interstitialFailures: Int = 0,
    val isInterstitialReady: Boolean = false,
    val lastInterstitialStatus: String = "尚未請求",
    val rewardedRequests: Int = 0,
    val rewardedSuccesses: Int = 0,
    val rewardedFailures: Int = 0,
    val isRewardedReady: Boolean = false,
    val lastRewardedStatus: String = "尚未請求",
    val lastRequestTime: String = "無"
)

/**
 * 全域 Google AdMob 廣告連線診斷記錄器
 */
object AdDiagnosticManager {
    private const val TAG = "AdDiagnostics"
    private const val MAX_LOGS = 50
    private const val LOG_FILE_NAME = "admob_diagnostic.log"

    private val logQueue = ArrayDeque<AdLogEntry>()
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private val _snapshot = MutableStateFlow(AdDiagnosticSnapshot())
    val snapshot: StateFlow<AdDiagnosticSnapshot> = _snapshot.asStateFlow()

    private var appContext: Context? = null

    /**
     * 初始化診斷記錄器
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext
        updateSnapshot {
            copy(
                bannerAdUnitId = AdManager.bannerAdUnitId,
                interstitialAdUnitId = AdManager.adUnitId,
                rewardedAdUnitId = AdManager.rewardedAdUnitId
            )
        }
        addLog("INIT", "START", "診斷記錄器啟動 (模式: ${if (BuildConfig.DEBUG) "DEBUG/測試單元" else "RELEASE/正式單元"})")
    }

    /**
     * 記錄 SDK 初始化結果
     */
    fun recordInitResult(success: Boolean, details: String) {
        val statusText = if (success) "初始化成功" else "初始化失敗: $details"
        updateSnapshot {
            copy(initStatus = statusText)
        }
        addLog("INIT", if (success) "SUCCESS" else "FAILED", statusText)
    }

    /**
     * 記錄橫幅廣告請求
     */
    fun recordBannerRequest() {
        val nowStr = timeFormat.format(Date())
        updateSnapshot {
            copy(
                bannerRequests = bannerRequests + 1,
                lastBannerStatus = "請求中...",
                lastRequestTime = nowStr
            )
        }
        addLog("BANNER", "REQUEST", "發送橫幅廣告請求 (${AdManager.bannerAdUnitId})")
    }

    /**
     * 記錄橫幅廣告載入成功
     */
    fun recordBannerLoaded() {
        val nowStr = timeFormat.format(Date())
        updateSnapshot {
            copy(
                bannerSuccesses = bannerSuccesses + 1,
                lastBannerStatus = "載入成功 ($nowStr)"
            )
        }
        addLog("BANNER", "LOADED", "橫幅廣告載入成功")
    }

    /**
     * 記錄橫幅廣告載入失敗
     */
    fun recordBannerFailed(error: LoadAdError) {
        val nowStr = timeFormat.format(Date())
        val desc = parseErrorCodeDescription(error.code, error.message)
        val shortStatus = parseShortStatus(error.code, error.message, nowStr)
        updateSnapshot {
            copy(
                bannerFailures = bannerFailures + 1,
                lastBannerStatus = shortStatus
            )
        }
        addLog(
            tag = "BANNER",
            event = "FAILED",
            message = "$desc - ${error.message}",
            errorCode = error.code,
            responseInfo = error.responseInfo?.toString()
        )
    }

    /**
     * 記錄純插頁廣告請求
     */
    fun recordInterstitialRequest() {
        val nowStr = timeFormat.format(Date())
        updateSnapshot {
            copy(
                interstitialRequests = interstitialRequests + 1,
                lastInterstitialStatus = "請求中...",
                lastRequestTime = nowStr
            )
        }
        addLog("INTERSTITIAL", "REQUEST", "發送通關插頁廣告請求 (${AdManager.adUnitId})")
    }

    /**
     * 記錄純插頁廣告載入成功
     */
    fun recordInterstitialLoaded() {
        val nowStr = timeFormat.format(Date())
        updateSnapshot {
            copy(
                interstitialSuccesses = interstitialSuccesses + 1,
                isInterstitialReady = true,
                lastInterstitialStatus = "就緒 ($nowStr)"
            )
        }
        addLog("INTERSTITIAL", "LOADED", "通關插頁廣告載入成功且就緒")
    }

    /**
     * 記錄純插頁廣告載入失敗
     */
    fun recordInterstitialFailed(error: LoadAdError) {
        val nowStr = timeFormat.format(Date())
        val desc = parseErrorCodeDescription(error.code, error.message)
        val shortStatus = parseShortStatus(error.code, error.message, nowStr)
        updateSnapshot {
            copy(
                interstitialFailures = interstitialFailures + 1,
                isInterstitialReady = false,
                lastInterstitialStatus = shortStatus
            )
        }
        addLog(
            tag = "INTERSTITIAL",
            event = "FAILED",
            message = "$desc - ${error.message}",
            errorCode = error.code,
            responseInfo = error.responseInfo?.toString()
        )
    }

    /**
     * 記錄純插頁廣告展示與關閉
     */
    fun recordInterstitialShow() {
        updateSnapshot {
            copy(isInterstitialReady = false, lastInterstitialStatus = "展示中")
        }
        addLog("INTERSTITIAL", "SHOW", "通關插頁廣告展示全螢幕內容")
    }

    fun recordInterstitialDismissed() {
        updateSnapshot {
            copy(lastInterstitialStatus = "已關閉")
        }
        addLog("INTERSTITIAL", "DISMISSED", "通關插頁廣告已關閉")
    }

    fun recordInterstitialShowFailed(reason: String) {
        updateSnapshot {
            copy(isInterstitialReady = false, lastInterstitialStatus = "展示失敗: $reason")
        }
        addLog("INTERSTITIAL", "SHOW_FAILED", "通關插頁展示失敗: $reason")
    }

    /**
     * 記錄插頁式獎勵廣告請求
     */
    fun recordRewardedRequest() {
        val nowStr = timeFormat.format(Date())
        updateSnapshot {
            copy(
                rewardedRequests = rewardedRequests + 1,
                lastRewardedStatus = "請求中...",
                lastRequestTime = nowStr
            )
        }
        addLog("REWARDED", "REQUEST", "發送提示獎勵廣告請求 (${AdManager.rewardedAdUnitId})")
    }

    /**
     * 記錄插頁式獎勵廣告載入成功
     */
    fun recordRewardedLoaded() {
        val nowStr = timeFormat.format(Date())
        updateSnapshot {
            copy(
                rewardedSuccesses = rewardedSuccesses + 1,
                isRewardedReady = true,
                lastRewardedStatus = "就緒 ($nowStr)"
            )
        }
        addLog("REWARDED", "LOADED", "提示獎勵廣告載入成功且就緒")
    }

    /**
     * 記錄插頁式獎勵廣告載入失敗
     */
    fun recordRewardedFailed(error: LoadAdError) {
        val nowStr = timeFormat.format(Date())
        val desc = parseErrorCodeDescription(error.code, error.message)
        val shortStatus = parseShortStatus(error.code, error.message, nowStr)
        updateSnapshot {
            copy(
                rewardedFailures = rewardedFailures + 1,
                isRewardedReady = false,
                lastRewardedStatus = shortStatus
            )
        }
        addLog(
            tag = "REWARDED",
            event = "FAILED",
            message = "$desc - ${error.message}",
            errorCode = error.code,
            responseInfo = error.responseInfo?.toString()
        )
    }

    /**
     * 記錄插頁式獎勵廣告展示、關閉與獲得獎勵
     */
    fun recordRewardedShow() {
        updateSnapshot {
            copy(isRewardedReady = false, lastRewardedStatus = "展示中")
        }
        addLog("REWARDED", "SHOW", "提示獎勵廣告展示全螢幕內容")
    }

    fun recordRewardedDismissed() {
        updateSnapshot {
            copy(lastRewardedStatus = "已關閉")
        }
        addLog("REWARDED", "DISMISSED", "提示獎勵廣告已關閉")
    }

    fun recordRewardedShowFailed(reason: String) {
        updateSnapshot {
            copy(isRewardedReady = false, lastRewardedStatus = "展示失敗: $reason")
        }
        addLog("REWARDED", "SHOW_FAILED", "提示獎勵展示失敗: $reason")
    }

    fun recordRewardEarned(type: String, amount: Int) {
        addLog("REWARDED", "EARNED", "玩家已獲得廣告獎勵 ($type: $amount)")
    }

    /**
     * 解析 AdMob 官方錯誤代碼與伺服器回傳訊息之精準中文意涵
     */
    fun parseErrorCodeDescription(code: Int, message: String? = null): String {
        val msg = message?.lowercase(Locale.getDefault()) ?: ""
        return when (code) {
            0 -> "Code 0: 內部底層錯誤 (ERROR_CODE_INTERNAL_ERROR)"
            1 -> {
                when {
                    msg.contains("doesn't match format") || msg.contains("not match format") ->
                        "Code 1: 格式不符 (廣告單元類型與請求格式不相符)"
                    else ->
                        "Code 1: 請求無效 (ERROR_CODE_INVALID_REQUEST，單元 ID 錯誤或 Manifest 缺少 App ID)"
                }
            }
            2 -> "Code 2: 網路錯誤 (ERROR_CODE_NETWORK_ERROR，連線超時或網路無連線)"
            3 -> {
                when {
                    msg.contains("account not approved") ->
                        "Code 3: 【審查中未放行】(ERROR_CODE_NO_FILL，AdMob 帳戶或 App 整備度審核中，請求遭閘門攔截，不計入後台報表)"
                    msg.contains("app not approved") ->
                        "Code 3: 【App 未就緒】(ERROR_CODE_NO_FILL，應用程式尚在準備中，未通過商店審查)"
                    msg.contains("publisher data not found") ->
                        "Code 3: 【發布商資料未同步】(Google 伺服器尚未同步此發布商或單元資料)"
                    else ->
                        "Code 3: 【無廣告庫存】(ERROR_CODE_NO_FILL，帳戶已放行，目前 Google 無匹配之廣告內容庫存)"
                }
            }
            else -> "Code $code: 未知狀態碼"
        }
    }

    /**
     * 產出精簡狀態摘要字串 (供 UI 儀表板卡片直接顯示)
     */
    fun parseShortStatus(code: Int, message: String?, timestampStr: String): String {
        val msg = message?.lowercase(Locale.getDefault()) ?: ""
        val tag = when {
            code == 3 && msg.contains("account not approved") -> "帳戶審查中 (Code 3)"
            code == 3 && msg.contains("app not approved") -> "App 審查中 (Code 3)"
            code == 3 && msg.contains("publisher data not found") -> "資料同步中 (Code 3)"
            code == 3 -> "無庫存 (Code 3)"
            code == 1 && (msg.contains("doesn't match format") || msg.contains("not match format")) -> "格式不符 (Code 1)"
            code == 2 -> "網路連線失敗 (Code 2)"
            else -> "失敗 Code $code"
        }
        return "$tag ($timestampStr)"
    }

    /**
     * 匯出完整診斷字串報告 (供使用者一鍵複製)
     */
    fun exportDiagnosticReport(): String {
        val s = _snapshot.value
        val sb = StringBuilder()
        sb.appendLine("=== MindGame AdMob 廣告連線診斷報告 ===")
        sb.appendLine("建置模式: ${if (s.isDebugMode) "DEBUG (測試單元 ID)" else "RELEASE (正式單元 ID)"}")
        sb.appendLine("SDK 初始化: ${s.initStatus}")
        sb.appendLine("頂部橫幅單元 ID: ${s.bannerAdUnitId}")
        sb.appendLine("橫幅廣告統計: 請求 ${s.bannerRequests} 次 / 成功 ${s.bannerSuccesses} 次 / 失敗 ${s.bannerFailures} 次")
        sb.appendLine("橫幅最後狀態: ${s.lastBannerStatus}")
        sb.appendLine("通關插頁單元 ID: ${s.interstitialAdUnitId}")
        sb.appendLine("插頁廣告統計: 請求 ${s.interstitialRequests} 次 / 成功 ${s.interstitialSuccesses} 次 / 失敗 ${s.interstitialFailures} 次")
        sb.appendLine("插頁最後狀態: ${s.lastInterstitialStatus} (就緒: ${s.isInterstitialReady})")
        sb.appendLine("提示獎勵單元 ID: ${s.rewardedAdUnitId}")
        sb.appendLine("獎勵廣告統計: 請求 ${s.rewardedRequests} 次 / 成功 ${s.rewardedSuccesses} 次 / 失敗 ${s.rewardedFailures} 次")
        sb.appendLine("獎勵最後狀態: ${s.lastRewardedStatus} (就緒: ${s.isRewardedReady})")
        sb.appendLine("最後請求時間: ${s.lastRequestTime}")
        sb.appendLine("--- 最近事件日誌 (最近 ${getLogs().size} 筆) ---")
        getLogs().forEach { entry ->
            sb.appendLine(entry.toFormattedString())
            if (!entry.responseInfo.isNullOrBlank()) {
                sb.appendLine("   ResponseInfo: ${entry.responseInfo}")
            }
        }
        sb.appendLine("=========================================")
        return sb.toString()
    }

    /**
     * 獲取日誌列表
     */
    @Synchronized
    fun getLogs(): List<AdLogEntry> {
        return logQueue.toList()
    }

    /**
     * 清理所有日誌
     */
    @Synchronized
    fun clearLogs() {
        logQueue.clear()
        updateSnapshot {
            copy(
                bannerRequests = 0,
                bannerSuccesses = 0,
                bannerFailures = 0,
                interstitialRequests = 0,
                interstitialSuccesses = 0,
                interstitialFailures = 0,
                rewardedRequests = 0,
                rewardedSuccesses = 0,
                rewardedFailures = 0,
                lastBannerStatus = "已重置",
                lastInterstitialStatus = "已重置",
                lastRewardedStatus = "已重置"
            )
        }
        addLog("SYSTEM", "CLEAR", "診斷日誌已手動重置")
    }

    @Synchronized
    private fun addLog(
        tag: String,
        event: String,
        message: String,
        errorCode: Int? = null,
        responseInfo: String? = null
    ) {
        val entry = AdLogEntry(
            tag = tag,
            event = event,
            message = message,
            errorCode = errorCode,
            responseInfo = responseInfo
        )
        if (logQueue.size >= MAX_LOGS) {
            logQueue.removeFirst()
        }
        logQueue.addLast(entry)

        // 同步寫入 Logcat
        try {
            when (event) {
                "FAILED", "SHOW_FAILED" -> Log.w(TAG, entry.toFormattedString())
                else -> Log.d(TAG, entry.toFormattedString())
            }
        } catch (e: Throwable) {
            // JVM 單元測試環境下 android.util.Log 未 mock 時安全忽略
        }

        // 非同步持久化至私有檔案
        appContext?.let { ctx ->
            try {
                val file = File(ctx.filesDir, LOG_FILE_NAME)
                file.appendText(entry.toFormattedString() + "\n")
            } catch (e: Exception) {
                // 忽略檔案寫入異常，防止影響主程序
            }
        }
    }

    private inline fun updateSnapshot(transform: AdDiagnosticSnapshot.() -> AdDiagnosticSnapshot) {
        _snapshot.update(transform)
    }
}
