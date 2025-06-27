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

/**
 * Adapter para exibição de tarefas na lista principal.
 * Utiliza DiffUtil para otimizar atualizações.
 *
 * @param onCheckChanged Callback para mudanças no status da tarefa (concluída/ativa)
 */
class TaskAdapter(
    private val onCheckChanged: (Task, TaskStatus) -> Unit
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    /**
     * Cria novas views (invocado pelo layout manager)
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task_layout, parent, false)
        return TaskViewHolder(view, onCheckChanged)
    }

    /**
     * Substitui o conteúdo das views (invocado pelo layout manager)
     */
    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = getItem(position)
        holder.bind(task)
    }

    /**
     * ViewHolder para cada item de tarefa
     */
    inner class TaskViewHolder(
        itemView: View,
        private val onCheckChanged: (Task, TaskStatus) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        // Referências para elementos da UI
        private val titleTextView: TextView = itemView.findViewById(R.id.textViewTaskTitle)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.textViewTaskDescription)
        private val dueDateTextView: TextView = itemView.findViewById(R.id.textViewTaskDueDate)
        private val importanceIndicatorView: View = itemView.findViewById(R.id.viewImportanceIndicator)
        private val importanceTextView: TextView = itemView.findViewById(R.id.textViewImportanceText)
        private val taskFinalizada: TextView = itemView.findViewById(R.id.finalizado)
        private val checkbox: CheckBox = itemView.findViewById(R.id.checkboxTaskCompleted)

        init {
            // Configura clique longo para abrir menu de contexto
            itemView.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val task = getItem(position)
                    showPopupMenu(itemView, task)
                }
                true
            }
        }

        /**
         * Vincula dados da tarefa aos elementos da UI
         */
        fun bind(task: Task) {
            // Preenche dados básicos
            titleTextView.text = task.title
            descriptionTextView.text = task.description

            // Oculta descrição se vazia
            if (task.description.isEmpty()) {
                descriptionTextView.visibility = View.GONE
            } else {
                descriptionTextView.visibility = View.VISIBLE
            }

            // Formata data de vencimento
            dueDateTextView.text = itemView.context.getString(R.string.due_date_format, task.dueDate)

            // Configura cor do indicador de importância
            val importanceColor = when (task.importance) {
                ImportanceLevel.HIGH -> ContextCompat.getColor(itemView.context, R.color.importance_high_color)
                ImportanceLevel.MEDIUM -> ContextCompat.getColor(itemView.context, R.color.importance_medium_color)
                ImportanceLevel.LIGHT -> ContextCompat.getColor(itemView.context, R.color.importance_light_color)
            }
            importanceIndicatorView.setBackgroundColor(importanceColor)

            // Exibe status da tarefa
            val taskStatusString = when (task.status) {
                TaskStatus.COMPLETED -> itemView.context.getString(R.string.status_complete)
                TaskStatus.ACTIVE -> itemView.context.getString(R.string.status_incomplete)
                TaskStatus.DELETED -> itemView.context.getString(R.string.status_deleted)
            }
            taskFinalizada.text = taskStatusString

            // Exibe texto de importância
            val importanceTextString = when (task.importance) {
                ImportanceLevel.HIGH -> itemView.context.getString(R.string.importance_high)
                ImportanceLevel.MEDIUM -> itemView.context.getString(R.string.importance_medium)
                ImportanceLevel.LIGHT -> itemView.context.getString(R.string.importance_light)
            }
            importanceTextView.text = importanceTextString

            // Configura checkbox de conclusão
            checkbox.setOnCheckedChangeListener(null) // Remove listener temporário
            checkbox.isChecked = task.status == TaskStatus.COMPLETED
            checkbox.setOnCheckedChangeListener { _, isChecked ->
                val newStatus = if (isChecked) TaskStatus.COMPLETED else TaskStatus.ACTIVE
                onCheckChanged(task, newStatus)
            }
        }

        /**
         * Exibe menu de contexto para ações na tarefa
         */
        private fun showPopupMenu(view: View, task: Task) {
            val popupMenu = PopupMenu(view.context, view)
            popupMenu.inflate(R.menu.context_menu_task)
            popupMenu.setOnMenuItemClickListener { menuItem ->
                // Delega ação para a MainActivity
                val activity = view.context as? MainActivity
                if (activity != null) {
                    activity.setSelectedTaskForContextMenu(task)
                    activity.onContextItemSelected(menuItem)
                }
                true
            }

            // Força exibição de ícones (hack para versões antigas)
            try {
                val fieldMPopup = PopupMenu::class.java.getDeclaredField("mPopup")
                fieldMPopup.isAccessible = true
                val mPopup = fieldMPopup.get(popupMenu)
                mPopup.javaClass
                    .getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                    .invoke(mPopup, true)
            } catch (e: Exception) {
                // Ignora falhas em versões mais recentes
            }
            popupMenu.show()
        }
    }

    /**
     * Callback para cálculo de diferenças entre listas
     */
    class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem == newItem
        }
    }
}
