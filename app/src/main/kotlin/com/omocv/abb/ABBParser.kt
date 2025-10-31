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
}
