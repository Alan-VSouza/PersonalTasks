// Repositório responsável por todas as operações CRUD de Task
// usando Firebase Realtime Database e autenticação do usuário.
package com.alansouza.personaltasks.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.alansouza.personaltasks.model.Task
import com.alansouza.personaltasks.model.TaskStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FirebaseTaskRepository {
    private val database = FirebaseDatabase.getInstance()
    // Obtém UID do usuário autenticado
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    // Insere nova tarefa (push gera ID único)
    fun insertTask(task: Task) {
        val ref = database.getReference("tasks/$userId").push()
        task.id = ref.key ?: ""
        ref.setValue(task)
    }

    // Atualiza tarefa existente
    fun updateTask(task: Task) {
        database.getReference("tasks/$userId/${task.id}").setValue(task)
    }

    // Marca tarefa como excluída (soft delete)
    fun softDeleteTask(taskId: String) {
        database.getReference("tasks/$userId/$taskId/status")
            .setValue(TaskStatus.DELETED.name)
    }

    // Reativa tarefa excluída
    fun reactivateTask(taskId: String) {
        database.getReference("tasks/$userId/$taskId/status")
            .setValue(TaskStatus.ACTIVE.name)
    }

    // Marca tarefa como concluída
    fun markTaskAsCompleted(taskId: String) {
        database.getReference("tasks/$userId/$taskId/status")
            .setValue(TaskStatus.COMPLETED.name)
    }

    // Retorna LiveData com tarefas ativas, opcionalmente ordenadas
    fun getActiveTasks(sortMoreImportantFirst: Boolean): LiveData<List<Task>> {
        return getTasksByStatus(TaskStatus.ACTIVE, sortMoreImportantFirst)
    }

    // Retorna LiveData com tarefas excluídas
    fun getDeletedTasks(): LiveData<List<Task>> {
        return getTasksByStatus(TaskStatus.DELETED, false)
    }

    // Interno: busca tarefas por status e aplica ordenação local
    private fun getTasksByStatus(
        status: TaskStatus,
        sortMoreImportantFirst: Boolean
    ): LiveData<List<Task>> {
        val liveData = MutableLiveData<List<Task>>()
        if (userId == null) {
            liveData.value = emptyList()
            return liveData
        }
        database.getReference("tasks/$userId")
            .orderByChild("status")
            .equalTo(status.name)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<Task>()
                    snapshot.children.forEach { ds ->
                        ds.getValue(Task::class.java)?.apply {
                            id = ds.key ?: ""
                            list.add(this)
                        }
                    }
                    // Ordena por importância e data
                    val sorted = if (sortMoreImportantFirst) {
                        list.sortedWith(compareByDescending<Task> { it.importance.ordinal }
                            .thenBy { it.dueDate })
                    } else {
                        list.sortedWith(compareBy<Task> { it.importance.ordinal }
                            .thenBy { it.dueDate })
                    }
                    liveData.value = sorted
                }
                override fun onCancelled(error: DatabaseError) {
                    liveData.value = emptyList()
                }
            })
        return liveData
    }
}
