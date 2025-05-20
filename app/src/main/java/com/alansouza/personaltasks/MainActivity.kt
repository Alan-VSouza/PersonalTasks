package com.alansouza.personaltasks

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alansouza.personaltasks.adapter.TaskAdapter
import com.alansouza.personaltasks.data.AppDatabase
import com.alansouza.personaltasks.data.TaskDao
import com.alansouza.personaltasks.model.Task
import kotlin.jvm.java


class MainActivity : AppCompatActivity() {

    private lateinit var recyclerViewTasks: RecyclerView
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var taskDao: TaskDao
    private lateinit var toolbar: Toolbar
    private lateinit var newTaskLauncher: ActivityResultLauncher<Intent>
    private lateinit var editTaskLauncher: ActivityResultLauncher<Intent>

    private var selectedTaskForContextMenu: Task? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Simplify"


        val rootLayout = findViewById<View>(R.id.main_container)
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        editTaskLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, getString(R.string.task_updated_successfully), Toast.LENGTH_SHORT).show()
            }
        }

        taskDao = AppDatabase.getDatabase(applicationContext).taskDao()

        recyclerViewTasks = findViewById(R.id.recyclerViewTasks)
        recyclerViewTasks.layoutManager = LinearLayoutManager(this)

        taskAdapter = TaskAdapter()
        recyclerViewTasks.adapter = taskAdapter

        loadAndObserveTasks()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_new_task -> {
                val intent = Intent(this, TaskDetailActivity::class.java)
                intent.putExtra("MODE", "NEW")
                newTaskLauncher.launch(intent)
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

    private fun loadAndObserveTasks() {
        taskDao.getAllTasks().observe(this, Observer { tasks ->
            tasks?.let {
                taskAdapter.submitList(it)
            }
        })
    }

}
