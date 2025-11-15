@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package com.omocv.abb.rapid

// ===================== 公共数据结构 =====================

data class Span(
    val startLine: Int,
    val startCol: Int,
    val endLine: Int,
    val endCol: Int
)

enum class Severity { INFO, WARNING, ERROR }

data class Diagnostic(
    val message: String,
    val span: Span,
    val severity: Severity = Severity.ERROR
)

data class RapidAnalyzeResult(
    val diagnostics: List<Diagnostic>,
    val program: Program?    // 语法错误严重时可能为 null
)

// ===================== AST 定义 =====================

sealed interface AstNode {
    val span: Span
}

data class Program(
    val modules: List<ModuleNode>,
    override val span: Span
) : AstNode

data class ModuleNode(
    val name: String,
    val declarations: List<DeclNode>,
    override val span: Span
) : AstNode

sealed interface DeclNode : AstNode

enum class StorageClass { VAR, PERS, CONST }

data class VarDecl(
    val storage: StorageClass,
    val typeName: String,
    val name: String,
    val initExpr: ExprNode?,
    override val span: Span
) : DeclNode

data class Param(
    val typeName: String,
    val name: String,
    val byRef: Boolean,
    val spanParam: Span
)

data class ProcDecl(
    val name: String,
    val params: List<Param>,
    val body: List<StmtNode>,
    override val span: Span
) : DeclNode

data class FuncDecl(
    val returnType: String,
    val name: String,
    val params: List<Param>,
    val body: List<StmtNode>,
    override val span: Span
) : DeclNode

// ------------ 语句 ------------

sealed interface StmtNode : AstNode

data class BlockStmt(
    val statements: List<StmtNode>,
    override val span: Span
) : StmtNode

data class AssignStmt(
    val target: ExprNode,
    val value: ExprNode,
    override val span: Span
) : StmtNode

data class ExprStmt(
    val expr: ExprNode,
    override val span: Span
) : StmtNode

data class IfBranch(
    val condition: ExprNode,
    val body: List<StmtNode>,
    val spanBranch: Span
)

data class IfStmt(
    val branches: List<IfBranch>,
    val elseBranch: List<StmtNode>?,
    override val span: Span
) : StmtNode

data class WhileStmt(
    val condition: ExprNode,
    val body: List<StmtNode>,
    override val span: Span
) : StmtNode

data class ForStmt(
    val loopVar: String,
    val fromExpr: ExprNode,
    val toExpr: ExprNode,
    val body: List<StmtNode>,
    override val span: Span
) : StmtNode

data class ReturnStmt(
    val expr: ExprNode?,
    override val span: Span
) : StmtNode

enum class MoveKind { MoveJ, MoveL, MoveC }

data class MoveStmt(
    val kind: MoveKind,
    val target: ExprNode,
    val speed: ExprNode,
    val zone: ExprNode,
    val tool: ExprNode,
    val wobj: ExprNode?,
    override val span: Span
) : StmtNode

// TEST 结构（简化版）
data class TestCase(
    val values: List<ExprNode>,
    val body: List<StmtNode>,
    val span: Span
)

data class TestStmt(
    val expr: ExprNode,
    val cases: List<TestCase>,
    val defaultBody: List<StmtNode>?,
    override val span: Span
) : StmtNode

// TRAP 声明（简化版，仅结构）
data class TrapDecl(
    val name: String,
    val body: List<StmtNode>,
    override val span: Span
) : DeclNode

data class ConnectStmt(
    val trapName: String,
    val errName: String,
    override val span: Span
) : StmtNode

data class RaiseStmt(
    val errName: String,
    override val span: Span
) : StmtNode

// RECORD 结构体声明
data class RecordField(
    val typeName: String,
    val fieldName: String,
    val span: Span
)

data class RecordDecl(
    val name: String,
    val fields: List<RecordField>,
    override val span: Span
) : DeclNode

// ------------ 表达式 ------------

sealed interface ExprNode : AstNode

data class NumLiteral(
    val value: Double,
    override val span: Span
) : ExprNode

data class BoolLiteral(
    val value: Boolean,
    override val span: Span
) : ExprNode

data class StringLiteral(
    val value: String,
    override val span: Span
) : ExprNode

data class VarRef(
    val name: String,
    override val span: Span
) : ExprNode

data class ArrayAccess(
    val base: ExprNode,
    val index: ExprNode,
    override val span: Span
) : ExprNode

data class FieldAccess(
    val base: ExprNode,
    val field: String,
    override val span: Span
) : ExprNode

data class DotAccess(
    val base: ExprNode,
    val field: String,
    override val span: Span
) : ExprNode

data class CallExpr(
    val name: String,
    val args: List<ExprNode>,
    override val span: Span
) : ExprNode

enum class UnaryOp { NOT, PLUS, MINUS }

data class UnaryExpr(
    val op: UnaryOp,
    val expr: ExprNode,
    override val span: Span
) : ExprNode

enum class BinaryOp {
    ADD, SUB, MUL, DIV,
    EQ, NEQ, LT, GT, LE, GE,
    AND, OR
}

data class BinaryExpr(
    val left: ExprNode,
    val op: BinaryOp,
    val right: ExprNode,
    override val span: Span
) : ExprNode

// ===================== 词法分析 =====================

enum class TokenType {
    IDENT, NUMBER, STRING,
    MODULE, ENDMODULE,
    PROC, ENDPROC,
    FUNC, ENDFUNC,
    VAR, PERS, CONST,
    IF, THEN, ELSEIF, ELSE, ENDIF,
    FOR, FROM, TO, ENDFOR,
    WHILE, ENDWHILE,
    RETURN,
    TRUE, FALSE,
    MOVEJ, MOVEL, MOVEC,
    AND, OR, NOT,
    TEST, CASE, DEFAULT, ENDTEST,
    TRAP, ENDTRAP,
    CONNECT, WITH,
    RAISE,
    RECORD,
    LPAREN, RPAREN,
    LBRACKET, RBRACKET,
    COMMA, COLON,
    ASSIGN,
    PLUS, MINUS, STAR, SLASH,
    EQ, NEQ, LT, GT, LE, GE,
    BACKSLASH,
    SEMICOLON,
    DOT,
    EOF
}

