package com.alansouza.personaltasks.model

import java.io.Serializable

/**
 * Representa uma tarefa na aplicação.
 * Modelo de dados usado para persistência no Firebase Realtime Database.
 * A classe é serializável para ser passada entre Activities.
 *
 * @property id Identificador único da tarefa, gerado pelo Firebase (chave do nó).
 * @property title Título da tarefa.
 * @property description Descrição detalhada da tarefa.
 * @property dueDate Data limite para a conclusão da tarefa (formato String "dd/MM/yyyy").
 * @property importance Nível de importância da tarefa (padrão: MÉDIA).
 * @property status Estado atual da tarefa (ativo, concluído, excluído).
 */
data class Task(
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var dueDate: String = "",
    var importance: ImportanceLevel = ImportanceLevel.MEDIUM,
    var status: TaskStatus = TaskStatus.ACTIVE
) : Serializable {
    constructor() : this("", "", "", "", ImportanceLevel.MEDIUM, TaskStatus.ACTIVE)
}
