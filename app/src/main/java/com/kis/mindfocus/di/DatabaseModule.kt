package com.kis.mindfocus.di

import androidx.room.Room
import com.kis.mindfocus.data.local.MindFocusDatabase
import com.kis.mindfocus.data.local.dao.FocusSessionDao
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {

    single {
        Room.databaseBuilder(
            androidContext(),
            MindFocusDatabase::class.java,
            MindFocusDatabase.NAME,
        ).build()
    }

    single<FocusSessionDao> { get<MindFocusDatabase>().focusSessionDao() }
}
