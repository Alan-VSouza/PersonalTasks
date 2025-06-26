package com.alansouza.personaltasks.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.alansouza.personaltasks.model.Task
import com.alansouza.personaltasks.model.TaskStatus

/**
 * Data Access Object (DAO) para a entidade [Task].
 * Define as operações de banco de dados para tarefas, como:
 * Inserir, atualizar, deletar, buscar todas ordenadas e buscar por ID.
 */
@Dao
interface TaskDao {

    /**
     * Insere uma tarefa no banco de dados.
     * Se uma tarefa com o mesmo ID já existir, ela será substituída.
     * Operação suspensa (assíncrona).
     * @param task A tarefa a ser inserida.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskOnDatabase(task: Task)

    /**
     * Atualiza uma tarefa existente no banco de dados.
     * Operação suspensa (assíncrona).
     * @param task A tarefa a ser atualizada.
     */
    @Update
    suspend fun updateTaskOnDatabase(task: Task)

    /**
     * Deleta uma tarefa do banco de dados.
     * Operação suspensa (assíncrona).
     * @param task A tarefa a ser deletada.
     */
    @Delete
    suspend fun deleteTaskOnDatabase(task: Task)

    /**
     * Recupera todas as tarefas do banco de dados, ordenadas condicionalmente.
     * A ordenação primária é por importância (HIGH > MEDIUM > LIGHT ou vice-versa),
     * seguida pela data de conclusão (dueDate, ascendente) e por ID (descendente).
     *
     * @param sortMoreImportantFirst Se `true`, ordena as tarefas mais importantes primeiro.
     *                             Se `false`, ordena as menos importantes primeiro.
     * @return Um [LiveData] contendo a lista de tarefas ordenadas.
     */
    @Query("""
        SELECT * FROM tasks
        ORDER BY
            -- Ordena por importância (ascendente se mais importante primeiro)
            CASE WHEN :sortMoreImportantFirst = 1 THEN
                (CASE importance
                    WHEN 'HIGH' THEN 1
                    WHEN 'MEDIUM' THEN 2
                    WHEN 'LIGHT' THEN 3
                    ELSE 4 -- Para valores inesperados, coloca no final
                END)
            END ASC,
            -- Ordena por importância (descendente se menos importante primeiro)
            CASE WHEN :sortMoreImportantFirst = 0 THEN
                (CASE importance
                    WHEN 'HIGH' THEN 1 -- HIGH (1) será ordenado por último com DESC
                    WHEN 'MEDIUM' THEN 2
                    WHEN 'LIGHT' THEN 3 -- LIGHT (3) será ordenado primeiro com DESC
                    ELSE 4
                END)
            END DESC,
        dueDate ASC, -- Ordenação secundária: data de conclusão mais próxima primeiro
        id DESC      -- Ordenação terciária: tarefas mais recentes (maior ID) primeiro
    """)
    fun getAllTasksOrdered(sortMoreImportantFirst: Boolean): LiveData<List<Task>>

    /**
     * Recupera uma tarefa específica pelo seu ID.
     * Operação suspensa (assíncrona).
     * @param taskId O ID da tarefa a ser buscada.
     * @return A [Task] correspondente, ou `null` se não encontrada.
     */
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Int): Task?

    @Query("SELECT * FROM tasks WHERE status = 'DELETED'")
    fun getDeletedTasks(): LiveData<List<Task>>

    @Query("UPDATE tasks SET status = :newStatus WHERE id = :taskId")
    suspend fun updateTaskStatus(taskId: Int, newStatus: TaskStatus)

    @Query("UPDATE tasks SET status = 'COMPLETED' WHERE id = :taskId")
    suspend fun markTaskAsCompleted(taskId: Int)

}