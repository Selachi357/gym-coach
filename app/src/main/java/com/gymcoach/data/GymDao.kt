package com.gymcoach.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface GymDao {
    // Exercises
    @Query("SELECT * FROM exercises ORDER BY muscleGroup, name")
    fun exercises(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    fun exercise(id: Long): Flow<Exercise?>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExercise(id: Long): Exercise?

    @Query("SELECT * FROM exercises WHERE name = :name")
    suspend fun getExerciseByName(name: String): Exercise?

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun exerciseCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExercises(exercises: List<Exercise>)

    // Targets
    @Query("SELECT * FROM targets")
    fun targets(): Flow<List<ExerciseTarget>>

    @Query("SELECT * FROM targets WHERE exerciseId = :exerciseId")
    suspend fun getTarget(exerciseId: Long): ExerciseTarget?

    @Upsert
    suspend fun upsertTarget(target: ExerciseTarget)

    // Sessions
    @Query("SELECT * FROM sessions WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun activeSession(): Flow<WorkoutSession?>

    @Query("SELECT * FROM sessions WHERE id = :id")
    fun session(id: Long): Flow<WorkoutSession?>

    @Insert
    suspend fun insertSession(session: WorkoutSession): Long

    @Update
    suspend fun updateSession(session: WorkoutSession)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSession(id: Long): WorkoutSession?

    @Query(
        """
        SELECT s.id, s.startedAt, s.endedAt,
            (SELECT COUNT(DISTINCT exerciseId) FROM sets WHERE sessionId = s.id) AS exerciseCount,
            (SELECT COUNT(*) FROM sets WHERE sessionId = s.id) AS setCount,
            (SELECT COALESCE(SUM(weightKg * reps), 0) FROM sets WHERE sessionId = s.id) AS volumeKg
        FROM sessions s WHERE s.endedAt IS NOT NULL ORDER BY s.startedAt DESC
        """
    )
    fun sessionSummaries(): Flow<List<SessionSummary>>

    @Query("SELECT COUNT(*) FROM sessions WHERE endedAt IS NOT NULL AND startedAt >= :since")
    fun finishedSessionsSince(since: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM sessions WHERE endedAt IS NOT NULL AND startedAt >= :since")
    suspend fun countFinishedSessionsSince(since: Long): Int

    // Session exercises
    @Query("SELECT e.* FROM exercises e JOIN session_exercises se ON se.exerciseId = e.id WHERE se.sessionId = :sessionId ORDER BY se.position")
    fun sessionExercises(sessionId: Long): Flow<List<Exercise>>

    @Query("SELECT COALESCE(MAX(position), -1) FROM session_exercises WHERE sessionId = :sessionId")
    suspend fun maxPosition(sessionId: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSessionExercise(se: SessionExercise)

    @Query("SELECT * FROM session_exercises WHERE sessionId = :sessionId AND exerciseId = :exerciseId")
    suspend fun getSessionExercise(sessionId: Long, exerciseId: Long): SessionExercise?

    @Delete
    suspend fun deleteSessionExercise(se: SessionExercise)

    // Sets
    @Query("SELECT * FROM sets WHERE sessionId = :sessionId ORDER BY createdAt")
    fun setsForSession(sessionId: Long): Flow<List<SetEntry>>

    @Query("SELECT * FROM sets WHERE sessionId = :sessionId ORDER BY createdAt")
    suspend fun getSetsForSession(sessionId: Long): List<SetEntry>

    @Query("SELECT * FROM sets WHERE exerciseId = :exerciseId ORDER BY createdAt DESC")
    fun setsForExercise(exerciseId: Long): Flow<List<SetEntry>>

    /** Sets for an exercise from its most recent finished sessions, newest first. */
    @Query(
        """
        SELECT st.* FROM sets st JOIN sessions s ON s.id = st.sessionId
        WHERE st.exerciseId = :exerciseId AND s.endedAt IS NOT NULL
          AND st.sessionId IN (
            SELECT DISTINCT st2.sessionId FROM sets st2 JOIN sessions s2 ON s2.id = st2.sessionId
            WHERE st2.exerciseId = :exerciseId AND s2.endedAt IS NOT NULL
            ORDER BY s2.startedAt DESC LIMIT :limit
          )
        ORDER BY s.startedAt DESC, st.createdAt
        """
    )
    suspend fun recentSetsForExercise(exerciseId: Long, limit: Int): List<SetEntry>

    @Query("SELECT MAX(weightKg) FROM sets WHERE exerciseId = :exerciseId AND reps > 0")
    suspend fun bestWeight(exerciseId: Long): Double?

    @Query("SELECT exerciseId AS exerciseId, MAX(weightKg) AS best FROM sets WHERE reps > 0 GROUP BY exerciseId")
    fun bestWeights(): Flow<List<BestWeight>>

    @Insert
    suspend fun insertSet(set: SetEntry): Long

    @Delete
    suspend fun deleteSet(set: SetEntry)

    // Goals
    @Query("SELECT * FROM goals ORDER BY achievedAt IS NOT NULL, createdAt DESC")
    fun goals(): Flow<List<Goal>>

    @Query("SELECT * FROM goals WHERE achievedAt IS NULL")
    suspend fun openGoals(): List<Goal>

    @Insert
    suspend fun insertGoal(goal: Goal): Long

    @Update
    suspend fun updateGoal(goal: Goal)

    @Delete
    suspend fun deleteGoal(goal: Goal)

    // Coach notes
    @Query("SELECT * FROM coach_notes ORDER BY createdAt DESC, id DESC LIMIT :limit")
    fun coachNotes(limit: Int): Flow<List<CoachNote>>

    @Insert
    suspend fun insertCoachNote(note: CoachNote)
}

data class BestWeight(val exerciseId: Long, val best: Double)
