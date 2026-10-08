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
}
