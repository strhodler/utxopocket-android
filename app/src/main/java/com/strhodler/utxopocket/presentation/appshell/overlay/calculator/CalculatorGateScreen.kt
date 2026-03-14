package com.strhodler.utxopocket.presentation.appshell.overlay.calculator

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.strhodler.utxopocket.R

@Composable
internal fun CalculatorGateScreen(
    onSolved: () -> Unit,
    modifier: Modifier = Modifier,
    initialState: CalculatorGateState = CalculatorGateState()
) {
    val currentOnSolved by rememberUpdatedState(onSolved)
    var state by remember(initialState) { mutableStateOf(initialState) }
    var solvedDispatched by remember(initialState) { mutableStateOf(false) }

    LaunchedEffect(state.unlockRequested) {
        if (state.unlockRequested && !solvedDispatched) {
            solvedDispatched = true
            currentOnSolved()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            CalculatorDisplay(
                value = state.display,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            CalculatorKeypad(
                onKeyPressed = { key ->
                    state = reduceCalculatorGateState(state, key)
                },
                onDoubleZeroPressed = {
                    state = reduceCalculatorGateState(state, CalculatorGateKey.Digit('0'))
                    state = reduceCalculatorGateState(state, CalculatorGateKey.Digit('0'))
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CalculatorDisplay(
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomEnd
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            maxLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CalculatorKeypad(
    onKeyPressed: (CalculatorGateKey) -> Unit,
    onDoubleZeroPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = listOf(
        listOf(
            KeySpec(R.string.calculator_key_clear) { onKeyPressed(CalculatorGateKey.Clear) },
            KeySpec(R.string.calculator_key_backspace) { onKeyPressed(CalculatorGateKey.Backspace) },
            KeySpec(R.string.calculator_key_divide) { onKeyPressed(CalculatorGateKey.Divide) },
            KeySpec(R.string.calculator_key_multiply) { onKeyPressed(CalculatorGateKey.Multiply) }
        ),
        listOf(
            KeySpec(R.string.calculator_key_7) { onKeyPressed(CalculatorGateKey.Digit('7')) },
            KeySpec(R.string.calculator_key_8) { onKeyPressed(CalculatorGateKey.Digit('8')) },
            KeySpec(R.string.calculator_key_9) { onKeyPressed(CalculatorGateKey.Digit('9')) },
            KeySpec(R.string.calculator_key_subtract) { onKeyPressed(CalculatorGateKey.Subtract) }
        ),
        listOf(
            KeySpec(R.string.calculator_key_4) { onKeyPressed(CalculatorGateKey.Digit('4')) },
            KeySpec(R.string.calculator_key_5) { onKeyPressed(CalculatorGateKey.Digit('5')) },
            KeySpec(R.string.calculator_key_6) { onKeyPressed(CalculatorGateKey.Digit('6')) },
            KeySpec(R.string.calculator_key_add) { onKeyPressed(CalculatorGateKey.Add) }
        ),
        listOf(
            KeySpec(R.string.calculator_key_1) { onKeyPressed(CalculatorGateKey.Digit('1')) },
            KeySpec(R.string.calculator_key_2) { onKeyPressed(CalculatorGateKey.Digit('2')) },
            KeySpec(R.string.calculator_key_3) { onKeyPressed(CalculatorGateKey.Digit('3')) },
            KeySpec(R.string.calculator_key_equals) { onKeyPressed(CalculatorGateKey.Equals) }
        ),
        listOf(
            KeySpec(R.string.calculator_key_0) { onKeyPressed(CalculatorGateKey.Digit('0')) },
            KeySpec(R.string.calculator_key_double_zero) { onDoubleZeroPressed() },
            KeySpec(R.string.calculator_key_dot) { onKeyPressed(CalculatorGateKey.Dot) }
        )
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    Button(
                        onClick = key.onClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                        ) {
                        Text(
                            text = stringResource(id = key.labelRes),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                if (row.size == 3) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private data class KeySpec(
    @param:StringRes val labelRes: Int,
    val onClick: () -> Unit
)
