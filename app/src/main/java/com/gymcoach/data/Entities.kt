package com.gymcoach.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "exercises", indices = [Index(value = ["name"], unique = true)])
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val muscleGroup: String,
    val equipment: String,
    val isBodyweight: Boolean,
    /** Step-by-step how-to, one step per line. */
    val instructions: String,
    /** Form tips and common mistakes, one per line. */
    val tips: String,
    val easierName: String? = null,
    val harderName: String? = null,
    val startWeightKg: Double = 0.0,
    /** Reps per set, or minutes for cardio. */
    val startReps: Int = 8,
    val startSets: Int = 3,
    /** Cardio exercises log minutes (in the reps column) and distance instead of weight. */
    val isCardio: Boolean = false,
)

/** The coach's current target for an exercise. For cardio, [reps] is minutes and [sets] is 1. */
@Entity(tableName = "targets")
data class ExerciseTarget(
    @PrimaryKey val exerciseId: Long,
    val weightKg: Double,
    val reps: Int,
    val sets: Int,
    val updatedAt: Long,
)

@Entity(tableName = "sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val endedAt: Long? = null,
)

/** An exercise added to a session, in display order. */
@Entity(
    tableName = "session_exercises",
    primaryKeys = ["sessionId", "exerciseId"],
    foreignKeys = [ForeignKey(entity = WorkoutSession::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("exerciseId")],
)
data class SessionExercise(
    val sessionId: Long,
    val exerciseId: Long,
    val position: Int,
)

@Entity(
    tableName = "sets",
    foreignKeys = [ForeignKey(entity = WorkoutSession::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("sessionId"), Index("exerciseId")],
)
data class SetEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val weightKg: Double,
    /** Reps, or minutes for cardio. */
    val reps: Int,
    val targetReps: Int,
    /** 1 = very easy, 10 = could not do another rep. */
    val effort: Int,
    /** Cardio only. */
    val distanceKm: Double = 0.0,
    val createdAt: Long,
)

enum class GoalType { STRENGTH, WEEKLY_SESSIONS }

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: GoalType,
    /** Only for STRENGTH goals. */
    val exerciseId: Long? = null,
    /** Kg for STRENGTH, sessions per week for WEEKLY_SESSIONS. */
    val target: Double,
    val createdAt: Long,
    val achievedAt: Long? = null,
)

@Entity(tableName = "coach_notes")
data class CoachNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val message: String,
    val createdAt: Long,
)

/** Summary row for the history list. */
data class SessionSummary(
    val id: Long,
    val startedAt: Long,
    val endedAt: Long?,
    val exerciseCount: Int,
    val setCount: Int,
    val volumeKg: Double,
)
