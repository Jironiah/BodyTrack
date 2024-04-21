package com.example.cuerpo_peso.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [imagen::class, imagenes_list::class], version = 1)
abstract class DB : RoomDatabase() {
    abstract fun DAO(): DAO

    companion object {
        @Volatile
        private var INSTANCE: DB? = null

        fun getDatabase(context: Context): DB {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext, DB::class.java, "peso_y_cuerpo.db"
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}