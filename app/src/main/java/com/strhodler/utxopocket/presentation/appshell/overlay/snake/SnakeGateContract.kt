package com.strhodler.utxopocket.presentation.appshell.overlay.snake

const val SNAKE_GATE_TARGET_PICKUPS: Int = 2

data class SnakeGateGrid(
    val width: Int = DEFAULT_WIDTH,
    val height: Int = DEFAULT_HEIGHT
) {
    init {
        require(width > 1) { "width must be greater than one" }
        require(height > 1) { "height must be greater than one" }
    }

    fun contains(position: SnakeGatePosition): Boolean =
        position.x in 0 until width && position.y in 0 until height

    fun allPositions(): List<SnakeGatePosition> = buildList(capacity = width * height) {
        repeat(height) { y ->
            repeat(width) { x ->
                add(SnakeGatePosition(x = x, y = y))
            }
        }
    }

    private companion object {
        private const val DEFAULT_WIDTH = 6
        private const val DEFAULT_HEIGHT = 6
    }
}

data class SnakeGatePosition(
    val x: Int,
    val y: Int
) {
    fun moved(direction: SnakeGateDirection): SnakeGatePosition =
        SnakeGatePosition(
            x = x + direction.dx,
            y = y + direction.dy
        )
}

enum class SnakeGateDirection(
    val dx: Int,
    val dy: Int
) {
    Up(dx = 0, dy = -1),
    Down(dx = 0, dy = 1),
    Left(dx = -1, dy = 0),
    Right(dx = 1, dy = 0)
}

data class SnakeGateState(
    val grid: SnakeGateGrid = SnakeGateGrid(),
    val snakeBody: List<SnakeGatePosition> = listOf(centerPositionFor(grid)),
    val direction: SnakeGateDirection = SnakeGateDirection.Right,
    val pickupPositions: Set<SnakeGatePosition> = emptySet(),
    val collectedCount: Int = 0,
    val solved: Boolean = false
) {
    init {
        require(snakeBody.isNotEmpty()) { "snake body cannot be empty" }
        require(collectedCount >= 0) { "collected count cannot be negative" }
        require(snakeBody.all(grid::contains)) { "snake body must stay inside the grid" }
        require(snakeBody.distinct().size == snakeBody.size) { "snake body cannot self-overlap" }
        require(pickupPositions.all(grid::contains)) { "pickups must stay inside the grid" }
        require(pickupPositions.none { it in snakeBody }) { "pickups cannot overlap snake body" }
    }
}

internal fun centerPositionFor(grid: SnakeGateGrid): SnakeGatePosition =
    SnakeGatePosition(
        x = grid.width / 2,
        y = grid.height / 2
    )
