package com.omocv.abb

import android.content.Context
import android.text.Editable
import android.text.Spannable
import android.text.TextWatcher
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText

/**
 * Custom EditText with real-time syntax highlighting for RAPID code
 */
class SyntaxHighlightEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {

    private val syntaxHighlighter = ABBSyntaxHighlighter()
    private var isInternalChange = false
    private var highlightingEnabled = true

    init {
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (!isInternalChange && highlightingEnabled && s != null) {
                    applySyntaxHighlighting(s)
                }
            }
        })
    }

    private fun applySyntaxHighlighting(editable: Editable) {
        try {
            isInternalChange = true
            
            // Skip if content is too large
            if (editable.length > 500000) { // 500KB limit for real-time highlighting
                android.util.Log.w("SyntaxHighlightEditText", "Content too large for real-time highlighting: ${editable.length} chars")
                isInternalChange = false
                return
            }
            
            // Save cursor position
            val cursorPosition = selectionStart
            
            // Save any BackgroundColorSpan (used for highlighting search results and errors)
            val backgroundColorSpans = editable.getSpans(0, editable.length, android.text.style.BackgroundColorSpan::class.java)
            val savedBackgroundColorSpans = backgroundColorSpans.mapNotNull { span ->
                try {
                    // Extract the color from the span to create a new one later
                    Triple(
                        (span as android.text.style.BackgroundColorSpan).backgroundColor,
                        editable.getSpanStart(span),
                        editable.getSpanEnd(span)
                    )
                } catch (e: Exception) {
                    android.util.Log.w("SyntaxHighlightEditText", "Error saving BackgroundColorSpan", e)
                    null
                }
            }
            
            // Save any LineBackgroundSpan.Standard (used for full-line highlighting)
            val lineBackgroundSpans = editable.getSpans(0, editable.length, android.text.style.LineBackgroundSpan.Standard::class.java)
            val savedLineBackgroundSpans = lineBackgroundSpans.mapNotNull { span ->
                try {
                    // Extract the color from the span to create a new one later
                    Triple(
                        (span as android.text.style.LineBackgroundSpan.Standard).color,
                        editable.getSpanStart(span),
                        editable.getSpanEnd(span)
                    )
                } catch (e: Exception) {
                    android.util.Log.w("SyntaxHighlightEditText", "Error saving LineBackgroundSpan", e)
                    null
                }
            }
            
            // Remove all existing spans (including background spans, which will be restored later)
            val spans = editable.getSpans(0, editable.length, Object::class.java)
            for (span in spans) {
                try {
                    editable.removeSpan(span)
                } catch (e: Exception) {
                    android.util.Log.w("SyntaxHighlightEditText", "Error removing span", e)
                }
            }
            
            // Apply new syntax highlighting
            val highlightedText = syntaxHighlighter.highlight(editable.toString())
            val newSpans = highlightedText.getSpans(0, highlightedText.length, Object::class.java)
            
            for (span in newSpans) {
                try {
                    val start = highlightedText.getSpanStart(span)
                    val end = highlightedText.getSpanEnd(span)
                    val flags = highlightedText.getSpanFlags(span)
                    
                    if (start >= 0 && end >= start && end <= editable.length) {
                        editable.setSpan(span, start, end, flags)
                    }
                } catch (e: Exception) {
                    android.util.Log.w("SyntaxHighlightEditText", "Error applying span", e)
                }
            }
            
            // Restore saved BackgroundColorSpan (for highlighting search results and errors)
            // Create NEW spans with the saved color values instead of reusing the old span objects
            for ((color, start, end) in savedBackgroundColorSpans) {
                try {
                    if (start >= 0 && end >= start && end <= editable.length) {
                        editable.setSpan(
                            android.text.style.BackgroundColorSpan(color),
                            start,
                            end,
                            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.w("SyntaxHighlightEditText", "Error restoring BackgroundColorSpan", e)
                }
            }
            
            // Restore saved LineBackgroundSpan.Standard (for full-line highlighting)
            // Create NEW spans with the saved color values instead of reusing the old span objects
            for ((color, start, end) in savedLineBackgroundSpans) {
                try {
                    if (start >= 0 && end >= start && end <= editable.length) {
                        editable.setSpan(
                            android.text.style.LineBackgroundSpan.Standard(color),
                            start,
                            end,
                            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.w("SyntaxHighlightEditText", "Error restoring LineBackgroundSpan", e)
                }
            }
            
            // Restore cursor position
            if (cursorPosition >= 0 && cursorPosition <= editable.length) {
                try {
                    setSelection(cursorPosition)
                } catch (e: Exception) {
                    android.util.Log.w("SyntaxHighlightEditText", "Error restoring cursor position", e)
                }
            }
            
        } catch (e: OutOfMemoryError) {
            android.util.Log.e("SyntaxHighlightEditText", "Out of memory applying syntax highlighting", e)
            // Disable highlighting to prevent further crashes
            highlightingEnabled = false
        } catch (e: Exception) {
            android.util.Log.e("SyntaxHighlightEditText", "Error applying syntax highlighting", e)
        } finally {
            isInternalChange = false
        }
    }

    fun setHighlightingEnabled(enabled: Boolean) {
        highlightingEnabled = enabled
        if (enabled) {
            try {
                text?.let { applySyntaxHighlighting(it) }
            } catch (e: Exception) {
                android.util.Log.e("SyntaxHighlightEditText", "Error enabling highlighting", e)
                highlightingEnabled = false
            }
        }
    }

    fun isHighlightingEnabled(): Boolean = highlightingEnabled
}
