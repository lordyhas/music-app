package com.lordyhas.muzik.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PlaylistEntity::class, PlaylistSongEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MuzikDatabase : RoomDatabase() {

    abstract fun playlistDao(): PlaylistDao

    companion object {
        @Volatile
        private var INSTANCE: MuzikDatabase? = null

        fun getInstance(context: Context): MuzikDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MuzikDatabase::class.java,
                    "muzik_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
