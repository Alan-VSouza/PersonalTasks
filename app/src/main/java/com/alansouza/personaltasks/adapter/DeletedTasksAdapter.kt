// Adapter para exibir tarefas excluídas em um RecyclerView.
// Recebe um callback `onAction` para reagir a ações (reativar ou ver detalhes).
package com.alansouza.personaltasks.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alansouza.personaltasks.R
import com.alansouza.personaltasks.model.Task

class DeletedTasksAdapter(
    // Função que recebe a tarefa e a ação selecionada (reativar ou detalhes)
    private val onAction: (Task, DeletedTasksAdapter.DeletedTaskAction) -> Unit
) : ListAdapter<Task, DeletedTasksAdapter.ViewHolder>(TaskDiffCallback()) {

    // Sealed class definindo ações possíveis no menu de contexto
    sealed class DeletedTaskAction {
        object REACTIVATE : DeletedTaskAction()   // Reativar tarefa excluída
        object VIEW_DETAILS : DeletedTaskAction() // Ver detalhes da tarefa
    }

    // ViewHolder para cada item de tarefa excluída
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Mapeia views do item XML
        private val titleTextView: TextView = view.findViewById(R.id.textViewTaskTitle)
        private val descriptionTextView: TextView = view.findViewById(R.id.textViewTaskDescription)
        private val dueDateTextView: TextView = view.findViewById(R.id.textViewTaskDueDate)
        private val statusTextView: TextView = view.findViewById(R.id.finalizado)

        init {
            // Configura clique longo para exibir o menu de ações
            itemView.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    // Abre o menu para a tarefa naquela posição
                    showContextMenu(getItem(position))
                }
                true
            }
        }

        // Vincula os dados da Task às views
        fun bind(task: Task) {
            titleTextView.text = task.title
            descriptionTextView.text = task.description
            // Formata data de vencimento conforme string de recurso
            dueDateTextView.text = itemView.context.getString(
                R.string.due_date_format, task.dueDate
            )
            // Exibe status como "Excluída"
            statusTextView.text = itemView.context.getString(R.string.status_deleted)
        }

        // Exibe um PopupMenu com opções "Reativar" e "Ver Detalhes"
        private fun showContextMenu(task: Task) {
            PopupMenu(itemView.context, itemView).apply {
                menuInflater.inflate(R.menu.deleted_task_context_menu, menu)
                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.menu_reactivate -> {
                            onAction(task, DeletedTaskAction.REACTIVATE)
                            true
                        }
                        R.id.menu_details -> {
                            onAction(task, DeletedTaskAction.VIEW_DETAILS)
                            true
                        }
                        else -> false
                    }
                }
                show()
            }
        }
    }

    // Infla o layout para cada item e cria o ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task_layout, parent, false)
        return ViewHolder(view)
    }

    // Chama bind em cada ViewHolder com a Task correspondente
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // DiffUtil para comparar items e melhorar performance de atualizações
    class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
            // Mesma tarefa se o ID for igual
            return oldItem.id == newItem.id
        }
        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
            // Conteúdo igual se todos os campos forem iguais
            return oldItem == newItem
        }
    }
}
