package com.strhodler.utxopocket.di

import com.strhodler.utxopocket.tor.control.TorControlFacade
import com.strhodler.utxopocket.tor.control.TorServiceControlFacade
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TorControlModule {

    @Binds
    @Singleton
    abstract fun bindTorControlFacade(
        impl: TorServiceControlFacade
    ): TorControlFacade
}
