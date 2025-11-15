package com.yourcompany.rapid.tools

import com.yourcompany.rapid.*

data class Location(
    val span: Span
)

data class DefinitionResult(
    val name: String,
    val definition: Location?,
    val references: List<Location>
)

object RapidNavigator {

    fun findDefinition(source: String, line: Int, col: Int): DefinitionResult? {
        val result = RapidCompiler.analyze(source)
        val program = result.program ?: return null
        val index = RapidSymbolIndex.build(program)

        val ident = findIdentAt(source, line, col) ?: return null
        val defs = index.defs[ident] ?: emptyList()
        val defLoc = defs.firstOrNull()?.span?.let { Location(it) }
        val refs = index.refs[ident]?.map { Location(it) } ?: emptyList()

        return DefinitionResult(ident, defLoc, refs)
    }

    private fun findIdentAt(source: String, line: Int, col: Int): String? {
        val lines = source.split("\n")
        if (line <= 0 || line > lines.size) return null
        val textLine = lines[line - 1]
        if (textLine.isEmpty()) return null
        val idx = (col - 1).coerceIn(0, textLine.length)

        var start = idx
        var end = idx
        while (start > 0 && (textLine[start - 1].isLetterOrDigit() || textLine[start - 1] == '_' || textLine[start - 1] == '.')) {
            start--
        }
        while (end < textLine.length && (textLine[end].isLetterOrDigit() || textLine[end] == '_' || textLine[end] == '.')) {
            end++
        }
        if (start == end) return null
        return textLine.substring(start, end)
    }
}
