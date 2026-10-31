package com.nth.thechatptit.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        WorkoutEntity::class,
        WorkoutSegmentEntity::class,
        TrackPointEntity::class,
        PostEntity::class,
        GoalEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun workoutSegmentDao(): WorkoutSegmentDao
    abstract fun trackPointDao(): TrackPointDao
    abstract fun postDao(): PostDao
    abstract fun goalDao(): GoalDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `posts` (" +
                        "`id` TEXT NOT NULL PRIMARY KEY, " +
                        "`ownerUserId` TEXT, " +
                        "`workoutId` TEXT NOT NULL, " +
                        "`caption` TEXT, " +
                        "`imageUrl` TEXT, " +
                        "`visibility` TEXT NOT NULL, " +
                        "`syncStatus` TEXT NOT NULL, " +
                        "`createdAtEpochMillis` INTEGER NOT NULL, " +
                        "`updatedAtEpochMillis` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_posts_workoutId` ON `posts` (`workoutId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_posts_createdAtEpochMillis` ON `posts` (`createdAtEpochMillis`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `goals` (" +
                        "`id` TEXT NOT NULL PRIMARY KEY, " +
                        "`ownerUserId` TEXT, " +
                        "`title` TEXT NOT NULL, " +
                        "`metric` TEXT NOT NULL, " +
                        "`workoutType` TEXT, " +
                        "`period` TEXT NOT NULL, " +
                        "`targetValue` REAL NOT NULL, " +
                        "`startDateEpochDay` INTEGER NOT NULL, " +
                        "`endDateEpochDay` INTEGER, " +
                        "`reminderMinutesOfDay` INTEGER, " +
                        "`reminderTimeZoneId` TEXT, " +
                        "`reminderDaysOfWeekMask` INTEGER, " +
                        "`isActive` INTEGER NOT NULL, " +
                        "`createdAtEpochMillis` INTEGER NOT NULL, " +
                        "`updatedAtEpochMillis` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_goals_ownerUserId` ON `goals` (`ownerUserId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_goals_isActive` ON `goals` (`isActive`)")
            }
        }
    }
}
