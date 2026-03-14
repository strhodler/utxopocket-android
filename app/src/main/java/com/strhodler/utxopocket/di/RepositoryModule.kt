package com.strhodler.utxopocket.di

import com.strhodler.utxopocket.data.bdk.DefaultWalletStorage
import com.strhodler.utxopocket.data.bdk.WalletStorage
import com.strhodler.utxopocket.data.node.DefaultNodeConnectionTester
import com.strhodler.utxopocket.data.preferences.DefaultAppPreferencesRepository
import com.strhodler.utxopocket.data.tor.DefaultTorManager
import com.strhodler.utxopocket.data.preferences.DefaultIncomingTxPreferencesRepository
import com.strhodler.utxopocket.data.wallet.DefaultWalletAddressRepository
import com.strhodler.utxopocket.data.wallet.DefaultWalletBackupRepository
import com.strhodler.utxopocket.data.wallet.DefaultWalletLabelRepository
import com.strhodler.utxopocket.data.wallet.DefaultWalletMaintenanceRepository
import com.strhodler.utxopocket.data.wallet.DefaultWalletProvisioningRepository
import com.strhodler.utxopocket.data.wallet.DefaultWalletReadRepository
import com.strhodler.utxopocket.data.wallet.DefaultWalletRepository
import com.strhodler.utxopocket.data.wiki.DefaultWikiRepository
import com.strhodler.utxopocket.data.glossary.DefaultGlossaryRepository
import com.strhodler.utxopocket.data.logs.DefaultNetworkErrorLogRepository
import com.strhodler.utxopocket.data.connection.ConnectionOrchestratorV2
import com.strhodler.utxopocket.data.utxo.DefaultUtxoCanvasRepository
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.NodeConfigurationRepository
import com.strhodler.utxopocket.domain.repository.WalletAddressRepository
import com.strhodler.utxopocket.domain.repository.WalletLabelRepository
import com.strhodler.utxopocket.domain.repository.WalletMaintenanceRepository
import com.strhodler.utxopocket.domain.repository.WalletBackupRepository
import com.strhodler.utxopocket.domain.repository.WalletProvisioningRepository
import com.strhodler.utxopocket.domain.repository.WalletReadRepository
import com.strhodler.utxopocket.domain.repository.WalletSyncRepository
import com.strhodler.utxopocket.domain.service.NodeConnectionTester
import com.strhodler.utxopocket.domain.repository.IncomingTxPreferencesRepository
import com.strhodler.utxopocket.domain.service.ConnectionOrchestrator
import com.strhodler.utxopocket.domain.service.TorManager
import com.strhodler.utxopocket.domain.repository.NetworkErrorLogRepository
import com.strhodler.utxopocket.domain.repository.IncomingTxPlaceholderRepository
import com.strhodler.utxopocket.domain.repository.GlossaryRepository
import com.strhodler.utxopocket.domain.repository.WikiRepository
import com.strhodler.utxopocket.data.preferences.DefaultIncomingTxPlaceholderRepository
import com.strhodler.utxopocket.data.preferences.DefaultWalletDetailPreferencesRepository
import com.strhodler.utxopocket.data.incoming.DefaultIncomingTxWatcher
import com.strhodler.utxopocket.domain.service.IncomingTxChecker
import com.strhodler.utxopocket.domain.service.IncomingTxWatcher
import com.strhodler.utxopocket.data.preferences.DefaultWalletSyncPreferencesRepository
import com.strhodler.utxopocket.domain.repository.WalletDetailPreferencesRepository
import com.strhodler.utxopocket.domain.repository.WalletSyncPreferencesRepository
import com.strhodler.utxopocket.domain.repository.UtxoCanvasRepository
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
    abstract fun bindWalletReadRepository(
        impl: DefaultWalletReadRepository
    ): WalletReadRepository

    @Binds
    @Singleton
    abstract fun bindWalletSyncRepository(
        impl: DefaultWalletRepository
    ): WalletSyncRepository

    @Binds
    @Singleton
    abstract fun bindWalletProvisioningRepository(
        impl: DefaultWalletProvisioningRepository
    ): WalletProvisioningRepository

    @Binds
    @Singleton
    abstract fun bindWalletAddressRepository(
        impl: DefaultWalletAddressRepository
    ): WalletAddressRepository

    @Binds
    @Singleton
    abstract fun bindWalletLabelRepository(
        impl: DefaultWalletLabelRepository
    ): WalletLabelRepository

    @Binds
    @Singleton
    abstract fun bindWalletMaintenanceRepository(
        impl: DefaultWalletMaintenanceRepository
    ): WalletMaintenanceRepository

    @Binds
    @Singleton
    abstract fun bindWalletBackupRepository(
        impl: DefaultWalletBackupRepository
    ): WalletBackupRepository

    @Binds
    @Singleton
    abstract fun bindTorManager(
        impl: DefaultTorManager
    ): TorManager

    @Binds
    @Singleton
    abstract fun bindConnectionOrchestrator(
        impl: ConnectionOrchestratorV2
    ): ConnectionOrchestrator

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
    abstract fun bindIncomingTxPlaceholderRepository(
        impl: DefaultIncomingTxPlaceholderRepository
    ): IncomingTxPlaceholderRepository

    @Binds
    @Singleton
    abstract fun bindIncomingTxChecker(
        impl: DefaultIncomingTxWatcher
    ): IncomingTxChecker

    @Binds
    @Singleton
    abstract fun bindIncomingTxWatcher(
        impl: DefaultIncomingTxWatcher
    ): IncomingTxWatcher

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

    @Binds
    @Singleton
    abstract fun bindWalletSyncPreferencesRepository(
        impl: DefaultWalletSyncPreferencesRepository
    ): WalletSyncPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindWalletDetailPreferencesRepository(
        impl: DefaultWalletDetailPreferencesRepository
    ): WalletDetailPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindUtxoCanvasRepository(
        impl: DefaultUtxoCanvasRepository
    ): UtxoCanvasRepository
}
