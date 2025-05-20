package com.alansouza.personaltasks

import android.app.Activity
import android.app.DatePickerDialog
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TaskDetailActivity : AppCompatActivity() {

    private lateinit var toolbarTaskDetail: Toolbar
    private lateinit var editTextTaskTitle: TextInputEditText
    private lateinit var editTextTaskDescription: TextInputEditText
    private lateinit var editTextTaskDueDate: TextInputEditText
    private lateinit var spinnerImportanceLevel: Spinner
    private lateinit var buttonSave: Button
    private lateinit var buttonCancel: Button

    private lateinit var taskDao: TaskDao
    private var mode: String? = null
    private val calendar = Calendar.getInstance()

    private var currentTask: Task? = null
    private var currentTaskId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_detail)

        toolbarTaskDetail = findViewById(R.id.toolbar_task_detail)
        editTextTaskTitle = findViewById(R.id.editTextTaskTitle)
        editTextTaskDescription = findViewById(R.id.editTextTaskDescription)
        editTextTaskDueDate = findViewById(R.id.editTextTaskDueDate)
        spinnerImportanceLevel = findViewById(R.id.spinnerImportanceLevel)
        buttonSave = findViewById(R.id.buttonSave)
        buttonCancel = findViewById(R.id.buttonCancel)

        setSupportActionBar(toolbarTaskDetail)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        taskDao = AppDatabase.getDatabase(applicationContext).taskDao()
        setupDatePicker()
        setupImportanceSpinner()

        mode = intent.getStringExtra("MODE")
        currentTaskId = intent.getIntExtra("TASK_ID", -1)

        when (mode) {
            "NEW" -> {
                supportActionBar?.title = getString(R.string.title_new_task)
                updateDateInView()
            }
            "EDIT" -> {
                supportActionBar?.title = getString(R.string.title_edit_task)
                if (currentTaskId != -1) {
                    loadTaskDetails(currentTaskId)
                } else {
                    Toast.makeText(this, "Erro: ID da tarefa inválido para edição.", Toast.LENGTH_LONG).show()
                    finish()
                    return
                }
            }
            "DETAILS" -> {
                supportActionBar?.title = getString(R.string.title_task_details)
                if (currentTaskId != -1) {
                    loadTaskDetails(currentTaskId)
                    disableEditing()
                } else {
                    Toast.makeText(this, "Erro: ID da tarefa inválido para visualização.", Toast.LENGTH_LONG).show()
                    finish()
                    return
                }
            }
            else -> {
                Toast.makeText(this, "Modo de operação desconhecido.", Toast.LENGTH_LONG).show()
                finish()
                return
            }
        }

        buttonSave.setOnClickListener {
            saveTask()
        }

        buttonCancel.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
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
            val datePickerDialog = DatePickerDialog(
                this,
                dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.datePicker.minDate = Calendar.getInstance().timeInMillis
            datePickerDialog.show()
        }
    }

    private fun updateDateInView() {
        val myFormat = "dd/MM/yyyy"
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        editTextTaskDueDate.setText(sdf.format(calendar.time))
    }

    private fun setupImportanceSpinner() {
        val importanceLevelsDisplay = ImportanceLevel.values().map { level ->
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
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerImportanceLevel.adapter = adapter
        if (mode == "NEW") {
            spinnerImportanceLevel.setSelection(ImportanceLevel.values().indexOf(ImportanceLevel.MEDIUM))
        }
    }

    private fun saveTask() {
        val title = editTextTaskTitle.text.toString().trim()
        val description = editTextTaskDescription.text.toString().trim()
        val dueDate = editTextTaskDueDate.text.toString().trim()

        if (title.isEmpty()) {
            editTextTaskTitle.error = getString(R.string.error_title_empty)
            editTextTaskTitle.requestFocus()
            return
        }
        editTextTaskTitle.error = null

        if (dueDate.isEmpty()) {
            editTextTaskDueDate.error = getString(R.string.error_due_date_empty)
            return
        }
        editTextTaskDueDate.error = null

        val selectedImportanceDisplayString = spinnerImportanceLevel.selectedItem.toString()
        val importance = when(selectedImportanceDisplayString) {
            getString(R.string.importance_high) -> ImportanceLevel.HIGH
            getString(R.string.importance_medium) -> ImportanceLevel.MEDIUM
            getString(R.string.importance_light) -> ImportanceLevel.LIGHT
            else -> ImportanceLevel.MEDIUM
        }

        if (mode == "NEW") {
            val newTask = Task(
                title = title,
                description = description,
                dueDate = dueDate,
                importance = importance
            )
            lifecycleScope.launch {
                taskDao.insertTaskOnDatabase(newTask)
                Toast.makeText(this@TaskDetailActivity, getString(R.string.toast_task_saved), Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_OK)
                finish()
            }
        } else if (mode == "EDIT" && currentTask != null) {
            val updatedTask = currentTask!!.copy(
                title = title,
                description = description,
                dueDate = dueDate,
                importance = importance
            )
            lifecycleScope.launch {
                taskDao.updateTaskOnDatabase(updatedTask)
                Toast.makeText(this@TaskDetailActivity, getString(R.string.toast_task_updated), Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_OK)
                finish()
            }
        } else if (mode == "EDIT" && currentTask == null) {
            Toast.makeText(this, "Erro ao salvar: tarefa original não encontrada.", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadTaskDetails(taskId: Int) {
        lifecycleScope.launch {
            currentTask = taskDao.getTaskById(taskId)
            currentTask?.let { task ->
                editTextTaskTitle.setText(task.title)
                editTextTaskDescription.setText(task.description)

                if (task.dueDate.isNotEmpty()) {
                    try {
                        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        val parsedDate = sdf.parse(task.dueDate)
                        parsedDate?.let {
                            calendar.time = it
                            updateDateInView()
                        }
                    } catch (e: Exception) {
                        editTextTaskDueDate.setText("")
                    }
                } else {
                    editTextTaskDueDate.setText("")
                }

                val importanceIndex = ImportanceLevel.values().indexOf(task.importance)
                if (importanceIndex >= 0) {
                    spinnerImportanceLevel.setSelection(importanceIndex)
                }
            } ?: run {
                Toast.makeText(this@TaskDetailActivity, "Tarefa não encontrada.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun disableEditing() {
        editTextTaskTitle.isEnabled = false
        editTextTaskDescription.isEnabled = false
        editTextTaskDueDate.isEnabled = false
        spinnerImportanceLevel.isEnabled = false
        buttonSave.visibility = View.GONE
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
