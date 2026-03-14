package com.strhodler.utxopocket.presentation.appshell.overlay.snake

class SnakeGateReducer(
    private val targetPickups: Int = SNAKE_GATE_TARGET_PICKUPS
) {
    init {
        require(targetPickups > 0) { "target pickups must be positive" }
    }

    fun initialState(grid: SnakeGateGrid = SnakeGateGrid()): SnakeGateState {
        val snakeBody = listOf(centerPositionFor(grid))
        val seed = spawnSeed(
            snakeBody = snakeBody,
            direction = SnakeGateDirection.Right,
            collectedCount = 0,
            grid = grid
        )
        val initialPickup = spawnPickup(
            grid = grid,
            snakeBody = snakeBody,
            existingPickups = emptySet(),
            seed = seed
        )
        return SnakeGateState(
            grid = grid,
            snakeBody = snakeBody,
            direction = SnakeGateDirection.Right,
            pickupPositions = initialPickup?.let(::setOf).orEmpty(),
            collectedCount = 0,
            solved = false
        )
    }

    fun reduce(
        state: SnakeGateState,
        direction: SnakeGateDirection
    ): SnakeGateState {
        if (state.solved) {
            return state
        }

        val nextHead = state.snakeBody.first().moved(direction)
        if (!state.grid.contains(nextHead) || nextHead in state.snakeBody) {
            return initialState(state.grid)
        }

        val collectedPickup = nextHead in state.pickupPositions
        val nextSnakeBody = if (collectedPickup) {
            listOf(nextHead) + state.snakeBody
        } else {
            listOf(nextHead) + state.snakeBody.dropLast(1)
        }
        val nextCollectedCount = if (collectedPickup) {
            state.collectedCount + 1
        } else {
            state.collectedCount
        }
        val nextSolved = nextCollectedCount >= targetPickups
        val remainingPickups = if (collectedPickup) {
            state.pickupPositions - nextHead
        } else {
            state.pickupPositions
        }
        val nextPickups = when {
            nextSolved -> emptySet()
            remainingPickups.isNotEmpty() -> remainingPickups
            else -> {
                val seed = spawnSeed(
                    snakeBody = nextSnakeBody,
                    direction = direction,
                    collectedCount = nextCollectedCount,
                    grid = state.grid
                )
                val spawned = spawnPickup(
                    grid = state.grid,
                    snakeBody = nextSnakeBody,
                    existingPickups = emptySet(),
                    seed = seed
                )
                spawned?.let(::setOf).orEmpty()
            }
        }

        return SnakeGateState(
            grid = state.grid,
            snakeBody = nextSnakeBody,
            direction = direction,
            pickupPositions = nextPickups,
            collectedCount = nextCollectedCount,
            solved = nextSolved
        )
    }

    private fun spawnSeed(
        snakeBody: List<SnakeGatePosition>,
        direction: SnakeGateDirection,
        collectedCount: Int,
        grid: SnakeGateGrid
    ): Int {
        val head = snakeBody.first()
        return (head.y * grid.width) + head.x + (snakeBody.size * 13) + (direction.ordinal * 17) + (collectedCount * 31)
    }

    private fun spawnPickup(
        grid: SnakeGateGrid,
        snakeBody: List<SnakeGatePosition>,
        existingPickups: Set<SnakeGatePosition>,
        seed: Int
    ): SnakeGatePosition? {
        val occupied = snakeBody.toSet() + existingPickups
        val availablePositions = grid.allPositions().filterNot(occupied::contains)
        if (availablePositions.isEmpty()) {
            return null
        }
        val index = floorMod(seed, availablePositions.size)
        return availablePositions[index]
    }

    private fun floorMod(value: Int, modulo: Int): Int {
        val remainder = value % modulo
        return if (remainder >= 0) remainder else remainder + modulo
    }
}
