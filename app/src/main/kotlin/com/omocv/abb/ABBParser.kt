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
                            variables = moduleVariables.toList()
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
            
            if (trimmed.isEmpty() || trimmed.startsWith("!")) {
                return@forEachIndexed
            }
            
            // Check MODULE/ENDMODULE matching
            when {
                trimmed.matches(Regex("^MODULE\\s+\\w+.*", RegexOption.IGNORE_CASE)) -> {
                    moduleCount++
                    blockStack.add(BlockInfo("MODULE", lineNumber))
                }
                // Check for malformed MODULE keywords
                trimmed.matches(Regex("^MODUL[EL]?\\s+\\w+.*", RegexOption.IGNORE_CASE)) && 
                !trimmed.matches(Regex("^MODULE\\s+\\w+.*", RegexOption.IGNORE_CASE)) -> {
                    errors.add(SyntaxError(lineNumber, "Invalid MODULE syntax - should be 'MODULE <name>'"))
                }
                trimmed.matches(Regex("^ENDMODULE\\s*$", RegexOption.IGNORE_CASE)) -> {
                    endModuleCount++
                    if (blockStack.isEmpty() || blockStack.last().type != "MODULE") {
                        errors.add(SyntaxError(lineNumber, "ENDMODULE without matching MODULE"))
                    } else {
                        blockStack.removeAt(blockStack.size - 1)
                    }
                }
                // Check for malformed ENDMODULE keywords
                trimmed.matches(Regex("^ENDMODUL[EL]?\\s*$", RegexOption.IGNORE_CASE)) && 
                !trimmed.matches(Regex("^ENDMODULE\\s*$", RegexOption.IGNORE_CASE)) -> {
                    errors.add(SyntaxError(lineNumber, "Invalid ENDMODULE syntax - should be 'ENDMODULE'"))
                }
                
                // Check PROC/ENDPROC matching
                trimmed.matches(Regex("^PROC\\s+\\w+.*", RegexOption.IGNORE_CASE)) -> {
                    blockStack.add(BlockInfo("PROC", lineNumber))
                }
                // Check for malformed PROC keywords
                trimmed.matches(Regex("^PRO[C]?\\s+\\w+.*", RegexOption.IGNORE_CASE)) && 
                !trimmed.matches(Regex("^PROC\\s+\\w+.*", RegexOption.IGNORE_CASE)) -> {
                    errors.add(SyntaxError(lineNumber, "Invalid PROC syntax - should be 'PROC <name>'"))
                }
                trimmed.matches(Regex("^ENDPROC\\s*$", RegexOption.IGNORE_CASE)) -> {
                    if (blockStack.isEmpty() || blockStack.last().type != "PROC") {
                        errors.add(SyntaxError(lineNumber, "ENDPROC without matching PROC"))
                    } else {
                        blockStack.removeAt(blockStack.size - 1)
                    }
                }
                // Check for malformed ENDPROC keywords
                trimmed.matches(Regex("^ENDPRO[C]?\\s*$", RegexOption.IGNORE_CASE)) && 
                !trimmed.matches(Regex("^ENDPROC\\s*$", RegexOption.IGNORE_CASE)) -> {
                    errors.add(SyntaxError(lineNumber, "Invalid ENDPROC syntax - should be 'ENDPROC'"))
                }
                
                // Check FUNC/ENDFUNC matching
                trimmed.matches(Regex("^FUNC\\s+\\w+\\s+\\w+.*", RegexOption.IGNORE_CASE)) -> {
                    blockStack.add(BlockInfo("FUNC", lineNumber))
                }
                // Check for malformed FUNC keywords
                trimmed.matches(Regex("^FUN[C]?\\s+\\w+.*", RegexOption.IGNORE_CASE)) && 
                !trimmed.matches(Regex("^FUNC\\s+\\w+\\s+\\w+.*", RegexOption.IGNORE_CASE)) -> {
                    errors.add(SyntaxError(lineNumber, "Invalid FUNC syntax - should be 'FUNC <returnType> <name>'"))
                }
                trimmed.matches(Regex("^ENDFUNC\\s*$", RegexOption.IGNORE_CASE)) -> {
                    if (blockStack.isEmpty() || blockStack.last().type != "FUNC") {
                        errors.add(SyntaxError(lineNumber, "ENDFUNC without matching FUNC"))
                    } else {
                        blockStack.removeAt(blockStack.size - 1)
                    }
                }
                // Check for malformed ENDFUNC keywords
                trimmed.matches(Regex("^ENDFUN[C]?\\s*$", RegexOption.IGNORE_CASE)) && 
                !trimmed.matches(Regex("^ENDFUNC\\s*$", RegexOption.IGNORE_CASE)) -> {
                    errors.add(SyntaxError(lineNumber, "Invalid ENDFUNC syntax - should be 'ENDFUNC'"))
                }
                
                // Check TRAP/ENDTRAP matching
                trimmed.matches(Regex("^TRAP\\s+\\w+.*", RegexOption.IGNORE_CASE)) -> {
                    blockStack.add(BlockInfo("TRAP", lineNumber))
                }
                // Check for malformed TRAP keywords
                trimmed.matches(Regex("^TRA[P]?\\s+\\w+.*", RegexOption.IGNORE_CASE)) && 
                !trimmed.matches(Regex("^TRAP\\s+\\w+.*", RegexOption.IGNORE_CASE)) -> {
                    errors.add(SyntaxError(lineNumber, "Invalid TRAP syntax - should be 'TRAP <name>'"))
                }
                trimmed.matches(Regex("^ENDTRAP\\s*$", RegexOption.IGNORE_CASE)) -> {
                    if (blockStack.isEmpty() || blockStack.last().type != "TRAP") {
                        errors.add(SyntaxError(lineNumber, "ENDTRAP without matching TRAP"))
                    } else {
                        blockStack.removeAt(blockStack.size - 1)
                    }
                }
                // Check for malformed ENDTRAP keywords
                trimmed.matches(Regex("^ENDTRA[P]?\\s*$", RegexOption.IGNORE_CASE)) && 
                !trimmed.matches(Regex("^ENDTRAP\\s*$", RegexOption.IGNORE_CASE)) -> {
                    errors.add(SyntaxError(lineNumber, "Invalid ENDTRAP syntax - should be 'ENDTRAP'"))
                }
                
                // Check IF/ENDIF matching
                trimmed.matches(Regex("^IF\\s+.+\\s+THEN\\s*$", RegexOption.IGNORE_CASE)) -> {
                    blockStack.add(BlockInfo("IF", lineNumber))
                }
                trimmed.matches(Regex("^ENDIF\\s*$", RegexOption.IGNORE_CASE)) -> {
                    if (blockStack.isEmpty() || blockStack.last().type != "IF") {
                        errors.add(SyntaxError(lineNumber, "ENDIF without matching IF"))
                    } else {
                        blockStack.removeAt(blockStack.size - 1)
                    }
                }
                
                // Check FOR/ENDFOR matching
                trimmed.matches(Regex("^FOR\\s+.+\\s+FROM\\s+.+\\s+TO\\s+.+\\s+DO\\s*$", RegexOption.IGNORE_CASE)) -> {
                    blockStack.add(BlockInfo("FOR", lineNumber))
                }
                trimmed.matches(Regex("^ENDFOR\\s*$", RegexOption.IGNORE_CASE)) -> {
                    if (blockStack.isEmpty() || blockStack.last().type != "FOR") {
                        errors.add(SyntaxError(lineNumber, "ENDFOR without matching FOR"))
                    } else {
                        blockStack.removeAt(blockStack.size - 1)
                    }
                }
                
                // Check WHILE/ENDWHILE matching
                trimmed.matches(Regex("^WHILE\\s+.+\\s+DO\\s*$", RegexOption.IGNORE_CASE)) -> {
                    blockStack.add(BlockInfo("WHILE", lineNumber))
                }
                trimmed.matches(Regex("^ENDWHILE\\s*$", RegexOption.IGNORE_CASE)) -> {
                    if (blockStack.isEmpty() || blockStack.last().type != "WHILE") {
                        errors.add(SyntaxError(lineNumber, "ENDWHILE without matching WHILE"))
                    } else {
                        blockStack.removeAt(blockStack.size - 1)
                    }
                }
                
                // Check TEST/ENDTEST matching
                trimmed.matches(Regex("^TEST\\s+.+$", RegexOption.IGNORE_CASE)) -> {
                    blockStack.add(BlockInfo("TEST", lineNumber))
                }
                trimmed.matches(Regex("^ENDTEST\\s*$", RegexOption.IGNORE_CASE)) -> {
                    if (blockStack.isEmpty() || blockStack.last().type != "TEST") {
                        errors.add(SyntaxError(lineNumber, "ENDTEST without matching TEST"))
                    } else {
                        blockStack.removeAt(blockStack.size - 1)
                    }
                }
            }
            
            // Check for invalid syntax patterns
            if (trimmed.contains(":=")) {
                // Check assignment statement has valid variable name
                val parts = trimmed.split(":=")
                if (parts.size >= 2) {
                    val varName = parts[0].trim().split(Regex("\\s+")).last()
                    if (!varName.matches(Regex("^[a-zA-Z_][a-zA-Z0-9_]*$"))) {
                        errors.add(SyntaxError(lineNumber, "Invalid variable name: $varName"))
                    }
                }
            }
        }
        
        // Check for unclosed blocks
        blockStack.forEach { block ->
            errors.add(SyntaxError(block.lineNumber, "Unclosed ${block.type} block"))
        }
        
        return errors
    }
    
    data class BlockInfo(val type: String, val lineNumber: Int)
}

data class SyntaxError(val lineNumber: Int, val message: String)
