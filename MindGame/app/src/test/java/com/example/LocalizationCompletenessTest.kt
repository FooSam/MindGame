package com.example

import com.example.data.model.AppLanguage
import com.example.data.model.Country
import com.example.data.model.GameType
import com.example.data.model.Localization
import com.example.data.model.LocalizedText
import com.example.game.brainintrouble.BrainColor
import com.example.game.brainintrouble.BrainShape
import com.example.game.stroop.StroopColor
import com.example.game.stroop.StroopShape
import com.example.game.turtlesoup.TurtleSoupRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalizationCompletenessTest {

    private val testKeys = listOf(
        "player_name_label", "settings_title", "category_test", "game_focus_test",
        "game_focus_train", "game_speed_match", "game_sudoku", "game_cat_sudoku",
        "game_turtle_soup", "game_block_puzzle", "game_pinball_flipper", "game_night_market_pinball",
        "game_brain_in_trouble", "game_fruit_master", "game_glass_puzzle_cube", "game_avatar_whack",
        "game_stroop_effect", "game_laser_maze", "leaderboard_title", "leaderboard_tab_global",
        "confirm", "cancel", "state_enabled", "state_disabled", "about_section_title",
        "theme_snow_white", "theme_dark", "theme_mechanical", "theme_cute",
        "diff_beginner", "diff_intermediate", "diff_advanced", "diff_hard", "diff_hell", "diff_epic",
        "game_completed", "your_time", "play_again", "back_to_menu", "watch_ad_button", "hint"
    )

    @Test
    fun testAllLanguagesKeyCompleteness() {
        for (lang in AppLanguage.entries) {
            for (key in testKeys) {
                val str = Localization.getString(key, lang)
                assertNotNull("Key [$key] should not return null for language $lang", str)
                assertTrue("Key [$key] should not be empty for language $lang", str.isNotEmpty())
                assertFalse("Key [$key] should not return raw key for language $lang", str == key)
            }
        }
    }

    @Test
    fun testJapaneseTranslationsIntegrity() {
        assertEquals("設定", Localization.getString("settings_button", AppLanguage.JAPANESE))
        assertEquals("集中力テスト", Localization.getString("game_focus_test", AppLanguage.JAPANESE))
        assertEquals("クラシック数独", Localization.getString("game_sudoku", AppLanguage.JAPANESE))
        assertEquals("レーザー迷路", Localization.getString("game_laser_maze", AppLanguage.JAPANESE))
        assertEquals("ストループ効果", Localization.getString("game_stroop_effect", AppLanguage.JAPANESE))
        assertEquals("左右ジレンマ", Localization.getString("game_brain_in_trouble", AppLanguage.JAPANESE))
    }

    @Test
    fun testKoreanTranslationsIntegrity() {
        assertEquals("설정", Localization.getString("settings_button", AppLanguage.KOREAN))
        assertEquals("집중력 테스트", Localization.getString("game_focus_test", AppLanguage.KOREAN))
        assertEquals("클래식 스도쿠", Localization.getString("game_sudoku", AppLanguage.KOREAN))
        assertEquals("레이저 미로", Localization.getString("game_laser_maze", AppLanguage.KOREAN))
        assertEquals("스트룹 효과", Localization.getString("game_stroop_effect", AppLanguage.KOREAN))
        assertEquals("좌우 딜레마", Localization.getString("game_brain_in_trouble", AppLanguage.KOREAN))
    }

    @Test
    fun testScoreUnitLocalization() {
        for (type in GameType.entries) {
            val zh = type.getScoreUnit(AppLanguage.TRADITIONAL_CHINESE)
            val en = type.getScoreUnit(AppLanguage.ENGLISH)
            val ja = type.getScoreUnit(AppLanguage.JAPANESE)
            val ko = type.getScoreUnit(AppLanguage.KOREAN)

            assertTrue(zh.isNotEmpty())
            assertTrue(en.isNotEmpty())
            assertTrue(ja.isNotEmpty())
            assertTrue(ko.isNotEmpty())

            if (type == GameType.SPEED_MATCH) {
                assertEquals("次", zh)
                assertEquals("times", en)
                assertEquals("回", ja)
                assertEquals("회", ko)
            } else {
                assertEquals("分", zh)
                assertEquals("pts", en)
                assertEquals("点", ja)
                assertEquals("점", ko)
            }
        }
    }

    @Test
    fun testStroopEnumsLocalization() {
        // Colors
        assertEquals("赤", StroopColor.RED.getName(AppLanguage.JAPANESE))
        assertEquals("빨강", StroopColor.RED.getName(AppLanguage.KOREAN))
        assertEquals("青", StroopColor.BLUE.getName(AppLanguage.JAPANESE))
        assertEquals("파랑", StroopColor.BLUE.getName(AppLanguage.KOREAN))

        // Shapes
        assertEquals("円形", StroopShape.CIRCLE.getName(AppLanguage.JAPANESE))
        assertEquals("원형", StroopShape.CIRCLE.getName(AppLanguage.KOREAN))
        assertEquals("四角形", StroopShape.SQUARE.getName(AppLanguage.JAPANESE))
        assertEquals("사각형", StroopShape.SQUARE.getName(AppLanguage.KOREAN))
    }

    @Test
    fun testBrainInTroubleEnumsLocalization() {
        // Shapes
        assertEquals("円形", BrainShape.CIRCLE.getDisplayName(AppLanguage.JAPANESE))
        assertEquals("원형", BrainShape.CIRCLE.getDisplayName(AppLanguage.KOREAN))
        assertEquals("正方形", BrainShape.SQUARE.getDisplayName(AppLanguage.JAPANESE))
        assertEquals("정사각형", BrainShape.SQUARE.getDisplayName(AppLanguage.KOREAN))

        // Colors
        assertEquals("赤", BrainColor.RED.getDisplayName(AppLanguage.JAPANESE))
        assertEquals("빨강", BrainColor.RED.getDisplayName(AppLanguage.KOREAN))
        assertEquals("青", BrainColor.BLUE.getDisplayName(AppLanguage.JAPANESE))
        assertEquals("파랑", BrainColor.BLUE.getDisplayName(AppLanguage.KOREAN))
    }

    @Test
    fun testLocalizedTextFallback() {
        val fullText = LocalizedText(zhTW = "測試", en = "Test", ja = "テスト", ko = "테스트")
        assertEquals("測試", fullText.get(AppLanguage.TRADITIONAL_CHINESE))
        assertEquals("Test", fullText.get(AppLanguage.ENGLISH))
        assertEquals("テスト", fullText.get(AppLanguage.JAPANESE))
        assertEquals("테스트", fullText.get(AppLanguage.KOREAN))

        // Fallback when ja/ko is empty
        val fallbackText = LocalizedText(zhTW = "測試", en = "Test")
        assertEquals("Test", fallbackText.get(AppLanguage.JAPANESE))
        assertEquals("Test", fallbackText.get(AppLanguage.KOREAN))
    }
}
