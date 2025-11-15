# RAPID Compiler & Tools (Kotlin)

This package contains a self-contained RAPID language parser, semantic checker,
and a set of editor-oriented tools (highlighting, go-to-definition, completion,
and formatting).

The code is written in Kotlin and is intended to be used on JVM/Android/Compose.

Files:
- RapidCompiler.kt      : AST + Lexer + Parser + Semantic + public API
- RapidHighlighter.kt   : Lightweight highlighter for RAPID source
- RapidSymbolIndex.kt   : Symbol index for navigation / references
- RapidNavigator.kt     : Go-to-definition helper
- RapidCompletion.kt    : Simple completion engine
- RapidFormatter.kt     : AST-based pretty printer

You can copy these files into your project, adjust the package name, and start
using them directly.
