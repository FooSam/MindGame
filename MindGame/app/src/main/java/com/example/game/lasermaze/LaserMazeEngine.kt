package com.example.game.lasermaze

import com.example.data.model.GameDifficulty
import com.example.data.model.LaserBoardState
import com.example.data.model.LaserDirection
import com.example.data.model.LaserLevelConfig
import com.example.data.model.LaserPiece
import com.example.data.model.LaserRaySegment
import com.example.data.model.OpticalPieceType
import java.util.LinkedList
import java.util.Queue
import kotlin.math.abs
import kotlin.random.Random

/**
 * 雷射迷宮光線追蹤核心引擎 (即時計算射線折射、分流、多波長濾光、稜鏡混光合成、時空傳送與共振照亮)
 */
object LaserRaycaster {

    // 核心光譜色常數
    const val COLOR_CYAN: Long = 0xFF00E5FF
    const val COLOR_RED: Long = 0xFFFF1744
    const val COLOR_GREEN: Long = 0xFF00E676
    const val COLOR_BLUE: Long = 0xFF2979FF
    const val COLOR_YELLOW: Long = 0xFFFFEA00
    const val COLOR_MAGENTA: Long = 0xFFFF007F
    const val COLOR_WHITE: Long = 0xFFFFFFFF

    private data class Ray(
        val row: Int,
        val col: Int,
        val dir: LaserDirection,
        val colorHex: Long
    )

