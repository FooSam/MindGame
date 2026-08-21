# AGENTS.md - 專案開發規則與注意事項

## 0. 專案最高等級規格指令 (Highest Priority Directives)

### 一、 語言與回覆規範
1. **語系規範**：一律使用繁體中文（台灣習慣用語）進行回覆。
2. **極簡專業**：回覆必須極度精簡、專業、誠實，直接針對問題核心回答。
3. **嚴禁贅述**：嚴禁任何形式的寒暄、多餘解釋、推銷、或系統/歷史進度總結。
4. **禁止洗板**：嚴禁重複說明已確認的規則、對話歷史或已完成的進度。

### 二、 真實性與誠實原則
1. **誠實原則**：絕對不能編造平台或程式碼中不存在的功能。
2. **坦白說明**：若平台或目前技術結構不支援，必須直接坦白說明，不允許任何欺騙、混淆或含糊其辭的行為。

---

## 1. 核心開發與修改規範 (Development Rules)

1. **架構規範**：
   - 採用 **Kotlin + Jetpack Compose (Material 3)** 進行開發。
   - 架構遵循 **MVVM (Model-View-ViewModel)**，狀態管理使用 `StateFlow` / `collectAsStateWithLifecycle`。
   - 本地持久化資料庫嚴格使用 **Room Database** (`AppDatabase.kt`, `ScoreDao.kt`, `ScoreRecord.kt`)，切勿隨意替換或破壞資料庫 schema。

2. **多語系支援 (i18n)**：
   - 所有畫面文字與遊戲提示均須透過 `Localization.kt` 進行中英文雙語對應，嚴禁在 Composable 元件中寫死字串。

3. **主題與背景規範**：
   - 所有樣式風格（雪白、暗黑、機械、可愛、陽光、企業、休閒等）必須配合 `AppBackground.kt` 與 `Theme.kt` 動態切換背景與元件色系。

4. **音效與音樂控制**：
   - 音效與音樂統一透過 `SoundManager.kt` 進行調度與開關判斷（`isSfxEnabled` / `isBgmEnabled`）。

5. **建置與驗證**：
   - 修改程式碼後，必須確保 Gradle 與單元測試 (`gradle :app:testDebugUnitTest`) 能正常通過。
   - `metadata.json` 與 `strings.xml` 的應用程式名稱需保持同步（`左右腦鍛鍊`）。