data class Token(
    val type: TokenType,
    val lexeme: String,
    val span: Span
)

class Lexer(private val source: String) {

    private val diagnostics = mutableListOf<Diagnostic>()
    fun diagnostics(): List<Diagnostic> = diagnostics

    private var index = 0
    private var line = 1
    private var col = 1

    private fun isAtEnd(): Boolean = index >= source.length
    private fun peek(): Char? = if (isAtEnd()) null else source[index]
    private fun peekNext(): Char? =
        if (index + 1 >= source.length) null else source[index + 1]

    private fun advance(): Char {
        val c = source[index++]
        if (c == '\n') {
            line++
            col = 1
        } else {
            col++
        }
        return c
    }

    private fun makeSpan(startLine: Int, startCol: Int): Span =
        Span(startLine, startCol, line, col)

    fun tokenize(): List<Token> {
        val tokens = mutableListOf<Token>()
        while (!isAtEnd()) {
            val c = peek()!!
            val startLine = line
            val startCol = col

            when {
                c == ' ' || c == '\t' || c == '\r' || c == '\n' -> {
                    advance()
                }
                c == '!' -> { // 注释到行尾
                    while (peek() != null && peek() != '\n') advance()
                }
                c.isLetter() || c == '_' -> {
                    val ident = readIdentifier()
                    val upper = ident.uppercase()
                    val type = when (upper) {
                        "MODULE" -> TokenType.MODULE
                        "ENDMODULE" -> TokenType.ENDMODULE
                        "PROC" -> TokenType.PROC
                        "ENDPROC" -> TokenType.ENDPROC
                        "FUNC" -> TokenType.FUNC
                        "ENDFUNC" -> TokenType.ENDFUNC
                        "VAR" -> TokenType.VAR
                        "PERS" -> TokenType.PERS
                        "CONST" -> TokenType.CONST
                        "IF" -> TokenType.IF
                        "THEN" -> TokenType.THEN
                        "ELSEIF" -> TokenType.ELSEIF
                        "ELSE" -> TokenType.ELSE
                        "ENDIF" -> TokenType.ENDIF
                        "FOR" -> TokenType.FOR
                        "FROM" -> TokenType.FROM
                        "TO" -> TokenType.TO
                        "ENDFOR" -> TokenType.ENDFOR
                        "WHILE" -> TokenType.WHILE
                        "ENDWHILE" -> TokenType.ENDWHILE
                        "RETURN" -> TokenType.RETURN
                        "TRUE" -> TokenType.TRUE
                        "FALSE" -> TokenType.FALSE
                        "MOVEJ" -> TokenType.MOVEJ
                        "MOVEL" -> TokenType.MOVEL
                        "MOVEC" -> TokenType.MOVEC
                        "AND" -> TokenType.AND
                        "OR" -> TokenType.OR
                        "NOT" -> TokenType.NOT
                        "TEST" -> TokenType.TEST
                        "CASE" -> TokenType.CASE
                        "DEFAULT" -> TokenType.DEFAULT
                        "ENDTEST" -> TokenType.ENDTEST
                        "TRAP" -> TokenType.TRAP
                        "ENDTRAP" -> TokenType.ENDTRAP
                        "CONNECT" -> TokenType.CONNECT
                        "WITH" -> TokenType.WITH
                        "RAISE" -> TokenType.RAISE
                        "RECORD" -> TokenType.RECORD
                        else -> TokenType.IDENT
                    }
                    tokens.add(Token(type, ident, makeSpan(startLine, startCol)))
                }
                c.isDigit() -> {
                    val number = readNumber()
                    tokens.add(Token(TokenType.NUMBER, number, makeSpan(startLine, startCol)))
                }
                c == '"' -> {
                    val str = readString(startLine, startCol)
                    tokens.add(Token(TokenType.STRING, str, makeSpan(startLine, startCol)))
                }
                else -> {
                    when (c) {
                        '(' -> {
                            advance(); tokens.add(Token(TokenType.LPAREN, "(", makeSpan(startLine, startCol)))
                        }
                        ')' -> {
                            advance(); tokens.add(Token(TokenType.RPAREN, ")", makeSpan(startLine, startCol)))
                        }
                        '[' -> {
                            advance(); tokens.add(Token(TokenType.LBRACKET, "[", makeSpan(startLine, startCol)))
                        }
                        ']' -> {
                            advance(); tokens.add(Token(TokenType.RBRACKET, "]", makeSpan(startLine, startCol)))
                        }
                        ',' -> {
                            advance(); tokens.add(Token(TokenType.COMMA, ",", makeSpan(startLine, startCol)))
                        }
                        ':' -> {
                            advance()
                            if (peek() == '=') {
                                advance()
                                tokens.add(Token(TokenType.ASSIGN, ":=", makeSpan(startLine, startCol)))
                            } else {
                                tokens.add(Token(TokenType.COLON, ":", makeSpan(startLine, startCol)))
                            }
                        }
                        '+' -> {
                            advance(); tokens.add(Token(TokenType.PLUS, "+", makeSpan(startLine, startCol)))
                        }
                        '-' -> {
                            advance(); tokens.add(Token(TokenType.MINUS, "-", makeSpan(startLine, startCol)))
                        }
                        '*' -> {
                            advance(); tokens.add(Token(TokenType.STAR, "*", makeSpan(startLine, startCol)))
                        }
                        '/' -> {
                            advance(); tokens.add(Token(TokenType.SLASH, "/", makeSpan(startLine, startCol)))
                        }
                        '=' -> {
                            advance(); tokens.add(Token(TokenType.EQ, "=", makeSpan(startLine, startCol)))
                        }
                        '<' -> {
                            advance()
                            when (peek()) {
                                '>' -> { advance(); tokens.add(Token(TokenType.NEQ, "<>", makeSpan(startLine, startCol))) }
                                '=' -> { advance(); tokens.add(Token(TokenType.LE, "<=", makeSpan(startLine, startCol))) }
                                else -> tokens.add(Token(TokenType.LT, "<", makeSpan(startLine, startCol)))
                            }
                        }
                        '>' -> {
                            advance()
                            if (peek() == '=') {
                                advance(); tokens.add(Token(TokenType.GE, ">=", makeSpan(startLine, startCol)))
                            } else {
                                tokens.add(Token(TokenType.GT, ">", makeSpan(startLine, startCol)))
                            }
                        }
                        '\\' -> {
                            advance(); tokens.add(Token(TokenType.BACKSLASH, "\\", makeSpan(startLine, startCol)))
                        }
                        ';' -> {
                            advance(); tokens.add(Token(TokenType.SEMICOLON, ";", makeSpan(startLine, startCol)))
                        }
                        '.' -> {
                            advance(); tokens.add(Token(TokenType.DOT, ".", makeSpan(startLine, startCol)))
                        }
                        else -> {
                            advance()
                            diagnostics.add(
                                Diagnostic(
                                    "无法识别的字符: '$c'",
                                    makeSpan(startLine, startCol),
                                    Severity.ERROR
                                )
                            )
                        }
                    }
                }
            }
        }
        val eofSpan = Span(line, col, line, col)
        tokens.add(Token(TokenType.EOF, "", eofSpan))
        return tokens
    }