    /**
     * 執行完整光線追蹤
     */
    fun trace(
        gridSize: Int,
        grid: List<List<LaserPiece?>>,
        emitterRow: Int,
        emitterCol: Int,
        emitterDir: LaserDirection,
        receivers: List<Pair<Int, Int>>,
        moveCount: Int = 0,
        receiverColors: Map<Pair<Int, Int>, Long> = emptyMap()
    ): LaserBoardState {
        val segments = mutableListOf<LaserRaySegment>()
        val litReceiverIndices = mutableSetOf<Int>()
        val visited = mutableSetOf<Triple<Int, Int, LaserDirection>>()

        val queue: Queue<Ray> = LinkedList()
        queue.add(Ray(emitterRow, emitterCol, emitterDir, COLOR_CYAN))

        // 建立傳送門索引表 (支援快速尋找對稱傳送門)
        val portals = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                if (grid[r][c]?.type == OpticalPieceType.PORTAL) {
                    portals.add(Pair(r, c))
                }
            }
        }

        while (queue.isNotEmpty()) {
            val ray = queue.poll() ?: continue

            var currR = ray.row
            var currC = ray.col
            val dir = ray.dir
            val color = ray.colorHex

            val nextR = currR + dir.dy
            val nextC = currC + dir.dx

            // 邊界檢查
            if (nextR !in 0 until gridSize || nextC !in 0 until gridSize) {
                segments.add(LaserRaySegment(currR, currC, nextR, nextC, dir, color))
                continue
            }

            val visitKey = Triple(nextR, nextC, dir)
            if (visitKey in visited) {
                // 迴圈終止
                segments.add(LaserRaySegment(currR, currC, nextR, nextC, dir, color))
                continue
            }
            visited.add(visitKey)

            segments.add(LaserRaySegment(currR, currC, nextR, nextC, dir, color))

            val piece = grid[nextR][nextC]

            // 1. 檢查是否抵達接收水晶
            val receiverIndex = receivers.indexOfFirst { it.first == nextR && it.second == nextC }
            if (receiverIndex != -1) {
                val requiredColor = piece?.requiredColorHex ?: receiverColors[Pair(nextR, nextC)]
                val isResonant = if (requiredColor == null) {
                    true
                } else {
                    isColorResonant(color, requiredColor)
                }
                if (isResonant) {
                    litReceiverIndices.add(receiverIndex)
                }
                // 光線被水晶吸收並充能，不再前進
                continue
            }

            if (piece == null || piece.type == OpticalPieceType.EMPTY) {
                // 空格直行
                queue.add(Ray(nextR, nextC, dir, color))
                continue
            }

            // 依光學元件類型處理物理折射、分流、濾光、傳送
            when (piece.type) {
                OpticalPieceType.EMITTER -> {
                    // 打回發射塔，吸收終止
                }

                OpticalPieceType.RECEIVER -> {
                    // 已在上方處理
                }

                OpticalPieceType.MIRROR_SINGLE -> {
                    // 單面 45° 鍍銀反光鏡 (背面吸收)
                    val outDir = calculateSingleMirrorReflection(dir, piece.rotation)
                    if (outDir != null) {
                        queue.add(Ray(nextR, nextC, outDir, color))
                    }
                }

                OpticalPieceType.MIRROR_DOUBLE -> {
                    // 雙面反光鏡 (兩側皆可 90° 折射)
                    val outDir = calculateDoubleMirrorReflection(dir, piece.rotation)
                    queue.add(Ray(nextR, nextC, outDir, color))
                }

                OpticalPieceType.BEAM_SPLITTER -> {
                    // 十字分光鏡：直穿 + 折射
                    queue.add(Ray(nextR, nextC, dir, color))
                    val splitDir = calculateDoubleMirrorReflection(dir, piece.rotation)
                    queue.add(Ray(nextR, nextC, splitDir, color))
                }

                OpticalPieceType.TUNNEL -> {
                    // 3D 交叉隧道：無阻直穿
                    queue.add(Ray(nextR, nextC, dir, color))
                }

                OpticalPieceType.OBSTACLE_ABSORBER -> {
                    // 碳素吸光石：吸收中斷
                }

                OpticalPieceType.OBSTACLE_FIXED_MIRROR -> {
                    // 固定折射斜壁
                    val outDir = calculateDoubleMirrorReflection(dir, piece.rotation)
                    queue.add(Ray(nextR, nextC, outDir, color))
                }

                OpticalPieceType.OBSTACLE_TINTED_GLASS -> {
                    // 半透晶石：直穿
                    queue.add(Ray(nextR, nextC, dir, color))
                }

                // 【三原色濾光鏡】
                OpticalPieceType.FILTER_RED -> {
                    val filteredColor = applyColorFilter(color, OpticalPieceType.FILTER_RED)
                    if (filteredColor != null) {
                        queue.add(Ray(nextR, nextC, dir, filteredColor))
                    }
                }

                OpticalPieceType.FILTER_GREEN -> {
                    val filteredColor = applyColorFilter(color, OpticalPieceType.FILTER_GREEN)
                    if (filteredColor != null) {
                        queue.add(Ray(nextR, nextC, dir, filteredColor))
                    }
                }

                OpticalPieceType.FILTER_BLUE -> {
                    val filteredColor = applyColorFilter(color, OpticalPieceType.FILTER_BLUE)
                    if (filteredColor != null) {
                        queue.add(Ray(nextR, nextC, dir, filteredColor))
                    }
                }

                // 【稜鏡混光合成器】
                OpticalPieceType.PRISM_SYNTHESIZER -> {
                    // 直穿並激發彩虹稜鏡合成
                    val synthColor = synthesizeColor(color, piece.rotation)
                    queue.add(Ray(nextR, nextC, dir, synthColor))
                    val splitDir = calculateDoubleMirrorReflection(dir, piece.rotation)
                    queue.add(Ray(nextR, nextC, splitDir, synthColor))
                }

                // 【時空傳送門】
                OpticalPieceType.PORTAL -> {
                    val targetPortal = piece.portalTarget ?: portals.firstOrNull { it != Pair(nextR, nextC) }
                    if (targetPortal != null && targetPortal != Pair(nextR, nextC)) {
                        // 躍遷射出
                        queue.add(Ray(targetPortal.first, targetPortal.second, dir, color))
                    }
                }

                OpticalPieceType.EMPTY -> {
                    queue.add(Ray(nextR, nextC, dir, color))
                }
            }
        }

        val isCleared = receivers.isNotEmpty() && litReceiverIndices.size == receivers.size

        return LaserBoardState(
            gridSize = gridSize,
            grid = grid,
            emitterRow = emitterRow,
            emitterCol = emitterCol,
            emitterDir = emitterDir,
            receivers = receivers,
            segments = segments,
            litReceiverIndices = litReceiverIndices,
            isCleared = isCleared,
            moveCount = moveCount,
            receiverColors = receiverColors
        )
    }

    /**
     * 三原色濾光運算
     */
    fun applyColorFilter(incomingColor: Long, filterType: OpticalPieceType): Long? {
        return when (filterType) {
            OpticalPieceType.FILTER_RED -> {
                when (incomingColor) {
                    COLOR_RED, COLOR_YELLOW, COLOR_MAGENTA, COLOR_WHITE, COLOR_CYAN -> COLOR_RED
                    else -> null // 吸收
                }
            }
            OpticalPieceType.FILTER_GREEN -> {
                when (incomingColor) {
                    COLOR_GREEN, COLOR_YELLOW, COLOR_CYAN, COLOR_WHITE -> COLOR_GREEN
                    else -> null
                }
            }
            OpticalPieceType.FILTER_BLUE -> {
                when (incomingColor) {
                    COLOR_BLUE, COLOR_CYAN, COLOR_MAGENTA, COLOR_WHITE -> COLOR_BLUE
                    else -> null
                }
            }
            else -> incomingColor
        }
    }

    /**
     * 稜鏡混光合成與波長演算法
     */
    fun synthesizeColor(incomingColor: Long, rotation: Int): Long {
        return when (incomingColor) {
            COLOR_RED -> if (rotation % 2 == 0) COLOR_YELLOW else COLOR_MAGENTA
            COLOR_GREEN -> if (rotation % 2 == 0) COLOR_YELLOW else COLOR_CYAN
            COLOR_BLUE -> if (rotation % 2 == 0) COLOR_CYAN else COLOR_MAGENTA
            COLOR_CYAN -> if (rotation % 2 == 0) COLOR_WHITE else COLOR_CYAN
            else -> COLOR_WHITE
        }
    }

    /**
     * 檢查射線色彩是否滿足接收水晶之共振波長
     */
    fun isColorResonant(rayColor: Long, requiredColor: Long): Boolean {
        if (rayColor == requiredColor) return true
        if (rayColor == COLOR_WHITE) return true
        // 複合光包容共振判定
        if (rayColor == COLOR_CYAN && (requiredColor == COLOR_CYAN || requiredColor == COLOR_BLUE || requiredColor == COLOR_GREEN)) return true
        if (rayColor == COLOR_YELLOW && (requiredColor == COLOR_YELLOW || requiredColor == COLOR_RED || requiredColor == COLOR_GREEN)) return true
        if (rayColor == COLOR_MAGENTA && (requiredColor == COLOR_MAGENTA || requiredColor == COLOR_RED || requiredColor == COLOR_BLUE)) return true
        return false
    }

    /**
     * 計算單面 45° 反光鏡出射方向
     */
    fun calculateSingleMirrorReflection(inDir: LaserDirection, rotation: Int): LaserDirection? {
        val rot = (rotation % 4 + 4) % 4
        val localIn = inDir.rotate(-rot)
        val localOut = when (localIn) {
            LaserDirection.LEFT -> LaserDirection.DOWN
            LaserDirection.UP -> LaserDirection.RIGHT
            else -> null
        } ?: return null
        return localOut.rotate(rot)
    }

    /**
     * 計算雙面反光鏡出射方向
     */
    fun calculateDoubleMirrorReflection(inDir: LaserDirection, rotation: Int): LaserDirection {
        val isSlash = (rotation % 2 == 0)
        return if (isSlash) {
            when (inDir) {
                LaserDirection.LEFT -> LaserDirection.DOWN
                LaserDirection.DOWN -> LaserDirection.LEFT
                LaserDirection.RIGHT -> LaserDirection.UP
                LaserDirection.UP -> LaserDirection.RIGHT
            }
        } else {
            when (inDir) {
                LaserDirection.RIGHT -> LaserDirection.DOWN
                LaserDirection.DOWN -> LaserDirection.RIGHT
                LaserDirection.LEFT -> LaserDirection.UP
                LaserDirection.UP -> LaserDirection.LEFT
            }
        }
    }
}

