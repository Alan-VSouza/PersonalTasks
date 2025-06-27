package com.alansouza.personaltasks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.alansouza.personaltasks.data.FirebaseTaskRepository
import com.alansouza.personaltasks.model.Task
import com.alansouza.personaltasks.model.TaskStatus

/**
 * ViewModel responsável por gerenciar a lista de tarefas ativas, excluídas e o filtro de busca.
 * Herda AndroidViewModel para ter acesso ao contexto da aplicação, se necessário.
 */
class TaskViewModel(application: Application) : AndroidViewModel(application) {

    // Repositório que abstrai operações de leitura/escrita no Firebase Realtime Database
    private val repository = FirebaseTaskRepository()

    // LiveData interna para armazenar todas as tarefas ativas carregadas do repositório
    private val _activeTasks = MutableLiveData<List<Task>>()

    // LiveData interna para armazenar todas as tarefas excluídas carregadas do repositório
    private val _deletedTasks = MutableLiveData<List<Task>>()

    // LiveData interna que guarda o texto digitado no campo de busca
    private val _searchQuery = MutableLiveData<String>("")

    // Exposição imutável das tarefas ativas para a View
    val activeTasks: LiveData<List<Task>> get() = _activeTasks

    // Exposição imutável das tarefas excluídas para a View
    val deletedTasks: LiveData<List<Task>> get() = _deletedTasks

    /**
     * LiveData que combina as tarefas ativas e o texto de busca (searchQuery),
     * retornando apenas as tarefas cujo título ou descrição contenham o termo pesquisado.
     * Usa MediatorLiveData para reagir a mudanças em ambas as fontes.
     */
    val filteredActiveTasks = MediatorLiveData<List<Task>>()

    init {
        // Inicializa as listas com lista vazia para evitar null pointers
        _activeTasks.value = emptyList()
        _deletedTasks.value = emptyList()

        // Adiciona como fonte de dados o _activeTasks
        filteredActiveTasks.addSource(_activeTasks) { tasks ->
            // Aplica filtro sempre que a lista de tarefas ativas for atualizada
            applyFilter(tasks, _searchQuery.value)
        }

        // Adiciona como fonte de dados o _searchQuery
        filteredActiveTasks.addSource(_searchQuery) { query ->
            // Aplica filtro sempre que o texto de busca for alterado
            applyFilter(_activeTasks.value ?: emptyList(), query)
        }

        // Carrega dados iniciais
        loadActiveTasks(sortMoreImportantFirst = true)
        loadDeletedTasks()
    }

    /**
     * Aplica o filtro de busca sobre a lista de tarefas.
     * Se a query estiver vazia ou nula, retorna a lista completa.
     * Caso contrário, retorna apenas as tarefas
     * cujo título ou descrição contenham o termo (ignora maiúsculas/minúsculas).
     *
     * @param tasks lista de todas tarefas ativas
     * @param query termo de busca digitado pelo usuário
     */
    private fun applyFilter(tasks: List<Task>, query: String?) {
        filteredActiveTasks.value = if (query.isNullOrBlank()) {
            tasks
        } else {
            tasks.filter { task ->
                // Verifica título
                task.title.contains(query, ignoreCase = true)
                        // ou descrição (pode ser nula)
                        || (task.description?.contains(query, ignoreCase = true) ?: false)
            }
        }
    }

    /**
     * Atualiza o termo de busca. Deve ser chamado pela Activity
     * sempre que o usuário digitar no campo de pesquisa.
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Carrega as tarefas ativas do repositório,
     * observando o LiveData retornado pelo FirebaseTaskRepository.
     *
     * @param sortMoreImportantFirst determina a ordem de exibição por importância
     */
    fun loadActiveTasks(sortMoreImportantFirst: Boolean) {
        repository.getActiveTasks(sortMoreImportantFirst)
            .observeForever { tasks ->
                // Atualiza a LiveData interna para notificar as Views
                _activeTasks.value = tasks
            }
    }

    /**
     * Carrega as tarefas excluídas do repositório,
     * observando o LiveData retornado pelo FirebaseTaskRepository.
     */
    fun loadDeletedTasks() {
        repository.getDeletedTasks()
            .observeForever { tasks ->
                _deletedTasks.value = tasks
            }
    }

    /**
     * Atualiza o status de uma tarefa (COMPLETED, ACTIVE ou DELETED)
     * acionando o método correspondente no repositório.
     *
     * @param taskId ID da tarefa a ser atualizada
     * @param newStatus novo status a ser atribuído
     */
    fun updateTaskStatus(taskId: String, newStatus: TaskStatus) {
        when (newStatus) {
            TaskStatus.COMPLETED -> repository.markTaskAsCompleted(taskId)
            TaskStatus.ACTIVE    -> repository.reactivateTask(taskId)
            TaskStatus.DELETED   -> repository.softDeleteTask(taskId)
        }
    }

    /**
     * Altera a ordenação das tarefas ativas e recarrega a lista.
     * Útil quando o usuário escolher ordenar por importância.
     *
     * @param moreImportantFirst true para ordenar mais importantes primeiro
     */
    fun setSortOrder(moreImportantFirst: Boolean) {
        loadActiveTasks(moreImportantFirst)
    }
}
