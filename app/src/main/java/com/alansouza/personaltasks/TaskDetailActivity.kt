package com.alansouza.personaltasks

import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.textfield.TextInputEditText


class TaskDetailActivity : AppCompatActivity() {

    private lateinit var toolbarTaskDetail: Toolbar
    private lateinit var editTextTaskTitle: TextInputEditText
    private lateinit var editTextTaskDescription: TextInputEditText
    private lateinit var editTextTaskDueDate: TextInputEditText
    private lateinit var buttonCancel: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_detail)

        toolbarTaskDetail = findViewById(R.id.toolbar_task_detail)
        editTextTaskTitle = findViewById(R.id.editTextTaskTitle)
        editTextTaskDescription = findViewById(R.id.editTextTaskDescription)
        editTextTaskDueDate = findViewById(R.id.editTextTaskDueDate)
        buttonCancel = findViewById(R.id.buttonCancel)

        setSupportActionBar(toolbarTaskDetail)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.title_task_details)

        buttonCancel.setOnClickListener {
            finish()
        }
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
