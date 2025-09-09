package com.example.multiurl.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [UrlEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun urlDao(): UrlDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "multiurl.db"
            ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
        }
    }
}
