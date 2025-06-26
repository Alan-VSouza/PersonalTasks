package com.alansouza.personaltasks.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.alansouza.personaltasks.model.ImportanceLevel
import com.alansouza.personaltasks.model.Task
import com.alansouza.personaltasks.model.TaskStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import kotlinx.coroutines.tasks.await

class FirebaseTaskRepository {

    private val database = FirebaseDatabase.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    fun insertTask(task: Task) {
        val taskRef = database.getReference("tasks/$userId").push()
        task.id = taskRef.key ?: ""
        taskRef.setValue(task)
    }

    fun updateTask(task: Task) {
        database.getReference("tasks/$userId/${task.id}").setValue(task)
    }

    fun softDeleteTask(taskId: String) {
        database.getReference("tasks/$userId/$taskId/status")
            .setValue(TaskStatus.DELETED.name)
    }

    fun reactivateTask(taskId: String) {
        database.getReference("tasks/$userId/$taskId/status")
            .setValue(TaskStatus.ACTIVE.name)
    }

    fun markTaskAsCompleted(taskId: String) {
        database.getReference("tasks/$userId/$taskId/status")
            .setValue(TaskStatus.COMPLETED.name)
    }

    fun getActiveTasks(sortMoreImportantFirst: Boolean): LiveData<List<Task>> {
        return getTasksByStatus(TaskStatus.ACTIVE, sortMoreImportantFirst)
    }

    fun getDeletedTasks(): LiveData<List<Task>> {
        return getTasksByStatus(TaskStatus.DELETED, false)
    }

    private fun getTasksByStatus(
        status: TaskStatus,
        sortMoreImportantFirst: Boolean
    ): LiveData<List<Task>> {
        val result = MutableLiveData<List<Task>>()

        if (userId == null) {
            result.value = emptyList()
            return result
        }

        database.getReference("tasks/$userId")
            .orderByChild("status")
            .equalTo(status.name)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val tasks = mutableListOf<Task>()

                    snapshot.children.forEach { taskSnapshot ->
                        taskSnapshot.getValue<Task>()?.let { task ->
                            task.id = taskSnapshot.key ?: ""
                            tasks.add(task)
                        }
                    }

                    // Ordenação local
                    val sortedTasks = if (sortMoreImportantFirst) {
                        tasks.sortedWith(compareByDescending<Task> { task ->
                            when (task.importance) {
                                ImportanceLevel.HIGH -> 3
                                ImportanceLevel.MEDIUM -> 2
                                ImportanceLevel.LIGHT -> 1
                            }
                        }.thenBy { it.dueDate })
                    } else {
                        tasks.sortedWith(compareBy<Task> { task ->
                            when (task.importance) {
                                ImportanceLevel.HIGH -> 3
                                ImportanceLevel.MEDIUM -> 2
                                ImportanceLevel.LIGHT -> 1
                            }
                        }.thenBy { it.dueDate })
                    }

                    result.value = sortedTasks
                }

                override fun onCancelled(error: DatabaseError) {
                    result.value = emptyList()
                }
            })

        return result
    }

    suspend fun getTaskById(taskId: String): Task? {
        return try {
            val snapshot = database.getReference("tasks/$userId/$taskId").get().await()
            snapshot.getValue<Task>()?.apply { id = snapshot.key ?: "" }
        } catch (e: Exception) {
            null
        }
    }
}
