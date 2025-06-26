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
import androidx.lifecycle.lifecycleScope
import com.alansouza.personaltasks.data.AppDatabase
import com.alansouza.personaltasks.data.TaskDao
import com.alansouza.personaltasks.model.ImportanceLevel
import com.alansouza.personaltasks.model.Task
import com.alansouza.personaltasks.model.TaskStatus
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Activity para Criar, Visualizar, Editar ou Confirmar a Exclusão de uma Tarefa.
 * O comportamento da tela é definido pelo 'modo' passado via Intent.
 */
class TaskDetailActivity : AppCompatActivity() {

    // Companion object para definir constantes usadas para passar dados via Intent e definir modos de operação.
    companion object {
        const val EXTRA_MODE = "MODE"                     // Chave para o modo da activity (NOVO, EDITAR, etc.)
        const val EXTRA_TASK_ID = "TASK_ID"               // Chave para o ID da tarefa (usado em EDITAR, DETALHES, EXCLUIR)
        const val EXTRA_MESSAGE_AFTER_OPERATION = "MESSAGE_AFTER_OPERATION" // Chave para a mensagem de feedback para MainActivity
        // Modos de operação da Activity
        const val MODE_NEW = "NEW"                        // Modo para criar uma nova tarefa
        const val MODE_EDIT = "EDIT"                      // Modo para editar uma tarefa existente
        const val MODE_VIEW_DETAILS = "DETAILS"           // Modo para visualizar detalhes de uma tarefa (somente leitura)
        const val MODE_DELETE_CONFIRM = "DELETE_CONFIRM"  // Modo para confirmar a exclusão de uma tarefa
    }

    // Declaração das Views da UI que serão inicializadas no onCreate
    private lateinit var toolbarTaskDetail: Toolbar
    private lateinit var editTextTaskTitle: TextInputEditText
    private lateinit var textFieldLayoutTitle: TextInputLayout // Usado para exibir erros de validação do título
    private lateinit var editTextTaskDescription: TextInputEditText
    private lateinit var editTextTaskDueDate: TextInputEditText
    private lateinit var textFieldLayoutDueDate: TextInputLayout // Usado para exibir erros de validação da data
    private lateinit var spinnerImportanceLevel: Spinner
    private lateinit var spinnerCompleteTasks: Spinner
    private lateinit var buttonSave: Button       // Botão principal (Salvar, Confirmar Exclusão)
    private lateinit var buttonCancel: Button     // Botão secundário (Cancelar, Voltar)

    // Para detecção de alterações não salvas
    private var originalTitle: String = ""
    private var originalDescription: String = ""
    private var originalDueDate: String = ""
    private var originalImportance: ImportanceLevel = ImportanceLevel.MEDIUM
    private var originalState: TaskStatus = TaskStatus.ACTIVE

    // Componentes de dados e estado da Activity
    private lateinit var taskDao: TaskDao                // Objeto de acesso aos dados das tarefas (Room DAO)
    private var currentMode: String? = null             // Modo de operação atual da Activity
    private val calendar = Calendar.getInstance()       // Instância do calendário para o DatePicker

    private var currentTask: Task? = null               // Armazena a tarefa atual sendo editada, visualizada ou excluída
    private var currentTaskIdFromIntent: Int = -1       // ID da tarefa recebido da MainActivity (se não for MODE_NEW)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_detail) // Define o layout da Activity

        // Inicializa as Views buscando-as pelo ID no layout
        toolbarTaskDetail = findViewById(R.id.toolbar_task_detail)
        editTextTaskTitle = findViewById(R.id.editTextTaskTitle)
        textFieldLayoutTitle = findViewById(R.id.textFieldLayoutTitle)
        editTextTaskDescription = findViewById(R.id.editTextTaskDescription)
        editTextTaskDueDate = findViewById(R.id.editTextTaskDueDate)
        textFieldLayoutDueDate = findViewById(R.id.textFieldLayoutDueDate)
        spinnerImportanceLevel = findViewById(R.id.spinnerImportanceLevel)
        spinnerCompleteTasks = findViewById(R.id.finalizado)
        buttonSave = findViewById(R.id.buttonSave)
        buttonCancel = findViewById(R.id.buttonCancel)

        // Configura a Toolbar como a ActionBar da Activity e habilita o botão "voltar" (up)
        setSupportActionBar(toolbarTaskDetail)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Inicializa o DAO para acesso ao banco de dados
        taskDao = AppDatabase.getDatabase(applicationContext).taskDao()
        // Configura o DatePicker e o Spinner de importância
        setupDatePicker()
        setupImportanceSpinner()
        setupTaskCompleted()

