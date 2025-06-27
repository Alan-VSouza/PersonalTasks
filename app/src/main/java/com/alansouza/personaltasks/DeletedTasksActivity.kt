// Activity que mostra tarefas excluídas e permite reativá-las ou ver detalhes.
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

        // Configura toolbar com botão de voltar
        setSupportActionBar(binding.toolbarDeletedTasks)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.deleted_tasks_title)

        viewModel = ViewModelProvider(this)[TaskViewModel::class.java]

        // Adapter com callback para ações de reativar ou ver detalhes
        adapter = DeletedTasksAdapter { task, action ->
            when (action) {
                DeletedTasksAdapter.DeletedTaskAction.REACTIVATE ->
                    viewModel.updateTaskStatus(task.id, TaskStatus.ACTIVE)
                DeletedTasksAdapter.DeletedTaskAction.VIEW_DETAILS ->
                    openTaskDetailScreen(task)
            }
        }

        // Configura RecyclerView
        binding.recyclerViewDeletedTasks.apply {
            layoutManager = LinearLayoutManager(this@DeletedTasksActivity)
            adapter = this@DeletedTasksActivity.adapter
        }

        // Observa LiveData de tarefas excluídas
        viewModel.deletedTasks.observe(this) { tasks ->
            adapter.submitList(tasks)
            if (tasks.isEmpty()) {
                Toast.makeText(this, "Nenhuma tarefa excluída", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Abre tela de detalhes em modo somente leitura
    private fun openTaskDetailScreen(task: Task) {
        val intent = Intent(this, TaskDetailActivity::class.java).apply {
            putExtra("task", task)
            putExtra(TaskDetailActivity.EXTRA_MODE, TaskDetailActivity.MODE_VIEW_DETAILS)
        }
        startActivity(intent)
    }

    // Trata o clique no botão voltar da toolbar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            finish() // Fecha Activity
            true
        } else super.onOptionsItemSelected(item)
    }
}
