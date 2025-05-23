package com.alansouza.personaltasks

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.alansouza.personaltasks.data.AppDatabase
import com.alansouza.personaltasks.data.TaskDao
import com.alansouza.personaltasks.model.ImportanceLevel
import com.alansouza.personaltasks.model.Task
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TaskDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MODE = "MODE"
        const val EXTRA_TASK_ID = "TASK_ID"
        const val EXTRA_MESSAGE_AFTER_OPERATION = "MESSAGE_AFTER_OPERATION"
        const val MODE_NEW = "NEW"
        const val MODE_EDIT = "EDIT"
        const val MODE_VIEW_DETAILS = "DETAILS"
        const val MODE_DELETE_CONFIRM = "DELETE_CONFIRM"
    }

    private lateinit var toolbarTaskDetail: Toolbar
    private lateinit var editTextTaskTitle: TextInputEditText
    private lateinit var textFieldLayoutTitle: TextInputLayout
    private lateinit var editTextTaskDescription: TextInputEditText
    private lateinit var editTextTaskDueDate: TextInputEditText
    private lateinit var textFieldLayoutDueDate: TextInputLayout
    private lateinit var spinnerImportanceLevel: Spinner
    private lateinit var buttonSave: Button
    private lateinit var buttonCancel: Button

    private lateinit var taskDao: TaskDao
    private var currentMode: String? = null
    private val calendar = Calendar.getInstance()

    private var currentTask: Task? = null
    private var currentTaskIdFromIntent: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_detail)

        toolbarTaskDetail = findViewById(R.id.toolbar_task_detail)
        editTextTaskTitle = findViewById(R.id.editTextTaskTitle)
        textFieldLayoutTitle = findViewById(R.id.textFieldLayoutTitle)
        editTextTaskDescription = findViewById(R.id.editTextTaskDescription)
        editTextTaskDueDate = findViewById(R.id.editTextTaskDueDate)
        textFieldLayoutDueDate = findViewById(R.id.textFieldLayoutDueDate)
        spinnerImportanceLevel = findViewById(R.id.spinnerImportanceLevel)
        buttonSave = findViewById(R.id.buttonSave)
        buttonCancel = findViewById(R.id.buttonCancel)

        setSupportActionBar(toolbarTaskDetail)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        taskDao = AppDatabase.getDatabase(applicationContext).taskDao()
        setupDatePicker()
        setupImportanceSpinner()

        currentMode = intent.getStringExtra(EXTRA_MODE)
        currentTaskIdFromIntent = intent.getIntExtra(EXTRA_TASK_ID, -1)

        setupUIForMode()

        buttonSave.setOnClickListener {
            handleSaveOrConfirmAction()
        }

        buttonCancel.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    private fun setupUIForMode() {
        when (currentMode) {
            MODE_NEW -> {
                supportActionBar?.title = getString(R.string.title_new_task)
                buttonSave.text = getString(R.string.button_save)
                buttonSave.visibility = View.VISIBLE
                updateDateInView()
            }
            MODE_EDIT -> {
                supportActionBar?.title = getString(R.string.title_edit_task)
                buttonSave.text = getString(R.string.button_save)
                buttonSave.visibility = View.VISIBLE
                if (currentTaskIdFromIntent != -1) {
                    loadTaskDetails(currentTaskIdFromIntent)
                } else {
                    Toast.makeText(this, "Erro: ID da tarefa inválido para edição.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            MODE_VIEW_DETAILS -> {
                supportActionBar?.title = getString(R.string.title_task_details)
                buttonSave.visibility = View.GONE
                buttonCancel.text = getString(R.string.button_cancel)
                if (currentTaskIdFromIntent != -1) {
                    loadTaskDetails(currentTaskIdFromIntent)
                    disableEditing()
                } else {
                    Toast.makeText(this, "Erro: ID da tarefa inválido para visualização.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            MODE_DELETE_CONFIRM -> {
                supportActionBar?.title = getString(R.string.delete_task_title)
                buttonSave.text = getString(R.string.delete)
                buttonSave.visibility = View.VISIBLE
                buttonCancel.text = getString(R.string.cancel)
                if (currentTaskIdFromIntent != -1) {
                    loadTaskDetails(currentTaskIdFromIntent)
                    disableEditing()
                } else {
                    Toast.makeText(this, "Erro: ID da tarefa inválido para exclusão.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            else -> {
                Toast.makeText(this, "Modo de operação desconhecido.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun setupDatePicker() {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, monthOfYear)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateInView()
        }

        editTextTaskDueDate.setOnClickListener {
            textFieldLayoutDueDate.error = null
            val dialog = DatePickerDialog(
                this,
                dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            if (currentMode == MODE_NEW) {
                val todayCalendar = Calendar.getInstance()
                todayCalendar.set(Calendar.HOUR_OF_DAY, 0)
                todayCalendar.set(Calendar.MINUTE, 0)
                todayCalendar.set(Calendar.SECOND, 0)
                todayCalendar.set(Calendar.MILLISECOND, 0)
                dialog.datePicker.minDate = todayCalendar.timeInMillis
            }

            dialog.show()
        }
    }

    private fun updateDateInView() {
        val myFormat = "dd/MM/yyyy"
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        editTextTaskDueDate.setText(sdf.format(calendar.time))
        textFieldLayoutDueDate.error = null
    }

    private fun setupImportanceSpinner() {
        val importanceLevelsDisplay = ImportanceLevel.entries.map { level ->
            when (level) {
                ImportanceLevel.HIGH -> getString(R.string.importance_high)
                ImportanceLevel.MEDIUM -> getString(R.string.importance_medium)
                ImportanceLevel.LIGHT -> getString(R.string.importance_light)
            }
        }
        val adapter = ArrayAdapter(
            this,
            R.layout.spinner_item_dark,
            importanceLevelsDisplay
        )
        adapter.setDropDownViewResource(R.layout.spinner_item_dark)
        spinnerImportanceLevel.adapter = adapter

        if (currentMode == MODE_NEW) {
            spinnerImportanceLevel.setSelection(ImportanceLevel.entries.indexOf(ImportanceLevel.MEDIUM))
        }
    }

    private fun handleSaveOrConfirmAction() {
        when (currentMode) {
            MODE_NEW, MODE_EDIT -> saveOrUpdateTask()
            MODE_DELETE_CONFIRM -> deleteTaskConfirmed()
            else -> {
                Toast.makeText(this, "Ação inválida para o modo atual.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveOrUpdateTask() {
        val title = editTextTaskTitle.text.toString().trim()
        val description = editTextTaskDescription.text.toString().trim()
        val dueDate = editTextTaskDueDate.text.toString().trim()

        var isValid = true
        if (!isValid) return

        val selectedImportanceDisplayString = spinnerImportanceLevel.selectedItem.toString()
        val importance = when(selectedImportanceDisplayString) {
            getString(R.string.importance_high) -> ImportanceLevel.HIGH
            getString(R.string.importance_medium) -> ImportanceLevel.MEDIUM
            getString(R.string.importance_light) -> ImportanceLevel.LIGHT
            else -> ImportanceLevel.MEDIUM
        }

        if (currentMode == MODE_NEW) {
            val newTask = Task(
                title = title,
                description = description,
                dueDate = dueDate,
                importance = importance
            )
            lifecycleScope.launch {
                taskDao.insertTaskOnDatabase(newTask)
                val operationMessage = getString(R.string.task_created_successfully)

                val resultIntent = Intent()
                resultIntent.putExtra(EXTRA_MESSAGE_AFTER_OPERATION, operationMessage)
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
        } else if (currentMode == MODE_EDIT) {
            val taskToUpdate = currentTask
            if (taskToUpdate != null) {
                val updatedTask = taskToUpdate.copy(
                    title = title,
                    description = description,
                    dueDate = dueDate,
                    importance = importance
                )
                lifecycleScope.launch {
                    taskDao.updateTaskOnDatabase(updatedTask)
                    val operationMessage = getString(R.string.task_updated_successfully)

                    val resultIntent = Intent()
                    resultIntent.putExtra(EXTRA_MESSAGE_AFTER_OPERATION, operationMessage)
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }
            } else {
                Toast.makeText(this, "Erro ao salvar: tarefa original não encontrada.", Toast.LENGTH_LONG).show()
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        }
    }

    private fun deleteTaskConfirmed() {
        val taskToDelete = currentTask
        if (taskToDelete != null) {
            lifecycleScope.launch {
                taskDao.deleteTaskOnDatabase(taskToDelete)
                val operationMessage = getString(R.string.task_deleted_message, taskToDelete.title)

                val resultIntent = Intent()
                resultIntent.putExtra(EXTRA_MESSAGE_AFTER_OPERATION, operationMessage)
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
        } else {
            Toast.makeText(this, "Erro ao excluir: tarefa não encontrada.", Toast.LENGTH_LONG).show()
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    private fun loadTaskDetails(taskId: Int) {
        lifecycleScope.launch {
            val taskFromDb = taskDao.getTaskById(taskId)
            currentTask = taskFromDb

            if (taskFromDb != null) {
                editTextTaskTitle.setText(taskFromDb.title)
                editTextTaskDescription.setText(taskFromDb.description)

                if (taskFromDb.dueDate.isNotEmpty()) {
                    try {
                        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        val parsedDate = sdf.parse(taskFromDb.dueDate)
                        if (parsedDate != null) {
                            calendar.time = parsedDate
                            updateDateInView()
                        } else {
                            editTextTaskDueDate.setText("")
                        }
                    } catch (e: Exception) {
                        editTextTaskDueDate.setText("")
                    }
                } else {
                    editTextTaskDueDate.setText("")
                }

                val importanceIndex = ImportanceLevel.entries.indexOf(taskFromDb.importance)
                if (importanceIndex >= 0) {
                    spinnerImportanceLevel.setSelection(importanceIndex)
                }
            } else {
                Toast.makeText(this@TaskDetailActivity, "Tarefa não encontrada.", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        }
    }

    private fun disableEditing() {
        editTextTaskTitle.isEnabled = false
        textFieldLayoutTitle.isEnabled = false
        editTextTaskDescription.isEnabled = false
        editTextTaskDueDate.isEnabled = false
        textFieldLayoutDueDate.isEnabled = false
        spinnerImportanceLevel.isEnabled = false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                setResult(Activity.RESULT_CANCELED)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
