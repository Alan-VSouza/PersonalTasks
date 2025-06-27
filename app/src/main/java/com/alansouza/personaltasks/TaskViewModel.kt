package com.alansouza.personaltasks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.alansouza.personaltasks.data.FirebaseTaskRepository
import com.alansouza.personaltasks.model.Task
import com.alansouza.personaltasks.model.TaskStatus

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FirebaseTaskRepository()
    private val _activeTasks = MutableLiveData<List<Task>>()
    private val _deletedTasks = MutableLiveData<List<Task>>()
    private val _searchQuery = MutableLiveData<String>("")

    val activeTasks: LiveData<List<Task>> get() = _activeTasks
    val deletedTasks: LiveData<List<Task>> get() = _deletedTasks
    val filteredActiveTasks = MediatorLiveData<List<Task>>()

    init {
        _activeTasks.value = emptyList()
        _deletedTasks.value = emptyList()

        filteredActiveTasks.addSource(_activeTasks) { tasks ->
            applyFilter(tasks ?: emptyList(), _searchQuery.value)
        }
        filteredActiveTasks.addSource(_searchQuery) { query ->
            applyFilter(_activeTasks.value ?: emptyList(), query)
        }
        loadActiveTasks(true)
        loadDeletedTasks()
    }

    private fun applyFilter(tasks: List<Task>, query: String?) { // Remove nullable
        filteredActiveTasks.value = if (query.isNullOrBlank()) {
            tasks
        } else {
            tasks.filter { task ->
                task.title.contains(query, ignoreCase = true) ||
                        task.description?.contains(query, ignoreCase = true) ?: false
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
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
            TaskStatus.COMPLETED -> repository.markTaskAsCompleted(taskId)
            TaskStatus.ACTIVE -> repository.reactivateTask(taskId)
            TaskStatus.DELETED -> repository.softDeleteTask(taskId)
        }
    }

    fun setSortOrder(moreImportantFirst: Boolean) {
        loadActiveTasks(moreImportantFirst)
    }
}
