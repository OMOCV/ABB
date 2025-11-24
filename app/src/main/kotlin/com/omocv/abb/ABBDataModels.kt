package com.omocv.abb

/**
 * Represents an ABB robot program module
 */
data class ABBModule(
    val name: String,
    val type: String,
    val routines: List<ABBRoutine> = emptyList(),
    val variables: List<String> = emptyList(),
    val startLine: Int = 0,
    val endLine: Int = 0
)

/**
 * Represents an ABB robot routine/procedure
 */
data class ABBRoutine(
    val name: String,
    val type: String, // PROC, FUNC, TRAP
    val parameters: List<String> = emptyList(),
    val localVariables: List<String> = emptyList(),
    val startLine: Int = 0,
    val endLine: Int = 0
)

/**
 * Represents a parsed ABB program file
 */
data class ABBProgramFile(
    val fileName: String,
    val fileType: String, // .mod, .prg, .sys, .pgf
    val content: String,
    val modules: List<ABBModule> = emptyList(),
    val routines: List<ABBRoutine> = emptyList()
)