    private fun readIdentifier(): String {
        val sb = StringBuilder()
        while (true) {
            val c = peek()
            if (c != null && (c.isLetterOrDigit() || c == '_' || c == '.')) {
                sb.append(c)
                advance()
            } else break
        }
        return sb.toString()
    }

    private fun readNumber(): String {
        val sb = StringBuilder()
        while (peek()?.isDigit() == true) {
            sb.append(advance())
        }
        if (peek() == '.' && peekNext()?.isDigit() == true) {
            sb.append(advance())
            while (peek()?.isDigit() == true) {
                sb.append(advance())
            }
        }
        return sb.toString()
    }

    private fun readString(startLine: Int, startCol: Int): String {
        advance() // "
        val sb = StringBuilder()
        while (true) {
            val c = peek()
            if (c == null || c == '\n') {
                diagnostics.add(
                    Diagnostic(
                        "字符串未闭合",
                        makeSpan(startLine, startCol),
                        Severity.ERROR
                    )
                )
                break
            }
            if (c == '"') {
                advance()
                break
            }
            sb.append(advance())
        }
        return sb.toString()
    }
}

// ===================== 语法分析 =====================

class ParseException(msg: String) : RuntimeException(msg)

class Parser(private val tokens: List<Token>) {

    val diagnostics = mutableListOf<Diagnostic>()
    private var pos = 0

    private fun peek(): Token = tokens[pos]
    private fun isAtEnd(): Boolean = peek().type == TokenType.EOF
    private fun advance(): Token = tokens[pos++]
    private fun match(vararg types: TokenType): Boolean {
        if (types.any { it == peek().type }) {
            advance()
            return true
        }
        return false
    }

    private fun expect(type: TokenType, msg: String): Token {
        if (peek().type == type) return advance()
        val actual = peek()
        val d = Diagnostic(
            "$msg，实际看到: ${actual.type} '${actual.lexeme}'",
            actual.span,
            Severity.ERROR
        )
        diagnostics.add(d)
        throw ParseException(d.message)
    }

    private fun mergeSpan(a: Span, b: Span): Span =
        Span(a.startLine, a.startCol, b.endLine, b.endCol)

    fun parseProgram(): Program {
        val modules = mutableListOf<ModuleNode>()
        val startSpan = if (!isAtEnd()) peek().span else Span(1, 1, 1, 1)
        while (!isAtEnd()) {
            modules.add(parseModule())
        }
        val endSpan = if (modules.isNotEmpty()) modules.last().span else startSpan
        return Program(modules, Span(startSpan.startLine, startSpan.startCol, endSpan.endLine, endSpan.endCol))
    }

    private fun parseModule(): ModuleNode {
        val modTok = expect(TokenType.MODULE, "期望 MODULE")
        val nameTok = expect(TokenType.IDENT, "MODULE 后应为模块名")
        val decls = mutableListOf<DeclNode>()
        while (!isAtEnd() && peek().type != TokenType.ENDMODULE) {
            decls.add(parseDecl())
        }
        val endTok = expect(TokenType.ENDMODULE, "缺少 ENDMODULE")
        return ModuleNode(nameTok.lexeme, decls, mergeSpan(modTok.span, endTok.span))
    }

    private fun parseDecl(): DeclNode {
        return when (peek().type) {
            TokenType.VAR, TokenType.PERS, TokenType.CONST -> parseVarDecl()
            TokenType.PROC -> parseProcDecl()
            TokenType.FUNC -> parseFuncDecl()
            TokenType.RECORD -> parseRecordDecl()
            TokenType.TRAP -> parseTrapDecl()
            else -> {
                val t = peek()
                diagnostics.add(Diagnostic("不期望的声明起始: ${t.type}", t.span, Severity.ERROR))
                throw ParseException("unexpected decl")
            }
        }
    }

