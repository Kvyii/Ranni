package com.ranni.app.data.repository

import com.ranni.app.data.db.ExerciseDao
import com.ranni.app.data.model.Exercise
import kotlinx.coroutines.flow.Flow

class ExerciseRepository(private val dao: ExerciseDao) {
    fun getAllExercises(): Flow<List<Exercise>> = dao.getAllExercises()
    suspend fun getById(id: Long): Exercise? = dao.getById(id)
    suspend fun save(exercise: Exercise): Long = dao.insert(exercise)
    suspend fun delete(exercise: Exercise) = dao.delete(exercise)
}
