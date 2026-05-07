package com.strhodler.utxopocket.di

import com.strhodler.utxopocket.domain.model.IncomingWatcherPolicy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PolicyModule {

    @Provides
    @Singleton
    fun provideIncomingWatcherPolicy(): IncomingWatcherPolicy = IncomingWatcherPolicy()
}
