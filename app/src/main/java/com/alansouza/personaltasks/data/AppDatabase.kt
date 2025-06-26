package com.alansouza.personaltasks.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.alansouza.personaltasks.model.Task
import com.alansouza.personaltasks.model.ImportanceLevel

/**
 * Conversores de tipo para o Room.
 * Permitem que o Room armazene e recupere tipos personalizados, como Enums.
 */
class Converters {
    /**
     * Converte um [ImportanceLevel] (enum) para sua representação String (o nome do enum).
     * Usado para salvar o nível de importância no banco de dados.
     * @param value O enum [ImportanceLevel].
     * @return O nome do enum como String, ou nulo se [value] for nulo.
     */
    @TypeConverter
    fun fromImportanceLevel(value: ImportanceLevel?): String? {
        return value?.name
    }

    /**
     * Converte uma String (o nome do enum) de volta para um [ImportanceLevel] (enum).
     * Usado para ler o nível de importância do banco de dados.
     * @param value A String que representa o nome do enum.
     * @return O enum [ImportanceLevel] correspondente, ou nulo se [value] for nulo ou inválido.
     */
    @TypeConverter
    fun toImportanceLevel(value: String?): ImportanceLevel? {
        return value?.let { ImportanceLevel.valueOf(it) } // Converte a String para o Enum
    }
}

/**
 * Classe principal do banco de dados da aplicação, usando Room.
 * Define as entidades do banco, a versão e os conversores de tipo.
 */
@Database(
    entities = [Task::class], // Lista de tabelas (entidades) do banco
    version = 3,              // Versão do banco. Incrementar ao mudar o esquema.
    exportSchema = false      // Não exportar o esquema do banco (opcional, mas comum para apps menores)
)
@TypeConverters(Converters::class) // Registra a classe de conversores
abstract class AppDatabase : RoomDatabase() {

    /**
     * Fornece acesso ao DAO [TaskDao] para interagir com a tabela de tarefas.
     */
    abstract fun taskDao(): TaskDao

    companion object {
        // @Volatile garante que o valor de INSTANCE seja sempre atualizado e visível para todas as threads.
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Retorna a instância singleton do [AppDatabase].
         * Cria o banco de dados na primeira vez que é chamado, usando um bloco sincronizado
         * para garantir que apenas uma instância seja criada (thread-safe).
         *
         * @param context O contexto da aplicação.
         * @return A instância única do [AppDatabase].
         */
        fun getDatabase(context: Context): AppDatabase {
            // Retorna a instância existente ou cria uma nova se for nula
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "personal_tasks_db" // Nome do arquivo do banco de dados
                )
                    // Permite que o Room recrie o banco se não houver uma migração definida
                    // ao incrementar a versão (útil para desenvolvimento).
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance // Armazena a instância criada
                instance // Retorna a nova instância
            }
        }
    }
}