    private fun parseVarDecl(): VarDecl {
        val storageTok = advance()
        val storage = when (storageTok.type) {
            TokenType.VAR -> StorageClass.VAR
            TokenType.PERS -> StorageClass.PERS
            TokenType.CONST -> StorageClass.CONST
            else -> throw ParseException("storage")
        }
        val typeTok = expect(TokenType.IDENT, "变量声明需要类型")
        val nameTok = expect(TokenType.IDENT, "变量声明需要名字")
        var initExpr: ExprNode? = null
        if (match(TokenType.ASSIGN)) {
            initExpr = parseExpression()
        }
        val semi = expect(TokenType.SEMICOLON, "变量声明需要以 ';' 结束")
        return VarDecl(
            storage,
            typeTok.lexeme,
            nameTok.lexeme,
            initExpr,
            mergeSpan(storageTok.span, semi.span)
        )
    }

    private fun parseRecordDecl(): RecordDecl {
        val recTok = expect(TokenType.RECORD, "期望 RECORD")
        val nameTok = expect(TokenType.IDENT, "RECORD 后需要名称")
        expect(TokenType.LPAREN, "RECORD 需要 '('")
        val fields = mutableListOf<RecordField>()
        if (!match(TokenType.RPAREN)) {
            while (true) {
                val typeTok = expect(TokenType.IDENT, "RECORD 字段需要类型")
                val fieldTok = expect(TokenType.IDENT, "RECORD 字段需要名字")
                fields.add(RecordField(typeTok.lexeme, fieldTok.lexeme, mergeSpan(typeTok.span, fieldTok.span)))
                if (match(TokenType.COMMA)) continue
                break
            }
            expect(TokenType.RPAREN, "RECORD 缺少 ')'")
        }
        val semi = expect(TokenType.SEMICOLON, "RECORD 声明缺少 ';'")
        return RecordDecl(nameTok.lexeme, fields, mergeSpan(recTok.span, semi.span))
    }

    private fun parseProcDecl(): ProcDecl {
        val procTok = expect(TokenType.PROC, "期望 PROC")
        val nameTok = expect(TokenType.IDENT, "PROC 后需要名字")
        val params = parseParamList()
        val body = mutableListOf<StmtNode>()
        while (!isAtEnd() && peek().type != TokenType.ENDPROC) {
            body.add(parseStatement())
        }
        val endTok = expect(TokenType.ENDPROC, "缺少 ENDPROC")
        return ProcDecl(nameTok.lexeme, params, body, mergeSpan(procTok.span, endTok.span))
    }

    private fun parseFuncDecl(): FuncDecl {
        val funcTok = expect(TokenType.FUNC, "期望 FUNC")
        val retTypeTok = expect(TokenType.IDENT, "FUNC 后需要返回类型")
        val nameTok = expect(TokenType.IDENT, "FUNC 需要名字")
        val params = parseParamList()
        val body = mutableListOf<StmtNode>()
        while (!isAtEnd() && peek().type != TokenType.ENDFUNC) {
            body.add(parseStatement())
        }
        val endTok = expect(TokenType.ENDFUNC, "缺少 ENDFUNC")
        return FuncDecl(retTypeTok.lexeme, nameTok.lexeme, params, body, mergeSpan(funcTok.span, endTok.span))
    }

    private fun parseTrapDecl(): TrapDecl {
        val trapTok = expect(TokenType.TRAP, "期望 TRAP")
        val nameTok = expect(TokenType.IDENT, "TRAP 需要名字")
        val body = mutableListOf<StmtNode>()
        while (!isAtEnd() && peek().type != TokenType.ENDTRAP) {
            body.add(parseStatement())
        }
        val endTok = expect(TokenType.ENDTRAP, "缺少 ENDTRAP")
        return TrapDecl(nameTok.lexeme, body, mergeSpan(trapTok.span, endTok.span))
    }

    private fun parseParamList(): List<Param> {
        val params = mutableListOf<Param>()
        if (!match(TokenType.LPAREN)) return params
        if (peek().type == TokenType.RPAREN) {
            advance()
            return params
        }
        while (true) {
            val typeTok = expect(TokenType.IDENT, "参数需要类型")
            val nameTok = expect(TokenType.IDENT, "参数需要名称")
            val span = mergeSpan(typeTok.span, nameTok.span)
            params.add(Param(typeTok.lexeme, nameTok.lexeme, false, span))
            if (match(TokenType.COMMA)) continue
            break
        }
        expect(TokenType.RPAREN, "参数列表缺少 ')'")
        return params
    }

    private fun parseStatement(): StmtNode {
        return when (peek().type) {
            TokenType.IF -> parseIfStmt()
            TokenType.WHILE -> parseWhileStmt()
            TokenType.FOR -> parseForStmt()
            TokenType.RETURN -> parseReturnStmt()
            TokenType.MOVEJ, TokenType.MOVEL, TokenType.MOVEC -> parseMoveStmt()
            TokenType.TEST -> parseTestStmt()
            TokenType.CONNECT -> parseConnectStmt()
            TokenType.RAISE -> parseRaiseStmt()
            else -> parseSimpleStmt()
        }
    }

    private fun parseIfStmt(): IfStmt {
        val ifTok = expect(TokenType.IF, "期望 IF")
        val cond = parseExpression()
        expect(TokenType.THEN, "IF 条件后需要 THEN")
        val branches = mutableListOf<IfBranch>()
        val ifBody = parseStmtBlockUntil(setOf(TokenType.ELSEIF, TokenType.ELSE, TokenType.ENDIF))
        branches.add(IfBranch(cond, ifBody, mergeSpan(ifTok.span, ifBody.lastOrNull()?.span ?: cond.span)))
        while (match(TokenType.ELSEIF)) {
            val elseifStart = tokens[pos - 1]
            val c = parseExpression()
            expect(TokenType.THEN, "ELSEIF 条件后需要 THEN")
            val body = parseStmtBlockUntil(setOf(TokenType.ELSEIF, TokenType.ELSE, TokenType.ENDIF))
            branches.add(IfBranch(c, body, mergeSpan(elseifStart.span, body.lastOrNull()?.span ?: c.span)))
        }
        var elseBody: List<StmtNode>? = null
        if (match(TokenType.ELSE)) {
            elseBody = parseStmtBlockUntil(setOf(TokenType.ENDIF))
        }
        val endTok = expect(TokenType.ENDIF, "IF 需要 ENDIF 结束")
        val allSpan = mergeSpan(ifTok.span, endTok.span)
        return IfStmt(branches, elseBody, allSpan)
    }

