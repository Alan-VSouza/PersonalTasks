package com.alansouza.personaltasks.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

/**
 * Representa uma tarefa na aplicação.
 * É uma entidade Room (tabela "tasks") e é serializável para ser passada entre Activities.
 *
 * @property id O identificador único da tarefa (chave primária, autogerada).
 * @property title O título da tarefa.
 * @property description A descrição detalhada da tarefa.
 * @property dueDate A data limite para a conclusão da tarefa (formato String "dd/MM/yyyy").
 * @property importance O nível de importância da tarefa (padrão: MÉDIA).
 */
@Entity(tableName = "tasks") // Define o nome da tabela no banco de dados
data class Task(
    @PrimaryKey(autoGenerate = true) // Define 'id' como chave primária autoincrementável
    val id: Int = 0,
    var title: String,
    var description: String,
    var dueDate: String, // Data como String, formato "dd/MM/yyyy"
    var importance: ImportanceLevel = ImportanceLevel.MEDIUM, // Nível de importância padrão
    var isChecked: TaskStatus = TaskStatus.INCOMPLETED
) : Serializable // Permite que objetos Task sejam passados em Intents