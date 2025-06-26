package com.alansouza.personaltasks

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alansouza.personaltasks.adapter.TaskAdapter
import com.alansouza.personaltasks.data.AppDatabase
import com.alansouza.personaltasks.data.TaskDao
import com.alansouza.personaltasks.model.Task
import androidx.core.content.edit
import com.alansouza.personaltasks.auth.LoginActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth


/**
 * Activity principal que exibe a lista de tarefas.
 * Lida com a navegação para adicionar/editar tarefas, ordenação da lista
 * e interações do menu de contexto.
 */
class MainActivity : AppCompatActivity() {

    // Views da UI

    private lateinit var auth: FirebaseAuth
    private lateinit var recyclerViewTasks: RecyclerView
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var taskDao: TaskDao
    private lateinit var toolbar: Toolbar
    private lateinit var textViewEmptyTasks: TextView // TextView para mostrar quando a lista está vazia

    // Variáveis de estado
    private var selectedTaskForContextMenu: Task? = null // Armazena a tarefa selecionada para o menu de contexto
    private var tasksLiveData: LiveData<List<Task>>? = null // LiveData para observar as tarefas do banco

    // Constantes para SharedPreferences e Intent Extras
    companion object {
        private const val PREFS_NAME = "PersonalTasksPrefs" // Nome do arquivo de SharedPreferences
        private const val KEY_SORT_ORDER = "sortMoreImportantFirst" // Chave para salvar a preferência de ordenação
        private var TAG = "EmailAndPassword"
        // Chaves para passar dados para TaskDetailActivity
        const val EXTRA_MODE = "MODE"
        const val EXTRA_TASK_ID = "TASK_ID"
    }

    private lateinit var sharedPreferences: SharedPreferences
    private var currentSortMoreImportantFirst: Boolean = true // Preferência de ordenação atual

