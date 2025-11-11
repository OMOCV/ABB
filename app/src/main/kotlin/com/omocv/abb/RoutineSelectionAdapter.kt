package com.omocv.abb

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter for selecting routines for replacement operations
 */
class RoutineSelectionAdapter(
    private val routines: List<ABBRoutine>,
    private val selectedRoutines: MutableSet<ABBRoutine> = mutableSetOf()
) : RecyclerView.Adapter<RoutineSelectionAdapter.RoutineViewHolder>() {

    class RoutineViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cbRoutineSelected: CheckBox = itemView.findViewById(R.id.cbRoutineSelected)
        val tvRoutineName: TextView = itemView.findViewById(R.id.tvRoutineName)
        val tvRoutineInfo: TextView = itemView.findViewById(R.id.tvRoutineInfo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoutineViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_routine_selection, parent, false)
        return RoutineViewHolder(view)
    }

    override fun onBindViewHolder(holder: RoutineViewHolder, position: Int) {
        val routine = routines[position]
        
        holder.tvRoutineName.text = routine.name
        holder.tvRoutineInfo.text = "${routine.type}, Lines ${routine.startLine}-${routine.endLine}"
        
        holder.cbRoutineSelected.isChecked = selectedRoutines.contains(routine)
        
        holder.cbRoutineSelected.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedRoutines.add(routine)
            } else {
                selectedRoutines.remove(routine)
            }
        }
        
        holder.itemView.setOnClickListener {
            holder.cbRoutineSelected.isChecked = !holder.cbRoutineSelected.isChecked
        }
    }

    override fun getItemCount(): Int = routines.size
    
    fun getSelectedRoutines(): List<ABBRoutine> = selectedRoutines.toList()
    
    fun selectAll() {
        selectedRoutines.clear()
        selectedRoutines.addAll(routines)
        notifyDataSetChanged()
    }
    
    fun deselectAll() {
        selectedRoutines.clear()
        notifyDataSetChanged()
    }
}
