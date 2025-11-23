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
    private var persistentHighlightSpan: android.text.style.BackgroundColorSpan? = null

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
            if (editable.isEmpty()) continue

            val clampedStart = start.coerceIn(0, editable.length - 1)
            val desiredEnd = if (end <= clampedStart) clampedStart + 1 else end
            val clampedEnd = desiredEnd.coerceIn(clampedStart + 1, editable.length)

            editable.setSpan(
                android.text.style.BackgroundColorSpan(color),
                clampedStart,
                clampedEnd,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE or android.text.Spanned.SPAN_PRIORITY
            )
        }

        // Reapply any persistent highlight (e.g., search or syntax error jump) so it isn't
        // lost when syntax highlighting refreshes the text.
        reapplyPersistentHighlight()
        
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

        // Apply immediately so callers don't need to worry about the timing of span restoration.
        reapplyPersistentHighlight()
    }

    /**
     * Clear any remembered highlight so future syntax highlighting passes don't reapply it.
     */
    fun clearPersistentHighlight() {
        persistentHighlight = null
        persistentHighlightSpan?.let { span ->
            text?.removeSpan(span)
        }
        persistentHighlightSpan = null
    }

    /**
     * Apply the currently remembered highlight span to the editable text if possible.
     */
    private fun reapplyPersistentHighlight() {
        val editable = text ?: return
        val (color, start, end) = persistentHighlight ?: return

        if (start < 0 || editable.isEmpty() || start >= editable.length) return

        val clampedStart = start.coerceIn(0, editable.length - 1)
        val desiredEnd = if (end <= start) start + 1 else end
        val clampedEnd = desiredEnd.coerceIn(clampedStart + 1, editable.length)

        // Remove any previous persistent span to avoid stacking and to ensure the most recent
        // color is visible above syntax spans.
        persistentHighlightSpan?.let { editable.removeSpan(it) }

        val newSpan = android.text.style.BackgroundColorSpan(color)
        persistentHighlightSpan = newSpan

        editable.setSpan(
            newSpan,
            clampedStart,
            clampedEnd,
            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE or android.text.Spanned.SPAN_PRIORITY
        )

        // Force a redraw so the background shows immediately in edit mode.
        invalidate()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        // Layout passes in edit mode can clear span rendering; ensure the persistent highlight
        // is reapplied so the background remains visible after jumps.
        reapplyPersistentHighlight()
    }
}