        // Obtém o modo de operação e o ID da tarefa (se houver) da Intent que iniciou esta Activity
        currentMode = intent.getStringExtra(EXTRA_MODE)
        currentTaskIdFromIntent = intent.getIntExtra(EXTRA_TASK_ID, -1)

        // Ajusta a UI (títulos, botões, campos editáveis) com base no modo de operação
        setupUIForMode()

        // Define o listener de clique para o botão principal (Salvar/Confirmar Exclusão)
        buttonSave.setOnClickListener {
            handleSaveOrConfirmAction()
        }

        // Define o listener de clique para o botão Cancelar/Voltar
        buttonCancel.setOnClickListener {
            tryExitWithConfirmation()
        }
    }

    /**
     * Configura a interface do usuário (título da Toolbar, texto e visibilidade dos botões,
     * campos editáveis) de acordo com o modo de operação atual ([currentMode]).
     */
    private fun setupUIForMode() {
        when (currentMode) {
            MODE_NEW -> { // Configuração para criar uma nova tarefa
                supportActionBar?.title = getString(R.string.title_new_task)
                buttonSave.text = getString(R.string.button_save)
                buttonSave.visibility = View.VISIBLE // Botão Salvar visível
                updateDateInView() // Define a data atual no campo de data para novas tarefas
                // Guarda os valores originais para detecção de alterações
                originalTitle = ""
                originalDescription = ""
                originalDueDate = editTextTaskDueDate.text?.toString() ?: ""
                originalImportance = ImportanceLevel.MEDIUM
                originalState = TaskStatus.ACTIVE
            }
            MODE_EDIT -> { // Configuração para editar uma tarefa existente
                supportActionBar?.title = getString(R.string.title_edit_task)
                buttonSave.text = getString(R.string.button_save)
                buttonSave.visibility = View.VISIBLE
                if (currentTaskIdFromIntent != -1) { // Se um ID válido foi passado
                    loadTaskDetails(currentTaskIdFromIntent) // Carrega os dados da tarefa para edição
                } else {
                    // Se o ID for inválido, mostra erro e fecha a tela
                    Toast.makeText(this, "Erro: ID da tarefa inválido para edição.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            MODE_VIEW_DETAILS -> { // Configuração para visualizar detalhes da tarefa (somente leitura)
                supportActionBar?.title = getString(R.string.title_task_details)
                buttonSave.visibility = View.GONE // Esconde o botão Salvar/Confirmar
                buttonCancel.text = getString(R.string.button_cancel)
                if (currentTaskIdFromIntent != -1) {
                    loadTaskDetails(currentTaskIdFromIntent)
                    disableEditing() // Desabilita a edição dos campos
                } else {
                    Toast.makeText(this, "Erro: ID da tarefa inválido para visualização.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            MODE_DELETE_CONFIRM -> { // Configuração para confirmar a exclusão de uma tarefa
                supportActionBar?.title = getString(R.string.delete_task_title) // Título como "Excluir Tarefa?"
                buttonSave.text = getString(R.string.delete) // Botão Salvar vira "Excluir"
                buttonSave.visibility = View.VISIBLE
                buttonCancel.text = getString(R.string.cancel)
                if (currentTaskIdFromIntent != -1) {
                    loadTaskDetails(currentTaskIdFromIntent)
                    disableEditing() // Campos não são editáveis neste modo
                } else {
                    Toast.makeText(this, "Erro: ID da tarefa inválido para exclusão.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            else -> { // Se o modo for desconhecido ou nulo
                Toast.makeText(this, "Modo de operação desconhecido.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    /**
     * Configura o DatePickerDialog para ser exibido quando o campo de data limite é clicado.
     * Agora SEMPRE impede datas passadas, inclusive na edição.
     */
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
            val todayCalendar = Calendar.getInstance()
            todayCalendar.set(Calendar.HOUR_OF_DAY, 0)
            todayCalendar.set(Calendar.MINUTE, 0)
            todayCalendar.set(Calendar.SECOND, 0)
            todayCalendar.set(Calendar.MILLISECOND, 0)
            dialog.datePicker.minDate = todayCalendar.timeInMillis
            dialog.show()
        }
    }

    /**
     * Atualiza o campo de texto [editTextTaskDueDate] com a data formatada ("dd/MM/yyyy")
     * a partir do objeto [calendar].
     */
    private fun updateDateInView() {
        val myFormat = "dd/MM/yyyy" // Formato desejado para a data
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault()) // Objeto para formatar a data
        editTextTaskDueDate.setText(sdf.format(calendar.time)) // Define o texto do campo
        textFieldLayoutDueDate.error = null // Limpa erros de data ao definir uma nova
    }

    private fun setupTaskCompleted(){
        val completedDisplay = TaskStatus.entries
            .filter { it != TaskStatus.DELETED }
            .map { status ->
                when (status) {
                    TaskStatus.ACTIVE -> getString(R.string.status_incomplete)
                    TaskStatus.COMPLETED -> getString(R.string.status_complete)
                    TaskStatus.DELETED -> getString(R.string.status_deleted)
                }
            }
        val adapter = ArrayAdapter(
            this,
            R.layout.spinner_item_dark,
            completedDisplay
        )
        adapter.setDropDownViewResource(R.layout.spinner_item_dark)
        spinnerCompleteTasks.adapter = adapter

        if(currentMode == MODE_NEW){
            spinnerCompleteTasks.setSelection(TaskStatus.entries.indexOf(TaskStatus.ACTIVE))
        }
    }

    /**
     * Configura o Spinner para seleção do nível de importância da tarefa.
     * Preenche o Spinner com os níveis de importância traduzidos e define um valor padrão.
     */
    private fun setupImportanceSpinner() {
        // Mapeia os valores do enum ImportanceLevel para suas strings traduzidas
        val importanceLevelsDisplay = ImportanceLevel.entries.map { level ->
            when (level) {
                ImportanceLevel.HIGH -> getString(R.string.importance_high)
                ImportanceLevel.MEDIUM -> getString(R.string.importance_medium)
                ImportanceLevel.LIGHT -> getString(R.string.importance_light)
            }
        }
        // Cria um ArrayAdapter para popular o Spinner
        val adapter = ArrayAdapter(
            this,
            R.layout.spinner_item_dark, // Layout customizado para o item selecionado do Spinner
            importanceLevelsDisplay
        )
        // Layout customizado para os itens na lista dropdown do Spinner
        adapter.setDropDownViewResource(R.layout.spinner_item_dark)
        spinnerImportanceLevel.adapter = adapter // Define o adapter no Spinner

        // Se for uma nova tarefa, define a importância padrão como MÉDIA
        if (currentMode == MODE_NEW) {
            spinnerImportanceLevel.setSelection(ImportanceLevel.entries.indexOf(ImportanceLevel.MEDIUM))
        }
    }

    /**
     * Lida com a ação do botão principal.
     * Chama [saveOrUpdateTask] para os modos NOVO/EDITAR,
     * ou [deleteTaskConfirmed] para o modo CONFIRMAR_EXCLUSAO.
     */
    private fun handleSaveOrConfirmAction() {
        when (currentMode) {
            MODE_NEW, MODE_EDIT -> saveOrUpdateTask()
            MODE_DELETE_CONFIRM -> deleteTaskConfirmed()
            else -> {
                Toast.makeText(this, "Ação inválida para o modo atual.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Valida os campos do formulário (título, descrição, data limite).
     * Agora inclui limites de caracteres.
     */
    private fun saveOrUpdateTask() {
        val title = editTextTaskTitle.text.toString().trim()
        val description = editTextTaskDescription.text.toString().trim()
        val dueDate = editTextTaskDueDate.text.toString().trim()

        var isValid = true

        // Validação do Título
        if (title.isEmpty()) {
            textFieldLayoutTitle.error = getString(R.string.error_title_empty)
            editTextTaskTitle.requestFocus()
            isValid = false
        } else if (title.length > 50) {
            textFieldLayoutTitle.error = "Título deve ter no máximo 50 caracteres"
            editTextTaskTitle.requestFocus()
            isValid = false
        } else {
            textFieldLayoutTitle.error = null
        }

        // Validação da Descrição
        if (isValid && description.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_description_empty), Toast.LENGTH_SHORT).show()
            editTextTaskDescription.requestFocus()
            isValid = false
        } else if (isValid && description.length > 250) {
            Toast.makeText(this, "Descrição deve ter no máximo 250 caracteres", Toast.LENGTH_SHORT).show()
            editTextTaskDescription.requestFocus()
            isValid = false
        }

        // Validação da Data Limite
        if (isValid && dueDate.isEmpty()) {
            textFieldLayoutDueDate.error = getString(R.string.error_due_date_empty)
            editTextTaskDueDate.requestFocus()
            isValid = false
        } else {
            textFieldLayoutDueDate.error = null
        }

        if (!isValid) return

        val selectedTaskStatus = spinnerCompleteTasks.selectedItem.toString()
        val status = when(selectedTaskStatus) {
            getString(R.string.status_incomplete) -> TaskStatus.ACTIVE
            getString(R.string.status_complete) -> TaskStatus.COMPLETED
            else -> TaskStatus.ACTIVE
        }

        // Obtém o nível de importância selecionado no Spinner
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
                importance = importance,
                status = status
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

    /**
     * Deleta a [currentTask] do banco de dados.
     * Chamado quando o usuário clica no botão "Excluir" no modo [MODE_DELETE_CONFIRM].
     */
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

    /**
     * Carrega os detalhes de uma tarefa existente (pelo [taskId]) do banco de dados
     * e preenche os campos da UI com esses dados.
     * Também salva os valores originais para detecção de alterações.
     */
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

                val statusIndex = listOf(TaskStatus.ACTIVE, TaskStatus.COMPLETED)
                    .indexOf(taskFromDb?.status ?: TaskStatus.ACTIVE)
                if (statusIndex >= 0) {
                    spinnerCompleteTasks.setSelection(statusIndex)
                }

                val importanceIndex = ImportanceLevel.entries.indexOf(taskFromDb.importance)
                if (importanceIndex >= 0) {
                    spinnerImportanceLevel.setSelection(importanceIndex)
                }
                // Salva os valores originais
                originalTitle = taskFromDb.title
                originalDescription = taskFromDb.description
                originalDueDate = taskFromDb.dueDate
                originalImportance = taskFromDb.importance
                originalState = taskFromDb.status
            } else {
                Toast.makeText(this@TaskDetailActivity, "Tarefa não encontrada.", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        }
    }

    /**
     * Desabilita os campos de edição da UI.
     * Usado nos modos [MODE_VIEW_DETAILS] e [MODE_DELETE_CONFIRM] para tornar os campos somente leitura.
     */
    private fun disableEditing() {
        editTextTaskTitle.isEnabled = false
        textFieldLayoutTitle.isEnabled = false
        editTextTaskDescription.isEnabled = false
        editTextTaskDueDate.isEnabled = false
        textFieldLayoutDueDate.isEnabled = false
        spinnerImportanceLevel.isEnabled = false
        spinnerCompleteTasks.isEnabled = false
    }

    /**
     * Detecta se há alterações não salvas no formulário.
     */
    private fun hasUnsavedChanges(): Boolean {
        val currentTitle = editTextTaskTitle.text.toString()
        val currentDescription = editTextTaskDescription.text.toString()
        val currentDueDate = editTextTaskDueDate.text.toString()
        val currentImportance = when (spinnerImportanceLevel.selectedItem.toString()) {
            getString(R.string.importance_high) -> ImportanceLevel.HIGH
            getString(R.string.importance_medium) -> ImportanceLevel.MEDIUM
            getString(R.string.importance_light) -> ImportanceLevel.LIGHT
            else -> ImportanceLevel.MEDIUM
        }
        val currentState = when(spinnerCompleteTasks.selectedItem.toString()){
            getString(R.string.status_incomplete) -> TaskStatus.ACTIVE
            getString(R.string.status_complete) -> TaskStatus.COMPLETED
            else -> TaskStatus.ACTIVE
        }

        return currentTitle != originalTitle ||
                currentDescription != originalDescription ||
                currentDueDate != originalDueDate ||
                currentImportance != originalImportance ||
                currentState != originalState
    }

    /**
     * Mostra diálogo de confirmação ao tentar sair com alterações não salvas.
     */
    private fun tryExitWithConfirmation() {
        if (hasUnsavedChanges()) {
            AlertDialog.Builder(this)
                .setTitle("Descartar alterações?")
                .setMessage("Você tem alterações não salvas. Deseja realmente sair?")
                .setPositiveButton("Sim") { _, _ -> finish() }
                .setNegativeButton("Não", null)
                .show()
        } else {
            finish()
        }
    }

    /**
     * Lida com seleções de itens da Toolbar (neste caso, apenas o botão "voltar" - up).
     * Agora usa confirmação ao sair.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                tryExitWithConfirmation()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Sobrescreve o comportamento do botão físico de voltar para exibir um diálogo de confirmação
     * caso haja alterações não salvas. Não chamamos super.onBackPressed() porque o fluxo de saída
     * (finish) é controlado manualmente após a confirmação do usuário.
     *
     * @Suppress("MissingSuperCall") é usado para suprimir o aviso do Lint, pois nesse caso
     * não queremos o comportamento padrão do sistema.
     */
    @Suppress("MissingSuperCall")
    override fun onBackPressed() {
        tryExitWithConfirmation()
    }
}
