package com.omocv.abb.rapid.tools

import com.omocv.abb.rapid.*

object RapidSymbolIndex {

    data class SymbolDef(
        val name: String,
        val span: Span,
        val node: AstNode
    )

    data class Index(
        val defs: MutableMap<String, MutableList<SymbolDef>> = mutableMapOf(),
        val refs: MutableMap<String, MutableList<Span>> = mutableMapOf()
    )

    fun build(program: Program?): Index {
        val idx = Index()
        if (program == null) return idx

        program.modules.forEach { mod ->
            mod.declarations.forEach { decl ->
                when (decl) {
                    is VarDecl -> addDef(idx, decl.name, decl.span, decl)
                    is ProcDecl -> addDef(idx, decl.name, decl.span, decl)
                    is FuncDecl -> addDef(idx, decl.name, decl.span, decl)
                    is RecordDecl -> addDef(idx, decl.name, decl.span, decl)
                    is TrapDecl -> addDef(idx, decl.name, decl.span, decl)
                }
            }
        }

        program.modules.forEach { mod ->
            walkDeclsForRefs(mod.declarations, idx)
        }

        return idx
    }

    private fun addDef(index: Index, name: String, span: Span, node: AstNode) {
        val list = index.defs.getOrPut(name) { mutableListOf() }
        list.add(SymbolDef(name, span, node))
    }

    private fun addRef(index: Index, name: String, span: Span) {
        val list = index.refs.getOrPut(name) { mutableListOf() }
        list.add(span)
    }

    private fun walkDeclsForRefs(decls: List<DeclNode>, index: Index) {
        decls.forEach { decl ->
            when (decl) {
                is VarDecl -> decl.initExpr?.let { walkExpr(it, index) }
                is ProcDecl -> decl.body.forEach { walkStmt(it, index) }
                is FuncDecl -> decl.body.forEach { walkStmt(it, index) }
                is RecordDecl -> {}
                is TrapDecl -> decl.body.forEach { walkStmt(it, index) }
            }
        }
    }

    private fun walkStmt(stmt: StmtNode, index: Index) {
        when (stmt) {
            is AssignStmt -> {
                walkExpr(stmt.target, index)
                walkExpr(stmt.value, index)
            }
            is LabelStmt -> { /* Labels are markers; no refs to record */ }
            is ExprStmt -> walkExpr(stmt.expr, index)
            is IfStmt -> {
                stmt.branches.forEach {
                    walkExpr(it.condition, index)
                    it.body.forEach { s -> walkStmt(s, index) }
                }
                stmt.elseBranch?.forEach { walkStmt(it, index) }
            }
            is WhileStmt -> {
                walkExpr(stmt.condition, index)
                stmt.body.forEach { walkStmt(it, index) }
            }
            is ForStmt -> {
                walkExpr(stmt.fromExpr, index)
                walkExpr(stmt.toExpr, index)
                stmt.body.forEach { walkStmt(it, index) }
            }
            is ReturnStmt -> stmt.expr?.let { walkExpr(it, index) }
            is MoveStmt -> {
                walkExpr(stmt.target, index)
                walkExpr(stmt.speed, index)
                walkExpr(stmt.zone, index)
                walkExpr(stmt.tool, index)
                stmt.wobj?.let { walkExpr(it, index) }
            }
            is TestStmt -> {
                walkExpr(stmt.expr, index)
                stmt.cases.forEach { c ->
                    c.values.forEach { walkExpr(it, index) }
                    c.body.forEach { walkStmt(it, index) }
                }
                stmt.defaultBody?.forEach { walkStmt(it, index) }
            }
            is ConnectStmt -> {
                addRef(index, stmt.trapName, stmt.span)
                addRef(index, stmt.errName, stmt.span)
            }
            is RaiseStmt -> addRef(index, stmt.errName, stmt.span)
            is BlockStmt -> stmt.statements.forEach { walkStmt(it, index) }
        }
    }

    private fun walkExpr(expr: ExprNode, index: Index) {
        when (expr) {
            is NumLiteral, is BoolLiteral, is StringLiteral -> {}
            is VarRef -> addRef(index, expr.name, expr.span)
            is ArrayAccess -> {
                walkExpr(expr.base, index)
                walkExpr(expr.index, index)
            }
            is FieldAccess -> walkExpr(expr.base, index)
            is DotAccess -> walkExpr(expr.base, index)
            is CallExpr -> {
                addRef(index, expr.name, expr.span)
                expr.args.forEach { walkExpr(it, index) }
            }
            is UnaryExpr -> walkExpr(expr.expr, index)
            is BinaryExpr -> {
                walkExpr(expr.left, index)
                walkExpr(expr.right, index)
            }
        }
    }
}
