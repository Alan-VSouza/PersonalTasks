package com.alansouza.personaltasks.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.alansouza.personaltasks.model.Task
import com.alansouza.personaltasks.model.ImportanceLevel

class Converters {
    @TypeConverter
    fun fromImportanceLevel(value: ImportanceLevel?): String? {
        return value?.name
    }

    @TypeConverter
    fun toImportanceLevel(value: String?): ImportanceLevel? {
        return value?.let { ImportanceLevel.valueOf(it) }
    }
}

@Database(
    entities = [Task::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "personal_tasks_db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
