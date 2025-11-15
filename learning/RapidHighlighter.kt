package com.yourcompany.rapid.tools

import com.yourcompany.rapid.*

data class HighlightToken(
    val line: Int,
    val startCol: Int,
    val endCol: Int,
    val kind: HighlightKind
)

enum class HighlightKind {
    Keyword,
    TypeName,
    Identifier,
    Number,
    String,
    Comment,
    Operator,
    Punctuation
}

object RapidHighlighter {

    private val keywords = setOf(
        "MODULE","ENDMODULE",
        "PROC","ENDPROC",
        "FUNC","ENDFUNC",
        "VAR","PERS","CONST",
        "IF","THEN","ELSEIF","ELSE","ENDIF",
        "FOR","FROM","TO","ENDFOR",
        "WHILE","ENDWHILE",
        "TEST","CASE","DEFAULT","ENDTEST",
        "TRAP","ENDTRAP",
        "CONNECT","WITH","RAISE",
        "RETURN","TRUE","FALSE",
        "MOVEJ","MOVEL","MOVEC"
    ).map { it.uppercase() }.toSet()

    private val types = setOf(
        "num","dnum","bool","string",
        "robtarget","jointtarget","tooldata","wobjdata",
        "speeddata","zonedata","confdata","orient","pose"
    ).map { it.lowercase() }.toSet()

    fun highlight(source: String): List<HighlightToken> {
        val result = mutableListOf<HighlightToken>()
        val lines = source.split("\n")

        lines.forEachIndexed { idx, raw ->
            val lineNo = idx + 1
            val line = raw
            var col = 1

            val trim = line.trimStart()
            if (trim.startsWith("!")) {
                val startCol = line.indexOf('!') + 1
                result += HighlightToken(lineNo, startCol, line.length + 1, HighlightKind.Comment)
                return@forEachIndexed
            }

            while (col <= line.length) {
                val c = line[col - 1]

                when {
                    c == '!' -> {
                        result += HighlightToken(lineNo, col, line.length + 1, HighlightKind.Comment)
                        break
                    }
                    c.isWhitespace() -> col++
                    c.isDigit() -> {
                        val start = col
                        var i = col
                        while (i <= line.length && (line[i - 1].isDigit() || line[i - 1] == '.')) i++
                        result += HighlightToken(lineNo, start, i, HighlightKind.Number)
                        col = i
                    }
                    c == '"' -> {
                        val start = col
                        var i = col + 1
                        while (i <= line.length && line[i - 1] != '"') i++
                        val end = (i + 1).coerceAtMost(line.length + 1)
                        result += HighlightToken(lineNo, start, end, HighlightKind.String)
                        col = end
                    }
                    c.isLetter() || c == '_' -> {
                        val start = col
                        var i = col
                        while (i <= line.length &&
                            (line[i - 1].isLetterOrDigit() || line[i - 1] == '_' || line[i - 1] == '.')) {
                            i++
                        }
                        val word = line.substring(start - 1, i - 1)
                        val u = word.uppercase()
                        val l = word.lowercase()
                        val kind = when {
                            keywords.contains(u) -> HighlightKind.Keyword
                            types.contains(l) -> HighlightKind.TypeName
                            else -> HighlightKind.Identifier
                        }
                        result += HighlightToken(lineNo, start, i, kind)
                        col = i
                    }
                    c in charArrayOf('+','-','*','/','=','<','>','\\') -> {
                        result += HighlightToken(lineNo, col, col + 1, HighlightKind.Operator)
                        col++
                    }
                    c in charArrayOf('(',')','[',']',',',':',';','.') -> {
                        result += HighlightToken(lineNo, col, col + 1, HighlightKind.Punctuation)
                        col++
                    }
                    else -> col++
                }
            }
        }

        return result
    }
}
