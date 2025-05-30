package com.alansouza.personaltasks.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
 * Adapter para o RecyclerView que exibe a lista de tarefas ([Task]).
 * Utiliza [ListAdapter] com [DiffUtil] para atualizações eficientes da lista.
 */
class TaskAdapter : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    /**
     * Chamado quando o RecyclerView precisa de um novo [TaskViewHolder] para representar um item.
     * Infla o layout do item da tarefa.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task_layout, parent, false)
        return TaskViewHolder(view)
    }

    /**
     * Chamado pelo RecyclerView para exibir os dados na posição especificada.
     * Vincula os dados da tarefa ([Task]) ao [TaskViewHolder].
     */
    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = getItem(position)
        holder.bind(task)
    }

    /**
     * ViewHolder para cada item de tarefa na lista.
     * Contém referências às views do layout do item e a lógica para preenchê-las.
     */
    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Referências para as Views do layout do item
        private val titleTextView: TextView = itemView.findViewById(R.id.textViewTaskTitle)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.textViewTaskDescription)
        private val dueDateTextView: TextView = itemView.findViewById(R.id.textViewTaskDueDate)
        private val importanceIndicatorView: View = itemView.findViewById(R.id.viewImportanceIndicator)
        private val importanceTextView: TextView = itemView.findViewById(R.id.textViewImportanceText)
        private val taskFinalizada: TextView = itemView.findViewById(R.id.finalizado)

        init {
            // Configura o listener de clique longo para mostrar o menu de contexto
            itemView.setOnLongClickListener {
                val position = bindingAdapterPosition // Posição atual do adapter
                if (position != RecyclerView.NO_POSITION) { // Verifica se a posição é válida
                    val task = getItem(position) // Obtém a tarefa na posição clicada
                    showPopupMenu(itemView, task) // Mostra o menu de contexto
                }
                true // Indica que o evento de clique longo foi consumido
            }
        }

        /**
         * Preenche as views do item da lista com os dados de uma tarefa específica.
         * @param task A tarefa a ser exibida.
         */
        fun bind(task: Task) {
            titleTextView.text = task.title
            descriptionTextView.text = task.description

            // Esconde o campo de descrição se estiver vazio
            if (task.description.isEmpty()) {
                descriptionTextView.visibility = View.GONE
            } else {
                descriptionTextView.visibility = View.VISIBLE
            }
            dueDateTextView.text = itemView.context.getString(R.string.due_date_format, task.dueDate)

            // Define a cor do indicador de importância
            val importanceColor = when (task.importance) {
                ImportanceLevel.HIGH -> ContextCompat.getColor(itemView.context, R.color.importance_high_color)
                ImportanceLevel.MEDIUM -> ContextCompat.getColor(itemView.context, R.color.importance_medium_color)
                ImportanceLevel.LIGHT -> ContextCompat.getColor(itemView.context, R.color.importance_light_color)
            }
            importanceIndicatorView.setBackgroundColor(importanceColor)

            val taskStatusString = when (task.isChecked) {
                TaskStatus.COMPLETE -> itemView.context.getString(R.string.status_complete)
                TaskStatus.INCOMPLETED -> itemView.context.getString(R.string.status_incomplete)
            }
            taskFinalizada.text = taskStatusString

            // Define o texto do nível de importância
            val importanceTextString = when (task.importance) {
                ImportanceLevel.HIGH -> itemView.context.getString(R.string.importance_high)
                ImportanceLevel.MEDIUM -> itemView.context.getString(R.string.importance_medium)
                ImportanceLevel.LIGHT -> itemView.context.getString(R.string.importance_light)
            }
            importanceTextView.text = importanceTextString
        }

        /**
         * Exibe um menu de contexto (popup) para o item da tarefa.
         * Permite ações como editar, excluir e ver detalhes.
         * @param view A view à qual o menu será ancorado (o próprio item da lista).
         * @param task A tarefa associada a este item.
         */
        private fun showPopupMenu(view: View, task: Task) {
            val popupMenu = PopupMenu(view.context, view)
            popupMenu.inflate(R.menu.context_menu_task) // Infla o menu definido em XML
            popupMenu.setOnMenuItemClickListener { menuItem ->
                // Tenta obter a MainActivity a partir do contexto da view
                val activity = view.context as? MainActivity
                if (activity != null) {
                    // Informa a MainActivity qual tarefa foi selecionada para o menu de contexto
                    activity.setSelectedTaskForContextMenu(task)
                    // Delega o tratamento do item de menu clicado para a MainActivity
                    activity.onContextItemSelected(menuItem)
                }
                true // Indica que o clique no item de menu foi tratado
            }

            // Hack para tentar forçar a exibição de ícones no PopupMenu (pode não funcionar em todas as APIs)
            val fieldMPopup = PopupMenu::class.java.getDeclaredField("mPopup")
            fieldMPopup.isAccessible = true
            val mPopup = fieldMPopup.get(popupMenu)
            mPopup.javaClass
                .getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                .invoke(mPopup, true)
            popupMenu.show() // Exibe o menu
        }
    }

    /**
     * Callback para calcular a diferença entre duas listas de tarefas.
     * Usado pelo [ListAdapter] para otimizar as atualizações do RecyclerView.
     */
    class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        /**
         * Verifica se dois itens representam o mesmo objeto (geralmente pelo ID).
         */
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem.id == newItem.id
        }

        /**
         * Verifica se os conteúdos de dois itens são os mesmos.
         * Chamado apenas se [areItemsTheSame] retornar true.
         */
        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem == newItem // Compara todos os campos da data class
        }
    }
}
