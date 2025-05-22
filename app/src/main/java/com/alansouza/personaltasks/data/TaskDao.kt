package com.alansouza.personaltasks.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.alansouza.personaltasks.model.Task

/**
 * Data Access Object (DAO) para a entidade Task.
 * Define as operações de banco de dados para tarefas:
 * Inserir, atualizar, deletar, buscar todos ordenado, buscar por id
 */

@Dao
interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskOnDatabase(task: Task)

    @Update
    suspend fun updateTaskOnDatabase(task: Task)

    @Delete
    suspend fun deleteTaskOnDatabase(task: Task)

    /**
     * Recupera todas as tarefas do banco de dados, ordenadas por importância e data de conclusão.
     * As tarefas podem ser ordenadas com os itens mais importantes primeiro ou por último,
     * baseado no parâmetro [sortMoreImportantFirst].
     * Ordem de importância: HIGH (1), MEDIUM (2), LIGHT (3).
     * Ordenação secundária por data de conclusão (ascendente), terciária por ID (descendente).
     *
     * @param sortMoreImportantFirst Se verdadeiro, ordena as tarefas mais importantes primeiro caso contrário, ordena as menos importantes primeiro.
     * @return LiveData contendo a lista de tarefas.
     */
    @Query("""
        SELECT * FROM tasks
        ORDER BY
            CASE WHEN :sortMoreImportantFirst = 1 THEN 
                (CASE importance  
                    WHEN 'HIGH' THEN 1
                    WHEN 'MEDIUM' THEN 2
                    WHEN 'LIGHT' THEN 3
                    ELSE 4 
                END)
            END ASC, 
            CASE WHEN :sortMoreImportantFirst = 0 THEN 
                (CASE importance
                    WHEN 'HIGH' THEN 1
                    WHEN 'MEDIUM' THEN 2
                    WHEN 'LIGHT' THEN 3
                    ELSE 4
                END)
            END DESC, 
        dueDate ASC,
        id DESC
    """)
    fun getAllTasksOrdered(sortMoreImportantFirst: Boolean): LiveData<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Int): Task?
}
