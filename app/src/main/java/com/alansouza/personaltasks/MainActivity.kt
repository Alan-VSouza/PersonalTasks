package com.alansouza.personaltasks

import android.os.Bundle
import android.view.View
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


class MainActivity : AppCompatActivity() {

    private lateinit var recyclerViewTasks: RecyclerView
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var taskDao: TaskDao
    private lateinit var toolbar: Toolbar

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

        taskDao = AppDatabase.getDatabase(applicationContext).taskDao()

        recyclerViewTasks = findViewById(R.id.recyclerViewTasks)
        recyclerViewTasks.layoutManager = LinearLayoutManager(this)

        taskAdapter = TaskAdapter()
        recyclerViewTasks.adapter = taskAdapter

        loadAndObserveTasks()
    }

    private fun loadAndObserveTasks() {
        taskDao.getAllTasks().observe(this, Observer { tasks ->
            tasks?.let {
                taskAdapter.submitList(it)
            }
        })
    }

}