/**
 * 程序化逆向光路關卡生成器 (邊緣起迄點錨定、長程宏觀光路、多維度進階機關)
 */
object LaserMazeGenerator {

    fun generate(
        difficulty: GameDifficulty,
        levelIndex: Int,
        random: Random = Random.Default
    ): LaserBoardState {
        val config = LaserLevelConfig.getConfig(difficulty, levelIndex)

        var attempts = 0
        while (attempts < 60) {
            attempts++
            val board = tryGenerateBoard(config, random)
            if (board != null) {
                return board
            }
        }

        return createFallbackBoard(config)
    }

    private fun tryGenerateBoard(config: LaserLevelConfig, random: Random): LaserBoardState? {
        val size = config.gridSize
        val grid = MutableList(size) { MutableList<LaserPiece?>(size) { null } }

        // 邊緣距離門檻
        val minDistance = when (config.difficulty) {
            GameDifficulty.BEGINNER -> 4
            GameDifficulty.INTERMEDIATE -> 5
            GameDifficulty.ADVANCED -> 7
            GameDifficulty.HARD -> 8
            GameDifficulty.HELL -> 9
            GameDifficulty.EPIC -> 11
        }
        val minControllableMirrors = when (config.difficulty) {
            GameDifficulty.BEGINNER -> 2
            GameDifficulty.INTERMEDIATE -> 3
            GameDifficulty.ADVANCED -> 3
            GameDifficulty.HARD -> 4
            GameDifficulty.HELL -> 4
            GameDifficulty.EPIC -> 5
        }

        // 1. 【邊緣起迄點錨定鐵律】：發射源 100% 錨定於最外圍邊框
        val edgeSide = random.nextInt(4) // 0: TOP, 1: RIGHT, 2: BOTTOM, 3: LEFT
        val (emitterR, emitterC, emitterDir) = when (edgeSide) {
            0 -> Triple(0, random.nextInt(1, size - 1), LaserDirection.DOWN)
            1 -> Triple(random.nextInt(1, size - 1), size - 1, LaserDirection.LEFT)
            2 -> Triple(size - 1, random.nextInt(1, size - 1), LaserDirection.UP)
            else -> Triple(random.nextInt(1, size - 1), 0, LaserDirection.RIGHT)
        }

        grid[emitterR][emitterC] = LaserPiece(
            type = OpticalPieceType.EMITTER,
            rotation = when (emitterDir) {
                LaserDirection.UP -> 0
                LaserDirection.RIGHT -> 1
                LaserDirection.DOWN -> 2
                LaserDirection.LEFT -> 3
            },
            isFixed = true
        )

        val occupied = mutableSetOf(Pair(emitterR, emitterC))
        val receivers = mutableListOf<Pair<Int, Int>>()
        val receiverColors = mutableMapOf<Pair<Int, Int>, Long>()
        val piecesToScramble = mutableListOf<Pair<Int, Int>>()

        var currR = emitterR
        var currC = emitterC
        var currDir = emitterDir
        val bouncesNeeded = maxOf(config.minBounces, minControllableMirrors)

        for (bounce in 0 until bouncesNeeded) {
            // 大步長漫遊，強迫射線穿梭大範圍棋盤
            val maxAllowedSteps = when {
                size >= 10 -> 5
                size >= 8 -> 4
                size >= 6 -> 3
                else -> 2
            }
            var stepped = 0
            while (stepped < maxAllowedSteps) {
                val nextR = currR + currDir.dy
                val nextC = currC + currDir.dx
                if (nextR in 0 until size && nextC in 0 until size && Pair(nextR, nextC) !in occupied) {
                    currR = nextR
                    currC = nextC
                    occupied.add(Pair(currR, currC))
                    stepped++
                    if (stepped >= 2 && random.nextBoolean()) break
                } else {
                    break
                }
            }

            if (stepped == 0) return null

            val possibleTurns = listOf(
                currDir.rotate(3),
                currDir.rotateClockwise()
            ).shuffled(random)

            val validTurn = possibleTurns.firstOrNull { tDir ->
                val tr = currR + tDir.dy
                val tc = currC + tDir.dx
                tr in 0 until size && tc in 0 until size && Pair(tr, tc) !in occupied
            } ?: return null

            val newDir = validTurn

            // 決定鏡片類型
            val useSplitter = config.allowSplitter && bounce == bouncesNeeded / 2 && receivers.size < config.targetReceivers
            val pieceType = if (useSplitter) {
                OpticalPieceType.BEAM_SPLITTER
            } else if (random.nextInt(10) < 3) {
                OpticalPieceType.MIRROR_DOUBLE
            } else {
                OpticalPieceType.MIRROR_SINGLE
            }

            val correctRot = findCorrectRotation(pieceType, currDir, newDir) ?: return null

            grid[currR][currC] = LaserPiece(
                type = pieceType,
                rotation = correctRot,
                isFixed = false,
                isDecoy = false
            )
            piecesToScramble.add(Pair(currR, currC))

            if (useSplitter) {
                var branchR = currR
                var branchC = currC
                var branchSteps = 0
                while (branchSteps < 2) {
                    val nR = branchR + currDir.dy
                    val nC = branchC + currDir.dx
                    if (nR in 0 until size && nC in 0 until size && Pair(nR, nC) !in occupied) {
                        branchR = nR
                        branchC = nC
                        occupied.add(Pair(branchR, branchC))
                        branchSteps++
                    } else {
                        break
                    }
                }
                if (branchSteps > 0 && Pair(branchR, branchC) != Pair(currR, currC)) {
                    val isEdgeBranch = branchR == 0 || branchR == size - 1 || branchC == 0 || branchC == size - 1
                    val colorReq = if (config.allowResonance && random.nextBoolean()) LaserRaycaster.COLOR_CYAN else LaserRaycaster.COLOR_CYAN
                    grid[branchR][branchC] = LaserPiece(
                        type = OpticalPieceType.RECEIVER,
                        isFixed = true,
                        requiredColorHex = colorReq
                    )
                    receivers.add(Pair(branchR, branchC))
                    receiverColors[Pair(branchR, branchC)] = colorReq
                }
            }

            currDir = newDir
        }

        // 終點延伸至外圍邊界格子放置水晶
        var finalR = currR
        var finalC = currC
        var finalSteps = 0
        while (finalSteps < 4) {
            val nextR = finalR + currDir.dy
            val nextC = finalC + currDir.dx
            if (nextR in 0 until size && nextC in 0 until size && Pair(nextR, nextC) !in occupied) {
                finalR = nextR
                finalC = nextC
                occupied.add(Pair(finalR, finalC))
                finalSteps++
            } else {
                break
            }
        }

        if (finalSteps == 0 || Pair(finalR, finalC) == Pair(emitterR, emitterC)) {
            return null
        }

        val finalColorReq = if (config.allowResonance && config.allowFilters) {
            when (random.nextInt(3)) {
                0 -> LaserRaycaster.COLOR_RED
                1 -> LaserRaycaster.COLOR_GREEN
                else -> LaserRaycaster.COLOR_BLUE
            }
        } else {
            LaserRaycaster.COLOR_CYAN
        }

        grid[finalR][finalC] = LaserPiece(
            type = OpticalPieceType.RECEIVER,
            isFixed = true,
            requiredColorHex = finalColorReq
        )
        receivers.add(Pair(finalR, finalC))
        receiverColors[Pair(finalR, finalC)] = finalColorReq

        // 檢查起迄點曼哈頓距離
        val manhattanDistance = abs(emitterR - finalR) + abs(emitterC - finalC)
        if (manhattanDistance < minDistance) {
            return null
        }

        // 檢查起點直射線上無水晶
        var checkRayR = emitterR + emitterDir.dy
        var checkRayC = emitterC + emitterDir.dx
        while (checkRayR in 0 until size && checkRayC in 0 until size) {
            if (Pair(checkRayR, checkRayC) in receivers) {
                return null
            }
            if (grid[checkRayR][checkRayC] != null) {
                break
            }
            checkRayR += emitterDir.dy
            checkRayC += emitterDir.dx
        }

        if (piecesToScramble.size < minControllableMirrors) {
            return null
        }

        // 3. 填充干擾鏡片、進階機關與障礙物
        val emptyCoords = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until size) {
            for (c in 0 until size) {
                if (Pair(r, c) !in occupied) {
                    emptyCoords.add(Pair(r, c))
                }
            }
        }
        emptyCoords.shuffle(random)

