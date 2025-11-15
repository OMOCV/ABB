package com.omocv.abb.rapid

/**
 * Example demonstrating the use of RapidCompiler for syntax checking
 * 演示如何使用 RapidCompiler 进行语法检查
 */
object RapidCompilerExample {

    /**
     * Example 1: Basic syntax checking
     * 示例 1：基本语法检查
     */
    fun example1_BasicSyntaxChecking() {
        val validCode = """
            MODULE TestModule
                VAR num counter;
                
                PROC Main()
                    counter := 10;
                    IF counter > 5 THEN
                        ! This is a comment
                    ENDIF
                ENDPROC
            ENDMODULE
        """.trimIndent()

        val result = RapidCompiler.analyze(validCode)
        println("=== Example 1: Valid Code ===")
        println("Diagnostics count: ${result.diagnostics.size}")
        result.diagnostics.forEach { diag ->
            println("Line ${diag.span.startLine}: ${diag.message}")
        }
        println()
    }

    /**
     * Example 2: Detecting syntax errors
     * 示例 2：检测语法错误
     */
    fun example2_DetectingSyntaxErrors() {
        val invalidCode = """
            MODULE TestModule
                VAR num counter
                
                PROC Main()
                    undefinedVar := 10;
                ENDPROC
            ENDMODULE
        """.trimIndent()

        val result = RapidCompiler.analyze(invalidCode)
        println("=== Example 2: Invalid Code ===")
        println("Diagnostics count: ${result.diagnostics.size}")
        result.diagnostics.forEach { diag ->
            println("[${diag.severity}] Line ${diag.span.startLine}: ${diag.message}")
        }
        println()
    }

    /**
     * Example 3: Getting the AST
     * 示例 3：获取抽象语法树
     */
    fun example3_AccessingAST() {
        val code = """
            MODULE TestModule
                VAR num x;
                VAR num y;
                
                PROC Calculate()
                    x := 10;
                    y := 20;
                ENDPROC
            ENDMODULE
        """.trimIndent()

        val result = RapidCompiler.analyze(code)
        val program = result.program
        
        println("=== Example 3: AST Structure ===")
        program?.modules?.forEach { module ->
            println("Module: ${module.name}")
            module.declarations.forEach { decl ->
                when (decl) {
                    is VarDecl -> println("  Variable: ${decl.name} (${decl.typeName})")
                    is ProcDecl -> println("  Procedure: ${decl.name}")
                    is FuncDecl -> println("  Function: ${decl.name} -> ${decl.returnType}")
                    else -> println("  Declaration: ${decl::class.simpleName}")
                }
            }
        }
        println()
    }

    /**
     * Example 4: Type checking
     * 示例 4：类型检查
     */
    fun example4_TypeChecking() {
        val typeErrorCode = """
            MODULE TestModule
                VAR num counter;
                VAR bool flag;
                
                PROC Main()
                    counter := TRUE;
                    flag := 10;
                ENDPROC
            ENDMODULE
        """.trimIndent()

        val result = RapidCompiler.analyze(typeErrorCode)
        println("=== Example 4: Type Errors ===")
        result.diagnostics.forEach { diag ->
            println("[${diag.severity}] Line ${diag.span.startLine}: ${diag.message}")
        }
        println()
    }

    /**
     * Example 5: Undefined variable detection
     * 示例 5：未定义变量检测
     */
    fun example5_UndefinedVariables() {
        val undefinedVarCode = """
            MODULE TestModule
                VAR num x;
                
                PROC Main()
                    x := 10;
                    y := 20;
                    z := x + y;
                ENDPROC
            ENDMODULE
        """.trimIndent()

        val result = RapidCompiler.analyze(undefinedVarCode)
        println("=== Example 5: Undefined Variables ===")
        result.diagnostics.forEach { diag ->
            println("[${diag.severity}] Line ${diag.span.startLine}, Col ${diag.span.startCol}: ${diag.message}")
        }
        println()
    }

    /**
     * Run all examples
     * 运行所有示例
     */
    @JvmStatic
    fun main(args: Array<String>) {
        println("======================================")
        println("RAPID Compiler Examples")
        println("RAPID 编译器示例")
        println("======================================")
        println()

        example1_BasicSyntaxChecking()
        example2_DetectingSyntaxErrors()
        example3_AccessingAST()
        example4_TypeChecking()
        example5_UndefinedVariables()

        println("======================================")
        println("All examples completed")
        println("所有示例执行完毕")
        println("======================================")
    }
}
