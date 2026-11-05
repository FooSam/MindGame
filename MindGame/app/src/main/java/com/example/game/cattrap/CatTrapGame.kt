package com.example.game.cattrap

import java.util.ArrayDeque

/**
 * 遊戲勝負狀態
 */
enum class CatTrapGameStatus {
    IDLE,       // 準備中/等待開局
    PLAYING,    // 遊戲進行中
    VICTORY,    // 玩家勝利 (貓咪被完全困住)
    DEFEAT      // NPC 勝利 (貓咪逃出邊緣)
}

/**
 * 悔棋記錄快照 (Undo Snapshot)
 */
data class CatTrapGameStateSnapshot(
    val catPos: HexCoord,
    val grid: Map<HexCoord, HexCellType>,
    val stepsTaken: Int,
    val toolsUsedCount: Int,
    val isFreezeActive: Boolean,
    val doubleBlockRemaining: Int
)

/**
 * 《喵影圍捕：別想逃！》核心遊戲狀態與 BFS 尋路演算法引擎
 */
class CatTrapGame(
    val mode: CatTrapMode = CatTrapMode.LevelMode(1),
    var selectedBreed: CatBreed = CatBreed.ORANGE_TABBY
) {
    val gridSize = CatTrapLevels.GRID_SIZE

    var status: CatTrapGameStatus = CatTrapGameStatus.IDLE
        private set

    var catPos: HexCoord = HexCoord(5, 5)
        private set

    var catEmotion: CatEmotion = CatEmotion.IDLE
        private set

    private val _grid = mutableMapOf<HexCoord, HexCellType>()
    val grid: Map<HexCoord, HexCellType> get() = _grid

    var stepsTaken: Int = 0
        private set

    var toolsUsedCount: Int = 0
        private set

    var freezeToolCount: Int = 2
        private set

    var doubleBlockToolCount: Int = 2
        private set

    var radarToolCount: Int = 3
        private set

    var isFreezeActive: Boolean = false
        private set

    var doubleBlockRemaining: Int = 0
        private set

    var currentEscapePath: List<HexCoord> = emptyList()
        private set

    var isRadarHighlit: Boolean = false
        private set

    val undoStack = ArrayDeque<CatTrapGameStateSnapshot>()

    init {
        startNewGame()
    }

    /**
     * 開啟全新一局遊戲
     */
    fun startNewGame() {
        _grid.clear()
        undoStack.clear()
        stepsTaken = 0
        toolsUsedCount = 0
        isFreezeActive = false
        doubleBlockRemaining = 0
        isRadarHighlit = false

        when (mode) {
            is CatTrapMode.LevelMode -> {
                val lvl = CatTrapLevels.getLevel(mode.levelId)
                catPos = lvl.startCatPos
                lvl.initialPonds.forEach { coord ->
                    if (coord != catPos) {
                        _grid[coord] = HexCellType.WATER_POND
                    }
                }
            }
            is CatTrapMode.RandomMode -> {
                catPos = HexCoord(5, 5)
                val allCoords = mutableListOf<HexCoord>()
                for (r in 0 until gridSize) {
                    for (q in 0 until gridSize) {
                        val c = HexCoord(q, r)
                        if (c != catPos && !c.isEdge(gridSize)) {
                            allCoords.add(c)
                        }
                    }
                }
                allCoords.shuffle()
                val count = mode.pondCount.coerceIn(6, 16)
                for (i in 0 until count) {
                    _grid[allCoords[i]] = HexCellType.WATER_POND
                }
            }
        }

        status = CatTrapGameStatus.PLAYING
        catEmotion = CatEmotion.IDLE
        refreshEscapePath()
    }

    /**
     * 玩家點選棋盤格子進行落子
     * @return 若落子成功返回 true，否則返回 false
     */
    fun onCellClicked(coord: HexCoord): Boolean {
        if (status != CatTrapGameStatus.PLAYING) return false
        if (coord == catPos) return false
        if (_grid[coord] != null && _grid[coord] != HexCellType.EMPTY) return false

        // 儲存悔棋快照
        pushUndoSnapshot()

        // 放置玩家路障
        _grid[coord] = HexCellType.BLOCK
        stepsTaken++

        // 檢查連環雙障礙狀態
        if (doubleBlockRemaining > 0) {
            doubleBlockRemaining--
            if (doubleBlockRemaining > 0) {
                // 還有一塊障礙可放，貓咪暫不移動
                refreshEscapePath()
                return true
            }
        }

        // 貓咪移動邏輯
        processCatTurn()
        return true
    }

    /**
     * 貓咪 AI 進行決策並移動
     */
    private fun processCatTurn() {
        // 1. 若處於冰凍狀態，貓咪暫停移動 1 回合
        if (isFreezeActive) {
            isFreezeActive = false
            catEmotion = CatEmotion.ALERT
            refreshEscapePath()
            checkVictoryCondition()
            return
        }

        // 2. 計算最短邊界逃逸路徑 (BFS)
        val shortestPath = findShortestEscapePath(catPos)
        if (shortestPath.isNotEmpty()) {
            val nextStep = shortestPath.first()
            catPos = nextStep
            catEmotion = if (shortestPath.size <= 3) CatEmotion.ALERT else CatEmotion.LEAP

            // 檢查貓咪是否抵達外圍邊界
            if (catPos.isEdge(gridSize)) {
                status = CatTrapGameStatus.DEFEAT
                catEmotion = CatEmotion.ESCAPED
                return
            }
        } else {
            // 所有通往邊界之路徑已被切斷（已形成包圍網）
            val wanderStep = findWanderStep(catPos)
            if (wanderStep != null) {
                catPos = wanderStep
                catEmotion = CatEmotion.ALERT
            } else {
                // 六週無路可走，玩家完全獲勝！
                status = CatTrapGameStatus.VICTORY
                catEmotion = CatEmotion.TRAPPED
                return
            }
        }

        refreshEscapePath()
        checkVictoryCondition()
    }

    /**
     * 檢查勝負終局狀態
     */
    private fun checkVictoryCondition() {
        if (catPos.isEdge(gridSize)) {
            status = CatTrapGameStatus.DEFEAT
            catEmotion = CatEmotion.ESCAPED
            return
        }

        // 檢查相鄰是否有任何可用空格
        val availableNeighbors = catPos.getNeighbors(gridSize).filter { neighbor ->
            val cellType = _grid[neighbor]
            cellType == null || cellType == HexCellType.EMPTY
        }

        if (availableNeighbors.isEmpty()) {
            status = CatTrapGameStatus.VICTORY
            catEmotion = CatEmotion.TRAPPED
        }
    }

    /**
     * 高智能六角廣度優先搜尋 (BFS) 尋找最短邊緣逃逸路徑
     * @return 從 catPos 下一步開始至抵達邊緣之完整路徑，若無路可通返回空列表
     */
    fun findShortestEscapePath(start: HexCoord): List<HexCoord> {
        if (start.isEdge(gridSize)) return listOf(start)

        val queue = ArrayDeque<HexCoord>()
        val parentMap = mutableMapOf<HexCoord, HexCoord>()
        val visited = mutableSetOf<HexCoord>()

        queue.add(start)
        visited.add(start)

        var edgeFound: HexCoord? = null

        while (queue.isNotEmpty()) {
            val current = queue.poll()
            if (current != start && current.isEdge(gridSize)) {
                edgeFound = current
                break
            }

            for (neighbor in current.getNeighbors(gridSize)) {
                val cell = _grid[neighbor]
                val isBlocked = (cell == HexCellType.BLOCK || cell == HexCellType.WATER_POND)
                if (!isBlocked && neighbor !in visited) {
                    visited.add(neighbor)
                    parentMap[neighbor] = current
                    queue.add(neighbor)
                }
            }
        }

        if (edgeFound == null) return emptyList()

        // 回溯構建路徑
        val fullPath = mutableListOf<HexCoord>()
        var curr = edgeFound
        while (curr != null && curr != start) {
            fullPath.add(curr)
            curr = parentMap[curr]
        }
        fullPath.reverse()
        return fullPath
    }

    /**
     * 當所有通往邊界之路徑皆被封鎖時，貓咪在封閉圈內尋找剩餘最大空間徘徊
     */
    private fun findWanderStep(from: HexCoord): HexCoord? {
        val neighbors = from.getNeighbors(gridSize).filter { n ->
            val cell = _grid[n]
            cell == null || cell == HexCellType.EMPTY
        }
        if (neighbors.isEmpty()) return null

        // 優先選擇其周遭空格最多的一格（最大自由度）
        return neighbors.maxByOrNull { n ->
            n.getNeighbors(gridSize).count { nn ->
                val c = _grid[nn]
                c == null || c == HexCellType.EMPTY
            }
        }
    }

    /**
     * 更新當前逃脫路徑緩存
     */
    fun refreshEscapePath() {
        currentEscapePath = findShortestEscapePath(catPos)
    }

    // ==========================================
    // 道具系統 (Tools)
    // ==========================================

    /**
     * 使用冰凍貓薄荷道具 (貓咪暫停移動 1 回合)
     */
    fun useFreezeTool(): Boolean {
        if (status != CatTrapGameStatus.PLAYING) return false
        if (freezeToolCount <= 0 || isFreezeActive) return false
        freezeToolCount--
        toolsUsedCount++
        isFreezeActive = true
        return true
    }

    /**
     * 使用連環雙障礙道具 (當前回合可連續放 2 塊障礙)
     */
    fun useDoubleBlockTool(): Boolean {
        if (status != CatTrapGameStatus.PLAYING) return false
        if (doubleBlockToolCount <= 0 || doubleBlockRemaining > 0) return false
        doubleBlockToolCount--
        toolsUsedCount++
        doubleBlockRemaining = 2
        return true
    }

    /**
     * 使用逃脫雷達道具 (高亮顯示當前最短逃脫路徑)
     */
    fun useRadarTool(): Boolean {
        if (status != CatTrapGameStatus.PLAYING) return false
        if (radarToolCount <= 0) return false
        radarToolCount--
        toolsUsedCount++
        isRadarHighlit = true
        refreshEscapePath()
        return true
    }

    fun addToolCount(tool: CatTrapTool, amount: Int = 1) {
        when (tool) {
            CatTrapTool.FREEZE -> freezeToolCount += amount
            CatTrapTool.DOUBLE_BLOCK -> doubleBlockToolCount += amount
            CatTrapTool.RADAR -> radarToolCount += amount
        }
    }

    // ==========================================
    // 悔棋系統 (Undo)
    // ==========================================

    private fun pushUndoSnapshot() {
        val snapshot = CatTrapGameStateSnapshot(
            catPos = catPos,
            grid = _grid.toMap(),
            stepsTaken = stepsTaken,
            toolsUsedCount = toolsUsedCount,
            isFreezeActive = isFreezeActive,
            doubleBlockRemaining = doubleBlockRemaining
        )
        if (undoStack.size >= 10) {
            undoStack.removeFirst()
        }
        undoStack.addLast(snapshot)
    }

    fun undo(): Boolean {
        if (status != CatTrapGameStatus.PLAYING) return false
        if (undoStack.isEmpty()) return false

        val snapshot = undoStack.removeLast()
        catPos = snapshot.catPos
        _grid.clear()
        _grid.putAll(snapshot.grid)
        stepsTaken = snapshot.stepsTaken
        toolsUsedCount = snapshot.toolsUsedCount
        isFreezeActive = snapshot.isFreezeActive
        doubleBlockRemaining = snapshot.doubleBlockRemaining
        catEmotion = CatEmotion.IDLE
        refreshEscapePath()
        return true
    }

    // ==========================================
    // 結算與星級計算
    // ==========================================

    /**
     * 計算獲得星數 (0..3)
     */
    fun calculateStars(): Int {
        if (status != CatTrapGameStatus.VICTORY) return 0
        if (mode !is CatTrapMode.LevelMode) return 3 // 自由模式獲勝直接給予滿星

        val lvl = CatTrapLevels.getLevel(mode.levelId)
        val goal = lvl.starGoal

        var stars = 1 // 基礎獲勝得 1 星
        if (stepsTaken <= goal.targetSteps) stars++
        if (stepsTaken <= goal.extremeSteps && toolsUsedCount == 0) stars++

        return stars.coerceIn(1, 3)
    }

    /**
     * 計算總積分 (供排行榜排名)
     */
    fun calculateScore(): Int {
        if (status != CatTrapGameStatus.VICTORY) return 0
        val stars = calculateStars()
        val stepBonus = (50 - stepsTaken).coerceAtLeast(0) * 20
        return stars * 1000 + stepBonus
    }
}
