package com.omocv.abb

import java.io.File

/**
 * Parser for ABB RAPID programming language files
 * Supports .mod (module), .prg (program), .sys (system) files
 */
class ABBParser {

    companion object {
        // ABB RAPID keywords
        val KEYWORDS = setOf(
            "MODULE", "ENDMODULE", "PROC", "ENDPROC", "FUNC", "ENDFUNC", "TRAP", "ENDTRAP",
            "VAR", "PERS", "CONST", "ALIAS", "LOCAL", "TASK",
            "IF", "THEN", "ELSEIF", "ELSE", "ENDIF",
            "FOR", "FROM", "TO", "STEP", "DO", "ENDFOR",
            "WHILE", "DO", "ENDWHILE",
            "TEST", "CASE", "DEFAULT", "ENDTEST",
            "GOTO", "LABEL", "RETURN", "EXIT",
            "TRUE", "FALSE",
            "AND", "OR", "NOT", "XOR", "DIV", "MOD",
            "MoveJ", "MoveL", "MoveC", "MoveAbsJ",
            "WaitTime", "SetDO", "SetAO", "Reset",
            "TPWrite", "TPReadNum", "TPReadFK"
        )

        // Data types
        val DATA_TYPES = setOf(
            "num", "bool", "string", "pos", "orient", "pose", "confdata",
            "robtarget", "jointtarget", "speeddata", "zonedata", "tooldata", "wobjdata",
            "loaddata", "clock", "intnum"
        )
    }

    /**
     * Parse an ABB program file
     */
    fun parseFile(file: File): ABBProgramFile {
        val content = file.readText()
        val fileName = file.name
        val fileType = file.extension

        val modules = parseModules(content)
        val routines = parseRoutines(content)

        return ABBProgramFile(
            fileName = fileName,
            fileType = fileType,
            content = content,
            modules = modules,
            routines = routines
        )
    }

    /**
     * Parse modules from content
     */
    private fun parseModules(content: String): List<ABBModule> {
        val modules = mutableListOf<ABBModule>()
        val lines = content.lines()
        
        var currentModule: String? = null
        var moduleType = ""
        var moduleStartLine = 0
        var moduleRoutines = mutableListOf<ABBRoutine>()
        var moduleVariables = mutableListOf<String>()

        for ((index, line) in lines.withIndex()) {
            val trimmedLine = line.trim()
            
            // Check for MODULE declaration
            if (trimmedLine.startsWith("MODULE", ignoreCase = true)) {
                val parts = trimmedLine.split(Regex("\\s+"))
                if (parts.size >= 2) {
                    currentModule = parts[1].removeSuffix("(")
                    moduleType = if (trimmedLine.contains("(")) {
                        trimmedLine.substringAfter("(").substringBefore(")").trim()
                    } else {
                        "NOSTEPIN"
                    }
                    moduleStartLine = index
                    moduleRoutines.clear()
                    moduleVariables.clear()
                }
            }
            
            // Check for ENDMODULE
            else if (trimmedLine.startsWith("ENDMODULE", ignoreCase = true)) {
                if (currentModule != null) {
                    modules.add(
                        ABBModule(
                            name = currentModule,
                            type = moduleType,
                            routines = moduleRoutines.toList(),
                            variables = moduleVariables.toList(),
                            startLine = moduleStartLine,
                            endLine = index
                        )
                    )
                    currentModule = null
                }
            }
            
            // Check for variable declarations
            else if (currentModule != null && 
                    (trimmedLine.startsWith("VAR", ignoreCase = true) || 
                     trimmedLine.startsWith("PERS", ignoreCase = true) ||
                     trimmedLine.startsWith("CONST", ignoreCase = true))) {
                moduleVariables.add(trimmedLine)
            }
        }

        return modules
    }

