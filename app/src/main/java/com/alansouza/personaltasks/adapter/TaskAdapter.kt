// Adapter para exibir tarefas na lista principal (ativas e concluídas),
// com suporte a marcar como concluída via CheckBox e menu de contexto.
package com.alansouza.personaltasks.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alansouza.personaltasks.MainActivity
import com.alansouza.personaltasks.R
import com.alansouza.personaltasks.model.ImportanceLevel
import com.alansouza.personaltasks.model.Task
import com.alansouza.personaltasks.model.TaskStatus

class TaskAdapter(
    // Callback para notificar mudança de status (COMPLETED <-> ACTIVE)
    private val onCheckChanged: (Task, TaskStatus) -> Unit
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    // Cria o ViewHolder, inflando o layout do item
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task_layout, parent, false)
        return TaskViewHolder(view, onCheckChanged)
    }

    // Chama bind para associar dados à view
    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // ViewHolder que mantém referências às views e configura listeners
    inner class TaskViewHolder(
        itemView: View,
        private val onCheckChanged: (Task, TaskStatus) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        // Elementos da UI do item
        private val titleTextView: TextView = itemView.findViewById(R.id.textViewTaskTitle)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.textViewTaskDescription)
        private val dueDateTextView: TextView = itemView.findViewById(R.id.textViewTaskDueDate)
        private val importanceIndicatorView: View = itemView.findViewById(R.id.viewImportanceIndicator)
        private val importanceTextView: TextView = itemView.findViewById(R.id.textViewImportanceText)
        private val taskFinalizada: TextView = itemView.findViewById(R.id.finalizado)
        private val checkbox: CheckBox = itemView.findViewById(R.id.checkboxTaskCompleted)

        init {
            // Clique longo abre menu de contexto para editar/excluir/visualizar
            itemView.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val task = getItem(position)
                    showPopupMenu(itemView, task)
                }
                true
            }
        }

        // Preenche as views com os dados da Task
        fun bind(task: Task) {
            titleTextView.text = task.title
            descriptionTextView.text = task.description
            // Oculta descrição se estiver vazia
            descriptionTextView.visibility = if (task.description.isEmpty()) View.GONE else View.VISIBLE
            // Formata data de vencimento
            dueDateTextView.text = itemView.context
                .getString(R.string.due_date_format, task.dueDate)

            // Cor do indicador de importância (HIGH=vermelho, MEDIUM=amarelo, LIGHT=verde)
            val color = when (task.importance) {
                ImportanceLevel.HIGH   -> R.color.importance_high_color
                ImportanceLevel.MEDIUM -> R.color.importance_medium_color
                ImportanceLevel.LIGHT  -> R.color.importance_light_color
            }
            importanceIndicatorView.setBackgroundColor(ContextCompat.getColor(itemView.context, color))

            // Texto de status (Concluído / Pendente / Excluído)
            val statusText = when (task.status) {
                TaskStatus.COMPLETED -> R.string.status_complete
                TaskStatus.ACTIVE    -> R.string.status_incomplete
                TaskStatus.DELETED   -> R.string.status_deleted
            }
            taskFinalizada.text = itemView.context.getString(statusText)

            // Texto de importância (Alta / Média / Baixa)
            val importanceText = when (task.importance) {
                ImportanceLevel.HIGH   -> R.string.importance_high
                ImportanceLevel.MEDIUM -> R.string.importance_medium
                ImportanceLevel.LIGHT  -> R.string.importance_light
            }
            importanceTextView.text = itemView.context.getString(importanceText)

            // Configura o CheckBox de conclusão
            checkbox.setOnCheckedChangeListener(null)                // Remove listener anterior
            checkbox.isChecked = (task.status == TaskStatus.COMPLETED)
            checkbox.setOnCheckedChangeListener { _, isChecked ->
                val newStatus = if (isChecked) TaskStatus.COMPLETED else TaskStatus.ACTIVE
                onCheckChanged(task, newStatus)                     // Notifica a Activity/ViewModel
            }
        }

        // Exibe PopupMenu para context actions (editar, excluir, detalhes)
        private fun showPopupMenu(view: View, task: Task) {
            val popupMenu = PopupMenu(view.context, view)
            popupMenu.inflate(R.menu.context_menu_task)
            popupMenu.setOnMenuItemClickListener { menuItem ->
                val activity = view.context as? MainActivity
                activity?.let {
                    it.setSelectedTaskForContextMenu(task)          // Armazena tarefa selecionada
                    it.onContextItemSelected(menuItem)              // Trata clique no Activity
                }
                true
            }
            // Força mostrar ícones no menu (workaround para algumas versões)
            try {
                val field = PopupMenu::class.java.getDeclaredField("mPopup")
                field.isAccessible = true
                val mPopup = field.get(popupMenu)
                mPopup.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                    .invoke(mPopup, true)
            } catch (_: Exception) {
            }
            popupMenu.show()
        }
    }

    // DiffUtil para detectar mudanças de itens e conteúdos de forma eficiente
    class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Task, newItem: Task) = oldItem == newItem
    }
}
