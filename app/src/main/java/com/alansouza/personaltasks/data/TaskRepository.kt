package com.alansouza.personaltasks.data

import androidx.lifecycle.LiveData
import com.alansouza.personaltasks.model.Task
import com.alansouza.personaltasks.model.TaskStatus

class TaskRepository(private val taskDao: TaskDao) {

    fun getAllTasksOrdered(sortMoreImportantFirst: Boolean): LiveData<List<Task>> =
        taskDao.getAllTasksOrdered(sortMoreImportantFirst)

    fun getDeletedTasks(): LiveData<List<Task>> =
        taskDao.getDeletedTasks()

    suspend fun insertTask(task: Task) = taskDao.insertTaskOnDatabase(task)
    suspend fun updateTask(task: Task) = taskDao.updateTaskOnDatabase(task)
    suspend fun updateTaskStatus(taskId: Int, newStatus: TaskStatus) =
        taskDao.updateTaskStatus(taskId, newStatus)
}
