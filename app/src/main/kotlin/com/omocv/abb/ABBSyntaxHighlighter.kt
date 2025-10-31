package com.omocv.abb

import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import java.util.regex.Pattern

/**
 * Syntax highlighter for ABB RAPID programming language
 */
class ABBSyntaxHighlighter(
    private val keywordColor: Int = Color.parseColor("#0000FF"),
    private val stringColor: Int = Color.parseColor("#008000"),
    private val commentColor: Int = Color.parseColor("#808080"),
    private val numberColor: Int = Color.parseColor("#FF00FF"),
    private val functionColor: Int = Color.parseColor("#800080"),
    private val typeColor: Int = Color.parseColor("#2B91AF")
) {

    companion object {
        // Regular expression patterns
        private val KEYWORD_PATTERN = Pattern.compile(
            "\\b(MODULE|ENDMODULE|PROC|ENDPROC|FUNC|ENDFUNC|TRAP|ENDTRAP|" +
            "VAR|PERS|CONST|ALIAS|LOCAL|TASK|" +
            "IF|THEN|ELSEIF|ELSE|ENDIF|" +
            "FOR|FROM|TO|STEP|DO|ENDFOR|" +
            "WHILE|ENDWHILE|" +
            "TEST|CASE|DEFAULT|ENDTEST|" +
            "GOTO|LABEL|RETURN|EXIT|" +
            "TRUE|FALSE|" +
            "AND|OR|NOT|XOR|DIV|MOD)\\b",
            Pattern.CASE_INSENSITIVE
        )

        private val TYPE_PATTERN = Pattern.compile(
            "\\b(num|bool|string|pos|orient|pose|confdata|" +
            "robtarget|jointtarget|speeddata|zonedata|tooldata|wobjdata|" +
            "loaddata|clock|intnum)\\b",
            Pattern.CASE_INSENSITIVE
        )

        private val FUNCTION_PATTERN = Pattern.compile(
            "\\b(MoveJ|MoveL|MoveC|MoveAbsJ|" +
            "WaitTime|SetDO|SetAO|Reset|" +
            "TPWrite|TPReadNum|TPReadFK|" +
            "Open|Close|Write|Read|" +
            "AccSet|VelSet|" +
            "ConfJ|ConfL|SingArea|" +
            "PathAccLim|" +
            "StartLoad|WaitLoad|" +
            "EOffsOn|EOffsOff|EOffsSet)\\b"
        )

        private val STRING_PATTERN = Pattern.compile("\"([^\"]*)\"")
        
        private val COMMENT_PATTERN = Pattern.compile("!.*$", Pattern.MULTILINE)
        
        private val NUMBER_PATTERN = Pattern.compile("\\b\\d+\\.?\\d*\\b")
    }

    /**
     * Apply syntax highlighting to text
     */
    fun highlight(text: String): SpannableString {
        val spannable = SpannableString(text)

        // Highlight comments (do this first so other patterns don't affect comments)
        highlightPattern(spannable, COMMENT_PATTERN, commentColor)

        // Highlight strings
        highlightPattern(spannable, STRING_PATTERN, stringColor)

        // Highlight keywords
        highlightPattern(spannable, KEYWORD_PATTERN, keywordColor)

        // Highlight data types
        highlightPattern(spannable, TYPE_PATTERN, typeColor)

        // Highlight functions
        highlightPattern(spannable, FUNCTION_PATTERN, functionColor)

        // Highlight numbers (do this last to avoid conflicts)
        highlightPattern(spannable, NUMBER_PATTERN, numberColor)

        return spannable
    }

    /**
     * Apply color to pattern matches
     */
    private fun highlightPattern(spannable: SpannableString, pattern: Pattern, color: Int) {
        val matcher = pattern.matcher(spannable)
        while (matcher.find()) {
            spannable.setSpan(
                ForegroundColorSpan(color),
                matcher.start(),
                matcher.end(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    /**
     * Highlight specific lines of text
     */
    fun highlightLines(text: String, startLine: Int, endLine: Int): SpannableString {
        val lines = text.lines()
        val selectedLines = lines.subList(
            startLine.coerceAtLeast(0),
            (endLine + 1).coerceAtMost(lines.size)
        )
        return highlight(selectedLines.joinToString("\n"))
    }
}
