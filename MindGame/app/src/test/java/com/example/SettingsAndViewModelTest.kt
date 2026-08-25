package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AppLanguage
import com.example.ui.theme.AppThemeStyle
import com.example.ui.viewmodel.GameViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}
