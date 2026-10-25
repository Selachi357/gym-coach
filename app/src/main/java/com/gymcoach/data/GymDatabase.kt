package com.gymcoach.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Converters {
    @TypeConverter
    fun goalTypeToString(type: GoalType): String = type.name

    @TypeConverter
    fun stringToGoalType(value: String): GoalType = GoalType.valueOf(value)
}

@Database(
    entities = [
        Exercise::class,
        ExerciseTarget::class,
        WorkoutSession::class,
        SessionExercise::class,
        SetEntry::class,
        Goal::class,
        CoachNote::class,
        Program::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class GymDatabase : RoomDatabase() {
    abstract fun dao(): GymDao

    companion object {
        fun create(context: Context): GymDatabase =
            Room.databaseBuilder(context, GymDatabase::class.java, "gym.db")
                .addMigrations(MIGRATION_1_2)
                .build()

        /** Adds 6-week programs without touching logged workouts. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `programs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`startedAt` INTEGER NOT NULL, `daysPerWeek` INTEGER NOT NULL, `focus` TEXT NOT NULL, `active` INTEGER NOT NULL)"
                )
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `programId` INTEGER")
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `programWeek` INTEGER")
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `programDay` INTEGER")
                db.execSQL("ALTER TABLE `session_exercises` ADD COLUMN `plannedSets` INTEGER")
                db.execSQL("ALTER TABLE `session_exercises` ADD COLUMN `plannedReps` INTEGER")
                db.execSQL("ALTER TABLE `session_exercises` ADD COLUMN `plannedWeightKg` REAL")
            }
        }
    }
}
