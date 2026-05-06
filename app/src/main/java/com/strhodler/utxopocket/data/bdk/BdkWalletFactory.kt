package com.strhodler.utxopocket.data.bdk

import com.strhodler.utxopocket.data.db.WalletEntity
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.toBdkNetwork
import org.bitcoindevkit.CreateWithPersistException
import org.bitcoindevkit.Descriptor
import org.bitcoindevkit.LoadWithPersistException
import org.bitcoindevkit.Persister
import org.bitcoindevkit.Wallet
import javax.inject.Inject
import javax.inject.Singleton

class BdkManagedWallet(
    val wallet: Wallet,
    val persister: Persister,
    val release: () -> Unit,
    val materializationSource: WalletMaterializationSource?
)

@Singleton
class BdkWalletFactory @Inject constructor(
    private val walletStorage: WalletStorage,
    private val persisterRegistry: BdkPersisterRegistry
) {

    fun create(entity: WalletEntity): BdkManagedWallet {
        val network = BitcoinNetwork.valueOf(entity.network)
        val bdkNetwork = network.toBdkNetwork()
        val (externalDescriptor, changeDescriptor) = resolveDescriptors(entity, bdkNetwork)
        val connectionPath = walletStorage.connectionPath(entity.id, network)
        val materializationState = walletStorage.materializationState(connectionPath)

        val handle = persisterRegistry.acquire(connectionPath)
        val persister = handle.persister

        try {
            val wallet = Wallet(
                descriptor = externalDescriptor,
                changeDescriptor = changeDescriptor,
                network = bdkNetwork,
                persister = persister
            )
            return BdkManagedWallet(
                wallet = wallet,
                persister = persister,
                release = { releasePersisterAndSeal(handle, connectionPath) },
                materializationSource = materializationState?.source
            )
        } catch (error: CreateWithPersistException) {
            if (error is CreateWithPersistException.DataAlreadyExists) {
                try {
                    val wallet = Wallet.load(
                        descriptor = externalDescriptor,
                        changeDescriptor = changeDescriptor,
                        persister = persister
                    )
                    return BdkManagedWallet(
                        wallet = wallet,
                        persister = persister,
                        release = { releasePersisterAndSeal(handle, connectionPath) },
                        materializationSource = materializationState?.source
                    )
                } catch (loadError: LoadWithPersistException) {
                    throwWithCleanup(loadError) {
                        releasePersisterAndSeal(handle, connectionPath)
                    }
                } catch (loadThrowable: Throwable) {
                    throwWithCleanup(loadThrowable) {
                        releasePersisterAndSeal(handle, connectionPath)
                    }
                }
            }
            throwWithCleanup(error) {
                releasePersisterAndSeal(handle, connectionPath)
            }
        } catch (throwable: Throwable) {
            throwWithCleanup(throwable) {
                releasePersisterAndSeal(handle, connectionPath)
            }
        }
    }

    fun removeStorage(walletId: Long, network: BitcoinNetwork) {
        val path = walletStorage.storagePath(walletId, network)
        persisterRegistry.evict(path)
        walletStorage.remove(walletId, network)
    }

    private fun resolveDescriptors(
        entity: WalletEntity,
        network: org.bitcoindevkit.Network
    ): Pair<Descriptor, Descriptor> {
        val external = Descriptor(
            descriptor = entity.descriptor,
            network = network
        )
        if (external.isMultipath()) {
            val singles = external.toSingleDescriptors()
            try {
                val branches = requireExactlyTwoMultipathBranches(singles) { descriptor ->
                    descriptor.destroy()
                }
                // Original multipath descriptor is no longer required once expanded.
                external.destroy()
                return branches
            } catch (error: Throwable) {
                runCatching { external.destroy() }
                throw error
            }
        }

        val change = entity.changeDescriptor?.let {
            Descriptor(
                descriptor = it,
                network = network
            )
        } ?: run {
            if (!entity.viewOnly) {
                throw IllegalStateException("Wallet ${entity.id} is missing change descriptor.")
            }
            external
        }
        return external to change
    }

    private fun releasePersisterAndSeal(
        handle: BdkPersisterRegistry.ManagedPersister,
        connectionPath: String
    ) {
        var failure: Throwable? = null
        runCatching { handle.close() }
            .onFailure { error -> failure = failure.addOrUse(error) }
        runCatching { walletStorage.seal(connectionPath) }
            .onFailure { error -> failure = failure.addOrUse(error) }
        failure?.let { throw it }
    }

    private fun throwWithCleanup(primary: Throwable, cleanup: () -> Unit): Nothing {
        runCatching { cleanup() }
            .onFailure { cleanupError -> primary.addSuppressed(cleanupError) }
        throw primary
    }

    private fun Throwable?.addOrUse(error: Throwable): Throwable =
        this?.also { it.addSuppressed(error) } ?: error

}

internal fun <T> requireExactlyTwoMultipathBranches(
    branches: List<T>,
    destroy: (T) -> Unit
): Pair<T, T> {
    if (branches.size != 2) {
        branches.forEach { branch -> runCatching { destroy(branch) } }
        throw IllegalArgumentException(
            "Multipath descriptor must expand to exactly two branches (external/change)."
        )
    }
    return branches[0] to branches[1]
}
