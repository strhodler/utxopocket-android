package com.strhodler.utxopocket.domain.model

sealed class NodeConnectionTestResult {
    data class Success(val serverVersion: String? = null) : NodeConnectionTestResult()
    data class Failure(val reason: String) : NodeConnectionTestResult()
}
