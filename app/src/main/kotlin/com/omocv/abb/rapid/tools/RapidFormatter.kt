package com.omocv.abb.rapid.tools

import com.omocv.abb.rapid.*

object RapidFormatter {

    fun format(source: String, indentSize: Int = 4): String {
        val result = RapidCompiler.analyze(source)
        val program = result.program ?: return source
        val sb = StringBuilder()
        val ctx = FormatContext(indentSize)
        ctx.formatProgram(program, sb)
        return sb.toString()
    }
}

class FormatContext(
    private val indentSize: Int
) {
    private var indentLevel = 0

    private fun indent(sb: StringBuilder) {
        repeat(indentLevel * indentSize) { sb.append(' ') }
    }

    fun formatProgram(p: Program, sb: StringBuilder) {
        p.modules.forEachIndexed { idx, mod ->
            formatModule(mod, sb)
            if (idx < p.modules.size - 1) sb.appendLine()
        }
    }

    private fun formatModule(m: ModuleNode, sb: StringBuilder) {
        sb.appendLine("MODULE ${m.name}")
        indentLevel++
        m.declarations.forEach {
            when (it) {
                is VarDecl -> formatVarDecl(it, sb)
                is ProcDecl -> formatProcDecl(it, sb)
                is FuncDecl -> formatFuncDecl(it, sb)
                is RecordDecl -> formatRecordDecl(it, sb)
                is TrapDecl -> formatTrapDecl(it, sb)
            }
            sb.appendLine()
        }
        indentLevel--
        sb.appendLine("ENDMODULE")
    }

    private fun formatVarDecl(v: VarDecl, sb: StringBuilder) {
        indent(sb)
        val storage = when (v.storage) {
            StorageClass.VAR -> "VAR"
            StorageClass.PERS -> "PERS"
            StorageClass.CONST -> "CONST"
        }
        sb.append(storage)
        sb.append(' ')
        sb.append(v.typeName)
        sb.append(' ')
        sb.append(v.name)
        v.initExpr?.let {
            sb.append(" := ")
            formatExpr(it, sb)
        }
        sb.append(';')
    }

    private fun formatRecordDecl(r: RecordDecl, sb: StringBuilder) {
        indent(sb)
        sb.append("RECORD ")
        sb.append(r.name)
        sb.append('(')
        r.fields.forEachIndexed { idx, f ->
            if (idx > 0) sb.append(", ")
            sb.append(f.typeName)
            sb.append(' ')
            sb.append(f.fieldName)
        }
        sb.append(");")
    }

    private fun formatTrapDecl(t: TrapDecl, sb: StringBuilder) {
        indent(sb)
        sb.append("TRAP ")
        sb.appendLine(t.name)
        indentLevel++
        t.body.forEach {
            formatStmt(it, sb)
            sb.appendLine()
        }
        indentLevel--
        indent(sb)
        sb.append("ENDTRAP")
    }

    private fun formatProcDecl(p: ProcDecl, sb: StringBuilder) {
        indent(sb)
        sb.append("PROC ")
        sb.append(p.name)
        sb.append('(')
        p.params.forEachIndexed { idx, param ->
            if (idx > 0) sb.append(", ")
            sb.append(param.typeName)
            sb.append(' ')
            sb.append(param.name)
        }
        sb.appendLine(")")
        indentLevel++
        p.body.forEach {
            formatStmt(it, sb)
            sb.appendLine()
        }
        indentLevel--
        indent(sb)
        sb.append("ENDPROC")
    }

    private fun formatFuncDecl(f: FuncDecl, sb: StringBuilder) {
        indent(sb)
        sb.append("FUNC ")
        sb.append(f.returnType)
        sb.append(' ')
        sb.append(f.name)
        sb.append('(')
        f.params.forEachIndexed { idx, param ->
            if (idx > 0) sb.append(", ")
            sb.append(param.typeName)
            sb.append(' ')
            sb.append(param.name)
        }
        sb.appendLine(")")
        indentLevel++
        f.body.forEach {
            formatStmt(it, sb)
            sb.appendLine()
        }
        indentLevel--
        indent(sb)
        sb.append("ENDFUNC")
    }

    private fun formatStmt(s: StmtNode, sb: StringBuilder) {
        when (s) {
            is AssignStmt -> {
                indent(sb)
                formatExpr(s.target, sb)
                sb.append(" := ")
                formatExpr(s.value, sb)
                sb.append(';')
            }
            is LabelStmt -> {
                indent(sb)
                sb.append(s.name)
                sb.append(':')
            }
            is ExprStmt -> {
                indent(sb)
                formatExpr(s.expr, sb)
                sb.append(';')
            }
            is IfStmt -> formatIfStmt(s, sb)
            is WhileStmt -> formatWhileStmt(s, sb)
            is ForStmt -> formatForStmt(s, sb)
            is ReturnStmt -> {
                indent(sb)
                sb.append("RETURN")
                s.expr?.let {
                    sb.append(' ')
                    formatExpr(it, sb)
                }
                sb.append(';')
            }
            is MoveStmt -> {
                indent(sb)
                sb.append(
                    when (s.kind) {
                        MoveKind.MoveJ -> "MoveJ "
                        MoveKind.MoveL -> "MoveL "
                        MoveKind.MoveC -> "MoveC "
                    }
                )
                formatExpr(s.target, sb)
                sb.append(", ")
                formatExpr(s.speed, sb)
                sb.append(", ")
                formatExpr(s.zone, sb)
                sb.append(", ")
                formatExpr(s.tool, sb)
                s.wobj?.let {
                    sb.append(" \\WObj:=")
                    formatExpr(it, sb)
                }
                sb.append(';')
            }
            is TestStmt -> formatTestStmt(s, sb)
            is ConnectStmt -> {
                indent(sb)
                sb.append("CONNECT ")
                sb.append(s.trapName)
                sb.append(" WITH ")
                sb.append(s.errName)
                sb.append(';')
            }
            is RaiseStmt -> {
                indent(sb)
                sb.append("RAISE ")
                sb.append(s.errName)
                sb.append(';')
            }
            is BlockStmt -> {
                s.statements.forEach {
                    formatStmt(it, sb)
                    sb.appendLine()
                }
            }
        }
    }

    private fun formatIfStmt(s: IfStmt, sb: StringBuilder) {
        s.branches.forEachIndexed { idx, br ->
            indent(sb)
            if (idx == 0) sb.append("IF ") else sb.append("ELSEIF ")
            formatExpr(br.condition, sb)
            sb.appendLine(" THEN")
            indentLevel++
            br.body.forEach {
                formatStmt(it, sb)
                sb.appendLine()
            }
            indentLevel--
        }
        s.elseBranch?.let { elseBody ->
            indent(sb)
            sb.appendLine("ELSE")
            indentLevel++
            elseBody.forEach {
                formatStmt(it, sb)
                sb.appendLine()
            }
            indentLevel--
        }
        indent(sb)
        sb.append("ENDIF")
    }

    private fun formatWhileStmt(s: WhileStmt, sb: StringBuilder) {
        indent(sb)
        sb.append("WHILE ")
        formatExpr(s.condition, sb)
        sb.appendLine(" DO")
        indentLevel++
        s.body.forEach {
            formatStmt(it, sb)
            sb.appendLine()
        }
        indentLevel--
        indent(sb)
        sb.append("ENDWHILE")
    }

    private fun formatForStmt(s: ForStmt, sb: StringBuilder) {
        indent(sb)
        sb.append("FOR ")
        sb.append(s.loopVar)
        sb.append(" FROM ")
        formatExpr(s.fromExpr, sb)
        sb.append(" TO ")
        formatExpr(s.toExpr, sb)
        sb.appendLine(" DO")
        indentLevel++
        s.body.forEach {
            formatStmt(it, sb)
            sb.appendLine()
        }
        indentLevel--
        indent(sb)
        sb.append("ENDFOR")
    }

    private fun formatTestStmt(s: TestStmt, sb: StringBuilder) {
        indent(sb)
        sb.append("TEST ")
        formatExpr(s.expr, sb)
        sb.appendLine()
        indentLevel++
        s.cases.forEach { c ->
            indent(sb)
            sb.append("CASE ")
            c.values.forEachIndexed { idx, v ->
                if (idx > 0) sb.append(", ")
                formatExpr(v, sb)
            }
            sb.appendLine(":")
            indentLevel++
            c.body.forEach {
                formatStmt(it, sb)
                sb.appendLine()
            }
            indentLevel--
        }
        s.defaultBody?.let { db ->
            indent(sb)
            sb.appendLine("DEFAULT:")
            indentLevel++
            db.forEach {
                formatStmt(it, sb)
                sb.appendLine()
            }
            indentLevel--
        }
        indentLevel--
        indent(sb)
        sb.append("ENDTEST")
    }

    private fun formatExpr(e: ExprNode, sb: StringBuilder) {
        when (e) {
            is NumLiteral -> sb.append(e.value)
            is BoolLiteral -> sb.append(if (e.value) "TRUE" else "FALSE")
            is StringLiteral -> {
                sb.append('"')
                sb.append(e.value)
                sb.append('"')
            }
            is VarRef -> sb.append(e.name)
            is ArrayAccess -> {
                formatExpr(e.base, sb)
                sb.append('[')
                formatExpr(e.index, sb)
                sb.append(']')
            }
            is FieldAccess -> {
                formatExpr(e.base, sb)
                sb.append('.')
                sb.append(e.field)
            }
            is DotAccess -> {
                formatExpr(e.base, sb)
                sb.append('.')
                sb.append(e.field)
            }
            is CallExpr -> {
                sb.append(e.name)
                sb.append('(')
                e.args.forEachIndexed { idx, arg ->
                    if (idx > 0) sb.append(", ")
                    formatExpr(arg, sb)
                }
                sb.append(')')
            }
            is UnaryExpr -> {
                when (e.op) {
                    UnaryOp.NOT -> sb.append("NOT ")
                    UnaryOp.PLUS -> sb.append('+')
                    UnaryOp.MINUS -> sb.append('-')
                }
                formatExpr(e.expr, sb)
            }
            is BinaryExpr -> {
                formatExpr(e.left, sb)
                sb.append(' ')
                val opStr = when (e.op) {
                    BinaryOp.ADD -> "+"
                    BinaryOp.SUB -> "-"
                    BinaryOp.MUL -> "*"
                    BinaryOp.DIV -> "/"
                    BinaryOp.EQ -> "="
                    BinaryOp.NEQ -> "<>"
                    BinaryOp.LT -> "<"
                    BinaryOp.GT -> ">"
                    BinaryOp.LE -> "<="
                    BinaryOp.GE -> ">="
                    BinaryOp.AND -> "AND"
                    BinaryOp.OR -> "OR"
                }
                sb.append(opStr)
                sb.append(' ')
                formatExpr(e.right, sb)
            }
        }
    }
}