    /**
     * Parse routines (PROC, FUNC, TRAP) from content
     */
    private fun parseRoutines(content: String): List<ABBRoutine> {
        val routines = mutableListOf<ABBRoutine>()
        val lines = content.lines()
        
        var currentRoutine: String? = null
        var routineType = ""
        var startLine = 0
        var parameters = mutableListOf<String>()
        var localVars = mutableListOf<String>()

        for ((index, line) in lines.withIndex()) {
            val trimmedLine = line.trim()
            
            // Check for PROC declaration
            if (trimmedLine.startsWith("PROC", ignoreCase = true)) {
                val parts = trimmedLine.split(Regex("[\\s()]+"))
                if (parts.size >= 2) {
                    currentRoutine = parts[1]
                    routineType = "PROC"
                    startLine = index
                    parameters.clear()
                    localVars.clear()
                    
                    // Parse parameters if present
                    if (trimmedLine.contains("(")) {
                        val paramStr = trimmedLine.substringAfter("(").substringBefore(")")
                        if (paramStr.isNotBlank()) {
                            parameters.addAll(paramStr.split(",").map { it.trim() })
                        }
                    }
                }
            }
            
            // Check for FUNC declaration
            else if (trimmedLine.startsWith("FUNC", ignoreCase = true)) {
                val parts = trimmedLine.split(Regex("[\\s()]+"))
                if (parts.size >= 3) {
                    currentRoutine = parts[2]
                    routineType = "FUNC"
                    startLine = index
                    parameters.clear()
                    localVars.clear()
                    
                    // Parse parameters if present
                    if (trimmedLine.contains("(")) {
                        val paramStr = trimmedLine.substringAfter("(").substringBefore(")")
                        if (paramStr.isNotBlank()) {
                            parameters.addAll(paramStr.split(",").map { it.trim() })
                        }
                    }
                }
            }
            
            // Check for TRAP declaration
            else if (trimmedLine.startsWith("TRAP", ignoreCase = true)) {
                val parts = trimmedLine.split(Regex("\\s+"))
                if (parts.size >= 2) {
                    currentRoutine = parts[1]
                    routineType = "TRAP"
                    startLine = index
                    parameters.clear()
                    localVars.clear()
                }
            }
            
            // Check for local variables in routine
            else if (currentRoutine != null && trimmedLine.startsWith("VAR", ignoreCase = true)) {
                localVars.add(trimmedLine)
            }
            
            // Check for ENDPROC, ENDFUNC, ENDTRAP
            else if (trimmedLine.startsWith("ENDPROC", ignoreCase = true) ||
                     trimmedLine.startsWith("ENDFUNC", ignoreCase = true) ||
                     trimmedLine.startsWith("ENDTRAP", ignoreCase = true)) {
                if (currentRoutine != null) {
                    routines.add(
                        ABBRoutine(
                            name = currentRoutine,
                            type = routineType,
                            parameters = parameters.toList(),
                            localVariables = localVars.toList(),
                            startLine = startLine,
                            endLine = index
                        )
                    )
                    currentRoutine = null
                }
            }
        }

        return routines
    }

    /**
     * Check if a file is a supported ABB file format
     */
    fun isSupportedFile(file: File): Boolean {
        val extension = file.extension.lowercase()
        return extension in setOf("mod", "prg", "sys")
    }
    
