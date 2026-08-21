package com.example.data.model

enum class AppLanguage(val code: String, val displayName: String) {
    TRADITIONAL_CHINESE("zh_TW", "繁體中文"),
    ENGLISH("en", "English")
}

object Localization {
    fun getString(key: String, language: AppLanguage, vararg args: Any): String {
        val map = when (language) {
            AppLanguage.TRADITIONAL_CHINESE -> zhTWMap
            AppLanguage.ENGLISH -> enMap
        }
        val template = map[key] ?: zhTWMap[key] ?: key
        return if (args.isNotEmpty()) {
            try {
                String.format(template, *args)
            } catch (e: Exception) {
                template
            }
        } else {
            template
        }
    }

    private val zhTWMap = mapOf(
        "app_title" to "",
        "app_subtitle" to "",
        "player_name_label" to "玩家名稱",
        "change_name_button" to "更改名稱",
        "input_name_title" to "輸入玩家名稱",
        "input_name_hint" to "請輸入要顯示在排行榜上的名字",
        "confirm" to "確認",
        "cancel" to "取消",
        "settings_title" to "系統設定",
        "settings_button" to "設定",
        "language_selector" to "語系切換",
        "theme_selector" to "風格樣式",
        "theme_title" to "風格樣式",
        "audio_settings_title" to "音效與音樂",
        "sfx_switch_label" to "操作音效",
        "bgm_switch_label" to "背景音樂",
        "state_enabled" to "已開啟",
        "state_disabled" to "已關閉",
        "about_section_title" to "關於本程式",
        "about_app_name_label" to "應用程式名稱",
        "about_app_name_value" to "左右腦鍛鍊 (Brain Training)",
        "about_version_label" to "當前版本",
        "about_version_value" to "v1.2.0",
        "about_dev_label" to "開發團隊",
        "about_dev_value" to "AI Studio 腦力研發小組",
        "about_desc_label" to "產品理念",
        "about_desc_value" to "專為全方位激發左右腦潛能而設計，結合舒爾特專注力方格、極速配對、經典數獨與創新貓咪數獨等多重認知訓練模組。",
        "theme_snow_white" to "雪白風格 (預設)",
        "theme_dark" to "暗黑風格",
        "theme_mechanical" to "機械風格",
        "theme_cute" to "可愛風格",
        "theme_sunny" to "陽光風格",
        "theme_corporate" to "上班族風格",
        "theme_casual" to "休閒風格",
        
        // Categories
        "category_test" to "測驗遊戲",
        "category_test_desc" to "反應速度與專注力極限挑戰",
        "category_brain" to "智力遊戲",
        "category_brain_desc" to "腦力激盪與邏輯推演：數獨挑戰",
        "category_deduction" to "推理遊戲",
        "category_deduction_desc" to "解開謎題與邏輯思考 (即將推出)",
        "category_casual" to "休閒遊戲",
        "category_casual_desc" to "輕鬆解壓與趣味益智 (即將推出)",

        // Specific Games
        "game_focus_test" to "專注力測驗",
        "game_focus_test_desc" to "舒爾特方格：依序點擊 1 到 N 的數字，考驗視覺搜尋與專注速度。",
        "game_focus_train" to "專注力訓練",
        "game_focus_train_desc" to "動態舒爾特圓盤：同心圓多向動態旋轉，依序點擊 1 到 N 的數字，挑戰動態追蹤與極限專注力！",
        "game_speed_match" to "極速配對",
        "game_speed_match_desc" to "1分鐘限時配對：找出網格中唯一一組相同數字，考驗極速觀察力與反應力！",
        "game_sudoku" to "數獨",
        "game_sudoku_desc" to "填入數字與符號解開盤面，考驗邏輯思考與推演能力！",
        "game_cat_sudoku" to "貓咪數獨",
        "game_cat_sudoku_desc" to "每行、每列、每個色塊各恰好放 1 隻貓咪，且相鄰 8 格不能有其他貓！",
        "select_game_item" to "選擇遊戲項目",
        "game_coming_soon" to "即將推出",
        "select_difficulty" to "選擇難易度",
        "game_categories_label" to "遊戲類別",

        // Cat Sudoku Specifics
        "cat_sudoku_completed" to "貓咪數獨挑戰完成！",
        "cats_placed_label" to "已放置貓咪",
        "cat_rule_row_col" to "同行同列只能放 1 隻貓咪",
        "cat_rule_region" to "同色區塊只能放 1 隻貓咪",
        "cat_rule_adjacent" to "周圍 8 格不得有其他貓咪相鄰",
        "cat_control_hint" to "點擊 1 次：標記 × 排除｜點擊 2 次：放貓 🐱｜點擊 3 次：清除",
        "auto_x_on_cat" to "放置貓咪時自動填叉",
        "clear_board" to "清空盤面",
        "check_rules" to "規則說明",

        // Sudoku Specifics
        "pencil_mode" to "筆記",
        "pencil_mode_on" to "筆記：開啟",
        "pencil_mode_off" to "筆記：關閉",
        "erase" to "擦除",
        "undo" to "復原",
        "mistakes_label" to "錯誤次數",
        "sudoku_completed" to "數獨挑戰完成！",

        // Speed Match Specifics
        "speed_match_start" to "開始",
        "speed_match_time_up" to "時間到！成績結算",
        "cleared_rounds" to "成功配對次數",
        "wrong_taps" to "點錯次數",
        "accuracy_rate" to "點擊正確率",
        "score_label" to "配對次數",

        // Difficulties
        "diff_beginner" to "初級 (3x3)",
        "diff_intermediate" to "中級 (4x4)",
        "diff_advanced" to "高級 (5x5)",
        "diff_hard" to "困難級 (6x6)",
        "diff_hell" to "地獄級 (7x7)",
        "diff_epic" to "史詩級 (8x8)",

        "diff_name_beginner" to "初級",
        "diff_name_intermediate" to "中級",
        "diff_name_advanced" to "高級",
        "diff_name_hard" to "困難級",
        "diff_name_hell" to "地獄級",
        "diff_name_epic" to "史詩級",

        // Game Screen
        "timer_label" to "計時",
        "target_label" to "下個數字",
        "start_game" to "開始測驗",
        "restart_game" to "重新開始",
        "game_ready_hint" to "按下「開始測驗」後，數字將隨機分佈並開始計時",
        "leaderboard_button" to "排行榜",

        // Leaderboard
        "leaderboard_title" to "排行榜 Top 10",
        "rank" to "排名",
        "player" to "玩家",
        "time" to "時間",
        "date" to "日期",
        "no_records" to "尚無紀錄，快來搶下第 1 名！",
        "clear_records" to "清除紀錄",

        // Complete Dialog
        "game_completed" to "測驗完成！",
        "your_time" to "完成時間",
        "focus_rating_fast" to "⚡ 神速專注！眼疾手快！",
        "focus_rating_great" to "🌟 表現優異！專注力極佳！",
        "focus_rating_good" to "👍 順利完成，繼續挑戰更高難度！",
        "score_saved" to "已自動更新至排行榜！",
        "play_again" to "再試一次",
        "back_to_menu" to "返回選單"
    )

