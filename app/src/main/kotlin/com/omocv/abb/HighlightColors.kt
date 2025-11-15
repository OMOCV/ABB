package com.omocv.abb

import android.content.Context
import android.graphics.Color
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat

/**
 * Centralized color management for code highlighting
 * Provides consistent colors for line highlights, error highlights, and search results
 */
object HighlightColors {
    
    /**
     * Get the appropriate highlight color for a regular line highlight
     * @param context The application context
     * @return The color value for line highlighting
     */
    fun getLineHighlightColor(context: Context): Int {
        return if (isDarkTheme()) {
            ContextCompat.getColor(context, R.color.code_highlight_line_dark)
        } else {
            ContextCompat.getColor(context, R.color.code_highlight_line)
        }
    }
    
    /**
     * Get the appropriate highlight color for error highlighting
     * @param context The application context
     * @return The color value for error highlighting
     */
    fun getErrorHighlightColor(context: Context): Int {
        return if (isDarkTheme()) {
            ContextCompat.getColor(context, R.color.code_highlight_error_dark)
        } else {
            ContextCompat.getColor(context, R.color.code_highlight_error)
        }
    }
    
    /**
     * Get the appropriate highlight color for search results
     * @param context The application context
     * @return The color value for search result highlighting
     */
    fun getSearchHighlightColor(context: Context): Int {
        return if (isDarkTheme()) {
            ContextCompat.getColor(context, R.color.code_highlight_search_dark)
        } else {
            ContextCompat.getColor(context, R.color.code_highlight_search)
        }
    }
    
    /**
     * Check if the app is currently in dark theme mode
     * @return true if dark theme is active, false otherwise
     */
    private fun isDarkTheme(): Boolean {
        return AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
    }
}