    private fun parseStmtBlockUntil(endTokens: Set<TokenType>): List<StmtNode> {
        val stmts = mutableListOf<StmtNode>()
        while (!isAtEnd() && !endTokens.contains(peek().type)) {
            stmts.add(parseStatement())
        }
        return stmts
    }

    private fun parseWhileStmt(): WhileStmt {
        val wTok = expect(TokenType.WHILE, "期望 WHILE")
        val cond = parseExpression()
        val body = mutableListOf<StmtNode>()
        while (!isAtEnd() && peek().type != TokenType.ENDWHILE) {
            body.add(parseStatement())
        }
        val endTok = expect(TokenType.ENDWHILE, "WHILE 需要 ENDWHILE")
        return WhileStmt(cond, body, mergeSpan(wTok.span, endTok.span))
    }

    private fun parseForStmt(): ForStmt {
        val fTok = expect(TokenType.FOR, "期望 FOR")
        val varTok = expect(TokenType.IDENT, "FOR 需要循环变量")
        expect(TokenType.FROM, "FOR 需要 FROM")
        val fromExpr = parseExpression()
        expect(TokenType.TO, "FOR 需要 TO")
        val toExpr = parseExpression()
        val body = mutableListOf<StmtNode>()
        while (!isAtEnd() && peek().type != TokenType.ENDFOR) {
            body.add(parseStatement())
        }
        val endTok = expect(TokenType.ENDFOR, "FOR 需要 ENDFOR")
        return ForStmt(varTok.lexeme, fromExpr, toExpr, body, mergeSpan(fTok.span, endTok.span))
    }

    private fun parseReturnStmt(): ReturnStmt {
        val rTok = expect(TokenType.RETURN, "期望 RETURN")
        val nextType = peek().type
        val expr = if (nextType != TokenType.SEMICOLON && nextType != TokenType.ENDPROC && nextType != TokenType.ENDFUNC) {
            parseExpression()
        } else null
        val semi = expect(TokenType.SEMICOLON, "RETURN 需要 ';' 结束")
        return ReturnStmt(expr, mergeSpan(rTok.span, semi.span))
    }

    private fun parseMoveStmt(): MoveStmt {
        val moveTok = advance()
        val kind = when (moveTok.type) {
            TokenType.MOVEJ -> MoveKind.MoveJ
            TokenType.MOVEL -> MoveKind.MoveL
            TokenType.MOVEC -> MoveKind.MoveC
            else -> throw ParseException("move kind")
        }
        val target = parseExpression()
        expect(TokenType.COMMA, "Move 指令需要 ',' 分隔参数")
        val speed = parseExpression()
        expect(TokenType.COMMA, "Move 指令需要 ',' 分隔参数")
        val zone = parseExpression()
        expect(TokenType.COMMA, "Move 指令需要 ',' 分隔参数")
        val tool = parseExpression()

        var wobjExpr: ExprNode? = null
        if (match(TokenType.BACKSLASH)) {
            val id = expect(TokenType.IDENT, "Move 扩展参数需要名字，例如 \\WObj")
            if (id.lexeme.equals("WObj", ignoreCase = true)) {
                expect(TokenType.ASSIGN, "WObj 后需要 :=")
                wobjExpr = parseExpression()
            } else {
                parseExpression()
            }
        }
        val semi = expect(TokenType.SEMICOLON, "Move 指令需要 ';' 结束")
        return MoveStmt(kind, target, speed, zone, tool, wobjExpr, mergeSpan(moveTok.span, semi.span))
    }

    private fun parseTestStmt(): TestStmt {
        val testTok = expect(TokenType.TEST, "期望 TEST")
        val testExpr = parseExpression()
        val cases = mutableListOf<TestCase>()
        var defaultBody: List<StmtNode>? = null

        while (true) {
            when {
                match(TokenType.CASE) -> {
                    val caseStart = tokens[pos - 1]
                    val values = mutableListOf<ExprNode>()
                    while (true) {
                        values.add(parseExpression())
                        if (match(TokenType.COMMA)) continue
                        break
                    }
                    expect(TokenType.COLON, "CASE 后缺少 ':'")
                    val body = parseStmtBlockUntil(setOf(TokenType.CASE, TokenType.DEFAULT, TokenType.ENDTEST))
                    cases.add(TestCase(values, body, mergeSpan(caseStart.span, body.lastOrNull()?.span ?: caseStart.span)))
                }
                match(TokenType.DEFAULT) -> {
                    expect(TokenType.COLON, "DEFAULT 后需要 ':'")
                    defaultBody = parseStmtBlockUntil(setOf(TokenType.ENDTEST))
                }
                else -> break
            }
        }

        val endTok = expect(TokenType.ENDTEST, "TEST 缺少 ENDTEST")
        return TestStmt(testExpr, cases, defaultBody, mergeSpan(testTok.span, endTok.span))
    }

    private fun parseConnectStmt(): ConnectStmt {
        val cTok = expect(TokenType.CONNECT, "期望 CONNECT")
        val trap = expect(TokenType.IDENT, "CONNECT 需要 trap 名").lexeme
        expect(TokenType.WITH, "CONNECT 需要 WITH")
        val err = expect(TokenType.IDENT, "WITH 需要错误名").lexeme
        val semi = expect(TokenType.SEMICOLON, "CONNECT 需要 ';'")
        return ConnectStmt(trap, err, mergeSpan(cTok.span, semi.span))
    }

