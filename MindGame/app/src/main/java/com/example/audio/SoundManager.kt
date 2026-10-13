package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * 現代化高品質遊戲音效與背景音樂管理引擎 (方案 A：SoundPool 母帶資源架構)
 *
 * 核心特色：
 * 1. Android SoundPool 16 路低延遲混音 (0ms 極速觸發)
 * 2. 母帶級 44.1kHz 立體聲手機喇叭專屬 EQ 調校 (Clean Midrange, Low-Cut 120Hz, Soft High Rolloff)
 * 3. 防疲勞微音高隨機化 (Pitch Micro-variation) 徹底消除機關槍效應
 * 4. 大調五聲音階 (Pentatonic Ladder) 連擊多巴胺反饋
 */
object SoundManager {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var appContext: Context? = null
    private var mediaPlayer: MediaPlayer? = null
    private var soundPool: SoundPool? = null

    // resId -> soundPoolId 快取表
    private val soundIdMap = ConcurrentHashMap<Int, Int>()

    var isSfxEnabled: Boolean = true
    var isBgmEnabled: Boolean = false
    private var isAppInForeground: Boolean = true

    private var currentTrackResId: Int? = null

    val bgmTracks = listOf(
        R.raw.bgm_01_little_idea,
        R.raw.bgm_02_sunny,
        R.raw.bgm_03_light_playful,
        R.raw.bgm_04_refreshing,
        R.raw.bgm_05_caves_of_dawn,
        R.raw.bgm_06_battle_dragons,
        R.raw.bgm_07_melody_nature,
        R.raw.bgm_08_lofi_study,
        R.raw.bgm_09_clay_zen,
        R.raw.bgm_10_peaceful_piano,
        R.raw.bgm_11_ceramic_breeze
    )

    private val allSfxResources = listOf(
        R.raw.sfx_ui_tap_default,
        R.raw.sfx_ui_tap_confirm,
        R.raw.sfx_ui_tap_back,
        R.raw.sfx_ui_modal_open,
        R.raw.sfx_ui_modal_close,
        R.raw.sfx_ui_toggle,
        R.raw.sfx_ui_reward_get,
        R.raw.sfx_ui_level_up,
        R.raw.sfx_act_step,
        R.raw.sfx_act_attack_swish,
        R.raw.sfx_act_hit_impact,
        R.raw.sfx_act_hit_crit,
        R.raw.sfx_act_skill_cast,
        R.raw.sfx_act_hurt_block,
        R.raw.sfx_env_tile_drop,
        R.raw.sfx_env_tile_slide,
        R.raw.sfx_env_crystal_ping,
        R.raw.sfx_env_hint_ping,
        R.raw.sfx_fx_coin,
        R.raw.sfx_fx_juicy_pop,
        R.raw.sfx_fx_bomb_boom,
        R.raw.sfx_fx_fever_blast,
        R.raw.sfx_fx_freeze_chime,
        R.raw.sfx_fx_warning_miss
    )

    fun initialize(context: Context) {
        appContext = context.applicationContext
        initSoundPool(context)
    }

