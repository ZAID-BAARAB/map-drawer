package com.sowit.mapdrawapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PlotArea::class],
    version = 2, // bumped from 1 -> 2 to add areaSqM
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun plotDao(): PlotDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "plots.db"
                )
                    .fallbackToDestructiveMigration() // OK for test project
                    .build().also { INSTANCE = it }
            }
    }
}