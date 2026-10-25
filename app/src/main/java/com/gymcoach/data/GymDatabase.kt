package com.gymcoach.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

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
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class GymDatabase : RoomDatabase() {
    abstract fun dao(): GymDao

    companion object {
        fun create(context: Context): GymDatabase =
            Room.databaseBuilder(context, GymDatabase::class.java, "gym.db").build()
    }
}
