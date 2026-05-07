package com.strhodler.utxopocket.domain.repository

class WalletNameAlreadyExistsException(name: String) :
    IllegalArgumentException("Wallet name already exists: $name")
