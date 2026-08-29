package com.omocv.abb

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat

/**
 * Custom adapter for displaying cloud providers with icons in a Spinner
 * 自定义适配器，用于在下拉菜单中显示带图标的云服务提供商
 */
class CloudProviderAdapter(
    context: Context,
    private val providers: List<CloudProvider>
) : ArrayAdapter<CloudProvider>(context, 0, providers) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createViewFromResource(position, convertView, parent, android.R.layout.simple_spinner_item)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createViewFromResource(position, convertView, parent, android.R.layout.simple_spinner_dropdown_item)
    }

    private fun createViewFromResource(
        position: Int,
        convertView: View?,
        parent: ViewGroup,
        resource: Int
    ): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(
            R.layout.item_cloud_provider,
            parent,
            false
        )

        val provider = getItem(position) ?: return view

        val iconView = view.findViewById<ImageView>(R.id.ivProviderIcon)
        val nameView = view.findViewById<TextView>(R.id.tvProviderName)

        // Set provider icon (use placeholder if specific icon not found)
        try {
            iconView?.setImageResource(provider.iconRes)
        } catch (e: Exception) {
            // Fallback to generic cloud icon
            iconView?.setImageResource(android.R.drawable.ic_menu_upload)
        }

        // Set provider name
        nameView?.text = context.getString(provider.displayNameRes)

        return view
    }
}
