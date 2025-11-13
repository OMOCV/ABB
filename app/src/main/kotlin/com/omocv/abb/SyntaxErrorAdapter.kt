package com.omocv.abb

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter for displaying syntax errors with clickable items to jump to error locations
 */
class SyntaxErrorAdapter(
    private val errors: List<SyntaxError>,
    private val onItemClick: (SyntaxError) -> Unit
) : RecyclerView.Adapter<SyntaxErrorAdapter.SyntaxErrorViewHolder>() {

    class SyntaxErrorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvErrorLine: TextView = itemView.findViewById(R.id.tvErrorLine)
        val tvErrorMessage: TextView = itemView.findViewById(R.id.tvErrorMessage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SyntaxErrorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_syntax_error, parent, false)
        return SyntaxErrorViewHolder(view)
    }

    override fun onBindViewHolder(holder: SyntaxErrorViewHolder, position: Int) {
        val error = errors[position]
        
        // Build the line/column display text
        val locationText = if (error.columnStart > 0 || error.columnEnd > 0) {
            holder.itemView.context.getString(
                R.string.line_column_number,
                error.lineNumber.toString(),
                (error.columnStart + 1).toString()
            )
        } else {
            holder.itemView.context.getString(
                R.string.line_number,
                error.lineNumber.toString()
            )
        }
        
        holder.tvErrorLine.text = locationText
        holder.tvErrorMessage.text = error.message
        
        holder.itemView.setOnClickListener {
            onItemClick(error)
        }
    }

    override fun getItemCount(): Int = errors.size
}
