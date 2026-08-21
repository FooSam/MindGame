# 左右腦鍛鍊 (MindGame / Brain Training) 🧠✨

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4.svg?logo=android&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Android SDK](https://img.shields.io/badge/Android%20SDK-24%20--%2036-3DDC84.svg?logo=android&logoColor=white)](https://developer.android.com/)
[![Room Database](https://img.shields.io/badge/SQLite-Room%20DB-FFA000.svg?logo=sqlite&logoColor=white)](https://developer.android.com/training/data-storage/room)
[![License](https://img.shields.io/badge/License-Proprietary-blue.svg)]()

> **「左右腦鍛鍊」**是一款專為全方位激發專注力、反應速度、邏輯推理與空間感知所設計的現代化 Android 益智應用程式。全 UI 採用最新 **Jetpack Compose (Material 3)** 打造，支援流暢動效、多種難易度、本地即時排行榜、中英雙語切換與 7 套精緻視覺主題。

---

## 🎮 核心遊戲訓練模組 (Game Modes)

| 遊戲項目 | 訓練領域 | 核心玩法特色 |
| :--- | :--- | :--- |
| 🎯 **專注力訓練 (Focus Train)** | **動態追蹤與視野辨識** | **創新動態舒爾特圓盤**！中心圓靜止，外圍 1~3 圈同心圓以順時針/逆時針動態旋轉（數字 1~69），文字維持正立向上，挑戰動態視覺專注力極限。 |
| ⏱️ **專注力測驗 (Focus Test)** | **視覺搜尋與抗干擾** | 經典舒爾特方格（Schulte Grid），提供 3x3 (初級 9格) 至 8x8 (史詩級 64格) 難度，依序點擊數字。 |
| ⚡ **極速配對 (Speed Match)** | **反應速度與瞬時記憶** | 1 分鐘限時挑戰！在隨機網格中找出唯一一組相同的數字組合，連續配對衝擊最高分。 |
| 🧩 **經典數獨 (Classic Sudoku)** | **邏輯思維與矩陣推演** | 提供 4x4 (入門)、6x6 (進階)、9x9 (標準三級難度) 與 12x12 (史詩級) 棋盤，支援筆記模式、擦除、復原與衝突高亮。 |
| 🐱 **貓咪數獨 (Cat Sudoku)** | **空間感知與幾何推理** | 創新色塊排貓益智玩法！每行、每列、每個同色區塊各恰好放置 1 隻貓咪，且相鄰 8 格不得有其他貓咪。 |

---

## 🎨 7 大主題風格與雙語支援 (Themes & Localization)

- ❄️ **雪白風格 (Snow White)** (預設明亮清爽)
- 🌙 **暗黑風格 (Dark Mode)** (極客沉浸感，省電舒適)
- ⚙️ **機械風格 (Mechanical)** (科技硬派金屬感)
- 🌸 **可愛風格 (Cute Pastel)** (溫馨柔和馬卡龍配色)
- ☀️ **陽光風格 (Sunny Vibrant)** (活力飽滿橙黃色調)
- 💼 **上班族風格 (Corporate)** (優雅專業商務藍調)
- ☕ **休閒風格 (Casual Earth)** (溫暖自然森系大地風)
- 🌐 **完整多語系**：繁體中文 (Traditional Chinese)、English。

---

## 🛠️ 技術架構與技術棧 (Tech Stack)

- **開發語言**: Kotlin 2.0+
- **UI 框架**: Jetpack Compose (Material 3) + Canvas 極座標動態旋轉幾何運算
- **狀態管理**: Kotlin Coroutines & StateFlow (響應式單向數據流架構)
- **本地資料庫**: Android Room (SQLite)，離線儲存各難度排行榜成績
- **音效引擎**: Android AudioTrack (自主 PCM 弦波即時合成，零額外大型音訊檔負擔)
- **安全加固**: ProGuard / R8 程式碼混淆與無用資源壓縮
- **單元測試**: Robolectric, JUnit4, Roborazzi

---

## 🚀 本地編譯與建置 (Build & Run)

### 環境需求
- **JDK**: OpenJDK 17
- **Gradle**: 9.3.1+
- **Android SDK**: API 36 (minSdk 24, targetSdk 36)

### 編譯指令
```bash
# 執行所有自動化單元測試
gradle test

# 編譯 Debug APK
gradle :app:assembleDebug

# 正式簽署與 R8 加固 Release APK 打包
gradle :app:assembleRelease

# 正式產出 Google Play 發布專用 Android App Bundle (.aab)
gradle :app:bundleRelease
```

---

## 🔒 隱私與資料安全 (Privacy & Data Safety)
- **零個人資料收集**：本程式為單機離線遊戲，**不收集、不上傳、不共享**任何使用者的個人身分識別資訊 (PII)。
- **隱私權政策完整網址**：請參閱 [PRIVACY_POLICY.md](../PRIVACY_POLICY.md) 或線上網頁 `https://<your-username>.github.io/<repo-name>/`。

---

## 📄 版權聲明 (License)
Copyright &copy; 2026 **Ordinary People Studio**. All rights reserved.
