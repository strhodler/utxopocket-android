package com.strhodler.utxopocket.presentation.appshell.overlay.snake

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SnakeGateReducerTest {

    private val reducer = SnakeGateReducer()

    @Test
    fun collectSecondPickupMarksRunSolved() {
        val state = SnakeGateState(
            grid = SnakeGateGrid(width = 4, height = 4),
            snakeBody = listOf(SnakeGatePosition(x = 1, y = 1)),
            direction = SnakeGateDirection.Right,
            pickupPositions = setOf(SnakeGatePosition(x = 2, y = 1)),
            collectedCount = 1,
            solved = false
        )

        val next = reducer.reduce(state, SnakeGateDirection.Right)

        assertEquals(2, next.collectedCount)
        assertTrue(next.solved)
        assertEquals(
            expected = listOf(
                SnakeGatePosition(x = 2, y = 1),
                SnakeGatePosition(x = 1, y = 1)
            ),
            actual = next.snakeBody
        )
        assertTrue(next.pickupPositions.isEmpty())
    }

    @Test
    fun wallCollisionResetsRunToInitialState() {
        val grid = SnakeGateGrid(width = 4, height = 4)
        val collisionState = SnakeGateState(
            grid = grid,
            snakeBody = listOf(SnakeGatePosition(x = 0, y = 0)),
            direction = SnakeGateDirection.Left,
            pickupPositions = setOf(SnakeGatePosition(x = 3, y = 3)),
            collectedCount = 1,
            solved = false
        )

        val next = reducer.reduce(collisionState, SnakeGateDirection.Left)

        assertEquals(reducer.initialState(grid), next)
        assertFalse(next.solved)
        assertEquals(0, next.collectedCount)
    }

    @Test
    fun selfCollisionResetsRunToInitialState() {
        val grid = SnakeGateGrid(width = 5, height = 5)
        val collisionState = SnakeGateState(
            grid = grid,
            snakeBody = listOf(
                SnakeGatePosition(x = 2, y = 2),
                SnakeGatePosition(x = 2, y = 3),
                SnakeGatePosition(x = 3, y = 3),
                SnakeGatePosition(x = 3, y = 2)
            ),
            direction = SnakeGateDirection.Down,
            pickupPositions = setOf(SnakeGatePosition(x = 0, y = 0)),
            collectedCount = 1,
            solved = false
        )

        val next = reducer.reduce(collisionState, SnakeGateDirection.Down)

        assertEquals(reducer.initialState(grid), next)
    }

    @Test
    fun collectedPickupSpawnsNewPickupOutsideSnakeBody() {
        val state = SnakeGateState(
            grid = SnakeGateGrid(width = 4, height = 4),
            snakeBody = listOf(SnakeGatePosition(x = 1, y = 1)),
            direction = SnakeGateDirection.Right,
            pickupPositions = setOf(SnakeGatePosition(x = 2, y = 1)),
            collectedCount = 0,
            solved = false
        )

        val next = reducer.reduce(state, SnakeGateDirection.Right)

        assertEquals(1, next.collectedCount)
        assertFalse(next.solved)
        assertEquals(1, next.pickupPositions.size)
        assertTrue(next.pickupPositions.none { pickup -> pickup in next.snakeBody })
        assertEquals(next, reducer.reduce(state, SnakeGateDirection.Right))
    }

    @Test
    fun moveAdvancesExactlyOneCellPerInputDirection() {
        val state = SnakeGateState(
            grid = SnakeGateGrid(width = 5, height = 5),
            snakeBody = listOf(SnakeGatePosition(x = 2, y = 2)),
            direction = SnakeGateDirection.Up,
            pickupPositions = setOf(SnakeGatePosition(x = 0, y = 0)),
            collectedCount = 0,
            solved = false
        )

        val up = reducer.reduce(state, SnakeGateDirection.Up)
        val down = reducer.reduce(state, SnakeGateDirection.Down)
        val left = reducer.reduce(state, SnakeGateDirection.Left)
        val right = reducer.reduce(state, SnakeGateDirection.Right)

        assertEquals(SnakeGatePosition(x = 2, y = 1), up.snakeBody.first())
        assertEquals(SnakeGatePosition(x = 2, y = 3), down.snakeBody.first())
        assertEquals(SnakeGatePosition(x = 1, y = 2), left.snakeBody.first())
        assertEquals(SnakeGatePosition(x = 3, y = 2), right.snakeBody.first())
    }
}