    private fun initSoundPool(context: Context) {
        try {
            if (soundPool == null) {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                soundPool = SoundPool.Builder()
                    .setMaxStreams(16)
                    .setAudioAttributes(audioAttributes)
                    .build()
            }

            val sp = soundPool ?: return
            // 背景預加載所有音效至記憶體
            scope.launch {
                for (resId in allSfxResources) {
                    if (!soundIdMap.containsKey(resId)) {
                        try {
                            val sId = sp.load(context, resId, 1)
                            soundIdMap[resId] = sId
                        } catch (_: Exception) {}
                    }
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * 核心 SoundPool 播放方法，支援音量平衡、基準音高與防疲勞微幅隨機化
     */
    fun playSfx(
        resId: Int,
        volume: Float = 1.0f,
        baseRate: Float = 1.0f,
        randomizePitch: Boolean = false
    ) {
        if (!isSfxEnabled) return
        val sp = soundPool ?: return
        val soundId = soundIdMap[resId] ?: return

        val finalRate = if (randomizePitch) {
            baseRate * (1.0f + (Random.nextFloat() * 0.08f - 0.04f)) // ±4% Pitch Micro-variation
        } else {
            baseRate
        }.coerceIn(0.5f, 2.0f)

        val finalVolume = volume.coerceIn(0.0f, 1.0f)
        try {
            sp.play(soundId, finalVolume, finalVolume, 1, 0, finalRate)
        } catch (_: Exception) {}
    }

    // ==========================================
    // UI 介面音效組 (Tactile & Crisp)
    // ==========================================

    /** 清脆按鈕點擊 (Switch Joy-Con Snap 質感) */
    fun playClick() {
        playSfx(R.raw.sfx_ui_tap_default, volume = 0.95f, randomizePitch = true)
    }

    /** Nintendo Switch 插入卡榫嵌入音效 */
    fun playSwitchSnap() {
        playSfx(R.raw.sfx_ui_tap_default, volume = 0.90f, randomizePitch = false)
    }

    /** 確認 / 開始按鈕 (Supercell 飽滿彈性上滑音) */
    fun playTapConfirm() {
        playSfx(R.raw.sfx_ui_tap_confirm, volume = 1.0f)
    }

    /** 返回 / 取消 (iOS 柔和阻尼返回) */
    fun playTapBack() {
        playSfx(R.raw.sfx_ui_tap_back, volume = 0.85f)
    }

    /** 彈窗視窗展開 (空氣感 Whoosh-Pop) */
    fun playModalOpen() {
        playSfx(R.raw.sfx_ui_modal_open, volume = 0.85f)
    }

    /** 彈窗視窗關閉 (俐落收合) */
    fun playModalClose() {
        playSfx(R.raw.sfx_ui_modal_close, volume = 0.80f)
    }

    /** 開關切換 (機械微動雙重嗒聲) */
    fun playToggle() {
        playSfx(R.raw.sfx_ui_toggle, volume = 0.90f)
    }

    /** 獲得獎勵 / 過關 (禮花與晶體三和弦) */
    fun playSuccess() {
        playSfx(R.raw.sfx_ui_reward_get, volume = 0.95f)
    }

    /** 勝利 / 榮譽升級大三和弦連續琶音 */
    fun playWin() {
        playSfx(R.raw.sfx_ui_level_up, volume = 1.0f)
    }

    /** 判斷失誤 / 錯誤警告 (柔和雙鋸齒防呆音，不刺耳) */
    fun playError() {
        playSfx(R.raw.sfx_fx_warning_miss, volume = 0.85f)
    }

    // ==========================================
    // 角色動作與戰鬥打擊音效組 (Action & Impact)
    // ==========================================

    /** 輕快步伐 (微幅隨機音高) */
    fun playStep() {
        playSfx(R.raw.sfx_act_step, volume = 0.70f, randomizePitch = true)
    }

    /** 水果切切樂 / 武器揮擊：柔和清脆破空刀痕聲 (Swish) */
    fun playFruitSliceSwish() {
        playSfx(R.raw.sfx_act_attack_swish, volume = 0.90f, randomizePitch = true)
    }

    /** 拳拳到肉扎實打擊命中 */
    fun playImpactHit(combo: Int = 1) {
        val comboRates = listOf(1.0f, 1.059f, 1.122f, 1.189f, 1.260f, 1.335f, 1.414f, 1.500f)
        val rate = comboRates[(combo - 1).coerceIn(0, comboRates.size - 1)]
        playSfx(R.raw.sfx_act_hit_impact, volume = 0.95f, baseRate = rate)
    }

    /** 暴擊重創金屬破裂鏗鏘 */
    fun playCriticalHit() {
        playSfx(R.raw.sfx_act_hit_crit, volume = 1.0f)
    }

    /** 技能充能釋放 / 能量脈衝 */
    fun playSkillCast() {
        playSfx(R.raw.sfx_act_skill_cast, volume = 0.90f)
    }

    /** 護盾格擋偏折 / 金屬彈刀撞擊 */
    fun playMetalClang() {
        playSfx(R.raw.sfx_act_hurt_block, volume = 0.90f, randomizePitch = true)
    }

    /** 受擊失誤 */
    fun playDamageMiss() {
        playSfx(R.raw.sfx_fx_warning_miss, volume = 0.90f)
    }

    // ==========================================
    // 環境與物理互動音效組 (Physics & Environment)
    // ==========================================

    /** 陶塊/棋子落子沉穩叩擊聲 (Ceramic Click & Tap) */
    fun playClayDrop() {
        playSfx(R.raw.sfx_env_tile_drop, volume = 0.95f, randomizePitch = true)
    }

    /** 切片工坊圓潤木質砧板打擊音 (Woodblock ASMR) */
    fun playWoodblockChop() {
        playSfx(R.raw.sfx_env_tile_drop, volume = 0.95f, randomizePitch = true)
    }

    /** 方塊滑動鎖定 / 鏡片旋轉機械卡榫聲 */
    fun playMirrorRotateSnap() {
        playSfx(R.raw.sfx_ui_tap_default, volume = 0.85f, randomizePitch = true)
    }

    fun playRotate() {
        playSfx(R.raw.sfx_env_tile_slide, volume = 0.90f, randomizePitch = true)
    }

    /** 水晶啟動 / 充能音 */
    fun playCrystalActivate() {
        playSfx(R.raw.sfx_env_crystal_ping, volume = 0.90f)
    }

    /** 溫暖靈光一閃 / 光路連接聚焦微鳴 */
    fun playLaserConnect() {
        playSfx(R.raw.sfx_env_hint_ping, volume = 0.90f)
    }

    /** 雷射迷宮全盤超載通關 */
    fun playLaserMazeClear() {
        playSfx(R.raw.sfx_ui_level_up, volume = 1.0f)
    }

    // ==========================================
    // 特殊效果與高多巴胺獎勵音組 (Climax & Euphoric FX)
    // ==========================================

    /** 金幣/香腸拾取 (清脆雙音叮噹) */
    fun playCoinOrSausage() {
        playSfx(R.raw.sfx_fx_coin, volume = 0.95f, randomizePitch = true)
    }

    /** 消除連擊大調五聲音階爬升 (Pentatonic Dopamine Ladder) */
    fun playComboChime(comboLevel: Int) {
        val pentatonicRates = listOf(1.0f, 1.122f, 1.260f, 1.498f, 1.682f, 2.0f)
        val rate = pentatonicRates[(comboLevel - 1).coerceIn(0, pentatonicRates.size - 1)]
        playSfx(R.raw.sfx_env_crystal_ping, volume = 0.95f, baseRate = rate)
    }

    /** 水果鮮嫩爆汁音 (Juicy Pop) */
    fun playFruitJuiceSplash() {
        playSfx(R.raw.sfx_fx_juicy_pop, volume = 0.95f, randomizePitch = true)
    }

    /** 炸彈/十字爆破重磅轟鳴 (帶壓限保護) */
    fun playBombExplosion() {
        playSfx(R.raw.sfx_fx_bomb_boom, volume = 1.0f)
    }

    fun playCrossBoom() {
        playSfx(R.raw.sfx_fx_bomb_boom, volume = 0.95f)
    }

    /** 寒冰晶瑩凝結脆裂顫音 */
    fun playFreezeEffect() {
        playSfx(R.raw.sfx_fx_freeze_chime, volume = 0.90f)
    }

    /** 彩虹全場引爆衝擊 */
    fun playRainbowExplosion() {
        playSfx(R.raw.sfx_ui_level_up, volume = 0.90f)
    }

    /** 狂熱模式啟動歡呼衝擊音 (Fever Fanfare) */
    fun playFeverFanfare() {
        playSfx(R.raw.sfx_fx_fever_blast, volume = 1.0f)
    }

    /** 旋轉攪拌音 */
    fun playBlenderWhir() {
        playSfx(R.raw.sfx_act_skill_cast, volume = 0.75f)
    }

    /** 彈珠碰撞 / 彈跳 */
    fun playPinballBounce() {
        playSfx(R.raw.sfx_ui_toggle, volume = 0.85f, randomizePitch = true)
    }

    fun playFlipperSnap() {
        playSfx(R.raw.sfx_ui_tap_confirm, volume = 0.90f, randomizePitch = true)
    }

    fun playSpringLaunch() {
        playSfx(R.raw.sfx_act_skill_cast, volume = 0.85f)
    }

    fun playPinDrop() {
        playSfx(R.raw.sfx_act_step, volume = 0.80f, randomizePitch = true)
    }

    /** 貓咪叫聲 (可愛音效保留) */
    fun playCatMeow() {
        playSfx(R.raw.sfx_env_hint_ping, volume = 0.85f, baseRate = 1.2f)
    }

    /** 蜈蚣竄逃滑稽滑音 */
    fun playCentipedeEscape() {
        playSfx(R.raw.sfx_env_tile_slide, volume = 0.85f, baseRate = 1.3f)
    }

    /** 低頻心跳脈衝雙擊 (咚-咚) */
    fun playHeartbeatPulse(speedMultiplier: Float = 1.0f) {
        if (!isSfxEnabled) return
        scope.launch {
            playSfx(R.raw.sfx_act_hit_impact, volume = 0.65f, baseRate = 0.7f)
            val delayInterval = (90L / speedMultiplier.coerceAtLeast(1f)).toLong()
            delay(delayInterval)
            playSfx(R.raw.sfx_act_hit_impact, volume = 0.45f, baseRate = 0.75f)
        }
    }

    // ==========================================
    // 背景音樂管理 (BGM Controller)
    // ==========================================

    fun onAppFocusChanged(isFocused: Boolean) {
        isAppInForeground = isFocused
        if (!isFocused) {
            pauseBgm()
        } else if (isBgmEnabled) {
            resumeBgm()
        }
    }

    fun pauseBgm() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (_: Exception) {}
    }

    fun resumeBgm() {
        if (!isBgmEnabled || !isAppInForeground) return
        try {
            if (mediaPlayer != null) {
                mediaPlayer?.start()
            } else {
                playRandomBgm()
            }
        } catch (_: Exception) {
            playRandomBgm()
        }
    }

    fun setBgmState(enabled: Boolean) {
        isBgmEnabled = enabled
        if (enabled) {
            resumeBgm()
        } else {
            stopBgm()
        }
    }

    fun stopBgm() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {
        } finally {
            mediaPlayer = null
        }
    }

    fun onGameSwitched() {
        if (isBgmEnabled && isAppInForeground) {
            playRandomBgm(forceNew = true)
        }
    }

    fun playRandomBgm(forceNew: Boolean = false) {
        if (!isBgmEnabled || !isAppInForeground) return
        val context = appContext ?: return
        val availableTracks = if (bgmTracks.size > 1 && currentTrackResId != null) {
            bgmTracks.filter { it != currentTrackResId }
        } else {
            bgmTracks
        }
        val nextResId = availableTracks.randomOrNull() ?: return
        playTrack(context, nextResId)
    }

    private fun playTrack(context: Context, resId: Int) {
        stopBgm()
        try {
            currentTrackResId = resId
            mediaPlayer = MediaPlayer.create(context, resId)?.apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setVolume(0.35f, 0.35f)
                isLooping = false
                setOnCompletionListener {
                    playRandomBgm(forceNew = true)
                }
                start()
            }
        } catch (_: Exception) {
            mediaPlayer = null
        }
    }
}
