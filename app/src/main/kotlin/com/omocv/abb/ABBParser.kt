package com.omocv.abb

import com.omocv.abb.rapid.RapidCompiler
import com.omocv.abb.rapid.Diagnostic as RapidDiagnostic
import com.omocv.abb.rapid.Severity
import java.io.File

/**
 * Parser for ABB RAPID programming language files
 * Supports .mod (module), .prg (program), .sys (system), .pgf (program data) files
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
        
        // Common RAPID instructions that should be spell-checked
        val INSTRUCTIONS = setOf(
            // Motion instructions
            "MoveJ", "MoveL", "MoveC", "MoveAbsJ", "MoveLDO", "MoveJDO",
            "SearchL", "SearchC", "TriggJ", "TriggL", "TriggC",
            // I/O instructions
            "SetDO", "SetAO", "SetGO", "Set", "PulseDO", "Reset",
            "ClkReset", "ClkStart", "ClkStop", "PLength",
            "WaitDI", "WaitAI", "WaitTime", "WaitUntil",
            // Program flow
            "Stop", "Exit", "ErrWrite", "ErrRaise",
            // String/output
            "TPWrite", "TPReadNum", "TPReadFK", "TPReadDnum", "TPShow",
            "TPErase", "TPReadFK",
            // Math and utility
            "AccSet", "VelSet", "PathAccLim", "SingArea",
            "ConfL", "ConfJ", "ActUnit", "DeactUnit",
            // Common functions
            "Abs", "Round", "Trunc", "Sqrt", "Pow",
            "Sin", "Cos", "Tan", "ASin", "ACos", "ATan", "ATan2",
            "Exp", "Log", "Ln",
            "Distance", "DotProd", "NOrient", "OrientZYX",
            "PoseInv", "PoseMult", "PoseVect",
            "Present", "IsPers", "Dim"
        )
        
        /**
         * Calculate Levenshtein distance between two strings
         */
        private fun levenshteinDistance(s1: String, s2: String): Int {
            val len1 = s1.length
            val len2 = s2.length
            
            val dp = Array(len1 + 1) { IntArray(len2 + 1) }
            
            for (i in 0..len1) dp[i][0] = i
            for (j in 0..len2) dp[0][j] = j
            
            for (i in 1..len1) {
                for (j in 1..len2) {
                    val cost = if (s1[i - 1].equals(s2[j - 1], ignoreCase = true)) 0 else 1
                    dp[i][j] = minOf(
                        dp[i - 1][j] + 1,      // deletion
                        dp[i][j - 1] + 1,      // insertion
                        dp[i - 1][j - 1] + cost // substitution
                    )
                }
            }
            
            return dp[len1][len2]
        }
        
        /**
         * Find the closest matching keyword for a potentially misspelled word
         */
        private fun findClosestKeyword(word: String): Pair<String, Int>? {
            val allKnownWords = KEYWORDS + DATA_TYPES + INSTRUCTIONS
            
            // Only check words that start with uppercase or known instruction patterns
            if (word.isEmpty() || (!word[0].isUpperCase() && word[0] != word[0].lowercase()[0])) {
                return null
            }
            
            var closestMatch: String? = null
            var minDistance = Int.MAX_VALUE
            
            for (knownWord in allKnownWords) {
                // Calculate distance
                val distance = levenshteinDistance(word, knownWord)
                
                // Consider it a potential typo if:
                // 1. Distance is 1-3 (small typos)
                // 2. Or the word is a prefix of the known word (incomplete typing)
                // 3. Or the word shares at least 60% of characters with known word
                val isPrefix = knownWord.startsWith(word, ignoreCase = true) && word.length >= 3
                val similarity = 1.0 - (distance.toDouble() / maxOf(word.length, knownWord.length))
                
                if (isPrefix || (distance in 1..3 && similarity >= 0.5)) {
                    if (distance < minDistance) {
                        minDistance = distance
                        closestMatch = knownWord
                    }
                }
            }
            
            return if (closestMatch != null) Pair(closestMatch, minDistance) else null
        }
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

        val modulesWithRoutines = modules.map { module ->
            val containedRoutines = routines.filter { routine ->
                routine.startLine >= module.startLine && routine.endLine <= module.endLine
            }
            module.copy(routines = containedRoutines)
        }

        return ABBProgramFile(
            fileName = fileName,
            fileType = fileType,
            content = content,
            modules = modulesWithRoutines,
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
                    moduleStartLine = index + 1
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
                            endLine = index + 1
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

        if (currentModule != null) {
            // Gracefully close the last module if the file ended without ENDMODULE
            modules.add(
                ABBModule(
                    name = currentModule,
                    type = moduleType,
                    routines = moduleRoutines.toList(),
                    variables = moduleVariables.toList(),
                    startLine = moduleStartLine,
                    endLine = lines.size
                )
            )
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
            
            val procMatch = Regex("^\\s*(LOCAL\\s+)?PROC\\s+(\\w+)\\s*(\\([^)]*\\))?", RegexOption.IGNORE_CASE)
                .find(trimmedLine)
            val funcMatch = Regex("^\\s*(LOCAL\\s+)?FUNC\\s+\\w+\\s+(\\w+)\\s*(\\([^)]*\\))?", RegexOption.IGNORE_CASE)
                .find(trimmedLine)
            val trapMatch = Regex("^\\s*(LOCAL\\s+)?TRAP\\s+(\\w+)", RegexOption.IGNORE_CASE)
                .find(trimmedLine)

            when {
                procMatch != null -> {
                    currentRoutine = procMatch.groupValues[2]
                    routineType = "PROC"
                    startLine = index + 1
                    parameters.clear()
                    localVars.clear()

                    val paramGroup = procMatch.groupValues.getOrElse(3) { "" }
                    if (paramGroup.contains("(")) {
                        val paramStr = paramGroup.substringAfter("(").substringBefore(")")
                        if (paramStr.isNotBlank()) {
                            parameters.addAll(paramStr.split(",").map { it.trim() })
                        }
                    }
                }

                funcMatch != null -> {
                    currentRoutine = funcMatch.groupValues[2]
                    routineType = "FUNC"
                    startLine = index + 1
                    parameters.clear()
                    localVars.clear()

                    val paramGroup = funcMatch.groupValues.getOrElse(3) { "" }
                    if (paramGroup.contains("(")) {
                        val paramStr = paramGroup.substringAfter("(").substringBefore(")")
                        if (paramStr.isNotBlank()) {
                            parameters.addAll(paramStr.split(",").map { it.trim() })
                        }
                    }
                }

                trapMatch != null -> {
                    currentRoutine = trapMatch.groupValues[2]
                    routineType = "TRAP"
                    startLine = index + 1
                    parameters.clear()
                    localVars.clear()
                }

                // Check for local variables in routine
                currentRoutine != null && trimmedLine.startsWith("VAR", ignoreCase = true) -> {
                    localVars.add(trimmedLine)
                }

                // Check for ENDPROC, ENDFUNC, ENDTRAP
                trimmedLine.startsWith("ENDPROC", ignoreCase = true) ||
                        trimmedLine.startsWith("ENDFUNC", ignoreCase = true) ||
                        trimmedLine.startsWith("ENDTRAP", ignoreCase = true) -> {
                    if (currentRoutine != null) {
                        routines.add(
                            ABBRoutine(
                                name = currentRoutine,
                                type = routineType,
                                parameters = parameters.toList(),
                                localVariables = localVars.toList(),
                                startLine = startLine,
                                endLine = index + 1
                            )
                        )
                        currentRoutine = null
                    }
                }
            }
        }

        if (currentRoutine != null) {
            // Close any unterminated routine at EOF so it still appears in selectors
            routines.add(
                ABBRoutine(
                    name = currentRoutine,
                    type = routineType,
                    parameters = parameters.toList(),
                    localVariables = localVars.toList(),
                    startLine = startLine,
                    endLine = lines.size
                )
            )
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
     * Enhanced syntax validation using the RapidCompiler with full AST parsing
     * This provides more accurate and comprehensive syntax checking
     */
    fun validateSyntaxEnhanced(content: String): List<SyntaxError> {
        val result = RapidCompiler.analyze(content)
        return result.diagnostics.map { diagnostic ->
            SyntaxError(
                lineNumber = diagnostic.span.startLine,
                message = diagnostic.message,
                columnStart = diagnostic.span.startCol - 1, // Convert to 0-based
                columnEnd = diagnostic.span.endCol - 1      // Convert to 0-based
            )
        }
    }

    /**
     * Comprehensive syntax validation that combines multiple analysis methods
     * This function analyzes EVERY LINE and detects ALL possible syntax errors by:
     * 1. Using RapidCompiler for full AST-based parsing and semantic analysis
     * 2. Using line-by-line analysis for patterns not caught by the compiler
     * 3. Detecting incomplete keywords and instructions
     * 
     * This approach ensures thorough error detection as requested in the problem statement:
     * "对于检查语法功能，你要对每一行进行解读分析，排除每一行所有的可能语法错误"
     */
    fun validateSyntaxComprehensive(content: String): List<SyntaxError> {
        // Collect errors from both validation methods
        val compilerErrors = validateSyntaxEnhanced(content)
        val lineByLineErrors = validateSyntax(content)
        
        // Combine and deduplicate errors
        val allErrors = mutableListOf<SyntaxError>()
        val errorSignatures = mutableSetOf<String>()
        
        // Add compiler errors first (they tend to be more accurate)
        for (error in compilerErrors) {
            val signature = "${error.lineNumber}:${error.columnStart}:${error.message.take(50)}"
            if (errorSignatures.add(signature)) {
                allErrors.add(error)
            }
        }
        
        // Add line-by-line errors that aren't duplicates
        for (error in lineByLineErrors) {
            val signature = "${error.lineNumber}:${error.columnStart}:${error.message.take(50)}"
            if (errorSignatures.add(signature)) {
                allErrors.add(error)
            }
        }
        
        val lines = content.lines()

        val filteredErrors = allErrors.filterNot { error ->
            val line = lines.getOrNull(error.lineNumber - 1) ?: return@filterNot false
            shouldIgnoreBalancedBracketError(line, error)
        }

        // Sort by line number, then column
        return filteredErrors.sortedWith(
            compareBy<SyntaxError> { it.lineNumber }
                .thenBy { it.columnStart }
        )
    }

    private fun shouldIgnoreBalancedBracketError(line: String, error: SyntaxError): Boolean {
        val openIndex = line.indexOf('[')
        val closeIndex = line.lastIndexOf(']')

        if (openIndex == -1 || closeIndex == -1 || closeIndex < openIndex) return false

        val bracketBalanced = line.count { it == '[' } == line.count { it == ']' }
        val spanInsideBracket = error.columnStart in openIndex..closeIndex
        val looksLikeArrayLiteral = line.contains(":=") && Regex("\\[[^\\]]*\\]").containsMatchIn(line)

        return bracketBalanced && spanInsideBracket && looksLikeArrayLiteral
    }

    /**
     * Comprehensive syntax validation for RAPID code with Chinese error messages
     * Note: Consider using validateSyntaxEnhanced() for more accurate checking
     */
    fun validateSyntax(content: String): List<SyntaxError> {
        val errors = mutableListOf<SyntaxError>()
        val lines = content.lines()
        val blockStack = mutableListOf<BlockInfo>()
        val potentialBlocks = mutableListOf<PotentialBlock>()
        
        lines.forEachIndexed { index, line ->
            val lineNumber = index + 1
            val trimmed = line.trim()
            
            // Skip empty lines and comments
            if (trimmed.isEmpty() || trimmed.startsWith("!")) {
                return@forEachIndexed
            }
            
            // Check for unclosed strings (highest priority)
            checkUnclosedStrings(line, lineNumber, errors)?.let {
                return@forEachIndexed // Skip further checks on this line
            }
            
            // Check for unmatched parentheses, brackets, and braces
            checkUnmatchedDelimiters(line, lineNumber, errors)
            
            // Check block structure matching
            checkBlockStructure(trimmed, line, lineNumber, blockStack, potentialBlocks, errors)
            
            // Check for invalid syntax patterns
            checkAssignmentStatements(trimmed, line, lineNumber, errors)
            checkControlStructures(trimmed, line, lineNumber, errors)
            checkDeclarations(trimmed, line, lineNumber, errors)
            checkReturnStatements(trimmed, lineNumber, blockStack, errors)
            checkIncompleteFunctionCalls(trimmed, lineNumber, errors)
            
            // Check for misspelled/incomplete keywords and instructions
            checkIncompleteKeywords(line, lineNumber, errors)
        }
        
        // Check for unclosed blocks
        checkUnclosedBlocks(blockStack, errors)
        
        return errors
    }
    
    /**
     * Check for unclosed strings in a line
     * Returns non-null if string error was found and further checks should be skipped
     */
    private fun checkUnclosedStrings(line: String, lineNumber: Int, errors: MutableList<SyntaxError>): Boolean? {
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
                return true // Skip further checks on this line
            }
        }
        return null
    }
    
    /**
     * Check for unmatched parentheses, brackets, and braces
     */
    private fun checkUnmatchedDelimiters(line: String, lineNumber: Int, errors: MutableList<SyntaxError>) {
        val lineWithoutStringsAndComments = removeStringsAndComments(line)
        
        // Check parentheses
        val parenCount = lineWithoutStringsAndComments.count { it == '(' } - lineWithoutStringsAndComments.count { it == ')' }
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
        
        // Check brackets
        val bracketCount = lineWithoutStringsAndComments.count { it == '[' } - lineWithoutStringsAndComments.count { it == ']' }
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
        
        // Check braces
        val braceCount = lineWithoutStringsAndComments.count { it == '{' } - lineWithoutStringsAndComments.count { it == '}' }
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
    }
    
    /**
     * Check block structure matching (MODULE, PROC, FUNC, TRAP, IF, FOR, WHILE, TEST)
     */
    private fun checkBlockStructure(
        trimmed: String,
        line: String,
        lineNumber: Int,
        blockStack: MutableList<BlockInfo>,
        potentialBlocks: MutableList<PotentialBlock>,
        errors: MutableList<SyntaxError>
    ) {
        var matchedBlockKeyword = false
        when {
            // MODULE/ENDMODULE
            trimmed.matches(Regex("^MODULE\\s+\\w+.*", RegexOption.IGNORE_CASE)) -> {
                blockStack.add(BlockInfo("MODULE", lineNumber))
                matchedBlockKeyword = true
            }
            trimmed.matches(Regex("^ENDMODULE\\s*.*", RegexOption.IGNORE_CASE)) -> {
                checkEndBlock("ENDMODULE", line, lineNumber, blockStack, potentialBlocks, errors, "MODULE")
                matchedBlockKeyword = true
            }
            
            // PROC/ENDPROC
            trimmed.matches(Regex("^PROC\\s+\\w+.*", RegexOption.IGNORE_CASE)) -> {
                blockStack.add(BlockInfo("PROC", lineNumber))
                matchedBlockKeyword = true
            }
            trimmed.matches(Regex("^ENDPROC\\s*.*", RegexOption.IGNORE_CASE)) -> {
                checkEndBlock("ENDPROC", line, lineNumber, blockStack, potentialBlocks, errors, "PROC")
                matchedBlockKeyword = true
            }
            
            // FUNC/ENDFUNC
            trimmed.matches(Regex("^FUNC\\s+\\w+\\s+\\w+.*", RegexOption.IGNORE_CASE)) -> {
                blockStack.add(BlockInfo("FUNC", lineNumber))
                matchedBlockKeyword = true
            }
            trimmed.matches(Regex("^ENDFUNC\\s*.*", RegexOption.IGNORE_CASE)) -> {
                checkEndBlock("ENDFUNC", line, lineNumber, blockStack, potentialBlocks, errors, "FUNC")
                matchedBlockKeyword = true
            }
            
            // TRAP/ENDTRAP
            trimmed.matches(Regex("^TRAP\\s+\\w+.*", RegexOption.IGNORE_CASE)) -> {
                blockStack.add(BlockInfo("TRAP", lineNumber))
                matchedBlockKeyword = true
            }
            trimmed.matches(Regex("^ENDTRAP\\s*.*", RegexOption.IGNORE_CASE)) -> {
                checkEndBlock("ENDTRAP", line, lineNumber, blockStack, potentialBlocks, errors, "TRAP")
                matchedBlockKeyword = true
            }
            
            // IF/ENDIF
            trimmed.matches(Regex("^IF\\s+.+\\s+THEN\\s*.*", RegexOption.IGNORE_CASE)) -> {
                blockStack.add(BlockInfo("IF", lineNumber))
                matchedBlockKeyword = true
            }
            trimmed.matches(Regex("^ENDIF\\s*.*", RegexOption.IGNORE_CASE)) -> {
                checkEndBlock("ENDIF", line, lineNumber, blockStack, potentialBlocks, errors, "IF")
                matchedBlockKeyword = true
            }
            
            // FOR/ENDFOR
            trimmed.matches(Regex("^FOR\\s+.+\\s+(FROM\\s+.+\\s+)?TO\\s+.+\\s+DO\\s*.*", RegexOption.IGNORE_CASE)) -> {
                blockStack.add(BlockInfo("FOR", lineNumber))
                matchedBlockKeyword = true
            }
            trimmed.matches(Regex("^ENDFOR\\s*.*", RegexOption.IGNORE_CASE)) -> {
                checkEndBlock("ENDFOR", line, lineNumber, blockStack, potentialBlocks, errors, "FOR")
                matchedBlockKeyword = true
            }
            
            // WHILE/ENDWHILE
            trimmed.matches(Regex("^WHILE\\s+.+\\s+DO\\s*.*", RegexOption.IGNORE_CASE)) -> {
                blockStack.add(BlockInfo("WHILE", lineNumber))
                matchedBlockKeyword = true
            }
            trimmed.matches(Regex("^ENDWHILE\\s*.*", RegexOption.IGNORE_CASE)) -> {
                checkEndBlock("ENDWHILE", line, lineNumber, blockStack, potentialBlocks, errors, "WHILE")
                matchedBlockKeyword = true
            }
            
            // TEST/ENDTEST
            trimmed.matches(Regex("^TEST\\s+.+", RegexOption.IGNORE_CASE)) -> {
                blockStack.add(BlockInfo("TEST", lineNumber))
                matchedBlockKeyword = true
            }
            trimmed.matches(Regex("^ENDTEST\\s*.*", RegexOption.IGNORE_CASE)) -> {
                checkEndBlock("ENDTEST", line, lineNumber, blockStack, potentialBlocks, errors, "TEST")
                matchedBlockKeyword = true
            }
        }

        if (!matchedBlockKeyword) {
            recordPotentialBlockStart(trimmed, lineNumber, potentialBlocks)
        }
    }
    
    private fun recordPotentialBlockStart(trimmed: String, lineNumber: Int, potentialBlocks: MutableList<PotentialBlock>) {
        val firstWord = trimmed.split(Regex("\\s+")).firstOrNull() ?: return
        val blockKeywords = listOf("MODULE", "PROC", "FUNC", "TRAP")
        val upperWord = firstWord.uppercase()

        if (blockKeywords.any { it.equals(upperWord, ignoreCase = true) }) return

        val closest = blockKeywords
            .map { keyword -> keyword to levenshteinDistance(upperWord, keyword) }
            .minByOrNull { it.second }
            ?: return

        val (keyword, distance) = closest
        val looksLikeDeclaration = trimmed.contains(Regex("^\\w+\\s+\\w+"))
        val resemblesBlock = when (keyword) {
            "PROC", "FUNC" -> looksLikeDeclaration && trimmed.contains("(")
            else -> looksLikeDeclaration
        }

        if (resemblesBlock && distance in 1..2) {
            potentialBlocks.add(PotentialBlock(keyword, lineNumber, firstWord))
        }
    }

    /**
     * Helper function to check END* block matching
     */
    private fun checkEndBlock(
        endKeyword: String,
        line: String,
        lineNumber: Int,
        blockStack: MutableList<BlockInfo>,
        potentialBlocks: MutableList<PotentialBlock>,
        errors: MutableList<SyntaxError>,
        expectedType: String
    ) {
        val columnStart = line.indexOf(endKeyword, ignoreCase = true).takeIf { it >= 0 } ?: 0
        val columnEnd = columnStart + endKeyword.length

        val startKeyword = when (expectedType) {
            "MODULE" -> "MODULE 模块名"
            "PROC" -> "PROC 过程名()"
            "FUNC" -> "FUNC 返回类型 函数名()"
            "TRAP" -> "TRAP 陷阱名"
            "IF" -> "IF 条件 THEN"
            "FOR" -> "FOR 变量 FROM 起始值 TO 结束值 DO"
            "WHILE" -> "WHILE 条件 DO"
            "TEST" -> "TEST 表达式"
            else -> expectedType
        }

        if (blockStack.isEmpty()) {
            errors.add(SyntaxError(
                lineNumber,
                "第 $lineNumber 行，第 ${columnStart + 1} 列：$endKeyword 没有对应的开始块 - 缺少 $expectedType 声明\n建议：在此之前添加 $startKeyword",
                columnStart,
                columnEnd
            ))
        } else {
            val matchingIndex = blockStack.indexOfLast { it.type == expectedType }

            if (matchingIndex == -1) {
                val relatedTypo = potentialBlocks.lastOrNull { it.type == expectedType && it.lineNumber < lineNumber }
                if (relatedTypo != null) {
                    errors.add(
                        SyntaxError(
                            lineNumber,
                            "第 $lineNumber 行，第 ${columnStart + 1} 列：$endKeyword 与第 ${relatedTypo.lineNumber} 行的 ${relatedTypo.keyword} 不匹配 - 该行可能是 ${expectedType} 声明的拼写错误\n建议：将第 ${relatedTypo.lineNumber} 行的关键字更正为 ${expectedType}，或调整此处的结束关键字以匹配实际的开始块",
                            columnStart,
                            columnEnd
                        )
                    )
                } else {
                    errors.add(
                        SyntaxError(
                            lineNumber,
                            "第 $lineNumber 行，第 ${columnStart + 1} 列：$endKeyword 没有对应的开始块 - 缺少 ${expectedType} 声明或其关键字可能存在拼写错误\n建议：在此之前添加 ${startKeyword}，并确保使用匹配的结束关键字",
                            columnStart,
                            columnEnd
                        )
                    )
                }
                return
            }

            // If there are unclosed inner blocks above the expected type, report them first
            for (i in blockStack.size - 1 downTo matchingIndex + 1) {
                val unclosedBlock = blockStack.removeAt(i)
                errors.add(
                    SyntaxError(
                        unclosedBlock.lineNumber,
                        "第 ${unclosedBlock.lineNumber} 行：${unclosedBlock.type} 代码块未正确闭合 - 在 $endKeyword 之前缺少 END${unclosedBlock.type}\n建议：在 $endKeyword 之前添加 END${unclosedBlock.type}",
                        0,
                        0
                    )
                )
            }

            blockStack.removeAt(matchingIndex)
        }
    }
    
    /**
     * Check assignment statements for completeness and validity
     */
    private fun checkAssignmentStatements(trimmed: String, line: String, lineNumber: Int, errors: MutableList<SyntaxError>) {
        if (!trimmed.contains(":=")) return
        
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
    
    /**
     * Check control structures (IF, WHILE, FOR) for required keywords
     */
    private fun checkControlStructures(trimmed: String, line: String, lineNumber: Int, errors: MutableList<SyntaxError>) {
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
            val sanitizedLine = removeStringsAndComments(line)
            val whileContent = Regex("^\\s*WHILE\\s+(.+)$", RegexOption.IGNORE_CASE).find(sanitizedLine)?.groupValues?.getOrNull(1)
            val possibleDo = whileContent
                ?.split(Regex("\\s+"))
                ?.filter { it.isNotEmpty() }
                ?.lastOrNull()

            val typoColumn = possibleDo?.let { line.indexOf(it) } ?: -1
            if (possibleDo != null && !possibleDo.equals("DO", ignoreCase = true) &&
                levenshteinDistance(possibleDo.uppercase(), "DO") in 1..2 && typoColumn >= 0) {
                errors.add(SyntaxError(
                    lineNumber,
                    "第 $lineNumber 行，第 ${typoColumn + 1} 列：WHILE 语句中的 DO 关键字可能拼写错误（发现 '$possibleDo'）\n建议：将该关键字更正为 DO，格式：WHILE 条件 DO",
                    typoColumn,
                    typoColumn + possibleDo.length
                ))
            } else {
                val columnStart = line.indexOf("WHILE", ignoreCase = true)
                errors.add(SyntaxError(
                    lineNumber,
                    "第 $lineNumber 行，第 ${columnStart + 1} 列：WHILE 循环缺少 DO 关键字\n建议：在条件表达式后添加 DO，格式：WHILE 条件 DO",
                    columnStart,
                    line.length
                ))
            }
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
    }
    
    /**
     * Check declarations (PROC, FUNC, TRAP, MODULE, VAR, PERS, CONST) for completeness
     */
    private fun checkDeclarations(trimmed: String, line: String, lineNumber: Int, errors: MutableList<SyntaxError>) {
        // Check for incomplete PROC declaration
        if (trimmed.matches(Regex("^PROC\\s*$", RegexOption.IGNORE_CASE))) {
            val columnStart = line.indexOf("PROC", ignoreCase = true)
            errors.add(SyntaxError(
                lineNumber,
                "第 $lineNumber 行，第 ${columnStart + 1} 列：PROC 声明不完整 - 缺少过程名称\n建议：添加过程名称和参数，格式：PROC 过程名(参数列表)",
                columnStart,
                line.length
            ))
        }
        
        // Check for incomplete FUNC declaration
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
        
        // Check for incomplete TRAP declaration
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
    }
    
    /**
     * Check RETURN statements for proper usage (FUNC must return a value)
     */
    private fun checkReturnStatements(
        trimmed: String, 
        lineNumber: Int, 
        blockStack: MutableList<BlockInfo>, 
        errors: MutableList<SyntaxError>
    ) {
        if (trimmed.matches(Regex("^RETURN\\s*$", RegexOption.IGNORE_CASE))) {
            val columnStart = trimmed.indexOf("RETURN", ignoreCase = true)
            // Check if we're in a FUNC block (should return a value)
            val inFunc = blockStack.any { it.type == "FUNC" }
            if (inFunc) {
                errors.add(SyntaxError(
                    lineNumber,
                    "第 $lineNumber 行，第 ${columnStart + 1} 列：RETURN 语句缺少返回值 - FUNC 函数必须返回一个值\n建议：在 RETURN 后添加返回值，格式：RETURN 表达式",
                    columnStart,
                    trimmed.length
                ))
            }
        }
    }
    
    /**
     * Check for incomplete function calls
     */
    private fun checkIncompleteFunctionCalls(trimmed: String, lineNumber: Int, errors: MutableList<SyntaxError>) {
        if (trimmed.matches(Regex(".*\\w+\\s*\\(\\s*$"))) {
            val openParenPos = trimmed.lastIndexOf('(')
            if (openParenPos > 0) {
                errors.add(SyntaxError(
                    lineNumber,
                    "第 $lineNumber 行，第 ${openParenPos + 1} 列：函数调用不完整 - 参数列表和右括号缺失\n建议：补充参数并闭合括号，格式：函数名(参数1, 参数2)",
                    openParenPos,
                    trimmed.length
                ))
            }
        }
    }
    
    /**
     * Check for unclosed blocks and add errors
     */
    private fun checkUnclosedBlocks(blockStack: MutableList<BlockInfo>, errors: MutableList<SyntaxError>) {
        blockStack.forEach { block ->
            errors.add(SyntaxError(
                block.lineNumber, 
                "第 ${block.lineNumber} 行：${block.type} 代码块未闭合 - 缺少 END${block.type}\n建议：在代码块结束位置添加 END${block.type}"
            ))
        }
    }
    
    /**
     * Check for incomplete or misspelled keywords and instructions
     * This addresses the issue where incomplete keywords like "VA" (should be "VAR"),
     * "WaitTim" (should be "WaitTime"), etc. are not detected
     * 
     * IMPORTANT: This function now checks context to avoid flagging user-defined names
     * (procedure names, variable names, etc.) as errors. It only checks identifiers that
     * appear in positions where keywords/instructions are expected.
     */
    private fun checkIncompleteKeywords(line: String, lineNumber: Int, errors: MutableList<SyntaxError>) {
        // Remove strings and comments first
        val cleanLine = removeStringsAndComments(line)
        val trimmed = cleanLine.trim()
        
        // Skip empty lines
        if (trimmed.isEmpty()) return
        
        // Tokenize the line into words
        val tokenRegex = Regex("\\b[a-zA-Z_][a-zA-Z0-9_]*\\b")
        val matches = tokenRegex.findAll(cleanLine).toList()
        
        if (matches.isEmpty()) return
        
        // Determine the context from the first token
        val firstWord = matches.first().value.uppercase()
        
        // Define positions where we should check vs. skip checking
        // For example, in "PROC Main()" we check PROC but not Main
        // In "VAR num counter;" we check VAR and num but not counter
        val positionsToCheck = mutableSetOf<Int>()
        
        when {
            // Declaration keywords: check position 0 (keyword) and position 1 (type if VAR/PERS/CONST)
            firstWord in setOf("VAR", "PERS", "CONST") -> {
                positionsToCheck.add(0) // The keyword itself (though it's already recognized)
                if (matches.size >= 2) {
                    positionsToCheck.add(1) // The data type
                }
                // Position 2 would be the variable name (user-defined, don't check)
            }
            // Procedure/function declarations: check position 0 (keyword) only, or position 1 for return type in FUNC
            firstWord in setOf("PROC", "TRAP") -> {
                positionsToCheck.add(0) // The keyword itself
                // Position 1 is the procedure/trap name (user-defined, don't check)
            }
            firstWord == "FUNC" -> {
                positionsToCheck.add(0) // The keyword itself
                if (matches.size >= 2) {
                    positionsToCheck.add(1) // The return type
                }
                // Position 2 is the function name (user-defined, don't check)
            }
            firstWord == "RECORD" -> {
                positionsToCheck.add(0) // The keyword itself
                // Position 1 is the record name (user-defined, don't check)
                // For RECORD field types, they appear after opening paren, harder to detect in simple line parsing
                // The RapidCompiler handles this better, so we'll rely on that
            }
            firstWord in setOf("MODULE", "ENDMODULE", "ENDPROC", "ENDFUNC", "ENDTRAP", "ENDTEST") -> {
                positionsToCheck.add(0) // Just check the keyword
                // Position 1 for MODULE is the module name (user-defined, don't check)
            }
            // Control structures: check the keywords and logical operators within the condition
            firstWord in setOf("IF", "ELSEIF", "WHILE", "FOR", "TEST", "RETURN") -> {
                positionsToCheck.add(0) // The keyword

                val logicalKeywords = setOf("AND", "OR", "XOR", "NOT", "THEN", "ELSEIF", "ELSE")
                matches.forEachIndexed { idx, match ->
                    if (idx == 0) return@forEachIndexed

                    val wordUpper = match.value.uppercase()
                    val isLogicalSlot = logicalKeywords.any { keyword ->
                        wordUpper == keyword ||
                            (keyword.startsWith(wordUpper) && wordUpper.length >= 2) ||
                            levenshteinDistance(wordUpper, keyword) == 1
                    }

                    if (isLogicalSlot) {
                        positionsToCheck.add(idx)
                    }
                }
            }
            // For statements starting with a potential instruction call (like WaitTime, TPWrite)
            // Check position 0 as it should be an instruction
            else -> {
                // This could be an instruction call or an assignment
                // Check the first word if it looks like an instruction (starts with uppercase)
                val firstToken = matches.first().value
                if (firstToken.isNotEmpty() && firstToken[0].isUpperCase()) {
                    positionsToCheck.add(0)
                }
            }
        }
        
        // Now check only the positions we identified
        for ((index, match) in matches.withIndex()) {
            if (index !in positionsToCheck) {
                continue // Skip user-defined names
            }
            
            val word = match.value
            val startPos = match.range.first
            
            // Skip if it's already a known keyword, instruction, or data type
            val allKnownWords = KEYWORDS + DATA_TYPES + INSTRUCTIONS
            if (allKnownWords.any { it.equals(word, ignoreCase = true) }) {
                continue
            }
            
            // Skip common variable patterns (lowercase words, mixed case that looks like variables)
            if (word.all { it.isLowerCase() || it == '_' || it.isDigit() }) {
                continue
            }
            
            // Skip if it looks like a number or hex value
            if (word.matches(Regex("^[0-9]+$")) || word.matches(Regex("^0x[0-9a-fA-F]+$"))) {
                continue
            }
            
            // Find closest matching keyword
            val closestMatch = findClosestKeyword(word)
            
            if (closestMatch != null) {
                val (suggestion, distance) = closestMatch
                
                // Determine if it's likely a typo
                val isLikelyTypo = when {
                    // If it's a prefix (incomplete word), it's very likely a typo
                    suggestion.startsWith(word, ignoreCase = true) && word.length >= 3 -> true
                    // If distance is 1-2, it's likely a simple typo
                    distance in 1..2 -> true
                    // If distance is 3 and words are similar enough
                    distance == 3 && word.length >= 5 -> true
                    else -> false
                }
                
                if (isLikelyTypo) {
                    val errorMsg = if (suggestion.startsWith(word, ignoreCase = true)) {
                        "第 $lineNumber 行，第 ${startPos + 1} 列：关键字或指令不完整 '$word'\n建议：可能是 '$suggestion'（缺少部分字母）"
                    } else {
                        "第 $lineNumber 行，第 ${startPos + 1} 列：可能存在拼写错误 '$word'\n建议：是否应该是 '$suggestion'？"
                    }
                    
                    errors.add(SyntaxError(
                        lineNumber,
                        errorMsg,
                        startPos,
                        startPos + word.length
                    ))
                }
            }
        }
    }
    
    data class BlockInfo(val type: String, val lineNumber: Int)
    data class PotentialBlock(val type: String, val lineNumber: Int, val keyword: String)
    
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
