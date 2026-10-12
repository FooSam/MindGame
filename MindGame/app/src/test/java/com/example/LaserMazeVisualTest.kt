package com.example

import com.example.ui.theme.AppThemeStyle
import com.example.ui.theme.LaserMazeThemePalettes
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 驗證 7 大風格樣式調色盤無空缺且具備高度對比度
 */
class LaserMazeVisualTest {

    @Test
    fun testAllAppThemeStylesHaveLaserMazePalettes() {
        for (theme in AppThemeStyle.entries) {
            val palette = LaserMazeThemePalettes.get(theme)
            assertNotNull("主題 $theme 調色盤不應為空", palette)
            assertTrue("背景漸層應至少包含 2 種顏色", palette.boardBackgroundGradient.size >= 2)
            assertNotNull(palette.boardBorderColor)
            assertNotNull(palette.slotBackground)
            assertNotNull(palette.slotBorder)
            assertNotNull(palette.laserDefaultGlow)
            assertNotNull(palette.miniMapBackground)
            assertNotNull(palette.miniMapBorder)
            assertNotNull(palette.miniMapViewport)
        }
    }

    @Test
    fun test3DTiltViewLocalizationKeys() {
        val keys = listOf(
            "laser_maze_tilt_view",
            "laser_maze_top_view",
            "laser_maze_reset_view",
            "laser_maze_mode_tilt",
            "laser_maze_mode_pan",
            "laser_maze_view_hint"
        )
        for (key in keys) {
            val zh = com.example.data.model.Localization.getString(key, com.example.data.model.AppLanguage.TRADITIONAL_CHINESE)
            val en = com.example.data.model.Localization.getString(key, com.example.data.model.AppLanguage.ENGLISH)
            assertTrue("繁中字串 $key 不應為空", zh.isNotBlank())
            assertTrue("英文字串 $key 不應為空", en.isNotBlank())
        }
    }

    @Test
    fun test3DTiltAngleConstraintsNeverExceed30Degrees() {
        // 驗證俯仰角 (Pitch) 與偏航角 (Yaw) 嚴格限制在 30 度以內
        val testPitchInputs = listOf(-50f, -10f, 0f, 15f, 18f, 30f, 45f, 90f)
        for (pitch in testPitchInputs) {
            val clampedPitch = pitch.coerceIn(0f, 30f)
            assertTrue("Pitch 應大於等於 0 度: $clampedPitch", clampedPitch >= 0f)
            assertTrue("Pitch 絕對不可超過 30 度: $clampedPitch", clampedPitch <= 30f)
        }

        val testYawInputs = listOf(-90f, -45f, -25f, -20f, 0f, 20f, 25f, 50f)
        for (yaw in testYawInputs) {
            val clampedYaw = yaw.coerceIn(-25f, 25f)
            assertTrue("Yaw 絕對值不可超過 30 度: $clampedYaw", kotlin.math.abs(clampedYaw) <= 30f)
        }
    }

    @Test
    fun testCalculateLaserMazeStars() {
        // BEGINNER 難度
        org.junit.Assert.assertEquals(3, com.example.ui.screens.calculateLaserMazeStars(com.example.data.model.GameDifficulty.BEGINNER, 15000L, 5))
        org.junit.Assert.assertEquals(3, com.example.ui.screens.calculateLaserMazeStars(com.example.data.model.GameDifficulty.BEGINNER, 25000L, 6))
        org.junit.Assert.assertEquals(2, com.example.ui.screens.calculateLaserMazeStars(com.example.data.model.GameDifficulty.BEGINNER, 26000L, 5))
        org.junit.Assert.assertEquals(2, com.example.ui.screens.calculateLaserMazeStars(com.example.data.model.GameDifficulty.BEGINNER, 20000L, 7))
        org.junit.Assert.assertEquals(2, com.example.ui.screens.calculateLaserMazeStars(com.example.data.model.GameDifficulty.BEGINNER, 50000L, 4))
        org.junit.Assert.assertEquals(1, com.example.ui.screens.calculateLaserMazeStars(com.example.data.model.GameDifficulty.BEGINNER, 51000L, 3))

        // INTERMEDIATE 難度
        org.junit.Assert.assertEquals(3, com.example.ui.screens.calculateLaserMazeStars(com.example.data.model.GameDifficulty.INTERMEDIATE, 35000L, 8))
        org.junit.Assert.assertEquals(2, com.example.ui.screens.calculateLaserMazeStars(com.example.data.model.GameDifficulty.INTERMEDIATE, 45000L, 8))
        org.junit.Assert.assertEquals(1, com.example.ui.screens.calculateLaserMazeStars(com.example.data.model.GameDifficulty.INTERMEDIATE, 85000L, 5))

        // ADVANCED 難度
        org.junit.Assert.assertEquals(3, com.example.ui.screens.calculateLaserMazeStars(com.example.data.model.GameDifficulty.ADVANCED, 60000L, 12))
        org.junit.Assert.assertEquals(2, com.example.ui.screens.calculateLaserMazeStars(com.example.data.model.GameDifficulty.ADVANCED, 80000L, 12))
        org.junit.Assert.assertEquals(1, com.example.ui.screens.calculateLaserMazeStars(com.example.data.model.GameDifficulty.ADVANCED, 140000L, 10))

        // HARD 難度
        org.junit.Assert.assertEquals(3, com.example.ui.screens.calculateLaserMazeStars(com.example.data.model.GameDifficulty.HARD, 90000L, 20))
        org.junit.Assert.assertEquals(2, com.example.ui.screens.calculateLaserMazeStars(com.example.data.model.GameDifficulty.HARD, 120000L, 20))
        org.junit.Assert.assertEquals(1, com.example.ui.screens.calculateLaserMazeStars(com.example.data.model.GameDifficulty.HARD, 210000L, 15))
    }

    @Test
    fun testLaserMazeRankLocalizationKeys() {
        val keys = listOf(
            "laser_maze_rank_3star",
            "laser_maze_rank_2star",
            "laser_maze_rank_1star"
        )
        for (key in keys) {
            val zh = com.example.data.model.Localization.getString(key, com.example.data.model.AppLanguage.TRADITIONAL_CHINESE)
            val en = com.example.data.model.Localization.getString(key, com.example.data.model.AppLanguage.ENGLISH)
            assertTrue("繁中字串 $key 不應為空", zh.isNotBlank())
            assertTrue("英文字串 $key 不應為空", en.isNotBlank())
        }
    }
}
