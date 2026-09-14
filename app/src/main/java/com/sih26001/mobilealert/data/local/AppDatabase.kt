package com.sih26001.mobilealert.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [AlertEntity::class, PendingAckEntity::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alertDao(): AlertDao
    abstract fun pendingAckDao(): PendingAckDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pending_acks ADD COLUMN lastFailureAt INTEGER")
                db.execSQL("ALTER TABLE pending_acks ADD COLUMN lastFailureMessage TEXT")
                db.execSQL("ALTER TABLE pending_acks ADD COLUMN completedAt INTEGER")
            }
        }
    }
}