    private fun parseRaiseStmt(): RaiseStmt {
        val rTok = expect(TokenType.RAISE, "期望 RAISE")
        val name = expect(TokenType.IDENT, "RAISE 后需要错误名")
        val semi = expect(TokenType.SEMICOLON, "RAISE 需要 ';'")
        return RaiseStmt(name.lexeme, mergeSpan(rTok.span, semi.span))
    }

    private fun parseSimpleStmt(): StmtNode {
        val expr = parseExpression()
        return if (match(TokenType.ASSIGN)) {
            val value = parseExpression()
            val semi = expect(TokenType.SEMICOLON, "赋值语句需要 ';' 结束")
            AssignStmt(expr, value, mergeSpan(expr.span, semi.span))
        } else {
            val semi = expect(TokenType.SEMICOLON, "语句需要 ';' 结束")
            ExprStmt(expr, mergeSpan(expr.span, semi.span))
        }
    }

    // ========= 表达式 =========

    private fun parseExpression(): ExprNode = parseOr()

    private fun parseOr(): ExprNode {
        var expr = parseAnd()
        while (match(TokenType.OR)) {
            val right = parseAnd()
            expr = BinaryExpr(expr, BinaryOp.OR, right, mergeSpan(expr.span, right.span))
        }
        return expr
    }

    private fun parseAnd(): ExprNode {
        var expr = parseEquality()
        while (match(TokenType.AND)) {
            val right = parseEquality()
            expr = BinaryExpr(expr, BinaryOp.AND, right, mergeSpan(expr.span, right.span))
        }
        return expr
    }

    private fun parseEquality(): ExprNode {
        var expr = parseComparison()
        while (true) {
            expr = when {
                match(TokenType.EQ) -> {
                    val right = parseComparison()
                    BinaryExpr(expr, BinaryOp.EQ, right, mergeSpan(expr.span, right.span))
                }
                match(TokenType.NEQ) -> {
                    val right = parseComparison()
                    BinaryExpr(expr, BinaryOp.NEQ, right, mergeSpan(expr.span, right.span))
                }
                else -> return expr
            }
        }
    }

    private fun parseComparison(): ExprNode {
        var expr = parseTerm()
        while (true) {
            expr = when {
                match(TokenType.LT) -> {
                    val right = parseTerm()
                    BinaryExpr(expr, BinaryOp.LT, right, mergeSpan(expr.span, right.span))
                }
                match(TokenType.GT) -> {
                    val right = parseTerm()
                    BinaryExpr(expr, BinaryOp.GT, right, mergeSpan(expr.span, right.span))
                }
                match(TokenType.LE) -> {
                    val right = parseTerm()
                    BinaryExpr(expr, BinaryOp.LE, right, mergeSpan(expr.span, right.span))
                }
                match(TokenType.GE) -> {
                    val right = parseTerm()
                    BinaryExpr(expr, BinaryOp.GE, right, mergeSpan(expr.span, right.span))
                }
                else -> return expr
            }
        }
    }

    private fun parseTerm(): ExprNode {
        var expr = parseFactor()
        while (true) {
            expr = when {
                match(TokenType.PLUS) -> {
                    val right = parseFactor()
                    BinaryExpr(expr, BinaryOp.ADD, right, mergeSpan(expr.span, right.span))
                }
                match(TokenType.MINUS) -> {
                    val right = parseFactor()
                    BinaryExpr(expr, BinaryOp.SUB, right, mergeSpan(expr.span, right.span))
                }
                else -> return expr
            }
        }
    }

    private fun parseFactor(): ExprNode {
        var expr = parseUnary()
        while (true) {
            expr = when {
                match(TokenType.STAR) -> {
                    val right = parseUnary()
                    BinaryExpr(expr, BinaryOp.MUL, right, mergeSpan(expr.span, right.span))
                }
                match(TokenType.SLASH) -> {
                    val right = parseUnary()
                    BinaryExpr(expr, BinaryOp.DIV, right, mergeSpan(expr.span, right.span))
                }
                else -> return expr
            }
        }
    }

    private fun parseUnary(): ExprNode {
        return when {
            match(TokenType.NOT) -> {
                val opTok = tokens[pos - 1]
                val expr = parseUnary()
                UnaryExpr(UnaryOp.NOT, expr, mergeSpan(opTok.span, expr.span))
            }
            match(TokenType.MINUS) -> {
                val opTok = tokens[pos - 1]
                val expr = parseUnary()
                UnaryExpr(UnaryOp.MINUS, expr, mergeSpan(opTok.span, expr.span))
            }
            match(TokenType.PLUS) -> {
                val opTok = tokens[pos - 1]
                val expr = parseUnary()
                UnaryExpr(UnaryOp.PLUS, expr, mergeSpan(opTok.span, expr.span))
            }
            else -> parsePrimary()
        }
    }

    private fun parsePrimary(): ExprNode {
        val tok = peek()
        return when (tok.type) {
            TokenType.NUMBER -> {
                advance()
                NumLiteral(tok.lexeme.toDoubleOrNull() ?: 0.0, tok.span)
            }
            TokenType.TRUE -> {
                advance()
                BoolLiteral(true, tok.span)
            }
            TokenType.FALSE -> {
                advance()
                BoolLiteral(false, tok.span)
            }
            TokenType.STRING -> {
                advance()
                StringLiteral(tok.lexeme, tok.span)
            }
            TokenType.IDENT -> parseIdentChain()
            TokenType.LPAREN -> {
                advance()
                val e = parseExpression()
                expect(TokenType.RPAREN, "缺少 ')'")
                e
            }
            else -> {
                diagnostics.add(Diagnostic("不期望的表达式起始: ${tok.type}", tok.span, Severity.ERROR))
                throw ParseException("expr")
            }
        }
    }

