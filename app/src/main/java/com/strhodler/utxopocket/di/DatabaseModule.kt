package com.strhodler.utxopocket.di

import android.content.Context
import androidx.room.Room
import com.strhodler.utxopocket.data.db.EncryptedSupportFactoryProvider
import com.strhodler.utxopocket.data.db.UtxoPocketDatabase
import com.strhodler.utxopocket.data.db.WalletMigrations
import com.strhodler.utxopocket.data.db.WalletDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        encryptedSupportFactoryProvider: EncryptedSupportFactoryProvider
    ): UtxoPocketDatabase =
        Room.databaseBuilder(
            context,
            UtxoPocketDatabase::class.java,
            UtxoPocketDatabase.NAME
        )
            .openHelperFactory(encryptedSupportFactoryProvider.create())
            .addMigrations(*WalletMigrations.ALL)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideWalletDao(database: UtxoPocketDatabase): WalletDao = database.walletDao()
}