    /**
     * Comprehensive syntax validation for RAPID code with Chinese error messages
     */
    fun validateSyntax(content: String): List<SyntaxError> {
        val errors = mutableListOf<SyntaxError>()
        val lines = content.lines()
        
        var moduleCount = 0
        var endModuleCount = 0
        val blockStack = mutableListOf<BlockInfo>()
        
        lines.forEachIndexed { index, line ->
            val lineNumber = index + 1
            val trimmed = line.trim()
            
            // Skip empty lines and comments
            if (trimmed.isEmpty() || trimmed.startsWith("!")) {
                return@forEachIndexed
            }
            
            // First, check for unclosed strings (highest priority to avoid false positives)
            val stringQuoteCount = line.count { it == '"' }
            if (stringQuoteCount % 2 != 0) {
                // Find the position of the unclosed quote
                var lastQuotePos = -1
                var inString = false
                for (i in line.indices) {
                    if (line[i] == '"') {
                        lastQuotePos = i
                        inString = !inString
                    }
                }
                if (inString && lastQuotePos >= 0) {
                    errors.add(SyntaxError(
                        lineNumber,
                        "第 $lineNumber 行，第 ${lastQuotePos + 1} 列：字符串未闭合 - 缺少结束引号\n建议：在字符串末尾添加双引号 \"",
                        lastQuotePos,
                        line.length
                    ))
                    return@forEachIndexed // Skip further checks on this line
                }
            }
            
            // Check for unmatched parentheses, brackets, and braces (excluding comments and strings)
            val lineWithoutStringsAndComments = removeStringsAndComments(line)
            val parenCount = lineWithoutStringsAndComments.count { it == '(' } - lineWithoutStringsAndComments.count { it == ')' }
            val bracketCount = lineWithoutStringsAndComments.count { it == '[' } - lineWithoutStringsAndComments.count { it == ']' }
            val braceCount = lineWithoutStringsAndComments.count { it == '{' } - lineWithoutStringsAndComments.count { it == '}' }
            
            if (parenCount > 0) {
                val lastOpenParen = lineWithoutStringsAndComments.lastIndexOf('(')
                if (lastOpenParen >= 0) {
                    errors.add(SyntaxError(
                        lineNumber,
                        "第 $lineNumber 行，第 ${lastOpenParen + 1} 列：左括号未闭合 - 缺少匹配的右括号\n建议：在语句末尾或适当位置添加 )",
                        lastOpenParen,
                        lastOpenParen + 1
                    ))
                }
            } else if (parenCount < 0) {
                val firstCloseParen = lineWithoutStringsAndComments.indexOf(')')
                if (firstCloseParen >= 0) {
                    errors.add(SyntaxError(
                        lineNumber,
                        "第 $lineNumber 行，第 ${firstCloseParen + 1} 列：右括号多余 - 没有匹配的左括号\n建议：删除此右括号或在前面添加左括号 (",
                        firstCloseParen,
                        firstCloseParen + 1
                    ))
                }
            }
            
            if (bracketCount > 0) {
                val lastOpenBracket = lineWithoutStringsAndComments.lastIndexOf('[')
                if (lastOpenBracket >= 0) {
                    errors.add(SyntaxError(
                        lineNumber,
                        "第 $lineNumber 行，第 ${lastOpenBracket + 1} 列：左方括号未闭合 - 缺少匹配的右方括号\n建议：在数组索引或列表末尾添加 ]",
                        lastOpenBracket,
                        lastOpenBracket + 1
                    ))
                }
            } else if (bracketCount < 0) {
                val firstCloseBracket = lineWithoutStringsAndComments.indexOf(']')
                if (firstCloseBracket >= 0) {
                    errors.add(SyntaxError(
                        lineNumber,
                        "第 $lineNumber 行，第 ${firstCloseBracket + 1} 列：右方括号多余 - 没有匹配的左方括号\n建议：删除此右方括号或在前面添加左方括号 [",
                        firstCloseBracket,
                        firstCloseBracket + 1
                    ))
                }
            }
            
            if (braceCount > 0) {
                val lastOpenBrace = lineWithoutStringsAndComments.lastIndexOf('{')
                if (lastOpenBrace >= 0) {
                    errors.add(SyntaxError(
                        lineNumber,
                        "第 $lineNumber 行，第 ${lastOpenBrace + 1} 列：左花括号未闭合 - 缺少匹配的右花括号\n建议：在数据结构末尾添加 }",
                        lastOpenBrace,
                        lastOpenBrace + 1
                    ))
                }
            } else if (braceCount < 0) {
                val firstCloseBrace = lineWithoutStringsAndComments.indexOf('}')
                if (firstCloseBrace >= 0) {
                    errors.add(SyntaxError(
                        lineNumber,
                        "第 $lineNumber 行，第 ${firstCloseBrace + 1} 列：右花括号多余 - 没有匹配的左花括号\n建议：删除此右花括号或在前面添加左花括号 {",
                        firstCloseBrace,
                        firstCloseBrace + 1
                    ))
                }
            }
            
            // Check MODULE/ENDMODULE matching
            when {
                trimmed.matches(Regex("^MODULE\\s+\\w+.*", RegexOption.IGNORE_CASE)) -> {
                    moduleCount++
                    blockStack.add(BlockInfo("MODULE", lineNumber))
                }
                trimmed.matches(Regex("^ENDMODULE\\s*.*", RegexOption.IGNORE_CASE)) -> {
                    endModuleCount++
                    val columnStart = line.indexOf("ENDMODULE", ignoreCase = true)
                    val columnEnd = columnStart + "ENDMODULE".length
                    if (blockStack.isEmpty()) {
                        errors.add(SyntaxError(
                            lineNumber, 
                            "第 $lineNumber 行，第 ${columnStart + 1} 列：ENDMODULE 没有对应的开始块 - 缺少 MODULE 声明\n建议：在文件开头添加 MODULE 模块名",
                            columnStart,
                            columnEnd
                        ))
                    } else if (blockStack.last().type != "MODULE") {
                        val openBlock = blockStack.last()
                        errors.add(SyntaxError(
                            lineNumber, 
                            "第 $lineNumber 行，第 ${columnStart + 1} 列：ENDMODULE 与第 ${openBlock.lineNumber} 行的 ${openBlock.type} 不匹配\n建议：应使用 END${openBlock.type} 而不是 ENDMODULE",
                            columnStart,
                            columnEnd
                        ))
                    } else {
                        blockStack.removeAt(blockStack.size - 1)
                    }
                }
                
                // Check PROC/ENDPROC matching
                trimmed.matches(Regex("^PROC\\s+\\w+.*", RegexOption.IGNORE_CASE)) -> {
                    blockStack.add(BlockInfo("PROC", lineNumber))
                }
                trimmed.matches(Regex("^ENDPROC\\s*.*", RegexOption.IGNORE_CASE)) -> {
                    val columnStart = line.indexOf("ENDPROC", ignoreCase = true)
                    val columnEnd = columnStart + "ENDPROC".length
                    if (blockStack.isEmpty()) {
                        errors.add(SyntaxError(
                            lineNumber, 
                            "第 $lineNumber 行，第 ${columnStart + 1} 列：ENDPROC 没有对应的开始块 - 缺少 PROC 声明\n建议：在此之前添加 PROC 过程名()",
                            columnStart,
                            columnEnd
                        ))
                    } else if (blockStack.last().type != "PROC") {
                        val openBlock = blockStack.last()
                        errors.add(SyntaxError(
                            lineNumber, 
                            "第 $lineNumber 行，第 ${columnStart + 1} 列：ENDPROC 与第 ${openBlock.lineNumber} 行的 ${openBlock.type} 不匹配\n建议：应使用 END${openBlock.type} 而不是 ENDPROC",
                            columnStart,
                            columnEnd
                        ))
                    } else {
                        blockStack.removeAt(blockStack.size - 1)
                    }
                }
                
                // Check FUNC/ENDFUNC matching
                trimmed.matches(Regex("^FUNC\\s+\\w+\\s+\\w+.*", RegexOption.IGNORE_CASE)) -> {
                    blockStack.add(BlockInfo("FUNC", lineNumber))
                }
                trimmed.matches(Regex("^ENDFUNC\\s*.*", RegexOption.IGNORE_CASE)) -> {
                    val columnStart = line.indexOf("ENDFUNC", ignoreCase = true)
                    val columnEnd = columnStart + "ENDFUNC".length
                    if (blockStack.isEmpty()) {
                        errors.add(SyntaxError(
                            lineNumber, 
                            "第 $lineNumber 行，第 ${columnStart + 1} 列：ENDFUNC 没有对应的开始块 - 缺少 FUNC 声明\n建议：在此之前添加 FUNC 返回类型 函数名()",
                            columnStart,
                            columnEnd
                        ))
                    } else if (blockStack.last().type != "FUNC") {
                        val openBlock = blockStack.last()
                        errors.add(SyntaxError(
                            lineNumber, 
                            "第 $lineNumber 行，第 ${columnStart + 1} 列：ENDFUNC 与第 ${openBlock.lineNumber} 行的 ${openBlock.type} 不匹配\n建议：应使用 END${openBlock.type} 而不是 ENDFUNC",
                            columnStart,
                            columnEnd
                        ))
                    } else {
                        blockStack.removeAt(blockStack.size - 1)
                    }
                }
                
                // Check TRAP/ENDTRAP matching
                trimmed.matches(Regex("^TRAP\\s+\\w+.*", RegexOption.IGNORE_CASE)) -> {
                    blockStack.add(BlockInfo("TRAP", lineNumber))
                }
                trimmed.matches(Regex("^ENDTRAP\\s*.*", RegexOption.IGNORE_CASE)) -> {
                    val columnStart = line.indexOf("ENDTRAP", ignoreCase = true)
                    val columnEnd = columnStart + "ENDTRAP".length
                    if (blockStack.isEmpty()) {
                        errors.add(SyntaxError(
                            lineNumber, 
                            "第 $lineNumber 行，第 ${columnStart + 1} 列：ENDTRAP 没有对应的开始块 - 缺少 TRAP 声明\n建议：在此之前添加 TRAP 陷阱名",
                            columnStart,
                            columnEnd
                        ))
                    } else if (blockStack.last().type != "TRAP") {
                        val openBlock = blockStack.last()
                        errors.add(SyntaxError(
                            lineNumber, 
                            "第 $lineNumber 行，第 ${columnStart + 1} 列：ENDTRAP 与第 ${openBlock.lineNumber} 行的 ${openBlock.type} 不匹配\n建议：应使用 END${openBlock.type} 而不是 ENDTRAP",
                            columnStart,
                            columnEnd
                        ))
                    } else {
                        blockStack.removeAt(blockStack.size - 1)
                    }
                }
                
                // Check IF/ENDIF matching
                trimmed.matches(Regex("^IF\\s+.+\\s+THEN\\s*.*", RegexOption.IGNORE_CASE)) -> {
                    blockStack.add(BlockInfo("IF", lineNumber))
                }
                trimmed.matches(Regex("^ENDIF\\s*.*", RegexOption.IGNORE_CASE)) -> {
                    val columnStart = line.indexOf("ENDIF", ignoreCase = true)
                    val columnEnd = columnStart + "ENDIF".length
                    if (blockStack.isEmpty()) {
                        errors.add(SyntaxError(
                            lineNumber, 
                            "第 $lineNumber 行，第 ${columnStart + 1} 列：ENDIF 没有对应的开始块 - 缺少 IF 语句\n建议：在此之前添加 IF 条件 THEN",
                            columnStart,
                            columnEnd
                        ))
                    } else if (blockStack.last().type != "IF") {
                        val openBlock = blockStack.last()
                        errors.add(SyntaxError(
                            lineNumber, 
                            "第 $lineNumber 行，第 ${columnStart + 1} 列：ENDIF 与第 ${openBlock.lineNumber} 行的 ${openBlock.type} 不匹配\n建议：应使用 END${openBlock.type} 而不是 ENDIF",
                            columnStart,
                            columnEnd
                        ))
                    } else {
                        blockStack.removeAt(blockStack.size - 1)
                    }
                }
                
                // Check FOR/ENDFOR matching
                trimmed.matches(Regex("^FOR\\s+.+\\s+(FROM\\s+.+\\s+)?TO\\s+.+\\s+DO\\s*.*", RegexOption.IGNORE_CASE)) -> {
                    blockStack.add(BlockInfo("FOR", lineNumber))
                }
                trimmed.matches(Regex("^ENDFOR\\s*.*", RegexOption.IGNORE_CASE)) -> {
                    val columnStart = line.indexOf("ENDFOR", ignoreCase = true)
                    val columnEnd = columnStart + "ENDFOR".length
                    if (blockStack.isEmpty()) {
                        errors.add(SyntaxError(
                            lineNumber, 
                            "第 $lineNumber 行，第 ${columnStart + 1} 列：ENDFOR 没有对应的开始块 - 缺少 FOR 循环\n建议：在此之前添加 FOR 变量 FROM 起始值 TO 结束值 DO",
                            columnStart,
                            columnEnd
                        ))
                    } else if (blockStack.last().type != "FOR") {
                        val openBlock = blockStack.last()
                        errors.add(SyntaxError(
                            lineNumber, 
                            "第 $lineNumber 行，第 ${columnStart + 1} 列：ENDFOR 与第 ${openBlock.lineNumber} 行的 ${openBlock.type} 不匹配\n建议：应使用 END${openBlock.type} 而不是 ENDFOR",
                            columnStart,
                            columnEnd
                        ))
                    } else {
                        blockStack.removeAt(blockStack.size - 1)
                    }
                }
                
                // Check WHILE/ENDWHILE matching
                trimmed.matches(Regex("^WHILE\\s+.+\\s+DO\\s*.*", RegexOption.IGNORE_CASE)) -> {
                    blockStack.add(BlockInfo("WHILE", lineNumber))
                }
                trimmed.matches(Regex("^ENDWHILE\\s*.*", RegexOption.IGNORE_CASE)) -> {
                    val columnStart = line.indexOf("ENDWHILE", ignoreCase = true)
                    val columnEnd = columnStart + "ENDWHILE".length
                    if (blockStack.isEmpty()) {
                        errors.add(SyntaxError(
                            lineNumber, 
                            "第 $lineNumber 行，第 ${columnStart + 1} 列：ENDWHILE 没有对应的开始块 - 缺少 WHILE 循环\n建议：在此之前添加 WHILE 条件 DO",
                            columnStart,
                            columnEnd
                        ))
                    } else if (blockStack.last().type != "WHILE") {
                        val openBlock = blockStack.last()
                        errors.add(SyntaxError(
                            lineNumber, 
                            "第 $lineNumber 行，第 ${columnStart + 1} 列：ENDWHILE 与第 ${openBlock.lineNumber} 行的 ${openBlock.type} 不匹配\n建议：应使用 END${openBlock.type} 而不是 ENDWHILE",
                            columnStart,
                            columnEnd
                        ))
                    } else {
                        blockStack.removeAt(blockStack.size - 1)
                    }
                }
                
                // Check TEST/ENDTEST matching
                trimmed.matches(Regex("^TEST\\s+.+", RegexOption.IGNORE_CASE)) -> {
                    blockStack.add(BlockInfo("TEST", lineNumber))
                }
                trimmed.matches(Regex("^ENDTEST\\s*.*", RegexOption.IGNORE_CASE)) -> {
                    val columnStart = line.indexOf("ENDTEST", ignoreCase = true)
                    val columnEnd = columnStart + "ENDTEST".length
                    if (blockStack.isEmpty()) {
                        errors.add(SyntaxError(
                            lineNumber, 
                            "第 $lineNumber 行，第 ${columnStart + 1} 列：ENDTEST 没有对应的开始块 - 缺少 TEST 语句\n建议：在此之前添加 TEST 表达式",
                            columnStart,
                            columnEnd
                        ))
                    } else if (blockStack.last().type != "TEST") {
                        val openBlock = blockStack.last()
                        errors.add(SyntaxError(
                            lineNumber, 
                            "第 $lineNumber 行，第 ${columnStart + 1} 列：ENDTEST 与第 ${openBlock.lineNumber} 行的 ${openBlock.type} 不匹配\n建议：应使用 END${openBlock.type} 而不是 ENDTEST",
                            columnStart,
                            columnEnd
                        ))
                    } else {
                        blockStack.removeAt(blockStack.size - 1)
                    }
                }
            }
            
            // Check for invalid syntax patterns (enhanced validation)
            
            // Check for incomplete or invalid assignment statements
            if (trimmed.contains(":=")) {
                val parts = trimmed.split(":=")
                if (parts.size >= 2) {
                    val leftPart = parts[0].trim()
                    val rightPart = parts[1].trim()
                    
                    // Check if left side is empty or invalid
                    if (leftPart.isEmpty()) {
                        val colonPos = line.indexOf(":=")
                        errors.add(SyntaxError(
                            lineNumber,
                            "第 $lineNumber 行，第 ${colonPos + 1} 列：赋值语句左侧缺少变量名\n建议：在 := 前添加变量名，例如：变量名 := 值",
                            colonPos,
                            colonPos + 2
                        ))
                    } else {
                        // Get the last token which should be the variable name
                        val tokens = leftPart.split(Regex("\\s+"))
                        if (tokens.isNotEmpty()) {
                            val varName = tokens.last()
                            // Allow array access, field access, but check basic variable pattern
                            val cleanVarName = varName.split(Regex("[.\\[{]")).first()
                            if (cleanVarName.isNotEmpty() && !cleanVarName.matches(Regex("^[a-zA-Z_][a-zA-Z0-9_]*$"))) {
                                val varIndex = line.indexOf(cleanVarName)
                                val columnStart = if (varIndex >= 0) varIndex else 0
                                val columnEnd = if (varIndex >= 0) varIndex + cleanVarName.length else 0
                                errors.add(SyntaxError(
                                    lineNumber, 
                                    "第 $lineNumber 行，第 ${columnStart + 1} 列：无效的变量名 '$cleanVarName'\n建议：变量名必须以字母或下划线开头，只能包含字母、数字和下划线",
                                    columnStart,
                                    columnEnd
                                ))
                            }
                        }
                    }
                    
                    // Check if right side is empty
                    if (rightPart.isEmpty()) {
                        val colonPos = line.indexOf(":=")
                        errors.add(SyntaxError(
                            lineNumber,
                            "第 $lineNumber 行，第 ${colonPos + 3} 列：赋值语句右侧缺少表达式\n建议：在 := 后添加要赋的值或表达式",
                            colonPos + 2,
                            line.length
                        ))
                    }
                } else {
                    // Assignment operator at the end of the line
                    val colonPos = line.indexOf(":=")
                    errors.add(SyntaxError(
                        lineNumber,
                        "第 $lineNumber 行，第 ${colonPos + 1} 列：赋值语句不完整 - 缺少右侧表达式\n建议：在 := 后添加要赋的值",
                        colonPos,
                        line.length
                    ))
                }
            }
            
            // Check for IF without THEN
            if (trimmed.matches(Regex("^IF\\s+.+", RegexOption.IGNORE_CASE)) && 
                !trimmed.matches(Regex("^IF\\s+.+\\s+THEN\\s*.*", RegexOption.IGNORE_CASE))) {
                val columnStart = line.indexOf("IF", ignoreCase = true)
                errors.add(SyntaxError(
                    lineNumber,
                    "第 $lineNumber 行，第 ${columnStart + 1} 列：IF 语句缺少 THEN 关键字\n建议：在条件表达式后添加 THEN，格式：IF 条件 THEN",
                    columnStart,
                    line.length
                ))
            }
            
            // Check for WHILE without DO
            if (trimmed.matches(Regex("^WHILE\\s+.+", RegexOption.IGNORE_CASE)) && 
                !trimmed.matches(Regex("^WHILE\\s+.+\\s+DO\\s*.*", RegexOption.IGNORE_CASE))) {
                val columnStart = line.indexOf("WHILE", ignoreCase = true)
                errors.add(SyntaxError(
                    lineNumber,
                    "第 $lineNumber 行，第 ${columnStart + 1} 列：WHILE 循环缺少 DO 关键字\n建议：在条件表达式后添加 DO，格式：WHILE 条件 DO",
                    columnStart,
                    line.length
                ))
            }
            
            // Check for FOR without TO or DO
            if (trimmed.matches(Regex("^FOR\\s+.+", RegexOption.IGNORE_CASE))) {
                if (!trimmed.contains("TO", ignoreCase = true)) {
                    val columnStart = line.indexOf("FOR", ignoreCase = true)
                    errors.add(SyntaxError(
                        lineNumber,
                        "第 $lineNumber 行，第 ${columnStart + 1} 列：FOR 循环缺少 TO 关键字\n建议：指定循环结束值，格式：FOR 变量 FROM 起始值 TO 结束值 DO",
                        columnStart,
                        line.length
                    ))
                } else if (!trimmed.contains("DO", ignoreCase = true)) {
                    val columnStart = line.indexOf("FOR", ignoreCase = true)
                    errors.add(SyntaxError(
                        lineNumber,
                        "第 $lineNumber 行，第 ${columnStart + 1} 列：FOR 循环缺少 DO 关键字\n建议：在循环范围后添加 DO，格式：FOR 变量 FROM 起始值 TO 结束值 DO",
                        columnStart,
                        line.length
                    ))
                }
            }
            
            // Check for invalid semicolon usage (RAPID doesn't use semicolons for statement termination)
            if (trimmed.endsWith(";") && !trimmed.matches(Regex(".*RETURN\\s+.*;.*", RegexOption.IGNORE_CASE))) {
                val semicolonPos = line.lastIndexOf(';')
                if (semicolonPos >= 0) {
                    errors.add(SyntaxError(
                        lineNumber,
                        "第 $lineNumber 行，第 ${semicolonPos + 1} 列：语句末尾不应有分号 - RAPID 语言不使用分号结束语句\n建议：删除分号，RAPID 语句通过换行自动结束",
                        semicolonPos,
                        semicolonPos + 1
                    ))
                }
            }
            
            // Check for incomplete PROC/FUNC/TRAP declarations
            if (trimmed.matches(Regex("^PROC\\s*$", RegexOption.IGNORE_CASE))) {
                val columnStart = line.indexOf("PROC", ignoreCase = true)
                errors.add(SyntaxError(
                    lineNumber,
                    "第 $lineNumber 行，第 ${columnStart + 1} 列：PROC 声明不完整 - 缺少过程名称\n建议：添加过程名称和参数，格式：PROC 过程名(参数列表)",
                    columnStart,
                    line.length
                ))
            }
            
            if (trimmed.matches(Regex("^FUNC\\s*$", RegexOption.IGNORE_CASE))) {
                val columnStart = line.indexOf("FUNC", ignoreCase = true)
                errors.add(SyntaxError(
                    lineNumber,
                    "第 $lineNumber 行，第 ${columnStart + 1} 列：FUNC 声明不完整 - 缺少返回类型和函数名\n建议：格式：FUNC 返回类型 函数名(参数列表)，例如：FUNC num GetValue()",
                    columnStart,
                    line.length
                ))
            } else if (trimmed.matches(Regex("^FUNC\\s+\\w+\\s*$", RegexOption.IGNORE_CASE))) {
                val columnStart = line.indexOf("FUNC", ignoreCase = true)
                errors.add(SyntaxError(
                    lineNumber,
                    "第 $lineNumber 行，第 ${columnStart + 1} 列：FUNC 声明不完整 - 缺少函数名\n建议：在返回类型后添加函数名，格式：FUNC 返回类型 函数名(参数列表)",
                    columnStart,
                    line.length
                ))
            }
            
            if (trimmed.matches(Regex("^TRAP\\s*$", RegexOption.IGNORE_CASE))) {
                val columnStart = line.indexOf("TRAP", ignoreCase = true)
                errors.add(SyntaxError(
                    lineNumber,
                    "第 $lineNumber 行，第 ${columnStart + 1} 列：TRAP 声明不完整 - 缺少陷阱例程名称\n建议：添加陷阱例程名称，格式：TRAP 陷阱名",
                    columnStart,
                    line.length
                ))
            }
            
            // Check for incomplete MODULE declaration
            if (trimmed.matches(Regex("^MODULE\\s*$", RegexOption.IGNORE_CASE))) {
                val columnStart = line.indexOf("MODULE", ignoreCase = true)
                errors.add(SyntaxError(
                    lineNumber,
                    "第 $lineNumber 行，第 ${columnStart + 1} 列：MODULE 声明不完整 - 缺少模块名称\n建议：添加模块名称，格式：MODULE 模块名",
                    columnStart,
                    line.length
                ))
            }
            
            // Check for invalid RETURN statement usage
            if (trimmed.matches(Regex("^RETURN\\s*$", RegexOption.IGNORE_CASE))) {
                val columnStart = line.indexOf("RETURN", ignoreCase = true)
                // Check if we're in a FUNC block (should return a value)
                val inFunc = blockStack.any { it.type == "FUNC" }
                if (inFunc) {
                    errors.add(SyntaxError(
                        lineNumber,
                        "第 $lineNumber 行，第 ${columnStart + 1} 列：RETURN 语句缺少返回值 - FUNC 函数必须返回一个值\n建议：在 RETURN 后添加返回值，格式：RETURN 表达式",
                        columnStart,
                        line.length
                    ))
                }
            }
            
            // Check for VAR/PERS/CONST without variable name or type
            if (trimmed.matches(Regex("^(VAR|PERS|CONST)\\s*$", RegexOption.IGNORE_CASE))) {
                val match = Regex("^(VAR|PERS|CONST)", RegexOption.IGNORE_CASE).find(trimmed)
                if (match != null) {
                    val keyword = match.value
                    val columnStart = line.indexOf(keyword, ignoreCase = true)
                    errors.add(SyntaxError(
                        lineNumber,
                        "第 $lineNumber 行，第 ${columnStart + 1} 列：变量声明不完整 - 缺少数据类型和变量名\n建议：格式：${keyword.uppercase()} 数据类型 变量名，例如：VAR num counter",
                        columnStart,
                        line.length
                    ))
                }
            } else if (trimmed.matches(Regex("^(VAR|PERS|CONST)\\s+\\w+\\s*$", RegexOption.IGNORE_CASE))) {
                val match = Regex("^(VAR|PERS|CONST)", RegexOption.IGNORE_CASE).find(trimmed)
                if (match != null) {
                    val keyword = match.value
                    val columnStart = line.indexOf(keyword, ignoreCase = true)
                    errors.add(SyntaxError(
                        lineNumber,
                        "第 $lineNumber 行，第 ${columnStart + 1} 列：变量声明不完整 - 缺少变量名\n建议：在数据类型后添加变量名，格式：${keyword.uppercase()} 数据类型 变量名",
                        columnStart,
                        line.length
                    ))
                }
            }
            
            // Check for incomplete function calls (function name followed by just opening paren at end of line)
            if (trimmed.matches(Regex(".*\\w+\\s*\\(\\s*$"))) {
                val openParenPos = trimmed.lastIndexOf('(')
                if (openParenPos > 0) {
                    errors.add(SyntaxError(
                        lineNumber,
                        "第 $lineNumber 行，第 ${openParenPos + 1} 列：函数调用不完整 - 参数列表和右括号缺失\n建议：补充参数并闭合括号，格式：函数名(参数1, 参数2)",
                        openParenPos,
                        line.length
                    ))
                }
            }
        }
        
        // Check for unclosed blocks - report with specific line numbers
        blockStack.forEach { block ->
            errors.add(SyntaxError(
                block.lineNumber, 
                "第 ${block.lineNumber} 行：${block.type} 代码块未闭合 - 缺少 END${block.type}\n建议：在代码块结束位置添加 END${block.type}"
            ))
        }
        
        return errors
    }
    
    data class BlockInfo(val type: String, val lineNumber: Int)
    
    /**
     * Remove strings and comments from a line to simplify syntax checking
     */
    private fun removeStringsAndComments(line: String): String {
        val result = StringBuilder()
        var inString = false
        var i = 0
        
        while (i < line.length) {
            val char = line[i]
            
            // Check for comment start
            if (!inString && char == '!' && i < line.length) {
                // Rest of line is a comment
                break
            }
            
            // Check for string delimiter
            if (char == '"') {
                inString = !inString
                result.append(' ') // Replace string content with space
            } else if (!inString) {
                result.append(char)
            } else {
                result.append(' ') // Replace string content with space
            }
            
            i++
        }
        
        return result.toString()
    }
}

data class SyntaxError(
    val lineNumber: Int, 
    val message: String,
    val columnStart: Int = 0,  // 0-based column index where error starts
    val columnEnd: Int = 0     // 0-based column index where error ends (0 means unknown/whole line)
)