    private fun parseIdentChain(): ExprNode {
        var expr: ExprNode = VarRef(peek().lexeme, peek().span)
        advance()
        loop@ while (true) {
            when {
                match(TokenType.LPAREN) -> {
                    val name = (expr as? VarRef)?.name
                        ?: throw ParseException("call on non-ident")
                    val args = mutableListOf<ExprNode>()
                    if (peek().type != TokenType.RPAREN) {
                        while (true) {
                            args.add(parseExpression())
                            if (match(TokenType.COMMA)) continue
                            break
                        }
                    }
                    val rParen = expect(TokenType.RPAREN, "调用需要 ')'")
                    expr = CallExpr(name, args, mergeSpan(expr.span, rParen.span))
                }
                match(TokenType.LBRACKET) -> {
                    val indexExpr = parseExpression()
                    val r = expect(TokenType.RBRACKET, "数组访问缺少 ']'")
                    expr = ArrayAccess(expr, indexExpr, mergeSpan(expr.span, r.span))
                }
                match(TokenType.DOT) -> {
                    val fieldTok = expect(TokenType.IDENT, "点号后需要字段名")
                    expr = DotAccess(expr, fieldTok.lexeme, mergeSpan(expr.span, fieldTok.span))
                }
                else -> break@loop
            }
        }
        return expr
    }
}

// ===================== 语义分析 =====================

data class Symbol(
    val name: String,
    val type: String?,
    val isConst: Boolean,
    val decl: AstNode
)

class Scope(val parent: Scope? = null) {
    private val symbols = mutableMapOf<String, Symbol>()

    fun define(sym: Symbol): Boolean {
        if (symbols.containsKey(sym.name)) return false
        symbols[sym.name] = sym
        return true
    }

    fun resolve(name: String): Symbol? =
        symbols[name] ?: parent?.resolve(name)
}

class SemanticAnalyzer {

    private val diagnostics = mutableListOf<Diagnostic>()

    private val builtinTypes = setOf(
        "num","dnum","bool","string",
        "robtarget","jointtarget","tooldata","wobjdata",
        "speeddata","zonedata","confdata","orient","pose"
    )

    fun analyze(program: Program): List<Diagnostic> {
        val global = Scope()
        program.modules.forEach { analyzeModule(it, global) }
        return diagnostics
    }

    private fun report(msg: String, span: Span, severity: Severity = Severity.ERROR) {
        diagnostics.add(Diagnostic(msg, span, severity))
    }

    private fun analyzeModule(module: ModuleNode, parent: Scope) {
        val scope = Scope(parent)
        module.declarations.forEach { decl ->
            when (decl) {
                is VarDecl -> {
                    if (!builtinTypes.contains(decl.typeName.lowercase()) &&
                        scope.resolve(decl.typeName) == null) {
                        report("未知类型: ${decl.typeName}", decl.span)
                    }
                    val ok = scope.define(
                        Symbol(
                            decl.name,
                            decl.typeName,
                            decl.storage == StorageClass.CONST,
                            decl
                        )
                    )
                    if (!ok) report("重复定义变量: ${decl.name}", decl.span)
                }
                is ProcDecl -> {
                    val ok = scope.define(Symbol(decl.name, null, false, decl))
                    if (!ok) report("重复定义 PROC: ${decl.name}", decl.span)
                }
                is FuncDecl -> {
                    val ok = scope.define(Symbol(decl.name, decl.returnType, false, decl))
                    if (!ok) report("重复定义 FUNC: ${decl.name}", decl.span)
                }
                is RecordDecl -> {
                    val ok = scope.define(Symbol(decl.name, "record", false, decl))
                    if (!ok) report("重复定义 RECORD: ${decl.name}", decl.span)
                }
                is TrapDecl -> {
                    val ok = scope.define(Symbol(decl.name, null, false, decl))
                    if (!ok) report("重复定义 TRAP: ${decl.name}", decl.span)
                }
            }
        }

        module.declarations.forEach {
            when (it) {
                is VarDecl -> it.initExpr?.let { e -> analyzeExpr(e, scope) }
                is ProcDecl -> analyzeProc(it, scope)
                is FuncDecl -> analyzeFunc(it, scope)
                is RecordDecl -> {}
                is TrapDecl -> analyzeTrap(it, scope)
            }
        }
    }

    private fun analyzeProc(proc: ProcDecl, parent: Scope) {
        val scope = Scope(parent)
        proc.params.forEach { p ->
            scope.define(Symbol(p.name, p.typeName, false, proc))
        }
        proc.body.forEach { analyzeStmt(it, scope, inFunc = false, returnType = null) }
    }

    private fun analyzeFunc(func: FuncDecl, parent: Scope) {
        val scope = Scope(parent)
        func.params.forEach { p ->
            scope.define(Symbol(p.name, p.typeName, false, func))
        }
        var hasReturn = false
        func.body.forEach { stmt ->
            if (stmt is ReturnStmt) hasReturn = true
            analyzeStmt(stmt, scope, inFunc = true, returnType = func.returnType)
        }
        if (!hasReturn) {
            report("FUNC ${func.name} 可能缺少 RETURN", func.span, Severity.WARNING)
        }
    }

    private fun analyzeTrap(trap: TrapDecl, parent: Scope) {
        val scope = Scope(parent)
        trap.body.forEach { analyzeStmt(it, scope, inFunc = false, returnType = null) }
    }

