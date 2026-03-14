package com.strhodler.utxopocket.presentation.appshell.overlay

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.presentation.appshell.overlay.snake.SNAKE_GATE_TARGET_PICKUPS
import com.strhodler.utxopocket.presentation.appshell.overlay.snake.SnakeGateDirection
import com.strhodler.utxopocket.presentation.appshell.overlay.snake.SnakeGateGrid
import com.strhodler.utxopocket.presentation.appshell.overlay.snake.SnakeGatePosition
import com.strhodler.utxopocket.presentation.appshell.overlay.snake.SnakeGateScreen
import com.strhodler.utxopocket.presentation.appshell.overlay.snake.SnakeGateState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SnakeGateScreenUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun progressTextShowsCollectedCount() {
        val progressLabel = composeRule.activity.getString(
            R.string.snake_gate_progress,
            0,
            SNAKE_GATE_TARGET_PICKUPS
        )

        composeRule.setContent {
            MaterialTheme {
                SnakeGateScreen(
                    initialState = SnakeGateState(
                        grid = SnakeGateGrid(width = 4, height = 4),
                        snakeBody = listOf(SnakeGatePosition(x = 1, y = 1)),
                        direction = SnakeGateDirection.Right,
                        pickupPositions = setOf(SnakeGatePosition(x = 3, y = 3)),
                        collectedCount = 0,
                        solved = false
                    ),
                    onSolved = {}
                )
            }
        }

        composeRule.onNodeWithText(progressLabel).assertIsDisplayed()
    }

    @Test
    fun solvingGateTriggersOnSolvedOnce() {
        val moveRightLabel = composeRule.activity.getString(R.string.snake_gate_move_right)
        val solvedProgress = composeRule.activity.getString(
            R.string.snake_gate_progress,
            SNAKE_GATE_TARGET_PICKUPS,
            SNAKE_GATE_TARGET_PICKUPS
        )
        var solvedCount by mutableIntStateOf(0)

        composeRule.setContent {
            MaterialTheme {
                SnakeGateScreen(
                    initialState = SnakeGateState(
                        grid = SnakeGateGrid(width = 4, height = 4),
                        snakeBody = listOf(SnakeGatePosition(x = 1, y = 1)),
                        direction = SnakeGateDirection.Right,
                        pickupPositions = setOf(SnakeGatePosition(x = 2, y = 1)),
                        collectedCount = 1,
                        solved = false
                    ),
                    onSolved = { solvedCount += 1 }
                )
            }
        }

        composeRule.onNodeWithText(moveRightLabel).performClick()

        composeRule.runOnIdle {
            assertEquals(1, solvedCount)
        }
        composeRule.onNodeWithText(solvedProgress).assertIsDisplayed()

        composeRule.onNodeWithText(moveRightLabel).performClick()
        composeRule.runOnIdle {
            assertEquals(1, solvedCount)
        }
    }
}
