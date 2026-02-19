package com.ranni.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val sets: Int,
    val setDurationSeconds: Int?,   // null = no timer
    val restDurationSeconds: Int
)
