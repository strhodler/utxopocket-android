package com.strhodler.utxopocket.presentation.appshell.overlay.snake

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.strhodler.utxopocket.R

private val DefaultSnakeGateReducer = SnakeGateReducer()

@Composable
fun SnakeGateScreen(
    onSolved: () -> Unit,
    modifier: Modifier = Modifier,
    reducer: SnakeGateReducer = DefaultSnakeGateReducer,
    initialState: SnakeGateState? = null
) {
    val currentOnSolved by rememberUpdatedState(onSolved)
    val baseState = remember(initialState, reducer) { initialState ?: reducer.initialState() }
    var state by remember(baseState) { mutableStateOf(baseState) }
    var solvedDispatched by remember(baseState) { mutableStateOf(false) }

    LaunchedEffect(state.solved) {
        if (state.solved && !solvedDispatched) {
            solvedDispatched = true
            currentOnSolved()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.snake_gate_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(id = R.string.snake_gate_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(
                    id = R.string.snake_gate_progress,
                    state.collectedCount.coerceAtMost(SNAKE_GATE_TARGET_PICKUPS),
                    SNAKE_GATE_TARGET_PICKUPS
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            SnakeGateBoard(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp, max = 320.dp)
            )

            SnakeGateDirectionPad(
                onMove = { direction ->
                    state = reducer.reduce(state, direction)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SnakeGateBoard(
    state: SnakeGateState,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    val headColor = MaterialTheme.colorScheme.primary
    val bodyColor = MaterialTheme.colorScheme.primaryContainer
    val pickupColor = MaterialTheme.colorScheme.tertiary
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    val borderColor = MaterialTheme.colorScheme.outlineVariant

    val snakeBody = remember(state.snakeBody) { state.snakeBody.toSet() }
    val snakeHead = state.snakeBody.first()

    Canvas(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .padding(8.dp)
    ) {
        val cellWidth = size.width / state.grid.width.toFloat()
        val cellHeight = size.height / state.grid.height.toFloat()
        val strokeWidth = 0.5.dp.toPx()
        for (y in 0 until state.grid.height) {
            for (x in 0 until state.grid.width) {
                val position = SnakeGatePosition(x = x, y = y)
                val color = when {
                    position == snakeHead -> headColor
                    position in snakeBody -> bodyColor
                    position in state.pickupPositions -> pickupColor
                    else -> emptyColor
                }
                val topLeft = Offset(x = x * cellWidth, y = y * cellHeight)
                val size = Size(width = cellWidth, height = cellHeight)
                drawRect(
                    color = color,
                    topLeft = topLeft,
                    size = size
                )
                drawRect(
                    color = borderColor,
                    topLeft = topLeft,
                    size = size,
                    style = Stroke(width = strokeWidth)
                )
            }
        }
    }
}

@Composable
private fun SnakeGateDirectionPad(
    onMove: (SnakeGateDirection) -> Unit,
    modifier: Modifier = Modifier
) {
    DirectionButtonLabels(
        onMove = onMove,
        modifier = modifier
    )
}

@Composable
private fun DirectionButtonLabels(
    onMove: (SnakeGateDirection) -> Unit,
    modifier: Modifier = Modifier
) {
    val up = stringResource(id = R.string.snake_gate_move_up)
    val down = stringResource(id = R.string.snake_gate_move_down)
    val left = stringResource(id = R.string.snake_gate_move_left)
    val right = stringResource(id = R.string.snake_gate_move_right)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = { onMove(SnakeGateDirection.Up) },
            modifier = Modifier.width(132.dp)
        ) {
            Text(text = up)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = { onMove(SnakeGateDirection.Left) },
                modifier = Modifier.width(132.dp)
            ) {
                Text(text = left)
            }
            OutlinedButton(
                onClick = { onMove(SnakeGateDirection.Right) },
                modifier = Modifier.width(132.dp)
            ) {
                Text(text = right)
            }
        }

        OutlinedButton(
            onClick = { onMove(SnakeGateDirection.Down) },
            modifier = Modifier.width(132.dp)
        ) {
            Text(text = down)
        }
    }
}