        // 放置干擾鏡片
        var decoysPlaced = 0
        while (decoysPlaced < config.decoyCount && emptyCoords.isNotEmpty()) {
            val (dr, dc) = emptyCoords.removeAt(0)
            val decoyType = if (random.nextBoolean()) OpticalPieceType.MIRROR_SINGLE else OpticalPieceType.MIRROR_DOUBLE
            grid[dr][dc] = LaserPiece(
                type = decoyType,
                rotation = random.nextInt(4),
                isFixed = false,
                isDecoy = true
            )
            piecesToScramble.add(Pair(dr, dc))
            decoysPlaced++
        }

        // 放置成對時空傳送門 (若該難度允許)
        if (config.allowPortals && emptyCoords.size >= 2 && random.nextBoolean()) {
            val p1 = emptyCoords.removeAt(0)
            val p2 = emptyCoords.removeAt(0)
            grid[p1.first][p1.second] = LaserPiece(
                type = OpticalPieceType.PORTAL,
                isFixed = true,
                portalTarget = p2
            )
            grid[p2.first][p2.second] = LaserPiece(
                type = OpticalPieceType.PORTAL,
                isFixed = true,
                portalTarget = p1
            )
        }

        // 放置濾光鏡或混光稜鏡 (若該難度允許)
        if (config.allowFilters && emptyCoords.isNotEmpty() && random.nextBoolean()) {
            val filterCoord = emptyCoords.removeAt(0)
            val filterType = when (random.nextInt(3)) {
                0 -> OpticalPieceType.FILTER_RED
                1 -> OpticalPieceType.FILTER_GREEN
                else -> OpticalPieceType.FILTER_BLUE
            }
            grid[filterCoord.first][filterCoord.second] = LaserPiece(
                type = filterType,
                isFixed = true
            )
        }

