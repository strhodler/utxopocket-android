package com.strhodler.utxopocket.domain.service

import com.strhodler.utxopocket.domain.model.CustomNode
import com.strhodler.utxopocket.domain.model.NodeConnectionTestResult

interface NodeConnectionTester {
    suspend fun test(node: CustomNode): NodeConnectionTestResult
}
