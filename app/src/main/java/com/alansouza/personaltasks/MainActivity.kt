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
import com.google.android.material.snackbar.Snackbar


class MainActivity : AppCompatActivity() {

    private lateinit var recyclerViewTasks: RecyclerView
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var taskDao: TaskDao
    private lateinit var toolbar: Toolbar
    private lateinit var textViewEmptyTasks: TextView

    private var selectedTaskForContextMenu: Task? = null
    private var tasksLiveData: LiveData<List<Task>>? = null

    companion object {
        private const val PREFS_NAME = "PersonalTasksPrefs"
        private const val KEY_SORT_ORDER = "sortMoreImportantFirst"
        const val EXTRA_MODE = "MODE"
        const val EXTRA_TASK_ID = "TASK_ID"
    }

    private lateinit var sharedPreferences: SharedPreferences
    private var currentSortMoreImportantFirst: Boolean = true

    private val taskDetailLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val message = result.data?.getStringExtra("MESSAGE_AFTER_OPERATION")
            if (!message.isNullOrEmpty()) {
                val rootView: View = findViewById(R.id.main_container)
                Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayShowTitleEnabled(false)

        val rootLayout = findViewById<View>(R.id.main_container)
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadSortPreference()

        textViewEmptyTasks = findViewById(R.id.textViewEmptyTasks)
        taskDao = AppDatabase.getDatabase(applicationContext).taskDao()

        recyclerViewTasks = findViewById(R.id.recyclerViewTasks)
        recyclerViewTasks.layoutManager = LinearLayoutManager(this)

        taskAdapter = TaskAdapter()
        recyclerViewTasks.adapter = taskAdapter

        Log.d("MainActivitySort", "onCreate: Initial sort order is $currentSortMoreImportantFirst")
        loadAndObserveTasks()
    }


    private fun openTaskDetailScreen(mode: String, task: Task? = null) {
        val intent = Intent(this, TaskDetailActivity::class.java).apply {
            putExtra(EXTRA_MODE, mode)
            task?.let { putExtra(EXTRA_TASK_ID, it.id) }
        }
        taskDetailLauncher.launch(intent)
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
            val isEmpty = tasks.isNullOrEmpty()
            recyclerViewTasks.visibility = if (isEmpty) View.GONE else View.VISIBLE
            textViewEmptyTasks.visibility = if (isEmpty) View.VISIBLE else View.GONE
            if (!isEmpty) {
                taskAdapter.submitList(tasks)
            } else {
                taskAdapter.submitList(emptyList())
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
        val moreImportantFirstItem = menu?.findItem(R.id.action_sort_more_important_first)
        val lessImportantFirstItem = menu?.findItem(R.id.action_sort_less_important_first)

        moreImportantFirstItem?.isChecked = currentSortMoreImportantFirst
        lessImportantFirstItem?.isChecked = !currentSortMoreImportantFirst
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d("MainActivitySort", "onOptionsItemSelected: Clicked item ${item.title}")
        return when (item.itemId) {
            R.id.action_new_task -> {
                openTaskDetailScreen(TaskDetailActivity.MODE_NEW)
                true
            }
            R.id.action_sort_more_important_first -> {
                setSortOrder(true)
                true
            }
            R.id.action_sort_less_important_first -> {
                setSortOrder(false)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val currentTask = selectedTaskForContextMenu
            ?: return super.onContextItemSelected(item)

        return when (item.itemId) {
            R.id.action_edit_task -> {
                openTaskDetailScreen(TaskDetailActivity.MODE_EDIT, currentTask)
                true
            }
            R.id.action_delete_task -> {
                showDeleteConfirmationDialog(currentTask)
                true
            }
            R.id.action_details_task -> {
                openTaskDetailScreen(TaskDetailActivity.MODE_VIEW_DETAILS, currentTask)
                true
            }
            else -> {
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
                openTaskDetailScreen(TaskDetailActivity.MODE_DELETE_CONFIRM, task)
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                selectedTaskForContextMenu = null
            }
            .setOnDismissListener {
            }
            .show()
    }

    override fun onContextMenuClosed(menu: Menu) {
        super.onContextMenuClosed(menu)
        selectedTaskForContextMenu = null
    }
}
