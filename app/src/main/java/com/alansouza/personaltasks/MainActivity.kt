package com.alansouza.personaltasks

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alansouza.personaltasks.adapter.TaskAdapter
import com.alansouza.personaltasks.data.AppDatabase
import com.alansouza.personaltasks.data.TaskDao
import com.alansouza.personaltasks.model.Task
import kotlinx.coroutines.launch
import androidx.core.content.edit


class MainActivity : AppCompatActivity() {

    private lateinit var recyclerViewTasks: RecyclerView
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var taskDao: TaskDao
    private lateinit var toolbar: Toolbar
    private lateinit var newTaskLauncher: ActivityResultLauncher<Intent>
    private lateinit var editTaskLauncher: ActivityResultLauncher<Intent>
    private lateinit var textViewEmptyTasks: TextView

    private var selectedTaskForContextMenu: Task? = null
    private var tasksLiveData: LiveData<List<Task>>? = null

    private val PREFS_NAME = "PersonalTasksPrefs"
    private val KEY_SORT_ORDER = "sortMoreImportantFirst"

    private lateinit var sharedPreferences: SharedPreferences
    private var currentSortMoreImportantFirst: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Simplify"

        val rootLayout = findViewById<View>(R.id.main_container)
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        loadSortPreference()

        newTaskLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, getString(R.string.task_created_successfully), Toast.LENGTH_SHORT).show()
            }
        }

        editTaskLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, getString(R.string.task_updated_successfully), Toast.LENGTH_SHORT).show()
            }
        }

        textViewEmptyTasks = findViewById(R.id.textViewEmptyTasks)
        taskDao = AppDatabase.getDatabase(applicationContext).taskDao()

        recyclerViewTasks = findViewById(R.id.recyclerViewTasks)
        recyclerViewTasks.layoutManager = LinearLayoutManager(this)

        taskAdapter = TaskAdapter()
        recyclerViewTasks.adapter = taskAdapter

        Log.d("MainActivitySort", "onCreate: Initial sort order is $currentSortMoreImportantFirst")
        loadAndObserveTasks()
    }

    private fun loadSortPreference() {
        currentSortMoreImportantFirst = sharedPreferences.getBoolean(KEY_SORT_ORDER, true)
        Log.d("MainActivitySort", "loadSortPreference: Loaded sort order as $currentSortMoreImportantFirst")
    }

    private fun saveSortPreference(isMoreImportantFirst: Boolean) {
        sharedPreferences.edit {
            putBoolean(KEY_SORT_ORDER, isMoreImportantFirst)
        }
        Log.d("MainActivitySort", "saveSortPreference: Saved sort order as $isMoreImportantFirst")
    }

    private fun loadAndObserveTasks() {
        Log.d("MainActivitySort", "loadAndObserveTasks: Using sort order $currentSortMoreImportantFirst")
        tasksLiveData?.removeObservers(this)
        tasksLiveData = taskDao.getAllTasksOrdered(currentSortMoreImportantFirst)
        tasksLiveData?.observe(this, Observer { tasks ->
            Log.d("MainActivitySort", "Observer received ${tasks?.size ?: "null"} tasks.")
            if (tasks.isNullOrEmpty()) {
                recyclerViewTasks.visibility = View.GONE
                textViewEmptyTasks.visibility = View.VISIBLE
            } else {
                recyclerViewTasks.visibility = View.VISIBLE
                textViewEmptyTasks.visibility = View.GONE
                taskAdapter.submitList(tasks)
            }
        })
    }

    private fun setSortOrder(newSortOrderIsMoreImportantFirst: Boolean) {
        Log.d("MainActivitySort", "setSortOrder called with: $newSortOrderIsMoreImportantFirst. Current: $currentSortMoreImportantFirst")
        if (currentSortMoreImportantFirst != newSortOrderIsMoreImportantFirst) {
            currentSortMoreImportantFirst = newSortOrderIsMoreImportantFirst
            saveSortPreference(currentSortMoreImportantFirst)
            loadAndObserveTasks()
            invalidateOptionsMenu()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        Log.d("MainActivitySort", "onPrepareOptionsMenu: Setting checks based on $currentSortMoreImportantFirst")
        menu?.findItem(R.id.action_sort_more_important_first)?.isChecked = currentSortMoreImportantFirst
        menu?.findItem(R.id.action_sort_less_important_first)?.isChecked = !currentSortMoreImportantFirst
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_new_task -> {
                val intent = Intent(this, TaskDetailActivity::class.java)
                intent.putExtra("MODE", "NEW")
                newTaskLauncher.launch(intent)
                return true
            }
            R.id.action_sort_more_important_first -> {
                setSortOrder(true)
                return true
            }
            R.id.action_sort_less_important_first -> {
                setSortOrder(false)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val currentTask = selectedTaskForContextMenu
            ?: return super.onContextItemSelected(item)

        return when (item.itemId) {
            R.id.action_edit_task -> {
                val intent = Intent(this, TaskDetailActivity::class.java)
                intent.putExtra("MODE", "EDIT")
                intent.putExtra("TASK_ID", currentTask.id)
                editTaskLauncher.launch(intent)
                true
            }
            R.id.action_delete_task -> {
                showDeleteConfirmationDialog(currentTask)
                true
            }
            R.id.action_details_task -> {
                val intent = Intent(this, TaskDetailActivity::class.java)
                intent.putExtra("MODE", "DETAILS")
                intent.putExtra("TASK_ID", currentTask.id)
                startActivity(intent)
                true
            }
            else -> {
                selectedTaskForContextMenu = null
                super.onContextItemSelected(item)
            }
        }
    }

    fun setSelectedTaskForContextMenu(task: Task) {
        selectedTaskForContextMenu = task
    }

    private fun showDeleteConfirmationDialog(task: Task) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_task_title))
            .setMessage(getString(R.string.delete_task_confirmation_message, task.title))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                deleteTask(task)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun deleteTask(task: Task) {
        lifecycleScope.launch {
            taskDao.deleteTaskOnDatabase(task)
            Toast.makeText(
                this@MainActivity,
                getString(R.string.task_deleted_message, task.title),
                Toast.LENGTH_SHORT
            ).show()
            selectedTaskForContextMenu = null
        }
    }

    override fun onContextMenuClosed(menu: Menu) {
        super.onContextMenuClosed(menu)
        selectedTaskForContextMenu = null
    }
}
