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
        val backgroundSpans = editable.getSpans(0, editable.length, android.text.style.BackgroundColorSpan::class.java)
        val savedSpans = backgroundSpans.map { span ->
            Triple(
                span,
                editable.getSpanStart(span),
                editable.getSpanEnd(span)
            )
        }
        
        // Remove all existing spans except BackgroundColorSpan
        val spans = editable.getSpans(0, editable.length, Object::class.java)
        for (span in spans) {
            if (span !is android.text.style.BackgroundColorSpan) {
                editable.removeSpan(span)
            }
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
        for ((span, start, end) in savedSpans) {
            if (start >= 0 && end <= editable.length) {
                editable.setSpan(span, start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
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
