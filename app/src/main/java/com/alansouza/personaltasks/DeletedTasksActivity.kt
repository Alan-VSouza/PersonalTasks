package com.alansouza.personaltasks

import android.content.Intent
import android.os.Bundle
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

        viewModel = ViewModelProvider(this)[TaskViewModel::class.java]

        adapter = DeletedTasksAdapter { task, action ->
            handleContextAction(task, action)
        }

        binding.recyclerViewDeletedTasks.apply {
            layoutManager = LinearLayoutManager(this@DeletedTasksActivity)
            this.adapter = this@DeletedTasksActivity.adapter
        }

        viewModel.deletedTasks.observe(this) { tasks ->
            adapter.submitList(tasks)
        }
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
