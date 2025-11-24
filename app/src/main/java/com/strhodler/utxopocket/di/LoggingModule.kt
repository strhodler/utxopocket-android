package com.strhodler.utxopocket.di

import android.content.Context
import androidx.room.Room
import com.strhodler.utxopocket.data.db.EncryptedSupportFactoryProvider
import com.strhodler.utxopocket.data.logs.NetworkErrorLogDao
import com.strhodler.utxopocket.data.logs.NetworkErrorLogDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LoggingModule {

    @Provides
    @Singleton
    fun provideNetworkErrorLogDatabase(
        @ApplicationContext context: Context,
        encryptedSupportFactoryProvider: EncryptedSupportFactoryProvider
    ): NetworkErrorLogDatabase =
        Room.databaseBuilder(
            context,
            NetworkErrorLogDatabase::class.java,
            NetworkErrorLogDatabase.NAME
        )
            .openHelperFactory(encryptedSupportFactoryProvider.create())
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideNetworkErrorLogDao(
        database: NetworkErrorLogDatabase
    ): NetworkErrorLogDao = database.networkErrorLogDao()
}
