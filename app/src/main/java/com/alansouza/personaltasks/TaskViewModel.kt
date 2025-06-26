package com.alansouza.personaltasks.viewmodel

import android.app.Application
import androidx.lifecycle.* // Adiciona esta importação
import com.alansouza.personaltasks.data.AppDatabase
import com.alansouza.personaltasks.data.TaskDao
import com.alansouza.personaltasks.model.Task
import com.alansouza.personaltasks.model.TaskStatus
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val taskDao: TaskDao = AppDatabase.getDatabase(application).taskDao()

    private val _sortOrder = MutableLiveData<Boolean>().apply { value = true }

    val tasks: LiveData<List<Task>> = _sortOrder.switchMap { sortOrder ->
        taskDao.getAllTasksOrdered(sortOrder)
    }

    val deletedTasks: LiveData<List<Task>> = taskDao.getDeletedTasks()

    fun setSortOrder(moreImportantFirst: Boolean) {
        _sortOrder.value = moreImportantFirst
    }

    fun updateTaskStatus(taskId: Int, newStatus: TaskStatus) {
        viewModelScope.launch {
            taskDao.updateTaskStatus(taskId, newStatus)
        }
    }
}
