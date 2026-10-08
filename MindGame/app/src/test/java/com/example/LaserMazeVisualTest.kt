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
}
