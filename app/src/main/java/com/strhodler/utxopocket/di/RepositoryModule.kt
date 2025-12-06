package com.strhodler.utxopocket.di

import com.strhodler.utxopocket.data.bdk.DefaultWalletStorage
import com.strhodler.utxopocket.data.bdk.WalletStorage
import com.strhodler.utxopocket.data.node.DefaultNodeConnectionTester
import com.strhodler.utxopocket.data.preferences.DefaultAppPreferencesRepository
import com.strhodler.utxopocket.data.tor.DefaultTorManager
import com.strhodler.utxopocket.data.preferences.DefaultIncomingTxPreferencesRepository
import com.strhodler.utxopocket.data.transactionhealth.DefaultTransactionHealthAnalyzer
import com.strhodler.utxopocket.data.wallet.DefaultWalletRepository
import com.strhodler.utxopocket.data.wiki.DefaultWikiRepository
import com.strhodler.utxopocket.data.wiki.WikiRepository
import com.strhodler.utxopocket.data.glossary.DefaultGlossaryRepository
import com.strhodler.utxopocket.data.glossary.GlossaryRepository
import com.strhodler.utxopocket.data.logs.DefaultNetworkErrorLogRepository
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.NodeConfigurationRepository
import com.strhodler.utxopocket.domain.repository.WalletRepository
import com.strhodler.utxopocket.domain.service.NodeConnectionTester
import com.strhodler.utxopocket.domain.repository.IncomingTxPreferencesRepository
import com.strhodler.utxopocket.domain.repository.TransactionHealthRepository
import com.strhodler.utxopocket.domain.service.TorManager
import com.strhodler.utxopocket.domain.service.TransactionHealthAnalyzer
import com.strhodler.utxopocket.data.transactionhealth.DefaultTransactionHealthRepository
import com.strhodler.utxopocket.data.utxohealth.DefaultUtxoHealthRepository
import com.strhodler.utxopocket.data.wallethealth.DefaultWalletHealthAggregator
import com.strhodler.utxopocket.data.wallethealth.DefaultWalletHealthRepository
import com.strhodler.utxopocket.domain.repository.UtxoHealthRepository
import com.strhodler.utxopocket.domain.repository.WalletHealthRepository
import com.strhodler.utxopocket.domain.service.WalletHealthAggregator
import com.strhodler.utxopocket.domain.repository.NetworkErrorLogRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindWalletRepository(
        impl: DefaultWalletRepository
    ): WalletRepository

    @Binds
    @Singleton
    abstract fun bindTorManager(
        impl: DefaultTorManager
    ): TorManager

    @Binds
    @Singleton
    abstract fun bindAppPreferencesRepository(
        impl: DefaultAppPreferencesRepository
    ): AppPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindIncomingTxPreferencesRepository(
        impl: DefaultIncomingTxPreferencesRepository
    ): IncomingTxPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindNodeConfigurationRepository(
        impl: DefaultAppPreferencesRepository
    ): NodeConfigurationRepository

    @Binds
    @Singleton
    abstract fun bindNodeConnectionTester(
        impl: DefaultNodeConnectionTester
    ): NodeConnectionTester

    @Binds
    @Singleton
    abstract fun bindTransactionHealthAnalyzer(
        impl: DefaultTransactionHealthAnalyzer
    ): TransactionHealthAnalyzer


    @Binds
    @Singleton
    abstract fun bindTransactionHealthRepository(
        impl: DefaultTransactionHealthRepository
    ): TransactionHealthRepository

    @Binds
    @Singleton
    abstract fun bindUtxoHealthRepository(
        impl: DefaultUtxoHealthRepository
    ): UtxoHealthRepository

    @Binds
    @Singleton
    abstract fun bindWalletHealthRepository(
        impl: DefaultWalletHealthRepository
    ): WalletHealthRepository

    @Binds
    @Singleton
    abstract fun bindWalletHealthAggregator(
        impl: DefaultWalletHealthAggregator
    ): WalletHealthAggregator

    @Binds
    @Singleton
    abstract fun bindWalletStorage(
        impl: DefaultWalletStorage
    ): WalletStorage

    @Binds
    @Singleton
    abstract fun bindWikiRepository(
        impl: DefaultWikiRepository
    ): WikiRepository

    @Binds
    @Singleton
    abstract fun bindGlossaryRepository(
        impl: DefaultGlossaryRepository
    ): GlossaryRepository

    @Binds
    @Singleton
    abstract fun bindNetworkErrorLogRepository(
        impl: DefaultNetworkErrorLogRepository
    ): NetworkErrorLogRepository
}
