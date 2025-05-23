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
    // private lateinit var textFieldLayoutDescription: TextInputLayout // Descomente se for usar para erros da descrição
    private lateinit var editTextTaskDueDate: TextInputEditText
    private lateinit var textFieldLayoutDueDate: TextInputLayout // Usado para exibir erros de validação da data
    private lateinit var spinnerImportanceLevel: Spinner
    private lateinit var buttonSave: Button       // Botão principal (Salvar, Confirmar Exclusão)
    private lateinit var buttonCancel: Button     // Botão secundário (Cancelar, Voltar)

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
        // textFieldLayoutDescription = findViewById(R.id.textFieldLayoutDescription) // Inicialize se usar
        editTextTaskDueDate = findViewById(R.id.editTextTaskDueDate)
        textFieldLayoutDueDate = findViewById(R.id.textFieldLayoutDueDate)
        spinnerImportanceLevel = findViewById(R.id.spinnerImportanceLevel)
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
            setResult(Activity.RESULT_CANCELED) // Informa à MainActivity que a operação foi cancelada
            finish() // Fecha esta Activity
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
     * Define também a data mínima para novas tarefas.
     */
    private fun setupDatePicker() {
        // Listener para quando o usuário seleciona uma data no DatePicker
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)           // Define o ano no objeto Calendar
            calendar.set(Calendar.MONTH, monthOfYear)    // Define o mês
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth) // Define o dia
            updateDateInView()                         // Atualiza o campo de texto com a data formatada
        }

        // Listener para o clique no campo de texto da data limite
        editTextTaskDueDate.setOnClickListener {
            textFieldLayoutDueDate.error = null // Limpa qualquer erro de validação anterior
            val dialog = DatePickerDialog(
                this,
                // R.style.YourDatePickerDialogTheme, // Descomente e use seu tema customizado se tiver
                dateSetListener,                    // O listener definido acima
                calendar.get(Calendar.YEAR),        // Ano inicial do DatePicker
                calendar.get(Calendar.MONTH),       // Mês inicial
                calendar.get(Calendar.DAY_OF_MONTH) // Dia inicial
            )

            // Se estiver criando uma NOVA tarefa, impede a seleção de datas passadas
            if (currentMode == MODE_NEW) {
                val todayCalendar = Calendar.getInstance()
                // Zera horas, minutos, segundos e milissegundos para pegar o início do dia de hoje
                todayCalendar.set(Calendar.HOUR_OF_DAY, 0)
                todayCalendar.set(Calendar.MINUTE, 0)
                todayCalendar.set(Calendar.SECOND, 0)
                todayCalendar.set(Calendar.MILLISECOND, 0)
                dialog.datePicker.minDate = todayCalendar.timeInMillis // Define a data mínima como hoje
            }
            // Em outros modos (ex: EDIÇÃO), permite datas passadas (se a tarefa já tinha ou para correção)

            dialog.show() // Exibe o DatePickerDialog
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
                // Caso algum modo inesperado chegue aqui
                Toast.makeText(this, "Ação inválida para o modo atual.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Valida os campos do formulário (título, descrição, data limite).
     * Se válidos, salva uma nova tarefa ou atualiza uma existente no banco de dados.
     */
    private fun saveOrUpdateTask() {
        // Obtém os valores dos campos, removendo espaços em branco extras (trim)
        val title = editTextTaskTitle.text.toString().trim()
        val description = editTextTaskDescription.text.toString().trim()
        val dueDate = editTextTaskDueDate.text.toString().trim()

        var isValid = true // Flag para controlar a validade do formulário

        // Validação do Título
        if (title.isEmpty()) {
            textFieldLayoutTitle.error = getString(R.string.error_title_empty) // Mostra erro no TextInputLayout
            if(isValid) editTextTaskTitle.requestFocus() // Foca no primeiro campo inválido encontrado
            isValid = false
        } else {
            textFieldLayoutTitle.error = null // Limpa o erro se o campo for válido
        }

        // Validação da Descrição
        if (description.isEmpty()) {
            // Se você tiver um textFieldLayoutDescription, use:
            // textFieldLayoutDescription.error = getString(R.string.error_description_empty)
            // Caso contrário, um Toast (ou outra forma de feedback):
            Toast.makeText(this, getString(R.string.error_description_empty), Toast.LENGTH_SHORT).show()
            if(isValid) editTextTaskDescription.requestFocus()
            isValid = false
        } else {
            // textFieldLayoutDescription.error = null // Limpe o erro se usar TextInputLayout
        }

        // Validação da Data Limite
        if (dueDate.isEmpty()) {
            textFieldLayoutDueDate.error = getString(R.string.error_due_date_empty)
            if(isValid) editTextTaskDueDate.requestFocus()
            isValid = false
        } else {
            textFieldLayoutDueDate.error = null
        }

        if (!isValid) { // Se algum campo obrigatório estiver inválido, interrompe a função
            return
        }

        // Obtém o nível de importância selecionado no Spinner
        val selectedImportanceDisplayString = spinnerImportanceLevel.selectedItem.toString()
        val importance = when(selectedImportanceDisplayString) { // Converte a string de volta para o Enum
            getString(R.string.importance_high) -> ImportanceLevel.HIGH
            getString(R.string.importance_medium) -> ImportanceLevel.MEDIUM
            getString(R.string.importance_light) -> ImportanceLevel.LIGHT
            else -> ImportanceLevel.MEDIUM // Valor padrão em caso de string inesperada
        }

        // Executa a operação de banco de dados (inserir ou atualizar)
        if (currentMode == MODE_NEW) {
            val newTask = Task( // Cria um novo objeto Task
                title = title,
                description = description,
                dueDate = dueDate,
                importance = importance
            )
            lifecycleScope.launch { // Executa a operação de banco em uma coroutine (thread separada)
                taskDao.insertTaskOnDatabase(newTask) // Insere a nova tarefa
                val operationMessage = getString(R.string.task_created_successfully) // Mensagem de sucesso

                // Prepara o resultado para retornar à MainActivity
                val resultIntent = Intent()
                resultIntent.putExtra(EXTRA_MESSAGE_AFTER_OPERATION, operationMessage)
                setResult(Activity.RESULT_OK, resultIntent) // Define o resultado como OK
                finish() // Fecha esta Activity
            }
        } else if (currentMode == MODE_EDIT) {
            val taskToUpdate = currentTask // Captura o valor de currentTask para uma val local (segurança com smart cast)
            if (taskToUpdate != null) { // Verifica se a tarefa a ser atualizada existe
                val updatedTask = taskToUpdate.copy( // Cria uma cópia da tarefa com os novos valores
                    title = title,
                    description = description,
                    dueDate = dueDate,
                    importance = importance
                )
                lifecycleScope.launch {
                    taskDao.updateTaskOnDatabase(updatedTask) // Atualiza a tarefa no banco
                    val operationMessage = getString(R.string.task_updated_successfully)

                    val resultIntent = Intent()
                    resultIntent.putExtra(EXTRA_MESSAGE_AFTER_OPERATION, operationMessage)
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }
            } else {
                // Caso currentTask seja nulo no modo de edição (erro de lógica)
                Toast.makeText(this, "Erro ao salvar: tarefa original não encontrada.", Toast.LENGTH_LONG).show()
                setResult(Activity.RESULT_CANCELED) // Informa que a operação falhou/foi cancelada
                finish()
            }
        }
    }

    /**
     * Deleta a [currentTask] do banco de dados.
     * Chamado quando o usuário clica no botão "Excluir" no modo [MODE_DELETE_CONFIRM].
     */
    private fun deleteTaskConfirmed() {
        val taskToDelete = currentTask // Captura local para segurança
        if (taskToDelete != null) { // Verifica se a tarefa a ser deletada existe
            lifecycleScope.launch { // Operação de banco em coroutine
                taskDao.deleteTaskOnDatabase(taskToDelete) // Deleta a tarefa
                // Prepara a mensagem de sucesso, incluindo o título da tarefa deletada
                val operationMessage = getString(R.string.task_deleted_message, taskToDelete.title)

                val resultIntent = Intent()
                resultIntent.putExtra(EXTRA_MESSAGE_AFTER_OPERATION, operationMessage)
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
        } else {
            // Caso currentTask seja nulo ao tentar deletar
            Toast.makeText(this, "Erro ao excluir: tarefa não encontrada.", Toast.LENGTH_LONG).show()
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    /**
     * Carrega os detalhes de uma tarefa existente (pelo [taskId]) do banco de dados
     * e preenche os campos da UI com esses dados.
     * @param taskId O ID da tarefa a ser carregada.
     */
    private fun loadTaskDetails(taskId: Int) {
        lifecycleScope.launch { // Operação de banco em coroutine
            val taskFromDb = taskDao.getTaskById(taskId) // Busca a tarefa no banco
            currentTask = taskFromDb // Armazena a tarefa carregada na propriedade da classe

            if (taskFromDb != null) { // Se a tarefa foi encontrada
                // Preenche os campos da UI com os dados da tarefa
                editTextTaskTitle.setText(taskFromDb.title)
                editTextTaskDescription.setText(taskFromDb.description)

                // Formata e define a data de conclusão no campo de texto
                if (taskFromDb.dueDate.isNotEmpty()) {
                    try {
                        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        val parsedDate = sdf.parse(taskFromDb.dueDate) // Tenta converter a String da data
                        if (parsedDate != null) {
                            calendar.time = parsedDate // Define o objeto Calendar para a data da tarefa
                            updateDateInView() // Atualiza o campo de texto
                        } else {
                            editTextTaskDueDate.setText("") // Limpa se a data for inválida
                        }
                    } catch (e: Exception) {
                        // Em caso de erro ao parsear a data (formato inesperado)
                        editTextTaskDueDate.setText("")
                    }
                } else {
                    editTextTaskDueDate.setText("") // Limpa se a data estiver vazia
                }

                // Define a seleção correta no Spinner de importância
                val importanceIndex = ImportanceLevel.entries.indexOf(taskFromDb.importance)
                if (importanceIndex >= 0) { // Garante que o índice é válido
                    spinnerImportanceLevel.setSelection(importanceIndex)
                }
            } else {
                // Se a tarefa com o ID fornecido não for encontrada no banco
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
        textFieldLayoutTitle.isEnabled = false // Desabilita o layout do campo também
        editTextTaskDescription.isEnabled = false
        editTextTaskDueDate.isEnabled = false
        textFieldLayoutDueDate.isEnabled = false
        spinnerImportanceLevel.isEnabled = false
    }

    /**
     * Lida com seleções de itens da Toolbar (neste caso, apenas o botão "voltar" - up).
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { // ID padrão para o botão "voltar" (up) da ActionBar/Toolbar
                setResult(Activity.RESULT_CANCELED) // Define o resultado como cancelado
                finish() // Fecha a Activity
                true // Indica que o evento foi tratado
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