    private val enMap = mapOf(
        "app_title" to "",
        "app_subtitle" to "",
        "player_name_label" to "Player Name",
        "change_name_button" to "Change Name",
        "input_name_title" to "Enter Player Name",
        "input_name_hint" to "Enter name for leaderboard records",
        "confirm" to "Confirm",
        "cancel" to "Cancel",
        "settings_title" to "Settings",
        "settings_button" to "Settings",
        "language_selector" to "Language",
        "theme_selector" to "Theme Style",
        "theme_title" to "Theme Style",
        "audio_settings_title" to "Audio & Music",
        "sfx_switch_label" to "Sound Effects",
        "bgm_switch_label" to "Background Music",
        "state_enabled" to "Enabled",
        "state_disabled" to "Disabled",
        "about_section_title" to "About App",
        "about_app_name_label" to "Application Name",
        "about_app_name_value" to "Brain Training (Left/Right Brain)",
        "about_version_label" to "Current Version",
        "about_version_value" to "v1.2.0",
        "about_dev_label" to "Development Team",
        "about_dev_value" to "AI Studio Brain Lab",
        "about_desc_label" to "Design Concept",
        "about_desc_value" to "Designed to stimulate left and right cognitive potential, integrating Schulte Focus Grids, Speed Match, Classic Sudoku, and Cat Sudoku.",
        "theme_snow_white" to "Snow White (Default)",
        "theme_dark" to "Dark Style",
        "theme_mechanical" to "Mechanical",
        "theme_cute" to "Cute Style",
        "theme_sunny" to "Sunny Style",
        "theme_corporate" to "Corporate",
        "theme_casual" to "Casual Style",

        // Categories
        "category_test" to "Test Games",
        "category_test_desc" to "Test reaction time & visual focus",
        "category_brain" to "Brain Games",
        "category_brain_desc" to "Logic teasers & mental math: Sudoku challenge",
        "category_deduction" to "Deduction Games",
        "category_deduction_desc" to "Puzzles & reasoning (Coming Soon)",
        "category_casual" to "Casual Games",
        "category_casual_desc" to "Relaxing mini arcade (Coming Soon)",

        // Specific Games
        "game_focus_test" to "Focus Test",
        "game_focus_test_desc" to "Schulte Grid: Tap numbers 1 to N sequentially as fast as possible.",
        "game_focus_train" to "Focus Train",
        "game_focus_train_desc" to "Dynamic Schulte Wheel: Rotating concentric rings. Tap numbers 1 to N sequentially in motion!",
        "game_speed_match" to "Speed Match",
        "game_speed_match_desc" to "1-Min Match: Find the only pair of matching numbers on the grid as fast as possible!",
        "game_sudoku" to "Sudoku",
        "game_sudoku_desc" to "Fill numbers & symbols into grid following classic Sudoku rules!",
        "game_cat_sudoku" to "Cat Sudoku",
        "game_cat_sudoku_desc" to "Place 1 cat per row, column & color region with no adjacent cats in 8 directions!",
        "select_game_item" to "Select Game",
        "game_coming_soon" to "Coming Soon",
        "select_difficulty" to "Select Difficulty",
        "game_categories_label" to "Game Categories",

        // Cat Sudoku Specifics
        "cat_sudoku_completed" to "Cat Sudoku Completed!",
        "cats_placed_label" to "Cats Placed",
        "cat_rule_row_col" to "1 cat per row & column",
        "cat_rule_region" to "1 cat per color region",
        "cat_rule_adjacent" to "No adjacent cats in 8 directions",
        "cat_control_hint" to "Tap 1: Mark ×｜Tap 2: Place Cat 🐱｜Tap 3: Clear",
        "auto_x_on_cat" to "Auto-fill × when placing cat",
        "clear_board" to "Clear Board",
        "check_rules" to "Rules",

        // Sudoku Specifics
        "pencil_mode" to "Pencil",
        "pencil_mode_on" to "Pencil: ON",
        "pencil_mode_off" to "Pencil: OFF",
        "erase" to "Erase",
        "undo" to "Undo",
        "mistakes_label" to "Mistakes",
        "sudoku_completed" to "Sudoku Completed!",

        // Speed Match Specifics
        "speed_match_start" to "Start",
        "speed_match_time_up" to "Time's Up! Settlement",
        "cleared_rounds" to "Successful Matches",
        "wrong_taps" to "Wrong Taps",
        "accuracy_rate" to "Accuracy",
        "score_label" to "Matches",

        // Difficulties
        "diff_beginner" to "Beginner (3x3)",
        "diff_intermediate" to "Intermediate (4x4)",
        "diff_advanced" to "Advanced (5x5)",
        "diff_hard" to "Hard (6x6)",
        "diff_hell" to "Hell (7x7)",
        "diff_epic" to "Epic (8x8)",

        "diff_name_beginner" to "Beginner",
        "diff_name_intermediate" to "Intermediate",
        "diff_name_advanced" to "Advanced",
        "diff_name_hard" to "Hard",
        "diff_name_hell" to "Hell",
        "diff_name_epic" to "Epic",

        // Game Screen
        "timer_label" to "Time",
        "target_label" to "Next Number",
        "start_game" to "Start Test",
        "restart_game" to "Restart",
        "game_ready_hint" to "Tap 'Start Test' to randomize grid & begin timer",
        "leaderboard_button" to "Leaderboard",

        // Leaderboard
        "leaderboard_title" to "Leaderboard Top 10",
        "rank" to "Rank",
        "player" to "Player",
        "time" to "Time",
        "date" to "Date",
        "no_records" to "No records yet. Be the first!",
        "clear_records" to "Clear Records",

        // Complete Dialog
        "game_completed" to "Test Completed!",
        "your_time" to "Your Time",
        "focus_rating_fast" to "⚡ Blazing Speed! Exceptional Focus!",
        "focus_rating_great" to "🌟 Great Job! Sharp Focus!",
        "focus_rating_good" to "👍 Well Done! Try Harder Levels!",
        "score_saved" to "Score saved to Leaderboard!",
        "play_again" to "Play Again",
        "back_to_menu" to "Back to Menu"
    )
}
