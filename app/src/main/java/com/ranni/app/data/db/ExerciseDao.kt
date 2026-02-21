package com.ranni.app.data.db

import androidx.room.*
import com.ranni.app.data.model.Exercise
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    // Ordered by user-defined position; name is secondary for ties (e.g. all new exercises at 0)
    @Query("SELECT * FROM exercises ORDER BY orderIndex ASC, name ASC")
    fun getAllExercises(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getById(id: Long): Exercise?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exercise: Exercise): Long

    @Delete
    suspend fun delete(exercise: Exercise)

    // Batch-updates the orderIndex for all exercises in one transaction after a drag reorder
    @Update
    suspend fun updateAll(exercises: List<Exercise>)
}
