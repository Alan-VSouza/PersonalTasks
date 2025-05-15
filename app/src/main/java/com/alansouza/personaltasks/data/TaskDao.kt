package com.alansouza.personaltasks.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.alansouza.personaltasks.model.Task

@Dao
interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskOnDatabase(task: Task)

    @Update
    suspend fun updateTaskOnDatabase(task: Task)

    @Delete
    suspend fun deleteTaskOnDatabase(task: Task)

    @Query("SELECT * FROM tasks ORDER BY CASE importance WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 WHEN 'LIGHT' THEN 3 ELSE 4 END ASC, dueDate ASC, id DESC")
    fun getAllTasks(): LiveData<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Int): Task?
}