    private fun analyzeStmt(stmt: StmtNode, scope: Scope, inFunc: Boolean, returnType: String?) {
        when (stmt) {
            is BlockStmt -> stmt.statements.forEach { analyzeStmt(it, Scope(scope), inFunc, returnType) }
            is AssignStmt -> {
                val leftType = analyzeExpr(stmt.target, scope)
                val rightType = analyzeExpr(stmt.value, scope)
                if (leftType != null && rightType != null && leftType != rightType) {
                    report("赋值类型不匹配: $leftType <- $rightType", stmt.span)
                }
            }
            is ExprStmt -> {
                analyzeExpr(stmt.expr, scope)
            }
            is IfStmt -> {
                stmt.branches.forEach { br ->
                    val ct = analyzeExpr(br.condition, scope)
                    if (ct != null && ct != "bool") {
                        report("IF 条件应为 bool，实际为 $ct", br.spanBranch)
                    }
                    val inner = Scope(scope)
                    br.body.forEach { analyzeStmt(it, inner, inFunc, returnType) }
                }
                stmt.elseBranch?.let {
                    val inner = Scope(scope)
                    it.forEach { s -> analyzeStmt(s, inner, inFunc, returnType) }
                }
            }
            is WhileStmt -> {
                val ct = analyzeExpr(stmt.condition, scope)
                if (ct != null && ct != "bool") {
                    report("WHILE 条件应为 bool，实际为 $ct", stmt.span)
                }
                val inner = Scope(scope)
                stmt.body.forEach { analyzeStmt(it, inner, inFunc, returnType) }
            }
            is ForStmt -> {
                val inner = Scope(scope)
                inner.define(Symbol(stmt.loopVar, "num", false, stmt))
                analyzeExpr(stmt.fromExpr, inner)
                analyzeExpr(stmt.toExpr, inner)
                stmt.body.forEach { analyzeStmt(it, inner, inFunc, returnType) }
            }
            is ReturnStmt -> {
                if (!inFunc && stmt.expr != null) {
                    report("PROC 中 RETURN 不能返回值", stmt.span)
                }
                if (inFunc && returnType != null) {
                    val t = stmt.expr?.let { analyzeExpr(it, scope) }
                    if (t != null && t != returnType) {
                        report("函数返回类型不匹配: 期望 $returnType，实际 $t", stmt.span)
                    }
                }
            }
            is MoveStmt -> {
                analyzeExpr(stmt.target, scope)
                analyzeExpr(stmt.speed, scope)
                analyzeExpr(stmt.zone, scope)
                analyzeExpr(stmt.tool, scope)
                stmt.wobj?.let { analyzeExpr(it, scope) }
            }
            is TestStmt -> {
                val et = analyzeExpr(stmt.expr, scope)
                stmt.cases.forEach { c ->
                    c.values.forEach { v ->
                        val vt = analyzeExpr(v, scope)
                        if (et != null && vt != null && et != vt) {
                            report("TEST CASE 值类型与 TEST 表达式不一致: $et vs $vt", c.span)
                        }
                    }
                    val inner = Scope(scope)
                    c.body.forEach { analyzeStmt(it, inner, inFunc, returnType) }
                }
                stmt.defaultBody?.let {
                    val inner = Scope(scope)
                    it.forEach { s -> analyzeStmt(s, inner, inFunc, returnType) }
                }
            }
            is ConnectStmt -> {
                val trapSym = scope.resolve(stmt.trapName)
                if (trapSym == null) {
                    report("CONNECT 使用了未定义的 TRAP: ${stmt.trapName}", stmt.span)
                }
            }
            is RaiseStmt -> {
                // 错误名这里不做类型检查
            }
        }
    }

    private fun analyzeExpr(expr: ExprNode, scope: Scope): String? {
        return when (expr) {
            is NumLiteral -> "num"
            is BoolLiteral -> "bool"
            is StringLiteral -> "string"
            is VarRef -> {
                val sym = scope.resolve(expr.name)
                if (sym == null) {
                    report("使用未定义变量: ${expr.name}", expr.span)
                    null
                } else sym.type
            }
            is ArrayAccess -> {
                analyzeExpr(expr.base, scope)
                analyzeExpr(expr.index, scope)
                null
            }
            is FieldAccess -> {
                analyzeExpr(expr.base, scope)
                null
            }
            is DotAccess -> {
                val baseType = analyzeExpr(expr.base, scope)
                val decl = scope.resolve(baseType ?: "")?.decl
                if (decl !is RecordDecl) {
                    report("类型 $baseType 不是 RECORD，不能点访问", expr.span)
                    null
                } else {
                    val field = decl.fields.find { it.fieldName == expr.field }
                    if (field == null) {
                        report("RECORD ${decl.name} 中不存在字段 ${expr.field}", expr.span)
                        null
                    } else field.typeName
                }
            }
            is CallExpr -> {
                val sym = scope.resolve(expr.name)
                if (sym == null) {
                    report("调用未定义过程/函数: ${expr.name}", expr.span)
                    null
                } else {
                    expr.args.forEach { analyzeExpr(it, scope) }
                    sym.type
                }
            }
            is UnaryExpr -> analyzeExpr(expr.expr, scope)
            is BinaryExpr -> {
                val lt = analyzeExpr(expr.left, scope)
                val rt = analyzeExpr(expr.right, scope)
                when (expr.op) {
                    BinaryOp.ADD, BinaryOp.SUB, BinaryOp.MUL, BinaryOp.DIV -> "num"
                    BinaryOp.AND, BinaryOp.OR -> "bool"
                    BinaryOp.EQ, BinaryOp.NEQ, BinaryOp.LT, BinaryOp.GT, BinaryOp.LE, BinaryOp.GE -> "bool"
                }
            }
        }
    }
}

// ===================== 对外统一入口 =====================

object RapidCompiler {

    fun analyze(source: String): RapidAnalyzeResult {
        val lexer = Lexer(source)
        val tokens = lexer.tokenize()
        val lexDiags = lexer.diagnostics()
        val parser = Parser(tokens)
        val program = try {
            parser.parseProgram()
        } catch (e: ParseException) {
            null
        }
        val parseDiags = parser.diagnostics
        val semaDiags = if (program != null) {
            SemanticAnalyzer().analyze(program)
        } else emptyList()

        val all = (lexDiags + parseDiags + semaDiags)
            .sortedWith(compareBy({ it.span.startLine }, { it.span.startCol }))

        return RapidAnalyzeResult(all, program)
    }
}
