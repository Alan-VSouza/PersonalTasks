package com.alansouza.personaltasks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.alansouza.personaltasks.data.FirebaseTaskRepository
import com.alansouza.personaltasks.model.Task
import com.alansouza.personaltasks.model.TaskStatus

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FirebaseTaskRepository()
    private val _activeTasks = MutableLiveData<List<Task>>()
    private val _deletedTasks = MutableLiveData<List<Task>>()

    val activeTasks: LiveData<List<Task>> get() = _activeTasks
    val deletedTasks: LiveData<List<Task>> get() = _deletedTasks

    init {
        loadActiveTasks(true)
        loadDeletedTasks()
    }

    fun loadActiveTasks(sortMoreImportantFirst: Boolean) {
        repository.getActiveTasks(sortMoreImportantFirst).observeForever { tasks ->
            _activeTasks.value = tasks
        }
    }

    fun loadDeletedTasks() {
        repository.getDeletedTasks().observeForever { tasks ->
            _deletedTasks.value = tasks
        }
    }

    fun updateTaskStatus(taskId: String, newStatus: TaskStatus) {
        when (newStatus) {
            TaskStatus.DELETED -> repository.softDeleteTask(taskId)
            TaskStatus.ACTIVE -> repository.reactivateTask(taskId)
            TaskStatus.COMPLETED -> repository.markTaskAsCompleted(taskId)
        }
    }

    fun setSortOrder(moreImportantFirst: Boolean) {
        loadActiveTasks(moreImportantFirst)
    }
}