        // 放置各類屬性障礙物
        var obstaclesPlaced = 0
        while (obstaclesPlaced < config.obstacleCount && emptyCoords.isNotEmpty()) {
            val (or, oc) = emptyCoords.removeAt(0)
            val obsType = when (random.nextInt(3)) {
                0 -> OpticalPieceType.OBSTACLE_FIXED_MIRROR
                1 -> OpticalPieceType.OBSTACLE_TINTED_GLASS
                else -> OpticalPieceType.OBSTACLE_ABSORBER
            }
            grid[or][oc] = LaserPiece(
                type = obsType,
                rotation = random.nextInt(4),
                isFixed = true
            )
            obstaclesPlaced++
        }

        // 4. 驗證正解狀態是否能照亮水晶
        val solutionState = LaserRaycaster.trace(
            gridSize = size,
            grid = grid,
            emitterRow = emitterR,
            emitterCol = emitterC,
            emitterDir = emitterDir,
            receivers = receivers,
            receiverColors = receiverColors
        )

        if (!solutionState.isCleared) {
            // 若因波長不符，將接收水晶要求自適應重設為抵達光束色彩以保證正解
            for (rec in receivers) {
                receiverColors[rec] = LaserRaycaster.COLOR_CYAN
            }
            val retrySolution = LaserRaycaster.trace(
                gridSize = size,
                grid = grid,
                emitterRow = emitterR,
                emitterCol = emitterC,
                emitterDir = emitterDir,
                receivers = receivers,
                receiverColors = receiverColors
            )
            if (!retrySolution.isCleared) return null
        }

