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
    private val onAction: (Task, DeletedTasksAdapter.DeletedTaskAction) -> Unit
) : ListAdapter<Task, DeletedTasksAdapter.ViewHolder>(TaskDiffCallback()) {

    sealed class DeletedTaskAction {
        object REACTIVATE : DeletedTaskAction()
        object VIEW_DETAILS : DeletedTaskAction()
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val titleTextView: TextView = view.findViewById(R.id.textViewTaskTitle)
        private val descriptionTextView: TextView = view.findViewById(R.id.textViewTaskDescription)
        private val dueDateTextView: TextView = view.findViewById(R.id.textViewTaskDueDate)
        private val statusTextView: TextView = view.findViewById(R.id.finalizado)

        init {
            // Clique longo para menu de contexto
            itemView.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    showContextMenu(getItem(position))
                }
                true
            }
        }

        fun bind(task: Task) {
            titleTextView.text = task.title
            descriptionTextView.text = task.description
            dueDateTextView.text = itemView.context.getString(R.string.due_date_format, task.dueDate)
            statusTextView.text = itemView.context.getString(R.string.status_deleted)
        }

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem == newItem
        }
    }
}
