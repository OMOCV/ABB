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
     * Comprehensive syntax validation for RAPID code
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
                            "ENDMODULE at line $lineNumber, column ${columnStart + 1} without any open block - missing MODULE declaration",
                            columnStart,
                            columnEnd
                        ))
                    } else if (blockStack.last().type != "MODULE") {
                        val openBlock = blockStack.last()
                        errors.add(SyntaxError(
                            lineNumber, 
                            "ENDMODULE at line $lineNumber, column ${columnStart + 1} does not match open ${openBlock.type} block at line ${openBlock.lineNumber} - expected END${openBlock.type}",
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
                            "ENDPROC at line $lineNumber, column ${columnStart + 1} without any open block - missing PROC declaration",
                            columnStart,
                            columnEnd
                        ))
                    } else if (blockStack.last().type != "PROC") {
                        val openBlock = blockStack.last()
                        errors.add(SyntaxError(
                            lineNumber, 
                            "ENDPROC at line $lineNumber, column ${columnStart + 1} does not match open ${openBlock.type} block at line ${openBlock.lineNumber} - expected END${openBlock.type}",
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
                            "ENDFUNC at line $lineNumber, column ${columnStart + 1} without any open block - missing FUNC declaration",
                            columnStart,
                            columnEnd
                        ))
                    } else if (blockStack.last().type != "FUNC") {
                        val openBlock = blockStack.last()
                        errors.add(SyntaxError(
                            lineNumber, 
                            "ENDFUNC at line $lineNumber, column ${columnStart + 1} does not match open ${openBlock.type} block at line ${openBlock.lineNumber} - expected END${openBlock.type}",
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
                            "ENDTRAP at line $lineNumber, column ${columnStart + 1} without any open block - missing TRAP declaration",
                            columnStart,
                            columnEnd
                        ))
                    } else if (blockStack.last().type != "TRAP") {
                        val openBlock = blockStack.last()
                        errors.add(SyntaxError(
                            lineNumber, 
                            "ENDTRAP at line $lineNumber, column ${columnStart + 1} does not match open ${openBlock.type} block at line ${openBlock.lineNumber} - expected END${openBlock.type}",
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
                            "ENDIF at line $lineNumber, column ${columnStart + 1} without any open block - missing IF statement",
                            columnStart,
                            columnEnd
                        ))
                    } else if (blockStack.last().type != "IF") {
                        val openBlock = blockStack.last()
                        errors.add(SyntaxError(
                            lineNumber, 
                            "ENDIF at line $lineNumber, column ${columnStart + 1} does not match open ${openBlock.type} block at line ${openBlock.lineNumber} - expected END${openBlock.type}",
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
                            "ENDFOR at line $lineNumber, column ${columnStart + 1} without any open block - missing FOR loop",
                            columnStart,
                            columnEnd
                        ))
                    } else if (blockStack.last().type != "FOR") {
                        val openBlock = blockStack.last()
                        errors.add(SyntaxError(
                            lineNumber, 
                            "ENDFOR at line $lineNumber, column ${columnStart + 1} does not match open ${openBlock.type} block at line ${openBlock.lineNumber} - expected END${openBlock.type}",
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
                            "ENDWHILE at line $lineNumber, column ${columnStart + 1} without any open block - missing WHILE loop",
                            columnStart,
                            columnEnd
                        ))
                    } else if (blockStack.last().type != "WHILE") {
                        val openBlock = blockStack.last()
                        errors.add(SyntaxError(
                            lineNumber, 
                            "ENDWHILE at line $lineNumber, column ${columnStart + 1} does not match open ${openBlock.type} block at line ${openBlock.lineNumber} - expected END${openBlock.type}",
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
                            "ENDTEST at line $lineNumber, column ${columnStart + 1} without any open block - missing TEST statement",
                            columnStart,
                            columnEnd
                        ))
                    } else if (blockStack.last().type != "TEST") {
                        val openBlock = blockStack.last()
                        errors.add(SyntaxError(
                            lineNumber, 
                            "ENDTEST at line $lineNumber, column ${columnStart + 1} does not match open ${openBlock.type} block at line ${openBlock.lineNumber} - expected END${openBlock.type}",
                            columnStart,
                            columnEnd
                        ))
                    } else {
                        blockStack.removeAt(blockStack.size - 1)
                    }
                }
            }
            
            // Check for invalid syntax patterns (but be lenient)
            if (trimmed.contains(":=")) {
                // Check assignment statement has valid variable name
                val parts = trimmed.split(":=")
                if (parts.size >= 2) {
                    val varPart = parts[0].trim()
                    // Get the last token which should be the variable name
                    val tokens = varPart.split(Regex("\\s+"))
                    if (tokens.isNotEmpty()) {
                        val varName = tokens.last()
                        // Allow array access, field access, but check basic variable pattern
                        val cleanVarName = varName.split(Regex("[.\\[{]")).first()
                        if (cleanVarName.isNotEmpty() && !cleanVarName.matches(Regex("^[a-zA-Z_][a-zA-Z0-9_]*$"))) {
                            // Find the position of the variable name in the original line
                            val varIndex = line.indexOf(cleanVarName)
                            val columnStart = if (varIndex >= 0) varIndex else 0
                            val columnEnd = if (varIndex >= 0) varIndex + cleanVarName.length else 0
                            errors.add(SyntaxError(
                                lineNumber, 
                                "Invalid variable name '$cleanVarName' at line $lineNumber, column ${columnStart + 1}",
                                columnStart,
                                columnEnd
                            ))
                        }
                    }
                }
            }
            
            // Check for IF without THEN
            if (trimmed.matches(Regex("^IF\\s+.+", RegexOption.IGNORE_CASE)) && 
                !trimmed.matches(Regex("^IF\\s+.+\\s+THEN\\s*.*", RegexOption.IGNORE_CASE))) {
                val columnStart = line.indexOf("IF", ignoreCase = true)
                errors.add(SyntaxError(
                    lineNumber,
                    "IF statement at line $lineNumber, column ${columnStart + 1} is missing THEN keyword",
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
                    "WHILE loop at line $lineNumber, column ${columnStart + 1} is missing DO keyword",
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
                        "FOR loop at line $lineNumber, column ${columnStart + 1} is missing TO keyword",
                        columnStart,
                        line.length
                    ))
                } else if (!trimmed.contains("DO", ignoreCase = true)) {
                    val columnStart = line.indexOf("FOR", ignoreCase = true)
                    errors.add(SyntaxError(
                        lineNumber,
                        "FOR loop at line $lineNumber, column ${columnStart + 1} is missing DO keyword",
                        columnStart,
                        line.length
                    ))
                }
            }
            
            // Check for unclosed strings
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
                        "Unclosed string at line $lineNumber, column ${lastQuotePos + 1}",
                        lastQuotePos,
                        line.length
                    ))
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
                        "Unclosed parenthesis at line $lineNumber, column ${lastOpenParen + 1}",
                        lastOpenParen,
                        lastOpenParen + 1
                    ))
                }
            } else if (parenCount < 0) {
                val firstCloseParen = lineWithoutStringsAndComments.indexOf(')')
                if (firstCloseParen >= 0) {
                    errors.add(SyntaxError(
                        lineNumber,
                        "Unmatched closing parenthesis at line $lineNumber, column ${firstCloseParen + 1}",
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
                        "Unclosed bracket at line $lineNumber, column ${lastOpenBracket + 1}",
                        lastOpenBracket,
                        lastOpenBracket + 1
                    ))
                }
            } else if (bracketCount < 0) {
                val firstCloseBracket = lineWithoutStringsAndComments.indexOf(']')
                if (firstCloseBracket >= 0) {
                    errors.add(SyntaxError(
                        lineNumber,
                        "Unmatched closing bracket at line $lineNumber, column ${firstCloseBracket + 1}",
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
                        "Unclosed brace at line $lineNumber, column ${lastOpenBrace + 1}",
                        lastOpenBrace,
                        lastOpenBrace + 1
                    ))
                }
            } else if (braceCount < 0) {
                val firstCloseBrace = lineWithoutStringsAndComments.indexOf('}')
                if (firstCloseBrace >= 0) {
                    errors.add(SyntaxError(
                        lineNumber,
                        "Unmatched closing brace at line $lineNumber, column ${firstCloseBrace + 1}",
                        firstCloseBrace,
                        firstCloseBrace + 1
                    ))
                }
            }
            
            // Check for invalid semicolon usage (RAPID doesn't use semicolons for statement termination)
            if (trimmed.endsWith(";") && !trimmed.matches(Regex(".*RETURN\\s+.*;.*", RegexOption.IGNORE_CASE))) {
                val semicolonPos = line.lastIndexOf(';')
                if (semicolonPos >= 0) {
                    errors.add(SyntaxError(
                        lineNumber,
                        "Unexpected semicolon at line $lineNumber, column ${semicolonPos + 1} - RAPID does not use semicolons for statement termination",
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
                    "Incomplete PROC declaration at line $lineNumber, column ${columnStart + 1} - missing procedure name",
                    columnStart,
                    line.length
                ))
            }
            
            if (trimmed.matches(Regex("^FUNC\\s*$", RegexOption.IGNORE_CASE)) || 
                trimmed.matches(Regex("^FUNC\\s+\\w+\\s*$", RegexOption.IGNORE_CASE))) {
                val columnStart = line.indexOf("FUNC", ignoreCase = true)
                errors.add(SyntaxError(
                    lineNumber,
                    "Incomplete FUNC declaration at line $lineNumber, column ${columnStart + 1} - missing return type or function name",
                    columnStart,
                    line.length
                ))
            }
            
            if (trimmed.matches(Regex("^TRAP\\s*$", RegexOption.IGNORE_CASE))) {
                val columnStart = line.indexOf("TRAP", ignoreCase = true)
                errors.add(SyntaxError(
                    lineNumber,
                    "Incomplete TRAP declaration at line $lineNumber, column ${columnStart + 1} - missing trap routine name",
                    columnStart,
                    line.length
                ))
            }
            
            // Check for incomplete MODULE declaration
            if (trimmed.matches(Regex("^MODULE\\s*$", RegexOption.IGNORE_CASE))) {
                val columnStart = line.indexOf("MODULE", ignoreCase = true)
                errors.add(SyntaxError(
                    lineNumber,
                    "Incomplete MODULE declaration at line $lineNumber, column ${columnStart + 1} - missing module name",
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
                        "RETURN statement at line $lineNumber, column ${columnStart + 1} is missing return value - FUNC requires a return value",
                        columnStart,
                        line.length
                    ))
                }
            }
            
            // Check for VAR/PERS/CONST without variable name
            if (trimmed.matches(Regex("^(VAR|PERS|CONST)\\s*$", RegexOption.IGNORE_CASE)) ||
                trimmed.matches(Regex("^(VAR|PERS|CONST)\\s+\\w+\\s*$", RegexOption.IGNORE_CASE))) {
                val match = Regex("^(VAR|PERS|CONST)", RegexOption.IGNORE_CASE).find(trimmed)
                if (match != null) {
                    val keyword = match.value
                    val columnStart = line.indexOf(keyword, ignoreCase = true)
                    errors.add(SyntaxError(
                        lineNumber,
                        "Incomplete variable declaration at line $lineNumber, column ${columnStart + 1} - missing variable name or type",
                        columnStart,
                        line.length
                    ))
                }
            }
        }
        
        // Check for unclosed blocks - report with specific line numbers
        blockStack.forEach { block ->
            errors.add(SyntaxError(block.lineNumber, "Unclosed ${block.type} block starting at line ${block.lineNumber} - missing END${block.type}"))
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
