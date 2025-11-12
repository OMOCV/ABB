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
        isInternalChange = true
        
        // Save cursor position
        val cursorPosition = selectionStart
        
        // Save any BackgroundColorSpan (used for highlighting search results and errors)
        val backgroundColorSpans = editable.getSpans(0, editable.length, android.text.style.BackgroundColorSpan::class.java)
        val savedBackgroundColorSpans = backgroundColorSpans.map { span ->
            // Extract the color from the span to create a new one later
            Triple(
                (span as android.text.style.BackgroundColorSpan).backgroundColor,
                editable.getSpanStart(span),
                editable.getSpanEnd(span)
            )
        }
        
        // Save any LineBackgroundSpan.Standard (used for full-line highlighting)
        val lineBackgroundSpans = editable.getSpans(0, editable.length, android.text.style.LineBackgroundSpan.Standard::class.java)
        val savedLineBackgroundSpans = lineBackgroundSpans.map { span ->
            // Extract the color from the span to create a new one later
            Triple(
                (span as android.text.style.LineBackgroundSpan.Standard).color,
                editable.getSpanStart(span),
                editable.getSpanEnd(span)
            )
        }
        
        // Remove all existing spans (including background spans, which will be restored later)
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
        for ((color, start, end) in savedBackgroundColorSpans) {
            if (start >= 0 && end <= editable.length) {
                editable.setSpan(
                    android.text.style.BackgroundColorSpan(color),
                    start,
                    end,
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
        
        // Restore saved LineBackgroundSpan.Standard (for full-line highlighting)
        // Create NEW spans with the saved color values instead of reusing the old span objects
        for ((color, start, end) in savedLineBackgroundSpans) {
            if (start >= 0 && end <= editable.length) {
                editable.setSpan(
                    android.text.style.LineBackgroundSpan.Standard(color),
                    start,
                    end,
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
}
