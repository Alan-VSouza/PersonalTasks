package com.alansouza.personaltasks.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    var title: String,
    var description: String,
    var dueDate: String,
    var importance: ImportanceLevel = ImportanceLevel.MEDIUM
) : Serializable