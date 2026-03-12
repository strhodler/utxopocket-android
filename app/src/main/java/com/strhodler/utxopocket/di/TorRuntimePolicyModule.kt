package com.strhodler.utxopocket.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ProjectTorConnectivityPolicyEnabled

@Module
@InstallIn(SingletonComponent::class)
object TorRuntimePolicyModule {

    @Provides
    @Singleton
    @ProjectTorConnectivityPolicyEnabled
    fun provideProjectTorConnectivityPolicyEnabled(): Boolean =
        PROJECT_OWNED_TOR_CONNECTIVITY_POLICY_ENABLED
}

private const val PROJECT_OWNED_TOR_CONNECTIVITY_POLICY_ENABLED = true
