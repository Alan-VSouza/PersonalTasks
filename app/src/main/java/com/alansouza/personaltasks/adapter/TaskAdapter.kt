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

class TaskAdapter : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task_layout, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = getItem(position)
        holder.bind(task)
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.textViewTaskTitle)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.textViewTaskDescription)
        private val dueDateTextView: TextView = itemView.findViewById(R.id.textViewTaskDueDate)
        private val importanceIndicatorView: View = itemView.findViewById(R.id.viewImportanceIndicator)

        init {
            itemView.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val task = getItem(position)
                    showPopupMenu(itemView, task)
                }
                true
            }
        }

        fun bind(task: Task) {
            titleTextView.text = task.title
            descriptionTextView.text = task.description
            if (task.description.isEmpty()) {
                descriptionTextView.visibility = View.GONE
            } else {
                descriptionTextView.visibility = View.VISIBLE
            }
            dueDateTextView.text = itemView.context.getString(R.string.due_date_format, task.dueDate)

            val importanceColor = when (task.importance) {
                ImportanceLevel.HIGH -> ContextCompat.getColor(itemView.context, R.color.importance_high_color)
                ImportanceLevel.MEDIUM -> ContextCompat.getColor(itemView.context, R.color.importance_medium_color)
                ImportanceLevel.LIGHT -> ContextCompat.getColor(itemView.context, R.color.importance_light_color)
            }
            importanceIndicatorView.setBackgroundColor(importanceColor)
        }

        private fun showPopupMenu(view: View, task: Task) {
            val popupMenu = PopupMenu(view.context, view)
            popupMenu.inflate(R.menu.context_menu_task)
            popupMenu.setOnMenuItemClickListener { menuItem ->
                val activity = view.context as? MainActivity
                activity?.setSelectedTaskForContextMenu(task)
                activity?.onContextItemSelected(menuItem) ?: false
            }

            try {
                val fieldMPopup = PopupMenu::class.java.getDeclaredField("mPopup")
                fieldMPopup.isAccessible = true
                val mPopup = fieldMPopup.get(popupMenu)
                mPopup.javaClass
                    .getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                    .invoke(mPopup, true)
            } catch (e: Exception) {
            }
            popupMenu.show()
        }
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
