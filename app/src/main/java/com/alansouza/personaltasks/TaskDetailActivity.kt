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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.alansouza.personaltasks.databinding.ActivityTaskDetailBinding
import com.alansouza.personaltasks.model.ImportanceLevel
import com.alansouza.personaltasks.model.Task
import com.alansouza.personaltasks.model.TaskStatus
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
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

    private lateinit var binding: ActivityTaskDetailBinding
    private lateinit var toolbarTaskDetail: Toolbar
    private lateinit var editTextTaskTitle: TextInputEditText
    private lateinit var textFieldLayoutTitle: TextInputLayout
    private lateinit var editTextTaskDescription: TextInputEditText
    private lateinit var editTextTaskDueDate: TextInputEditText
    private lateinit var textFieldLayoutDueDate: TextInputLayout
    private lateinit var spinnerImportanceLevel: Spinner
    private lateinit var spinnerCompleteTasks: Spinner
    private lateinit var buttonSave: Button
    private lateinit var buttonCancel: Button

    private var originalTitle: String = ""
    private var originalDescription: String = ""
    private var originalDueDate: String = ""
    private var originalImportance: ImportanceLevel = ImportanceLevel.MEDIUM
    private var originalState: TaskStatus = TaskStatus.ACTIVE

    private var currentMode: String? = null
    private val calendar = Calendar.getInstance()
    private var currentTask: Task? = null
    private var currentTaskId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicialização das views
        toolbarTaskDetail = binding.toolbarTaskDetail
        editTextTaskTitle = binding.editTextTaskTitle
        textFieldLayoutTitle = binding.textFieldLayoutTitle
        editTextTaskDescription = binding.editTextTaskDescription
        editTextTaskDueDate = binding.editTextTaskDueDate
        textFieldLayoutDueDate = binding.textFieldLayoutDueDate
        spinnerImportanceLevel = binding.spinnerImportanceLevel
        spinnerCompleteTasks = binding.finalizado
        buttonSave = binding.buttonSave
        buttonCancel = binding.buttonCancel

        setSupportActionBar(toolbarTaskDetail)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Configurações iniciais
        setupDatePicker()
        setupImportanceSpinner()
        setupTaskCompleted()

        // Obtém modo e ID da tarefa
        currentMode = intent.getStringExtra(EXTRA_MODE)
        currentTaskId = intent.getStringExtra(EXTRA_TASK_ID)

        // Se não vier o ID, tente pegar o objeto Task direto (ex: vindo de um putExtra("task", task))
        if (currentTaskId == null && intent.hasExtra("task")) {
            val task = intent.getSerializableExtra("task") as? Task
            currentTaskId = task?.id
            currentTask = task
        }

        // Configura UI conforme o modo
        setupUIForMode()

        // Configura listeners
        buttonSave.setOnClickListener { handleSaveOrConfirmAction() }
        buttonCancel.setOnClickListener { tryExitWithConfirmation() }
    }

    private fun setupUIForMode() {
        when (currentMode) {
            MODE_NEW -> {
                supportActionBar?.title = getString(R.string.title_new_task)
                buttonSave.text = getString(R.string.button_save)
                buttonSave.visibility = View.VISIBLE
                updateDateInView()
                originalTitle = ""
                originalDescription = ""
                originalDueDate = editTextTaskDueDate.text?.toString() ?: ""
                originalImportance = ImportanceLevel.MEDIUM
                originalState = TaskStatus.ACTIVE
            }
            MODE_EDIT, MODE_VIEW_DETAILS, MODE_DELETE_CONFIRM -> {
                // Carrega detalhes se necessário
                if (currentTask == null && currentTaskId != null) {
                    loadTaskDetails(currentTaskId!!)
                } else if (currentTask != null) {
                    preencherCampos(currentTask!!)
                } else {
                    Toast.makeText(this, "ID da tarefa inválido", Toast.LENGTH_LONG).show()
                    finish()
                }

                when (currentMode) {
                    MODE_EDIT -> {
                        supportActionBar?.title = getString(R.string.title_edit_task)
                        buttonSave.text = getString(R.string.button_save)
                        buttonSave.visibility = View.VISIBLE
                    }
                    MODE_VIEW_DETAILS -> {
                        supportActionBar?.title = getString(R.string.title_task_details)
                        buttonSave.visibility = View.GONE
                        disableEditing()
                    }
                    MODE_DELETE_CONFIRM -> {
                        supportActionBar?.title = getString(R.string.delete_task_title)
                        buttonSave.text = getString(R.string.delete)
                        buttonSave.visibility = View.VISIBLE
                        disableEditing()
                    }
                }
            }
            else -> {
                Toast.makeText(this, "Modo de operação desconhecido", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun preencherCampos(task: Task) {
        editTextTaskTitle.setText(task.title)
        editTextTaskDescription.setText(task.description)
        editTextTaskDueDate.setText(task.dueDate)
        spinnerImportanceLevel.setSelection(
            when (task.importance) {
                ImportanceLevel.HIGH -> 0
                ImportanceLevel.LIGHT -> 2
                else -> 1
            }
        )
        spinnerCompleteTasks.setSelection(
            when (task.status) {
                TaskStatus.COMPLETED -> 1
                else -> 0
            }
        )
        originalTitle = task.title
        originalDescription = task.description
        originalDueDate = task.dueDate
        originalImportance = task.importance
        originalState = task.status
    }

    private fun setupDatePicker() {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, day)
            updateDateInView()
        }

        editTextTaskDueDate.setOnClickListener {
            textFieldLayoutDueDate.error = null
            DatePickerDialog(
                this,
                dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).apply {
                datePicker.minDate = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }.show()
        }
    }

    private fun updateDateInView() {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        editTextTaskDueDate.setText(sdf.format(calendar.time))
        textFieldLayoutDueDate.error = null
    }

    private fun setupTaskCompleted() {
        val statusOptions = listOf(
            getString(R.string.status_incomplete),
            getString(R.string.status_complete)
        )

        spinnerCompleteTasks.adapter = ArrayAdapter(
            this,
            R.layout.spinner_item_dark,
            statusOptions
        ).apply {
            setDropDownViewResource(R.layout.spinner_item_dark)
        }

        if (currentMode == MODE_NEW) {
            spinnerCompleteTasks.setSelection(0)
        }
    }

    private fun setupImportanceSpinner() {
        val importanceOptions = ImportanceLevel.entries.map {
            when (it) {
                ImportanceLevel.HIGH -> getString(R.string.importance_high)
                ImportanceLevel.MEDIUM -> getString(R.string.importance_medium)
                ImportanceLevel.LIGHT -> getString(R.string.importance_light)
            }
        }

        spinnerImportanceLevel.adapter = ArrayAdapter(
            this,
            R.layout.spinner_item_dark,
            importanceOptions
        ).apply {
            setDropDownViewResource(R.layout.spinner_item_dark)
        }

        if (currentMode == MODE_NEW) {
            spinnerImportanceLevel.setSelection(1) // MÉDIA
        }
    }

    private fun handleSaveOrConfirmAction() {
        when (currentMode) {
            MODE_NEW, MODE_EDIT -> saveOrUpdateTask()
            MODE_DELETE_CONFIRM -> deleteTaskConfirmed()
        }
    }

    private fun saveOrUpdateTask() {
        val title = editTextTaskTitle.text.toString().trim()
        val description = editTextTaskDescription.text.toString().trim()
        val dueDate = editTextTaskDueDate.text.toString().trim()

        if (title.isEmpty()) {
            textFieldLayoutTitle.error = "Título obrigatório"
            return
        }
        if (description.isEmpty()) {
            textFieldLayoutTitle.error = null
            editTextTaskDescription.error = "Descrição obrigatória"
            return
        }
        if (dueDate.isEmpty()) {
            textFieldLayoutTitle.error = null
            editTextTaskDescription.error = null
            textFieldLayoutDueDate.error = "Data obrigatória"
            return
        }

        textFieldLayoutTitle.error = null
        editTextTaskDescription.error = null
        textFieldLayoutDueDate.error = null

        val status = when (spinnerCompleteTasks.selectedItem.toString()) {
            getString(R.string.status_complete) -> TaskStatus.COMPLETED
            else -> TaskStatus.ACTIVE
        }

        val importance = when (spinnerImportanceLevel.selectedItem.toString()) {
            getString(R.string.importance_high) -> ImportanceLevel.HIGH
            getString(R.string.importance_light) -> ImportanceLevel.LIGHT
            else -> ImportanceLevel.MEDIUM
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Usuário não autenticado", Toast.LENGTH_LONG).show()
            return
        }

        val database = FirebaseDatabase.getInstance().reference
        val tasksRef = database.child("tasks").child(userId)

        when (currentMode) {
            MODE_NEW -> {
                val newTaskRef = tasksRef.push()
                val newTask = Task(
                    id = newTaskRef.key ?: "",
                    title = title,
                    description = description,
                    dueDate = dueDate,
                    importance = importance,
                    status = status
                )

                newTaskRef.setValue(newTask)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Tarefa criada com sucesso!", Toast.LENGTH_SHORT).show()
                        val resultIntent = Intent().apply {
                            putExtra(EXTRA_MESSAGE_AFTER_OPERATION, "Tarefa criada com sucesso!")
                        }
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }

            MODE_EDIT -> {
                currentTask?.let { task ->
                    val updatedTask = task.copy(
                        title = title,
                        description = description,
                        dueDate = dueDate,
                        importance = importance,
                        status = status
                    )

                    tasksRef.child(task.id).setValue(updatedTask)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Tarefa atualizada!", Toast.LENGTH_SHORT).show()
                            val resultIntent = Intent().apply {
                                putExtra(EXTRA_MESSAGE_AFTER_OPERATION, "Tarefa atualizada!")
                            }
                            setResult(Activity.RESULT_OK, resultIntent)
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                } ?: run {
                    Toast.makeText(this, "Tarefa não encontrada", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun deleteTaskConfirmed() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null && currentTask != null) {
            FirebaseDatabase.getInstance().reference
                .child("tasks")
                .child(userId)
                .child(currentTask!!.id)
                .child("status")
                .setValue(TaskStatus.DELETED.name)
                .addOnSuccessListener {
                    handleSuccess("Tarefa movida para excluídas")
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun loadTaskDetails(taskId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            FirebaseDatabase.getInstance().reference
                .child("tasks")
                .child(userId)
                .child(taskId)
                .get()
                .addOnSuccessListener { snapshot ->
                    currentTask = snapshot.getValue(Task::class.java)?.apply {
                        id = taskId
                    }

                    currentTask?.let { preencherCampos(it) } ?: run {
                        Toast.makeText(this, "Tarefa não encontrada", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erro ao carregar tarefa", Toast.LENGTH_SHORT).show()
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
        spinnerCompleteTasks.isEnabled = false
    }

    private fun hasUnsavedChanges(): Boolean {
        val currentTitle = editTextTaskTitle.text.toString()
        val currentDescription = editTextTaskDescription.text.toString()
        val currentDueDate = editTextTaskDueDate.text.toString()

        val currentImportance = when (spinnerImportanceLevel.selectedItem.toString()) {
            getString(R.string.importance_high) -> ImportanceLevel.HIGH
            getString(R.string.importance_light) -> ImportanceLevel.LIGHT
            else -> ImportanceLevel.MEDIUM
        }

        val currentState = when (spinnerCompleteTasks.selectedItem.toString()) {
            getString(R.string.status_complete) -> TaskStatus.COMPLETED
            else -> TaskStatus.ACTIVE
        }

        return currentTitle != originalTitle ||
                currentDescription != originalDescription ||
                currentDueDate != originalDueDate ||
                currentImportance != originalImportance ||
                currentState != originalState
    }

    private fun tryExitWithConfirmation() {
        if (hasUnsavedChanges()) {
            AlertDialog.Builder(this)
                .setTitle("Descartar alterações?")
                .setMessage("Há alterações não salvas. Deseja realmente sair?")
                .setPositiveButton("Sim") { _, _ -> finish() }
                .setNegativeButton("Não", null)
                .show()
        } else {
            finish()
        }
    }

    private fun handleSuccess(message: String) {
        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(EXTRA_MESSAGE_AFTER_OPERATION, message)
        })
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            tryExitWithConfirmation()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (!hasUnsavedChanges()) {
            super.onBackPressed()
        } else {
            tryExitWithConfirmation()
        }
    }
}
