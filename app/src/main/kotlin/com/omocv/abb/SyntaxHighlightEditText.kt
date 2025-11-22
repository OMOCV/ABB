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
    private var persistentHighlight: Triple<Int, Int, Int>? = null

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
        isInternalChange = true
        
        // Save cursor position
        val cursorPosition = selectionStart
        
        // Save any BackgroundColorSpan (used for highlighting search results and errors)
        val backgroundSpans = editable.getSpans(0, editable.length, android.text.style.BackgroundColorSpan::class.java)
        val savedSpans = backgroundSpans.map { span ->
            // Extract the color from the span to create a new one later
            Triple(
                (span as android.text.style.BackgroundColorSpan).backgroundColor,
                editable.getSpanStart(span),
                editable.getSpanEnd(span)
            )
        }
        
        // Remove all existing spans (including BackgroundColorSpan, which will be restored later)
        val spans = editable.getSpans(0, editable.length, Object::class.java)
        for (span in spans) {
            editable.removeSpan(span)
        }
        
        // Apply new syntax highlighting
        val highlightedText = syntaxHighlighter.highlight(editable.toString())
        val newSpans = highlightedText.getSpans(0, highlightedText.length, Object::class.java)
        
        for (span in newSpans) {
            val start = highlightedText.getSpanStart(span)
            val end = highlightedText.getSpanEnd(span)
            val flags = highlightedText.getSpanFlags(span)
            
            if (start >= 0 && end <= editable.length) {
                editable.setSpan(span, start, end, flags)
            }
        }
        
        // Restore saved BackgroundColorSpan (for highlighting search results and errors)
        // Create NEW spans with the saved color values instead of reusing the old span objects
        for ((color, start, end) in savedSpans) {
            if (start >= 0 && end <= editable.length) {
                editable.setSpan(
                    android.text.style.BackgroundColorSpan(color),
                    start,
                    end,
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        // Reapply any persistent highlight (e.g., search or syntax error jump) so it isn't
        // lost when syntax highlighting refreshes the text.
        persistentHighlight?.let { (color, start, end) ->
            if (start in 0..editable.length) {
                val clampedEnd = end.coerceIn(start, editable.length)
                editable.setSpan(
                    android.text.style.BackgroundColorSpan(color),
                    start,
                    clampedEnd,
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
        
        // Restore cursor position
        if (cursorPosition >= 0 && cursorPosition <= editable.length) {
            setSelection(cursorPosition)
        }
        
        isInternalChange = false
    }

    fun setHighlightingEnabled(enabled: Boolean) {
        highlightingEnabled = enabled
        if (enabled) {
            text?.let { applySyntaxHighlighting(it) }
        }
    }

    fun isHighlightingEnabled(): Boolean = highlightingEnabled

    /**
     * Remember an external highlight range so it survives subsequent syntax highlighting updates.
     */
    fun setPersistentHighlight(color: Int, start: Int, end: Int) {
        persistentHighlight = Triple(color, start, end)
    }

    /**
     * Clear any remembered highlight so future syntax highlighting passes don't reapply it.
     */
    fun clearPersistentHighlight() {
        persistentHighlight = null
    }
}
