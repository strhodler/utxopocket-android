package com.strhodler.utxopocket.domain.repository

interface WalletRepository :
    WalletReadRepository,
    WalletSyncRepository,
    WalletProvisioningRepository,
    WalletAddressRepository,
    WalletLabelRepository,
    WalletMaintenanceRepository

class WalletNameAlreadyExistsException(name: String) :
    IllegalArgumentException("Wallet name already exists: $name")
