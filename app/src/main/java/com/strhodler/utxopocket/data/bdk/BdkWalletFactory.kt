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
    val release: () -> Unit
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

        var handle = persisterRegistry.acquire(connectionPath)
        var persister = handle.persister

        while (true) {
            try {
                val wallet = Wallet(
                    descriptor = externalDescriptor,
                    changeDescriptor = changeDescriptor,
                    network = bdkNetwork,
                    persister = persister
                )
                return BdkManagedWallet(
                    wallet = wallet,
                    persister = persister
                ) {
                    handle.close()
                    walletStorage.seal(connectionPath)
                }
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
                            persister = persister
                        ) {
                            handle.close()
                            walletStorage.seal(connectionPath)
                        }
                    } catch (loadError: LoadWithPersistException) {
                        handle.close()
                        walletStorage.remove(entity.id, network)
                        persisterRegistry.evict(connectionPath)
                        handle = persisterRegistry.acquire(connectionPath)
                        persister = handle.persister
                        continue
                    }
                }
                handle.close()
                throw error
            } catch (throwable: Throwable) {
                handle.close()
                walletStorage.seal(connectionPath)
                throw throwable
            }
        }
    }

    fun removeStorage(walletId: Long, network: BitcoinNetwork) {
        val path = walletStorage.connectionPath(walletId, network)
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
            require(singles.size >= 2) {
                "Multipath descriptor must expand to at least two paths."
            }
            // Original multipath descriptor is no longer required once expanded.
            external.destroy()
            val receiveDescriptor = singles[0]
            val changeDescriptor = singles[1]
            return receiveDescriptor to changeDescriptor
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

}
