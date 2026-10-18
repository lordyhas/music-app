package com.lordyhas.muzik.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PlaylistEntity::class,
        PlaylistSongEntity::class,
        LikedSongEntity::class,
        PlayStatEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class MuzikDatabase : RoomDatabase() {

    abstract fun playlistDao(): PlaylistDao
    abstract fun libraryDao(): LibraryDao

    companion object {
        @Volatile
        private var INSTANCE: MuzikDatabase? = null

        /** v2: liked songs + listening stats. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `liked_songs` (`songId` INTEGER NOT NULL, `likedAt` INTEGER NOT NULL, PRIMARY KEY(`songId`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `play_stats` (`songId` INTEGER NOT NULL, `playCount` INTEGER NOT NULL, `lastPlayedAt` INTEGER NOT NULL, PRIMARY KEY(`songId`))"
                )
            }
        }

        fun getInstance(context: Context): MuzikDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MuzikDatabase::class.java,
                    "muzik_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
