package com.strhodler.utxopocket.domain.service

import com.strhodler.utxopocket.domain.model.CustomNode
import com.strhodler.utxopocket.domain.model.NodeConnectionTestResult

interface NodeConnectionTester {
    suspend fun testHostPort(host: String, port: Int): NodeConnectionTestResult
    suspend fun testOnion(onion: String): NodeConnectionTestResult
    suspend fun test(node: CustomNode): NodeConnectionTestResult
}
