package com.strhodler.utxopocket.data.wallet

import com.strhodler.utxopocket.data.db.WalletDao
import com.strhodler.utxopocket.data.db.WalletEntity
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.WalletAddressType
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest

class WalletAddressManagerCancellationTest {

    @Test
    fun getAddressDetailRethrowsCancellationException() = runTest {
        val manager = WalletAddressManager(
            walletDao = walletDaoStub(
                WalletEntity(
                    id = 1L,
                    name = "wallet",
                    descriptor = "wpkh(test/*)",
                    network = BitcoinNetwork.TESTNET.name,
                    balanceSats = 0,
                    transactionCount = 0,
                    lastSyncStatus = "IDLE",
                    lastSyncError = null
                )
            ),
            sessionRunner = CancellingSessionRunner,
            ioDispatcher = StandardTestDispatcher(testScheduler),
            logTag = "WalletAddressManagerCancellationTest"
        )

        assertFailsWith<CancellationException> {
            manager.getAddressDetail(
                walletId = 1L,
                type = WalletAddressType.EXTERNAL,
                derivationIndex = 0
            )
        }
    }

    private fun walletDaoStub(entity: WalletEntity): WalletDao {
        val handler = InvocationHandler { _, method, _ ->
            when (method.name) {
                "findById" -> entity
                "toString" -> "WalletDaoStub"
                "hashCode" -> 0
                "equals" -> false
                else -> defaultValue(method.returnType)
            }
        }
        @Suppress("UNCHECKED_CAST")
        return Proxy.newProxyInstance(
            WalletDao::class.java.classLoader,
            arrayOf(WalletDao::class.java),
            handler
        ) as WalletDao
    }

    private fun defaultValue(type: Class<*>): Any? = when {
        !type.isPrimitive -> null
        type == Boolean::class.javaPrimitiveType -> false
        type == Int::class.javaPrimitiveType -> 0
        type == Long::class.javaPrimitiveType -> 0L
        type == Float::class.javaPrimitiveType -> 0f
        type == Double::class.javaPrimitiveType -> 0.0
        type == Short::class.javaPrimitiveType -> 0.toShort()
        type == Byte::class.javaPrimitiveType -> 0.toByte()
        type == Char::class.javaPrimitiveType -> '\u0000'
        else -> null
    }
}

private object CancellingSessionRunner : WalletSessionRunner {
    override suspend fun <T> withWallet(
        entity: WalletEntity,
        sealAfterUse: Boolean,
        block: suspend (
            org.bitcoindevkit.Wallet,
            org.bitcoindevkit.Persister,
            com.strhodler.utxopocket.data.bdk.WalletMaterializationSource?
        ) -> T
    ): T {
        throw CancellationException("cancelled")
    }
}
