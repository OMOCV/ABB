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
                    if (blockStack.isEmpty()) {
                        errors.add(SyntaxError(lineNumber, "ENDMODULE at line $lineNumber without any open block - missing MODULE declaration"))
                    } else if (blockStack.last().type != "MODULE") {
                        val openBlock = blockStack.last()
                        errors.add(SyntaxError(lineNumber, "ENDMODULE at line $lineNumber does not match open ${openBlock.type} block at line ${openBlock.lineNumber} - expected END${openBlock.type}"))
                    } else {
                        blockStack.removeAt(blockStack.size - 1)
                    }
                }
                
                // Check PROC/ENDPROC matching
                trimmed.matches(Regex("^PROC\\s+\\w+.*", RegexOption.IGNORE_CASE)) -> {
                    blockStack.add(BlockInfo("PROC", lineNumber))
                }
                trimmed.matches(Regex("^ENDPROC\\s*.*", RegexOption.IGNORE_CASE)) -> {
                    if (blockStack.isEmpty()) {
                        errors.add(SyntaxError(lineNumber, "ENDPROC at line $lineNumber without any open block - missing PROC declaration"))
                    } else if (blockStack.last().type != "PROC") {
                        val openBlock = blockStack.last()
                        errors.add(SyntaxError(lineNumber, "ENDPROC at line $lineNumber does not match open ${openBlock.type} block at line ${openBlock.lineNumber} - expected END${openBlock.type}"))
                    } else {
                        blockStack.removeAt(blockStack.size - 1)
                    }
                }
                
                // Check FUNC/ENDFUNC matching
                trimmed.matches(Regex("^FUNC\\s+\\w+\\s+\\w+.*", RegexOption.IGNORE_CASE)) -> {
                    blockStack.add(BlockInfo("FUNC", lineNumber))
                }
                trimmed.matches(Regex("^ENDFUNC\\s*.*", RegexOption.IGNORE_CASE)) -> {
                    if (blockStack.isEmpty()) {
                        errors.add(SyntaxError(lineNumber, "ENDFUNC at line $lineNumber without any open block - missing FUNC declaration"))
                    } else if (blockStack.last().type != "FUNC") {
                        val openBlock = blockStack.last()
                        errors.add(SyntaxError(lineNumber, "ENDFUNC at line $lineNumber does not match open ${openBlock.type} block at line ${openBlock.lineNumber} - expected END${openBlock.type}"))
                    } else {
                        blockStack.removeAt(blockStack.size - 1)
                    }
                }
                
                // Check TRAP/ENDTRAP matching
                trimmed.matches(Regex("^TRAP\\s+\\w+.*", RegexOption.IGNORE_CASE)) -> {
                    blockStack.add(BlockInfo("TRAP", lineNumber))
                }
                trimmed.matches(Regex("^ENDTRAP\\s*.*", RegexOption.IGNORE_CASE)) -> {
                    if (blockStack.isEmpty()) {
                        errors.add(SyntaxError(lineNumber, "ENDTRAP at line $lineNumber without any open block - missing TRAP declaration"))
                    } else if (blockStack.last().type != "TRAP") {
                        val openBlock = blockStack.last()
                        errors.add(SyntaxError(lineNumber, "ENDTRAP at line $lineNumber does not match open ${openBlock.type} block at line ${openBlock.lineNumber} - expected END${openBlock.type}"))
                    } else {
                        blockStack.removeAt(blockStack.size - 1)
                    }
                }
                
                // Check IF/ENDIF matching
                trimmed.matches(Regex("^IF\\s+.+\\s+THEN\\s*.*", RegexOption.IGNORE_CASE)) -> {
                    blockStack.add(BlockInfo("IF", lineNumber))
                }
                trimmed.matches(Regex("^ENDIF\\s*.*", RegexOption.IGNORE_CASE)) -> {
                    if (blockStack.isEmpty()) {
                        errors.add(SyntaxError(lineNumber, "ENDIF at line $lineNumber without any open block - missing IF statement"))
                    } else if (blockStack.last().type != "IF") {
                        val openBlock = blockStack.last()
                        errors.add(SyntaxError(lineNumber, "ENDIF at line $lineNumber does not match open ${openBlock.type} block at line ${openBlock.lineNumber} - expected END${openBlock.type}"))
                    } else {
                        blockStack.removeAt(blockStack.size - 1)
                    }
                }
                
                // Check FOR/ENDFOR matching
                trimmed.matches(Regex("^FOR\\s+.+\\s+(FROM\\s+.+\\s+)?TO\\s+.+\\s+DO\\s*.*", RegexOption.IGNORE_CASE)) -> {
                    blockStack.add(BlockInfo("FOR", lineNumber))
                }
                trimmed.matches(Regex("^ENDFOR\\s*.*", RegexOption.IGNORE_CASE)) -> {
                    if (blockStack.isEmpty()) {
                        errors.add(SyntaxError(lineNumber, "ENDFOR at line $lineNumber without any open block - missing FOR loop"))
                    } else if (blockStack.last().type != "FOR") {
                        val openBlock = blockStack.last()
                        errors.add(SyntaxError(lineNumber, "ENDFOR at line $lineNumber does not match open ${openBlock.type} block at line ${openBlock.lineNumber} - expected END${openBlock.type}"))
                    } else {
                        blockStack.removeAt(blockStack.size - 1)
                    }
                }
                
                // Check WHILE/ENDWHILE matching
                trimmed.matches(Regex("^WHILE\\s+.+\\s+DO\\s*.*", RegexOption.IGNORE_CASE)) -> {
                    blockStack.add(BlockInfo("WHILE", lineNumber))
                }
                trimmed.matches(Regex("^ENDWHILE\\s*.*", RegexOption.IGNORE_CASE)) -> {
                    if (blockStack.isEmpty()) {
                        errors.add(SyntaxError(lineNumber, "ENDWHILE at line $lineNumber without any open block - missing WHILE loop"))
                    } else if (blockStack.last().type != "WHILE") {
                        val openBlock = blockStack.last()
                        errors.add(SyntaxError(lineNumber, "ENDWHILE at line $lineNumber does not match open ${openBlock.type} block at line ${openBlock.lineNumber} - expected END${openBlock.type}"))
                    } else {
                        blockStack.removeAt(blockStack.size - 1)
                    }
                }
                
                // Check TEST/ENDTEST matching
                trimmed.matches(Regex("^TEST\\s+.+", RegexOption.IGNORE_CASE)) -> {
                    blockStack.add(BlockInfo("TEST", lineNumber))
                }
                trimmed.matches(Regex("^ENDTEST\\s*.*", RegexOption.IGNORE_CASE)) -> {
                    if (blockStack.isEmpty()) {
                        errors.add(SyntaxError(lineNumber, "ENDTEST at line $lineNumber without any open block - missing TEST statement"))
                    } else if (blockStack.last().type != "TEST") {
                        val openBlock = blockStack.last()
                        errors.add(SyntaxError(lineNumber, "ENDTEST at line $lineNumber does not match open ${openBlock.type} block at line ${openBlock.lineNumber} - expected END${openBlock.type}"))
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
                            errors.add(SyntaxError(lineNumber, "Invalid variable name '$cleanVarName' at line $lineNumber"))
                        }
                    }
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
}

data class SyntaxError(val lineNumber: Int, val message: String)
