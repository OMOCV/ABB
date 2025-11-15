package com.yourcompany.rapid.tools

import com.yourcompany.rapid.*

data class CompletionItem(
    val label: String,
    val kind: CompletionKind,
    val insertText: String
)

enum class CompletionKind {
    Keyword,
    Type,
    Variable,
    Function,
    Record,
    Snippet
}

object RapidCompletion {

    private val keywordList = listOf(
        "MODULE", "ENDMODULE",
        "PROC", "ENDPROC",
        "FUNC", "ENDFUNC",
        "VAR", "PERS", "CONST",
        "IF", "THEN", "ELSEIF", "ELSE", "ENDIF",
        "FOR", "FROM", "TO", "ENDFOR",
        "WHILE", "ENDWHILE",
        "TEST", "CASE", "DEFAULT", "ENDTEST",
        "TRAP", "ENDTRAP",
        "CONNECT", "WITH", "RAISE",
        "RETURN",
        "MoveJ", "MoveL", "MoveC"
    )

    private val typeList = listOf(
        "num","dnum","bool","string",
        "robtarget","jointtarget","tooldata","wobjdata",
        "speeddata","zonedata","confdata","orient","pose"
    )

    private val snippets = listOf(
        CompletionItem(
            "IF ... THEN ... ENDIF",
            CompletionKind.Snippet,
            "IF  THEN\n    \nENDIF"
        ),
        CompletionItem(
            "WHILE ... DO ... ENDWHILE",
            CompletionKind.Snippet,
            "WHILE  DO\n    \nENDWHILE"
        ),
        CompletionItem(
            "FOR i FROM 1 TO 10 DO ... ENDFOR",
            CompletionKind.Snippet,
            "FOR i FROM 1 TO 10 DO\n    \nENDFOR"
        ),
        CompletionItem(
            "PROC main() ... ENDPROC",
            CompletionKind.Snippet,
            "PROC main()\n    \nENDPROC"
        ),
        CompletionItem(
            "FUNC num MyFunc() ... ENDFUNC",
            CompletionKind.Snippet,
            "FUNC num MyFunc()\n    \nENDFUNC"
        )
    )

    fun suggest(source: String, line: Int, col: Int): List<CompletionItem> {
        val result = RapidCompiler.analyze(source)
        val program = result.program

        val currentWord = currentPrefix(source, line, col)
        val list = mutableListOf<CompletionItem>()

        keywordList
            .filter { currentWord == null || it.startsWith(currentWord, ignoreCase = true) }
            .forEach { list += CompletionItem(it, CompletionKind.Keyword, it) }

        typeList
            .filter { currentWord == null || it.startsWith(currentWord, ignoreCase = true) }
            .forEach { list += CompletionItem(it, CompletionKind.Type, it) }

        if (program != null) {
            val idx = RapidSymbolIndex.build(program)
            idx.defs.forEach { (name, defs) ->
                if (currentWord != null && !name.startsWith(currentWord, ignoreCase = true)) return@forEach
                val node = defs.first().node
                val kind = when (node) {
                    is VarDecl -> CompletionKind.Variable
                    is ProcDecl, is FuncDecl -> CompletionKind.Function
                    is RecordDecl -> CompletionKind.Record
                    else -> CompletionKind.Variable
                }
                list += CompletionItem(name, kind, name)
            }
        }

        snippets
            .filter { currentWord == null || it.label.uppercase().contains(currentWord.uppercase()) }
            .forEach { list += it }

        return list.distinctBy { it.label }
    }

    private fun currentPrefix(source: String, line: Int, col: Int): String? {
        val lines = source.split("\n")
        if (line < 1 || line > lines.size) return null
        val textLine = lines[line - 1]
        if (textLine.isEmpty()) return null
        val idx = (col - 1).coerceIn(0, textLine.length)

        var start = idx
        while (start > 0 && (textLine[start - 1].isLetterOrDigit() || textLine[start - 1] == '_' )) {
            start--
        }
        if (start == idx) return null
        return textLine.substring(start, idx)
    }
}
