package com.alansouza.personaltasks

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.alansouza.personaltasks.adapter.DeletedTasksAdapter
import com.alansouza.personaltasks.databinding.ActivityDeletedTasksBinding
import com.alansouza.personaltasks.model.Task
import com.alansouza.personaltasks.model.TaskStatus

class DeletedTasksActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDeletedTasksBinding
    private lateinit var adapter: DeletedTasksAdapter
    private lateinit var viewModel: TaskViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeletedTasksBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarDeletedTasks)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.deleted_tasks_title)

        viewModel = ViewModelProvider(this)[TaskViewModel::class.java]

        adapter = DeletedTasksAdapter { task, action ->
            when (action) {
                DeletedTasksAdapter.DeletedTaskAction.REACTIVATE ->
                    viewModel.updateTaskStatus(task.id, TaskStatus.ACTIVE)
                DeletedTasksAdapter.DeletedTaskAction.VIEW_DETAILS ->
                    openTaskDetailScreen(task)
            }
        }

        binding.recyclerViewDeletedTasks.apply {
            layoutManager = LinearLayoutManager(this@DeletedTasksActivity)
            this.adapter = this@DeletedTasksActivity.adapter
        }

        viewModel.deletedTasks.observe(this) { tasks ->
            adapter.submitList(tasks)
        }
    }

    private fun openTaskDetailScreen(task: Task) {
        val intent = Intent(this, TaskDetailActivity::class.java).apply {
            putExtra("task", task)
            putExtra(TaskDetailActivity.EXTRA_MODE, TaskDetailActivity.MODE_VIEW_DETAILS)
        }
        startActivity(intent)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun handleContextAction(task: Task, action: DeletedTasksAdapter.DeletedTaskAction) {
        when (action) {
            DeletedTasksAdapter.DeletedTaskAction.REACTIVATE -> reactivateTask(task)
            DeletedTasksAdapter.DeletedTaskAction.VIEW_DETAILS -> viewTaskDetails(task)
        }
    }

    private fun reactivateTask(task: Task) {
        viewModel.updateTaskStatus(task.id, TaskStatus.ACTIVE)
        Toast.makeText(this, "Tarefa reativada", Toast.LENGTH_SHORT).show()
    }

    private fun viewTaskDetails(task: Task) {
        startActivity(Intent(this, TaskDetailActivity::class.java).apply {
            putExtra("task", task)
        })
    }
}
