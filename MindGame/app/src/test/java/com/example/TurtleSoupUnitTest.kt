package com.example

import com.example.data.model.TurtleSoupPuzzle
import com.example.game.turtlesoup.TurtleSoupRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TurtleSoupUnitTest {

    @Test
    fun testPuzzlesJsonParsing() {
        val jsonFile = File("src/main/assets/puzzles.json")
        assertTrue("puzzles.json should exist in assets", jsonFile.exists())

        val jsonString = jsonFile.readText()
        assertTrue("puzzles.json should not be empty", jsonString.isNotEmpty())

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val type = Types.newParameterizedType(List::class.java, TurtleSoupPuzzle::class.java)
        val adapter = moshi.adapter<List<TurtleSoupPuzzle>>(type)
        val puzzles = adapter.fromJson(jsonString)

        assertNotNull("Puzzles list should not be null", puzzles)
        assertTrue("Puzzles list should contain at least 1 puzzle", (puzzles?.size ?: 0) > 0)

        puzzles?.forEachIndexed { index, puzzle ->
            assertNotNull("Puzzle $index ID should not be null", puzzle.id)
            assertTrue("Puzzle $index ID should not be empty", puzzle.id.isNotEmpty())
            assertTrue("Puzzle $index should have questions", puzzle.questions.isNotEmpty())
            assertTrue("Puzzle $index should have slots", puzzle.slotDeduction.slots.isNotEmpty())
            
            // 驗證 Slot 正確答案 index 必須在 options 範圍內
            puzzle.slotDeduction.slots.forEachIndexed { sIdx, slot ->
                val zhCount = slot.options.zhTW.size
                val enCount = slot.options.en.size
                val correct = slot.correctIndex
                assertTrue(
                    "Puzzle ${puzzle.id} slot $sIdx correct_index $correct should be < zh options size $zhCount",
                    correct in 0 until zhCount
                )
                assertTrue(
                    "Puzzle ${puzzle.id} slot $sIdx correct_index $correct should be < en options size $enCount",
                    correct in 0 until enCount
                )
            }
        }
    }

    @Test
    fun testCalculateScoreAndStars_NormalEasy() {
        // Easy: Base 1000, 剩餘 6 次機會, 消耗 2 次, 用時 10 秒
        val (score, stars) = TurtleSoupRepository.calculateScoreAndStars(
            difficulty = "Easy",
            usedChances = 2,
            remainingChances = 6,
            elapsedSeconds = 10,
            usedAdReward = false
        )

        // 1000 + (6 * 100) - (10 * 2) = 1580
        assertEquals(1580, score)
        // usedChances <= 2 -> 3 stars
        assertEquals(3, stars)
    }

    @Test
    fun testCalculateScoreAndStars_NormalMedium() {
        // Medium: Base 2000, 剩餘 4 次機會, 消耗 4 次, 用時 30 秒
        val (score, stars) = TurtleSoupRepository.calculateScoreAndStars(
            difficulty = "Medium",
            usedChances = 4,
            remainingChances = 4,
            elapsedSeconds = 30,
            usedAdReward = false
        )

        // 2000 + (4 * 100) - (30 * 2) = 2340
        assertEquals(2340, score)
        // usedChances = 4 (3..5) -> 2 stars
        assertEquals(2, stars)
    }

    @Test
    fun testCalculateScoreAndStars_NormalHard() {
        // Hard: Base 3000, 剩餘 0 次機會, 消耗 6 次, 用時 45 秒
        val (score, stars) = TurtleSoupRepository.calculateScoreAndStars(
            difficulty = "Hard",
            usedChances = 6,
            remainingChances = 0,
            elapsedSeconds = 45,
            usedAdReward = false
        )

        // 3000 + 0 - (45 * 2) = 2910
        assertEquals(2910, score)
        // usedChances >= 6 -> 1 star
        assertEquals(1, stars)
    }

    @Test
    fun testCalculateScoreAndStars_AdReward() {
        // 廣告回補通關：分數固定為 BaseScore 的 50%，星級固定為 0
        val (scoreEasy, starsEasy) = TurtleSoupRepository.calculateScoreAndStars(
            difficulty = "Easy",
            usedChances = 3,
            remainingChances = 2,
            elapsedSeconds = 20,
            usedAdReward = true
        )
        assertEquals(500, scoreEasy)
        assertEquals(0, starsEasy)

        val (scoreMedium, starsMedium) = TurtleSoupRepository.calculateScoreAndStars(
            difficulty = "Medium",
            usedChances = 5,
            remainingChances = 1,
            elapsedSeconds = 40,
            usedAdReward = true
        )
        assertEquals(1000, scoreMedium)
        assertEquals(0, starsMedium)

        val (scoreHard, starsHard) = TurtleSoupRepository.calculateScoreAndStars(
            difficulty = "Hard",
            usedChances = 7,
            remainingChances = 2,
            elapsedSeconds = 50,
            usedAdReward = true
        )
        assertEquals(1500, scoreHard)
        assertEquals(0, starsHard)
    }

    @Test
    fun testInitialChancesByDifficulty() {
        assertEquals(4, TurtleSoupRepository.getInitialChances("Easy"))
        assertEquals(4, TurtleSoupRepository.getInitialChances("BEGINNER"))
        assertEquals(3, TurtleSoupRepository.getInitialChances("Medium"))
        assertEquals(3, TurtleSoupRepository.getInitialChances("Hard"))
    }

    @Test
    fun testMultiDimensionalInvestigationEngine() {
        val jsonFile = File("src/main/assets/puzzles.json")
        val jsonString = jsonFile.readText()
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val type = Types.newParameterizedType(List::class.java, TurtleSoupPuzzle::class.java)
        val adapter = moshi.adapter<List<TurtleSoupPuzzle>>(type)
        val puzzles = adapter.fromJson(jsonString)!!
        
        assertEquals("Total puzzles count should be exactly 100", 100, puzzles.size)

        // 驗證全部 100 道題目皆具備高品質 4 大維度且切合情境 (每維度 5~8 個優質選項，且無長句答案劇透)
        puzzles.forEachIndexed { idx, puzzle ->
            val dims = TurtleSoupRepository.getEffectiveDimensions(puzzle, com.example.data.model.AppLanguage.TRADITIONAL_CHINESE)
            assertEquals("Puzzle ${puzzle.id} (#${idx+1}) must have exactly 4 dimensions", 4, dims.size)
            
            val dimIds = dims.map { it.id }.toSet()
            assertTrue("Puzzle ${puzzle.id} must have character dimension", dimIds.contains("character"))
            assertTrue("Puzzle ${puzzle.id} must have item dimension", dimIds.contains("item"))
            assertTrue("Puzzle ${puzzle.id} must have event dimension", dimIds.contains("event"))
            assertTrue("Puzzle ${puzzle.id} must have scene dimension", dimIds.contains("scene"))

            dims.forEach { dim ->
                val zhOpts = dim.options.zhTW
                val enOpts = dim.options.en
                assertTrue("Puzzle ${puzzle.id} dimension ${dim.id} zh options >= 5", zhOpts.size >= 5)
                assertTrue("Puzzle ${puzzle.id} dimension ${dim.id} en options >= 5", enOpts.size >= 5)
                assertEquals("Puzzle ${puzzle.id} dimension ${dim.id} zh/en options count must match", zhOpts.size, enOpts.size)
                
                // 驗證選項皆為詞彙短語 (長度不超過 30 字，避免長句答案)
                zhOpts.forEach { opt ->
                    assertTrue("Option '$opt' in puzzle ${puzzle.id} should be concise phrase", opt.length <= 30)
                }
            }
        }

        // 測試第 1 題 (soup_001) - 驗證客觀提問組合能命中線索
        val firstPuzzle = puzzles.first()
        val coreQuery = mapOf("character" to "開車男子", "event" to "製造不在場證明")
        val coreLog = TurtleSoupRepository.evaluateQuery(firstPuzzle, coreQuery, com.example.data.model.AppLanguage.TRADITIONAL_CHINESE)
        assertTrue("Should match core clue or YES", coreLog.isCore || coreLog.answer == com.example.data.model.QuestionAnswer.YES)

        // 測試第 2 題 (soup_002) - 驗證去劇透：人物為「要水的男子」，嚴禁「打嗝男子」
        val secondPuzzle = puzzles[1]
        val dims2 = TurtleSoupRepository.getEffectiveDimensions(secondPuzzle, com.example.data.model.AppLanguage.TRADITIONAL_CHINESE)
        val charDim2 = dims2.find { it.id == "character" }!!
        assertTrue("soup_002 character dimension should contain 要水的男子", charDim2.options.zhTW.contains("要水的男子"))
        assertTrue("soup_002 character dimension should NOT contain 打嗝男子", !charDim2.options.zhTW.contains("打嗝男子"))
        assertTrue("soup_002 character dimension should contain 酒吧酒保", charDim2.options.zhTW.contains("酒吧酒保"))

        // 測試第 4 題 (soup_004)
        val fourthPuzzle = puzzles[3]
        assertEquals("soup_004 title should match original Docs/puzzles.json", "雨中的禿頭男子", fourthPuzzle.title.zhTW)

        // 測試煙霧彈無關組合
        val smokeQuery = mapOf("character" to "路過司機", "item" to "行駛中的汽車", "event" to "發生嚴重車禍")
        val smokeLog = TurtleSoupRepository.evaluateQuery(firstPuzzle, smokeQuery, com.example.data.model.AppLanguage.TRADITIONAL_CHINESE)
        assertNotNull(smokeLog.detail)
    }
}

