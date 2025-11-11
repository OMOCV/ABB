package com.omocv.abb

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter for selecting modules for replacement operations
 */
class ModuleSelectionAdapter(
    private val modules: List<ABBModule>,
    private val selectedModules: MutableSet<ABBModule> = mutableSetOf()
) : RecyclerView.Adapter<ModuleSelectionAdapter.ModuleViewHolder>() {

    class ModuleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cbModuleSelected: CheckBox = itemView.findViewById(R.id.cbModuleSelected)
        val tvModuleName: TextView = itemView.findViewById(R.id.tvModuleName)
        val tvModuleInfo: TextView = itemView.findViewById(R.id.tvModuleInfo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModuleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_module_selection, parent, false)
        return ModuleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ModuleViewHolder, position: Int) {
        val module = modules[position]
        
        holder.tvModuleName.text = module.name
        holder.tvModuleInfo.text = "${module.type}, Lines ${module.startLine}-${module.endLine}"
        
        holder.cbModuleSelected.isChecked = selectedModules.contains(module)
        
        holder.cbModuleSelected.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedModules.add(module)
            } else {
                selectedModules.remove(module)
            }
        }
        
        holder.itemView.setOnClickListener {
            holder.cbModuleSelected.isChecked = !holder.cbModuleSelected.isChecked
        }
    }

    override fun getItemCount(): Int = modules.size
    
    fun getSelectedModules(): List<ABBModule> = selectedModules.toList()
    
    fun selectAll() {
        selectedModules.clear()
        selectedModules.addAll(modules)
        notifyDataSetChanged()
    }
    
    fun deselectAll() {
        selectedModules.clear()
        notifyDataSetChanged()
    }
}
