# RAPID Compiler & Tools (Kotlin)

**Status: INTEGRATED INTO MAIN APP**

This package originally contained a self-contained RAPID language parser, semantic checker,
and a set of editor-oriented tools (highlighting, go-to-definition, completion,
and formatting).

## Integration Status

All files from this directory have been successfully integrated into the main ABB application:

### Integrated Files:
- ✅ RapidCompiler.kt      → `app/src/main/kotlin/com/omocv/abb/rapid/RapidCompiler.kt`
- ✅ RapidHighlighter.kt   → `app/src/main/kotlin/com/omocv/abb/rapid/RapidHighlighter.kt`
- ✅ RapidSymbolIndex.kt   → `app/src/main/kotlin/com/omocv/abb/rapid/RapidSymbolIndex.kt`
- ✅ RapidNavigator.kt     → `app/src/main/kotlin/com/omocv/abb/rapid/RapidNavigator.kt`
- ✅ RapidCompletion.kt    → `app/src/main/kotlin/com/omocv/abb/rapid/RapidCompletion.kt`
- ✅ RapidFormatter.kt     → `app/src/main/kotlin/com/omocv/abb/rapid/RapidFormatter.kt`

### Package Structure Change:
- Original: `com.yourcompany.rapid` / `com.yourcompany.rapid.tools`
- Updated: `com.omocv.abb.rapid` / `com.omocv.abb.rapid.tools`

## Usage in Main App

The RapidCompiler is now integrated with the existing ABBParser:

```kotlin
// Enhanced syntax validation using RapidCompiler
val parser = ABBParser()
val errors = parser.validateSyntaxEnhanced(content)

// Or use the original validation (still available for compatibility)
val errors = parser.validateSyntax(content)
```

## Features Available

1. **AST-based Parsing**: Full abstract syntax tree construction
2. **Semantic Analysis**: Type checking, symbol resolution, scope validation
3. **Syntax Highlighting**: Token-based highlighting with span information
4. **Symbol Navigation**: Go-to-definition and find references
5. **Code Completion**: Keyword, type, variable, and function completion
6. **Code Formatting**: AST-based pretty printer

## Architecture

The RAPID compiler provides three layers:
1. **Lexical Analysis**: Token stream generation with error recovery
2. **Syntax Analysis**: AST construction with detailed diagnostics
3. **Semantic Analysis**: Type checking and symbol resolution

All error messages are provided in Chinese (中文) to match the application locale.

## Original Documentation

For technical details about the compiler implementation, see the source files in:
`app/src/main/kotlin/com/omocv/abb/rapid/`

The code is self-documenting with comprehensive comments in both English and Chinese.