        // 5. 隨機打亂可轉動鏡片
        for ((r, c) in piecesToScramble) {
            val currentPiece = grid[r][c] ?: continue
            val scrambleSteps = random.nextInt(1, 4)
            grid[r][c] = currentPiece.copy(
                rotation = (currentPiece.rotation + scrambleSteps) % 4
            )
        }

        var initialState = LaserRaycaster.trace(
            gridSize = size,
            grid = grid,
            emitterRow = emitterR,
            emitterCol = emitterC,
            emitterDir = emitterDir,
            receivers = receivers,
            receiverColors = receiverColors
        )

        var scrambleTries = 0
        while (initialState.isCleared && scrambleTries < 10) {
            scrambleTries++
            val (sr, sc) = piecesToScramble.random(random)
            val p = grid[sr][sc]!!
            grid[sr][sc] = p.copy(rotation = (p.rotation + 1) % 4)
            initialState = LaserRaycaster.trace(
                gridSize = size,
                grid = grid,
                emitterRow = emitterR,
                emitterCol = emitterC,
                emitterDir = emitterDir,
                receivers = receivers,
                receiverColors = receiverColors
            )
        }

        if (initialState.isCleared) {
            return null
        }

        return initialState
    }

    private fun findCorrectRotation(
        type: OpticalPieceType,
        inDir: LaserDirection,
        targetOutDir: LaserDirection
    ): Int? {
        for (rot in 0..3) {
            val out = when (type) {
                OpticalPieceType.MIRROR_SINGLE -> LaserRaycaster.calculateSingleMirrorReflection(inDir, rot)
                OpticalPieceType.MIRROR_DOUBLE,
                OpticalPieceType.BEAM_SPLITTER,
                OpticalPieceType.OBSTACLE_FIXED_MIRROR -> LaserRaycaster.calculateDoubleMirrorReflection(inDir, rot)
                else -> inDir
            }
            if (out == targetOutDir) {
                return rot
            }
        }
        return null
    }

    private fun createFallbackBoard(config: LaserLevelConfig): LaserBoardState {
        val size = config.gridSize
        val grid = MutableList(size) { MutableList<LaserPiece?>(size) { null } }

        // 發射器在 (0, 0) 朝右
        grid[0][0] = LaserPiece(type = OpticalPieceType.EMITTER, rotation = 1, isFixed = true)
        // 鏡片 1 在 (0, size - 1) 折向下
        grid[0][size - 1] = LaserPiece(type = OpticalPieceType.MIRROR_SINGLE, rotation = 1, isFixed = false)
        // 鏡片 2 在 (size - 1, size - 1) 折向左
        grid[size - 1][size - 1] = LaserPiece(type = OpticalPieceType.MIRROR_SINGLE, rotation = 2, isFixed = false)
        // 接收水晶在 (size - 1, 0)
        grid[size - 1][0] = LaserPiece(type = OpticalPieceType.RECEIVER, isFixed = true)

        val receivers = listOf(Pair(size - 1, 0))

        // 打亂 (0, size - 1)
        grid[0][size - 1] = grid[0][size - 1]!!.copy(rotation = 0)

        return LaserRaycaster.trace(
            gridSize = size,
            grid = grid,
            emitterRow = 0,
            emitterCol = 0,
            emitterDir = LaserDirection.RIGHT,
            receivers = receivers
        )
    }
}
