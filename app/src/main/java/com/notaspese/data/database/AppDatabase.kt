package com.notaspese.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.notaspese.data.model.NotaSpese
import com.notaspese.data.model.Spesa

@Database(entities = [NotaSpese::class, Spesa::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun notaSpeseDao(): NotaSpeseDao
    abstract fun spesaDao(): SpesaDao
    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "nota_spese_database").build().also { INSTANCE = it }
            }
        }
    }
}
