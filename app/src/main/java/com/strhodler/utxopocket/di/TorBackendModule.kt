package com.strhodler.utxopocket.di

import com.strhodler.utxopocket.tor.control.TorServiceBackend
import com.strhodler.utxopocket.tor.control.TorServiceClient
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TorBackendModule {

    @Binds
    @Singleton
    abstract fun bindTorServiceBackend(
        impl: TorServiceClient
    ): TorServiceBackend
}
