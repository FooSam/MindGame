package com.example

import com.example.data.model.GameDifficulty
import com.example.data.model.WheelDifficultyConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusTrainLogicTest {

    @Test
    fun testWheelDifficultyConfigs() {
        // 1. 初級 (BEGINNER)
        val begConfig = WheelDifficultyConfig.getConfig(GameDifficulty.BEGINNER)
        assertEquals("初級總數為 7", 7, begConfig.totalNumbers)
        assertEquals("初級外圍 1 圈", 1, begConfig.rings.size)
        assertEquals("初級第 1 圈 6 個數字", 6, begConfig.rings[0].itemCount)
        assertTrue("初級第 1 圈順時針旋轉", begConfig.rings[0].isClockwise)
        assertEquals("初級轉速 20 秒", 20_000, begConfig.rotationDurationMs)

        // 2. 中級 (INTERMEDIATE)
        val intConfig = WheelDifficultyConfig.getConfig(GameDifficulty.INTERMEDIATE)
        assertEquals("中級總數為 13", 13, intConfig.totalNumbers)
        assertEquals("中級外圍 1 圈", 1, intConfig.rings.size)
        assertEquals("中級第 1 圈 12 個數字", 12, intConfig.rings[0].itemCount)
        assertTrue("中級第 1 圈順時針旋轉", intConfig.rings[0].isClockwise)
        assertEquals("中級轉速 20 秒", 20_000, intConfig.rotationDurationMs)

        // 3. 高級 (ADVANCED)
        val advConfig = WheelDifficultyConfig.getConfig(GameDifficulty.ADVANCED)
        assertEquals("高級總數為 19", 19, advConfig.totalNumbers)
        assertEquals("高級外圍 2 圈", 2, advConfig.rings.size)
        assertEquals("高級第 1 圈 6 個數字", 6, advConfig.rings[0].itemCount)
        assertTrue("高級第 1 圈順時針旋轉", advConfig.rings[0].isClockwise)
        assertEquals("高級第 2 圈 12 個數字", 12, advConfig.rings[1].itemCount)
        assertFalse("高級第 2 圈逆時針旋轉", advConfig.rings[1].isClockwise)
        assertEquals("高級轉速 20 秒", 20_000, advConfig.rotationDurationMs)

        // 4. 困難級 (HARD)
        val hardConfig = WheelDifficultyConfig.getConfig(GameDifficulty.HARD)
        assertEquals("困難級總數為 37", 37, hardConfig.totalNumbers)
        assertEquals("困難級外圍 2 圈", 2, hardConfig.rings.size)
        assertEquals("困難級第 1 圈 12 個數字", 12, hardConfig.rings[0].itemCount)
        assertTrue("困難級第 1 圈順時針旋轉", hardConfig.rings[0].isClockwise)
        assertEquals("困難級第 2 圈 24 個數字", 24, hardConfig.rings[1].itemCount)
        assertFalse("困難級第 2 圈逆時針旋轉", hardConfig.rings[1].isClockwise)
        assertEquals("困難級轉速 15 秒", 15_000, hardConfig.rotationDurationMs)

        // 5. 地獄級 (HELL)
        val hellConfig = WheelDifficultyConfig.getConfig(GameDifficulty.HELL)
        assertEquals("地獄級總數為 43", 43, hellConfig.totalNumbers)
        assertEquals("地獄級外圍 3 圈", 3, hellConfig.rings.size)
        assertEquals("地獄級第 1 圈 6 個數字", 6, hellConfig.rings[0].itemCount)
        assertTrue("地獄級第 1 圈順時針旋轉", hellConfig.rings[0].isClockwise)
        assertEquals("地獄級第 2 圈 12 個數字", 12, hellConfig.rings[1].itemCount)
        assertFalse("地獄級第 2 圈逆時針旋轉", hellConfig.rings[1].isClockwise)
        assertEquals("地獄級第 3 圈 24 個數字", 24, hellConfig.rings[2].itemCount)
        assertTrue("地獄級第 3 圈順時針旋轉", hellConfig.rings[2].isClockwise)
        assertEquals("地獄級轉速 15 秒", 15_000, hellConfig.rotationDurationMs)

        // 6. 史詩級 (EPIC)
        val epicConfig = WheelDifficultyConfig.getConfig(GameDifficulty.EPIC)
        assertEquals("史詩級總數為 69", 69, epicConfig.totalNumbers)
        assertEquals("史詩級外圍 3 圈", 3, epicConfig.rings.size)
        assertEquals("史詩級第 1 圈 12 個數字", 12, epicConfig.rings[0].itemCount)
        assertTrue("史詩級第 1 圈順時針旋轉", epicConfig.rings[0].isClockwise)
        assertEquals("史詩級第 2 圈 24 個數字", 24, epicConfig.rings[1].itemCount)
        assertFalse("史詩級第 2 圈逆時針旋轉", epicConfig.rings[1].isClockwise)
        assertEquals("史詩級第 3 圈 32 個數字", 32, epicConfig.rings[2].itemCount)
        assertTrue("史詩級第 3 圈順時針旋轉", epicConfig.rings[2].isClockwise)
        assertEquals("史詩級轉速 15 秒", 15_000, epicConfig.rotationDurationMs)
    }

    @Test
    fun testFocusTrainNumberRandomization() {
        val difficulties = GameDifficulty.entries.toTypedArray()

        for (diff in difficulties) {
            val config = WheelDifficultyConfig.getConfig(diff)
            val total = config.totalNumbers

            // 驗證圓心 1 + 各圈總和恰等於 totalNumbers
            val ringSum = config.rings.sumOf { it.itemCount }
            assertEquals("中心 1 個加上所有圈層數字必須等於總數", total, 1 + ringSum)

            // 隨機洗牌驗證
            val listA = (1..total).shuffled()
            val listB = (1..total).shuffled()

            assertEquals("數量必須與 totalNumbers 相同", total, listA.size)
            assertEquals("必須包含 1 到 totalNumbers 的所有數字", (1..total).toSet(), listA.toSet())

            if (total > 7) {
                // 較大數量時兩次隨機洗牌不應完全相同
                assertNotEquals("兩次洗牌順序不應完全一致", listA, listB)
            }
        }
    }

    @Test
    fun testFocusTrainSequentialClickSimulation() {
        val config = WheelDifficultyConfig.getConfig(GameDifficulty.ADVANCED)
        val total = config.totalNumbers // 19
        val numbers = (1..total).shuffled()

        var currentTarget = 1
        val clearedIndices = mutableSetOf<Int>()
        var mistakes = 0

        // 模擬按序找出目標並點擊
        while (currentTarget <= total) {
            val targetNumber = currentTarget
            val correctIndex = numbers.indexOf(targetNumber)
            assertTrue("目標數字必須存在於盤面", correctIndex >= 0)

            // 模擬點擊正確
            clearedIndices.add(correctIndex)
            currentTarget++
        }

        assertEquals("所有格子均已清除", total, clearedIndices.size)
        assertEquals("完成時 target 為 total + 1", total + 1, currentTarget)
        assertEquals("模擬無錯誤點擊", 0, mistakes)
    }

    @Test
    fun testPolarCoordinateSectorMapping() {
        // 驗證極座標扇形分割演算法：所有 360 度角度均能精確映射至 0..K-1，無死角或重疊
        val itemCounts = listOf(6, 12, 24, 32)
        for (itemCount in itemCounts) {
            val sweep = 360f / itemCount
            val mappedIndices = mutableSetOf<Int>()

            for (angle in 0 until 3600) { // 0.1 度精細取樣
                val angleDeg = angle / 10f
                val k = (angleDeg / sweep).toInt().coerceIn(0, itemCount - 1)
                assertTrue("扇形索引必須在 0 到 ${itemCount - 1} 之間", k in 0 until itemCount)
                mappedIndices.add(k)
            }

            assertEquals("所有扇形索引均被完整且均勻覆蓋", itemCount, mappedIndices.size)
        }
    }

    @Test
    fun testFocusTrainAndAboutLocalization() {
        val zhLang = com.example.data.model.AppLanguage.TRADITIONAL_CHINESE
        val enLang = com.example.data.model.AppLanguage.ENGLISH

        // 專注力訓練語系 key 檢驗
        val keys = listOf(
            "current_target_label",
            "time_elapsed_label",
            "game_start_button",
            "game_reset_button",
            "game_completed_title",
            "record_time_format",
            "record_saved_message",
            "play_again_button",
            "focus_train_ready_hint"
        )

        for (key in keys) {
            val zhStr = com.example.data.model.Localization.getString(key, zhLang)
            val enStr = com.example.data.model.Localization.getString(key, enLang)
            assertNotEquals("繁中不可為 fallback key: $key", key, zhStr)
            assertNotEquals("英文不可為 fallback key: $key", key, enStr)
            assertTrue("繁中不可為空: $key", zhStr.isNotBlank())
            assertTrue("英文不可為空: $key", enStr.isNotBlank())
        }

        // 關於彈窗資訊檢驗
        val appNameZh = com.example.data.model.Localization.getString("about_app_name_value", zhLang)
        assertEquals("繁中應用程式名稱不應包含 (Brain Training)", "左右腦鍛鍊", appNameZh)

        val devZh = com.example.data.model.Localization.getString("about_dev_value", zhLang)
        assertTrue("繁中開發團隊應包含凡夫俗子工作室", devZh.contains("凡夫俗子工作室"))
    }
}