    // ActivityResultLauncher para receber resultados da TaskDetailActivity
    private val taskDetailLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Se a TaskDetailActivity retornou OK, exibe a mensagem de feedback
            val message = result.data?.getStringExtra(TaskDetailActivity.EXTRA_MESSAGE_AFTER_OPERATION)
            if (!message.isNullOrEmpty()) {
                val rootView: View = findViewById(R.id.main_container)
                Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show()
            }
            // A lista de tarefas será atualizada automaticamente pelo LiveData
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
            return
        }

        val buttonLogout = findViewById<Button>(R.id.buttonLogout)
        buttonLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }

        // Inicializa SharedPreferences e carrega a preferência de ordenação
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadSortPreference()

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        // Desabilita o título padrão da ActionBar, pois usamos um TextView customizado no layout da Toolbar
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Ajusta o padding da tela para acomodar as barras do sistema (status bar, navigation bar)
        val rootLayout = findViewById<View>(R.id.main_container)
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        // Inicializa as Views
        textViewEmptyTasks = findViewById(R.id.textViewEmptyTasks)
        taskDao = AppDatabase.getDatabase(applicationContext).taskDao()

        recyclerViewTasks = findViewById(R.id.recyclerViewTasks)
        recyclerViewTasks.layoutManager = LinearLayoutManager(this)
        taskAdapter = TaskAdapter()
        recyclerViewTasks.adapter = taskAdapter

        Log.d("MainActivitySort", "onCreate: Ordem de classificação inicial é $currentSortMoreImportantFirst")
        // Carrega e observa as tarefas do banco de dados
        loadAndObserveTasks()
    }

    private fun createUserWithEmailAndPassword(email: String, password: String){
        auth.createUserWithEmailAndPassword(email,password).addOnCompleteListener {  task ->
            if(task.isSuccessful){
                Log.d(TAG, "createUserWithEmailAndPassword:Sucess")
                val user = auth.currentUser
            }else{
                Log.w(TAG, "createUserWithEmailAndPassword:Failure", task.exception)
                Toast.makeText(baseContext, "Authentication Failure", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun signInWithEmailAndPassword(email: String, password: String){
        auth.signInWithEmailAndPassword(email,password).addOnCompleteListener { task ->
            if(task.isSuccessful){
                Log.d(TAG, "signInUserWithEmailAndPassword:Sucess")
                val user = auth.currentUser
            }else{
                Log.w(TAG, "signInUserWithEmailAndPassword:Failure", task.exception)
                Toast.makeText(baseContext, "Authentication Failure", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Abre a [TaskDetailActivity] em um modo específico (nova, editar, detalhes, etc.).
     * @param mode O modo de operação (ex: [TaskDetailActivity.MODE_NEW]).
     * @param task A tarefa a ser passada para a activity (opcional, usado para editar/detalhes).
     */
    private fun openTaskDetailScreen(mode: String, task: Task? = null) {
        val intent = Intent(this, TaskDetailActivity::class.java).apply {
            putExtra(EXTRA_MODE, mode) // Passa o modo
            task?.let { putExtra(EXTRA_TASK_ID, it.id) } // Passa o ID da tarefa, se houver
        }
        taskDetailLauncher.launch(intent) // Inicia a activity e espera um resultado
    }

    /**
     * Carrega a preferência de ordenação salva em SharedPreferences.
     * O padrão é ordenar por "mais importante primeiro".
     */
    private fun loadSortPreference() {
        currentSortMoreImportantFirst = sharedPreferences.getBoolean(KEY_SORT_ORDER, true)
        Log.d("MainActivitySort", "loadSortPreference: Preferência de ordenação carregada: $currentSortMoreImportantFirst")
    }

    /**
     * Salva a preferência de ordenação atual em SharedPreferences.
     * @param isMoreImportantFirst True se a ordenação for por "mais importante primeiro".
     */
    private fun saveSortPreference(isMoreImportantFirst: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_SORT_ORDER, isMoreImportantFirst) }
        Log.d("MainActivitySort", "saveSortPreference: Preferência de ordenação salva: $isMoreImportantFirst")
    }

    /**
     * Carrega as tarefas do banco de dados com a ordenação atual e configura um Observer
     * para atualizar a UI quando os dados mudarem.
     */
    private fun loadAndObserveTasks() {
        Log.d("MainActivitySort", "loadAndObserveTasks: Usando ordenação $currentSortMoreImportantFirst")
        // Remove observadores antigos para evitar duplicações se este metodo for chamado múltiplas vezes
        tasksLiveData?.removeObservers(this)

        // Obtém o LiveData do DAO com a preferência de ordenação atual
        tasksLiveData = taskDao.getAllTasksOrdered(currentSortMoreImportantFirst)
        // Observa o LiveData
        tasksLiveData?.observe(this, Observer { tasks ->
            Log.d("MainActivitySort", "Observer recebeu ${tasks?.size ?: "nenhuma"} tarefa(s).")
            // Log dos primeiros itens para depuração da ordenação
            if (tasks != null && tasks.isNotEmpty()) {
                Log.d("MainActivitySort", "Observer: Preferência de ordenação no escopo do observer: $currentSortMoreImportantFirst")
                Log.d("MainActivitySort", "Observer: Primeiras 3 tarefas recebidas (ou menos):")
                tasks.take(3).forEachIndexed { index, task ->
                    Log.d("MainActivitySort", "  Tarefa[$index]: ${task.title}, Importância: ${task.importance}, Data: ${task.dueDate}")
                }
            }

            val isEmpty = tasks.isNullOrEmpty()
            // Mostra/esconde o RecyclerView e a mensagem de lista vazia
            recyclerViewTasks.visibility = if (isEmpty) View.GONE else View.VISIBLE
            textViewEmptyTasks.visibility = if (isEmpty) View.VISIBLE else View.GONE

            // Atualiza o adapter com a nova lista de tarefas (ou uma lista vazia)
            taskAdapter.submitList(tasks ?: emptyList())
        })
    }

    /**
     * Define a nova ordem de classificação, salva a preferência e recarrega as tarefas.
     * @param newSortOrderIsMoreImportantFirst True para "mais importante primeiro", false caso contrário.
     */
    private fun setSortOrder(newSortOrderIsMoreImportantFirst: Boolean) {
        Log.d("MainActivitySort", "setSortOrder: Tentando mudar para $newSortOrderIsMoreImportantFirst. Atual é $currentSortMoreImportantFirst")
        if (currentSortMoreImportantFirst != newSortOrderIsMoreImportantFirst) {
            currentSortMoreImportantFirst = newSortOrderIsMoreImportantFirst
            saveSortPreference(currentSortMoreImportantFirst)
            loadAndObserveTasks()
            invalidateOptionsMenu()
        } else {
            Log.d("MainActivitySort", "setSortOrder: A ordem NÃO mudou. Nenhuma ação tomada.")
        }
    }


    // Métodos do Menu de Opções (Options Menu)
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        Log.d("MainActivitySort", "onCreateOptionsMenu: Menu inflado.")
        return true
    }

    /**
     * Chamado antes do menu de opções ser exibido.
     * Usado aqui para definir o estado (checado/não checado) dos itens de ordenação.
     */
    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        Log.d("MainActivitySort", "onPrepareOptionsMenu: Configurando checks baseado em currentSortMoreImportantFirst = $currentSortMoreImportantFirst")
        val moreImportantFirstItem = menu?.findItem(R.id.action_sort_more_important_first)
        val lessImportantFirstItem = menu?.findItem(R.id.action_sort_less_important_first)

        moreImportantFirstItem?.isChecked = currentSortMoreImportantFirst
        lessImportantFirstItem?.isChecked = !currentSortMoreImportantFirst

        Log.d("MainActivitySort", "onPrepareOptionsMenu: moreImportant.isChecked=${moreImportantFirstItem?.isChecked}, lessImportant.isChecked=${lessImportantFirstItem?.isChecked}")
        return super.onPrepareOptionsMenu(menu)
    }

    /**
     * Chamado quando um item do menu de opções é selecionado.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_new_task -> {
                openTaskDetailScreen(TaskDetailActivity.MODE_NEW)
                true
            }
            R.id.action_sort_more_important_first -> {
                item.isChecked = true
                setSortOrder(true)
                true
            }
            R.id.action_sort_less_important_first -> {
                item.isChecked = true
                setSortOrder(false)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Metodos do Menu de Contexto (Context Menu)
    /**
     * Chamado quando um item do menu de contexto (aberto pelo adapter) é selecionado.
     */
    override fun onContextItemSelected(item: MenuItem): Boolean {
        // Obtém a tarefa que foi selecionada (definida pelo adapter via setSelectedTaskForContextMenu)
        val currentTask = selectedTaskForContextMenu
            ?: return super.onContextItemSelected(item) // Se nenhuma tarefa foi selecionada, não faz nada

        return when (item.itemId) {
            R.id.action_edit_task -> {
                openTaskDetailScreen(TaskDetailActivity.MODE_EDIT, currentTask) // Abre para editar
                true
            }
            R.id.action_delete_task -> {
                // Mostra um diálogo de confirmação antes de ir para a tela de confirmação de exclusão
                showDeleteConfirmationDialog(currentTask)
                true
            }
            R.id.action_details_task -> {
                openTaskDetailScreen(TaskDetailActivity.MODE_VIEW_DETAILS, currentTask) // Abre para ver detalhes
                true
            }
            else -> {
                super.onContextItemSelected(item)
            }
        }
    }

    /**
     * Metodo chamado pelo [TaskAdapter] para informar qual tarefa foi selecionada
     * quando o menu de contexto é acionado.
     * @param task A tarefa selecionada.
     */
    fun setSelectedTaskForContextMenu(task: Task) {
        selectedTaskForContextMenu = task
    }

    /**
     * Exibe um diálogo de confirmação antes de prosseguir para a tela de confirmação de exclusão.
     * @param task A tarefa que pode ser excluída.
     */
    private fun showDeleteConfirmationDialog(task: Task) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_task_title))
            .setMessage(getString(R.string.delete_task_confirmation_message, task.title))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                // Se confirmado, abre a TaskDetailActivity no modo de confirmação de exclusão
                openTaskDetailScreen(TaskDetailActivity.MODE_DELETE_CONFIRM, task)
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                // Se cancelado, limpa a tarefa selecionada
                selectedTaskForContextMenu = null
            }
            .setOnDismissListener {
                selectedTaskForContextMenu = null
            }
            .show()
    }

    /**
     * Chamado quando o menu de contexto é fechado.
     * Usado aqui para limpar a referência à tarefa selecionada.
     */
    override fun onContextMenuClosed(menu: Menu) {
        super.onContextMenuClosed(menu)
        selectedTaskForContextMenu = null // Limpa a tarefa selecionada
    }
}
