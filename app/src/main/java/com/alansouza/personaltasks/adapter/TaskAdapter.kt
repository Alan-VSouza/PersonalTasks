package com.alansouza.personaltasks.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alansouza.personaltasks.MainActivity
import com.alansouza.personaltasks.R
import com.alansouza.personaltasks.model.Task

class TaskAdapter() :
    ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task_layout, parent, false)
        return TaskViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val currentTask = getItem(position)
        holder.bind(currentTask)
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val textViewTitle: TextView = itemView.findViewById(R.id.textViewTaskTitle)
        private val textViewDescription: TextView = itemView.findViewById(R.id.textViewTaskDescription)
        private val textViewDueDate: TextView = itemView.findViewById(R.id.textViewTaskDueDate)

        private lateinit var currentBoundTask: Task

        init {

            itemView.setOnLongClickListener { viewClicked ->
                if (itemView.context is MainActivity) {
                    (itemView.context as MainActivity).setSelectedTaskForContextMenu(currentBoundTask)
                } else {
                    Log.w("TaskAdapter", "Contexto do ViewHolder não é MainActivity.")
                }

                showPopupMenu(viewClicked)
                true
            }
        }

        fun bind(task: Task) {
            currentBoundTask = task
            textViewTitle.text = task.title
            textViewDescription.text = task.description
            textViewDueDate.text = itemView.context.getString(R.string.due_date_format, task.dueDate)
        }

        private fun showPopupMenu(anchorView: View) {
            val context = anchorView.context
            val popup = PopupMenu(context, anchorView)
            popup.menuInflater.inflate(R.menu.context_menu_task, popup.menu)

            try {
                val fieldMPopup = PopupMenu::class.java.getDeclaredField("mPopup")
                fieldMPopup.isAccessible = true
                val mPopup = fieldMPopup.get(popup)
                mPopup.javaClass
                    .getDeclaredMethod("setForceShowIcon", Boolean::class.javaPrimitiveType)
                    .invoke(mPopup, true)
            } catch (e: Exception) {
                Log.e("TaskAdapter", "Erro ao forçar exibição de ícones no PopupMenu", e)
            }

            popup.setOnMenuItemClickListener { menuItem ->
                if (context is MainActivity) {

                    return@setOnMenuItemClickListener context.onContextItemSelected(menuItem)
                }
                false
            }
            popup.show()
        }

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
