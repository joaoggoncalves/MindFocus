package com.kis.mindfocus.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kis.mindfocus.data.local.dao.FocusSessionDao
import com.kis.mindfocus.data.local.entity.DistractionEventEntity
import com.kis.mindfocus.data.local.entity.FocusSessionEntity

@Database(
    entities = [FocusSessionEntity::class, DistractionEventEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class MindFocusDatabase : RoomDatabase() {
    abstract fun focusSessionDao(): FocusSessionDao

    companion object {
        const val NAME = "mindfocus.db"
    }
}
