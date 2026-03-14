package com.strhodler.utxopocket.presentation.appshell.overlay.calculator

internal const val CALCULATOR_UNLOCK_CODE = "21000000"

internal data class CalculatorGateState(
    val expression: String = "",
    val display: String = "0",
    val unlockRequested: Boolean = false,
    val resultShown: Boolean = false,
    val errorShown: Boolean = false
)

internal sealed interface CalculatorGateKey {
    data class Digit(val value: Char) : CalculatorGateKey

    data object Dot : CalculatorGateKey
    data object Add : CalculatorGateKey
    data object Subtract : CalculatorGateKey
    data object Multiply : CalculatorGateKey
    data object Divide : CalculatorGateKey
    data object Equals : CalculatorGateKey
    data object Clear : CalculatorGateKey
    data object Backspace : CalculatorGateKey
}

internal fun reduceCalculatorGateState(
    state: CalculatorGateState,
    key: CalculatorGateKey
): CalculatorGateState = when (key) {
    CalculatorGateKey.Clear -> CalculatorGateState()

    CalculatorGateKey.Backspace -> {
        if (state.resultShown || state.errorShown) {
            CalculatorGateState()
        } else {
            val trimmed = state.expression.dropLast(1)
            if (trimmed.isEmpty()) {
                CalculatorGateState()
            } else {
                state.copy(
                    expression = trimmed,
                    display = trimmed,
                    unlockRequested = false,
                    resultShown = false,
                    errorShown = false
                )
            }
        }
    }

    is CalculatorGateKey.Digit -> {
        val base = if (state.resultShown || state.errorShown) "" else state.expression
        val next = if (base == "0") {
            key.value.toString()
        } else {
            base + key.value
        }
        state.copy(
            expression = next,
            display = next,
            unlockRequested = false,
            resultShown = false,
            errorShown = false
        )
    }

    CalculatorGateKey.Dot -> {
        val base = if (state.resultShown || state.errorShown) "" else state.expression
        val segment = base.takeLastWhile { !isOperator(it) }
        if (segment.contains('.')) {
            state
        } else {
            val next = if (base.isEmpty() || isOperator(base.last())) {
                base + "0."
            } else {
                base + "."
            }
            state.copy(
                expression = next,
                display = next,
                unlockRequested = false,
                resultShown = false,
                errorShown = false
            )
        }
    }

    CalculatorGateKey.Add,
    CalculatorGateKey.Subtract,
    CalculatorGateKey.Multiply,
    CalculatorGateKey.Divide -> {
        val operator = when (key) {
            CalculatorGateKey.Add -> '+'
            CalculatorGateKey.Subtract -> '-'
            CalculatorGateKey.Multiply -> '*'
            CalculatorGateKey.Divide -> '/'
        }

        val base = when {
            state.errorShown -> ""
            state.resultShown -> state.expression
            else -> state.expression
        }
        if (base.isEmpty()) {
            state
        } else {
            val next = if (isOperator(base.last())) {
                base.dropLast(1) + operator
            } else {
                base + operator
            }
            state.copy(
                expression = next,
                display = next,
                unlockRequested = false,
                resultShown = false,
                errorShown = false
            )
        }
    }

    CalculatorGateKey.Equals -> {
        if (state.expression == CALCULATOR_UNLOCK_CODE) {
            state.copy(
                display = CALCULATOR_UNLOCK_CODE,
                unlockRequested = true,
                resultShown = true,
                errorShown = false
            )
        } else {
            val expression = state.expression
            if (expression.isEmpty() || isOperator(expression.last())) {
                state
            } else {
                val evaluated = evaluateExpression(expression)
                if (evaluated == null || !evaluated.isFinite()) {
                    state.copy(
                        expression = "",
                        display = "0",
                        unlockRequested = false,
                        resultShown = false,
                        errorShown = true
                    )
                } else {
                    val formatted = formatResult(evaluated)
                    state.copy(
                        expression = formatted,
                        display = formatted,
                        unlockRequested = false,
                        resultShown = true,
                        errorShown = false
                    )
                }
            }
        }
    }
}

private fun evaluateExpression(expression: String): Double? {
    val tokens = tokenizeExpression(expression) ?: return null
    if (tokens.isEmpty()) {
        return null
    }

    val numbers = mutableListOf<Double>()
    val operators = mutableListOf<Char>()

    tokens.forEach { token ->
        if (token.length == 1 && isOperator(token.first())) {
            operators += token.first()
        } else {
            val number = token.toDoubleOrNull() ?: return null
            numbers += number
        }
    }

    if (numbers.size != operators.size + 1) {
        return null
    }

    val collapsedNumbers = mutableListOf<Double>()
    val collapsedOperators = mutableListOf<Char>()
    var current = numbers.first()

    operators.forEachIndexed { index, operator ->
        val next = numbers[index + 1]
        when (operator) {
            '*' -> current *= next
            '/' -> {
                if (next == 0.0) {
                    return null
                }
                current /= next
            }

            '+', '-' -> {
                collapsedNumbers += current
                collapsedOperators += operator
                current = next
            }
        }
    }
    collapsedNumbers += current

    var result = collapsedNumbers.first()
    collapsedOperators.forEachIndexed { index, operator ->
        val next = collapsedNumbers[index + 1]
        result = if (operator == '+') result + next else result - next
    }

    return result
}

private fun tokenizeExpression(expression: String): List<String>? {
    val tokens = mutableListOf<String>()
    val currentNumber = StringBuilder()

    expression.forEach { char ->
        when {
            char.isDigit() || char == '.' -> currentNumber.append(char)
            isOperator(char) -> {
                if (currentNumber.isEmpty()) {
                    return null
                }
                tokens += currentNumber.toString()
                currentNumber.clear()
                tokens += char.toString()
            }

            else -> return null
        }
    }

    if (currentNumber.isEmpty()) {
        return null
    }

    tokens += currentNumber.toString()
    return tokens
}

private fun formatResult(value: Double): String {
    val whole = value.toLong()
    if (value == whole.toDouble()) {
        return whole.toString()
    }
    return value.toString().trimEnd('0').trimEnd('.')
}

private fun isOperator(char: Char): Boolean =
    char == '+' || char == '-' || char == '*' || char == '/'
