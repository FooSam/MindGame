package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AppLanguage
import com.example.ui.theme.AppThemeStyle
import com.example.ui.viewmodel.GameViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsAndViewModelTest {

    @Test
    fun testFullScreenToggleAndPersistence() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = GameViewModel(app)

        // 預設全螢幕為 true
        assertTrue(viewModel.isFullScreenEnabled.value)

        // 切換為 false
        viewModel.toggleFullScreen(false)
        assertFalse(viewModel.isFullScreenEnabled.value)

        // 新建 ViewModel 實例驗證持久化
        val viewModel2 = GameViewModel(app)
        assertFalse("重啟後應讀取儲存的 false 設定", viewModel2.isFullScreenEnabled.value)

        // 再次切換為 true
        viewModel2.toggleFullScreen(true)
        assertTrue(viewModel2.isFullScreenEnabled.value)
    }

    @Test
    fun testLanguageAndThemePersistence() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = GameViewModel(app)

        viewModel.setLanguage(AppLanguage.ENGLISH)
        viewModel.setThemeStyle(AppThemeStyle.DARK)

        assertEquals(AppLanguage.ENGLISH, viewModel.language.value)
        assertEquals(AppThemeStyle.DARK, viewModel.appTheme.value)

        // 驗證重啟後恢復
        val viewModel2 = GameViewModel(app)
        assertEquals(AppLanguage.ENGLISH, viewModel2.language.value)
        assertEquals(AppThemeStyle.DARK, viewModel2.appTheme.value)
    }

    @Test
    fun testNewGameDiscoveryAndDismissal() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        // 清理可能殘留的 SharedPreferences
        app.getSharedPreferences("mindgame_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .remove("pref_viewed_game_keys")
            .commit()

        val viewModel = GameViewModel(app)

        // 初始狀態：六面合體為新遊戲，大腦分類應有新遊戲標籤
        assertTrue("六面合體魔方預設為未探索新遊戲", viewModel.isGameNew(com.example.data.model.GameType.GLASS_PUZZLE_CUBE))
        assertTrue("智力分類應顯示有新遊戲", viewModel.isCategoryHasNew(com.example.data.model.GameCategory.BRAIN))

        // 標記六面合體已查看
        viewModel.markGameAsViewed(com.example.data.model.GameType.GLASS_PUZZLE_CUBE)
        assertFalse("標記已看過後 isGameNew 應為 false", viewModel.isGameNew(com.example.data.model.GameType.GLASS_PUZZLE_CUBE))
        assertFalse("智力分類在唯一新遊戲看過後應無新遊戲提示", viewModel.isCategoryHasNew(com.example.data.model.GameCategory.BRAIN))

        // 驗證重啟持久化
        val viewModel2 = GameViewModel(app)
        assertFalse("重啟後六面合體仍應為已探索狀態", viewModel2.isGameNew(com.example.data.model.GameType.GLASS_PUZZLE_CUBE))
    }

    @Test
    fun testOpenFeaturedGameDirectly() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = GameViewModel(app)

        // 點擊跑馬燈直達六面合體
        viewModel.openFeaturedGameDirectly(com.example.data.model.GameType.GLASS_PUZZLE_CUBE)

        assertEquals(com.example.ui.viewmodel.ScreenState.CATEGORY_DETAIL, viewModel.currentScreen.value)
        assertEquals(com.example.data.model.GameCategory.BRAIN, viewModel.selectedCategory.value)
        assertEquals(com.example.data.model.GameType.GLASS_PUZZLE_CUBE, viewModel.selectedGameType.value)
        assertEquals("跑馬燈直達時應原地展開該焦點遊戲", com.example.data.model.GameType.GLASS_PUZZLE_CUBE, viewModel.expandedGameType.value)
        assertFalse("直達後六面合體應被自動標記為已讀", viewModel.isGameNew(com.example.data.model.GameType.GLASS_PUZZLE_CUBE))
    }

    @Test
    fun testSubmenuDefaultCollapsedAndToggle() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = GameViewModel(app)

        // 1. 從首頁點進智力分類次選單
        viewModel.selectCategory(com.example.data.model.GameCategory.BRAIN)
        assertEquals(com.example.ui.viewmodel.ScreenState.CATEGORY_DETAIL, viewModel.currentScreen.value)
        assertNull("進入次選單時應預設不展開任何遊戲卡片", viewModel.expandedGameType.value)

        // 2. 點擊六面合體魔方展開
        viewModel.toggleGameTypeExpanded(com.example.data.model.GameType.GLASS_PUZZLE_CUBE)
        assertEquals("點擊後應展開六面合體魔方", com.example.data.model.GameType.GLASS_PUZZLE_CUBE, viewModel.expandedGameType.value)
        assertEquals(com.example.data.model.GameType.GLASS_PUZZLE_CUBE, viewModel.selectedGameType.value)

        // 3. 再次點擊同一個卡片收合
        viewModel.toggleGameTypeExpanded(com.example.data.model.GameType.GLASS_PUZZLE_CUBE)
        assertNull("再次點擊同卡片應收合為 null", viewModel.expandedGameType.value)

        // 4. 點擊另一款遊戲（經典數獨）展開
        viewModel.toggleGameTypeExpanded(com.example.data.model.GameType.SUDOKU)
        assertEquals("點擊後應展開數獨", com.example.data.model.GameType.SUDOKU, viewModel.expandedGameType.value)
        assertEquals(com.example.data.model.GameType.SUDOKU, viewModel.selectedGameType.value)

        // 5. 切換回首頁再進專注分類次選單，仍應預設全部收合
        viewModel.navigateTo(com.example.ui.viewmodel.ScreenState.HOME)
        viewModel.selectCategory(com.example.data.model.GameCategory.TEST)
        assertNull("重新進入專注分類次選單時亦應預設不展開任何卡片", viewModel.expandedGameType.value)
    }

    @Test
    fun testOpenLeaderboardDialogWithHotGames() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = GameViewModel(app)

        viewModel.openLeaderboardDialog(initialMainTab = com.example.ui.components.LeaderboardMainTab.HOT_GAMES)
        assertTrue("排行榜對話框應開啟", viewModel.showLeaderboardDialog.value)
        assertEquals("初始分頁應為熱門風雲榜", com.example.ui.components.LeaderboardMainTab.HOT_GAMES, viewModel.leaderboardInitialTab.value)
    }

    @Test
    fun testSetLeaderboardGame() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = GameViewModel(app)

        viewModel.setLeaderboardGame(com.example.data.model.GameType.GLASS_PUZZLE_CUBE)
        assertEquals(com.example.data.model.GameType.GLASS_PUZZLE_CUBE, viewModel.selectedGameType.value)
        assertEquals(com.example.data.model.GameCategory.BRAIN, viewModel.selectedCategory.value)

        viewModel.setLeaderboardGame(com.example.data.model.GameType.FRUIT_MASTER)
        assertEquals(com.example.data.model.GameType.FRUIT_MASTER, viewModel.selectedGameType.value)
        assertEquals(com.example.data.model.GameCategory.CASUAL, viewModel.selectedCategory.value)
        assertTrue(
            "水果切切樂僅支援前三種難度",
            viewModel.leaderboardDifficulty.value in listOf(
                com.example.data.model.GameDifficulty.BEGINNER,
                com.example.data.model.GameDifficulty.INTERMEDIATE,
                com.example.data.model.GameDifficulty.ADVANCED
            )
        )
    }
}
