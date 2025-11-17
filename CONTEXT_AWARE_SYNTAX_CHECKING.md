# Context-Aware Syntax Checking Implementation

## Problem Statement (问题陈述)

经过测试发现检查语法功能目前在分析时会将非指令集的内容纳入范围，现在需要做的是分析也只是对真实的指令进行语义检查，先分析具体的指令是哪个然后再对这个指令本身进行语义检查这样去排查问题，不相关的内容全部纳进分析范围不是脱裤子放屁多此一举嘛？

比如:
```rapid
PROC Main()
…………
…………
ENDPROC
```

需要检查的对象是 PROC 这个指令区块，后面的 Main 只是一个例行程序的名字，这是用户定义的，你检查它做什么呢？

**Translation:** After testing, it was found that the syntax checking function currently includes non-instruction content in the analysis scope. What needs to be done now is that the analysis should only do semantic checking on real instructions. First analyze what the specific instruction is, then do semantic checking on the instruction itself to troubleshoot problems. Including all unrelated content in the analysis scope is redundant and unnecessary, isn't it?

For example, in `PROC Main()`, the object that needs to be checked is the PROC instruction block. The "Main" following it is just a routine name, which is user-defined. Why check it?

## Solution Overview (解决方案概述)

The solution implements **context-aware syntax checking** that distinguishes between:

1. **Keywords/Instructions** (should be checked): PROC, VAR, WaitTime, TPWrite, etc.
2. **Data Types** (should be checked): num, bool, robtarget, speeddata, etc.
3. **User-defined names** (should NOT be checked): procedure names, variable names, parameter names, etc.

## Implementation Details (实现细节)

### 1. RapidCompiler.kt Changes

#### A. Removed Blanket Checking in Lexer
**Before:** The lexer called `checkIncompleteKeyword()` for every identifier that wasn't a recognized keyword.

**After:** The lexer no longer checks identifiers since it lacks context about whether an identifier is a keyword or a user-defined name.

```kotlin
// OLD CODE (removed):
else -> {
    checkIncompleteKeyword(ident, startLine, startCol)  // ❌ Checks everything
    TokenType.IDENT
}

// NEW CODE:
else -> {
    // Don't check incomplete keywords here - identifiers could be user-defined names
    // Context-aware checking happens during parsing.
    TokenType.IDENT  // ✓ No blanket checking
}
```

#### B. Added Context-Aware Checking in Parser

##### 1. Statement-Level Instructions
```kotlin
private fun parseSimpleStmt(): StmtNode {
    val expr = parseExpression()
    // ...
    when (expr) {
        is CallExpr -> checkStatementLevelIdent(expr.name, expr.span)  // ✓ Check instruction name
        is VarRef -> checkStatementLevelIdent(expr.name, expr.span)
        else -> { /* Other expressions don't need checking */ }
    }
    // ...
}
```

**Examples:**
- `WaitTime 1.0;` → Checks "WaitTime"
- `TPWrite "Hello";` → Checks "TPWrite"
- `SetDO output, 1;` → Checks "SetDO"

##### 2. Variable Declarations
```kotlin
private fun parseVarDecl(): VarDecl {
    // ...
    val typeTok = expect(TokenType.IDENT, "变量声明需要类型")
    checkDataType(typeTok.lexeme, typeTok.span)  // ✓ Check data type
    val nameTok = expect(TokenType.IDENT, "变量声明需要名字")
    // nameTok is user-defined, don't check it  // ✓ Skip variable name
    // ...
}
```

**Examples:**
- `VAR num counter;` → Checks "num", skips "counter"
- `PERS robtarget target1;` → Checks "robtarget", skips "target1"
- `VAR nu value;` → Flags "nu" as incomplete (should be "num")

##### 3. Function Declarations
```kotlin
private fun parseFuncDecl(): FuncDecl {
    val funcTok = expect(TokenType.FUNC, "期望 FUNC")
    val retTypeTok = expect(TokenType.IDENT, "FUNC 后需要返回类型")
    checkDataType(retTypeTok.lexeme, retTypeTok.span)  // ✓ Check return type
    val nameTok = expect(TokenType.IDENT, "FUNC 需要名字")
    // nameTok is user-defined, don't check it  // ✓ Skip function name
    // ...
}
```

**Examples:**
- `FUNC num GetValue()` → Checks "num", skips "GetValue"
- `FUNC nu Calculate()` → Flags "nu" as incomplete, skips "Calculate"

##### 4. Parameter Lists
```kotlin
private fun parseParamList(): List<Param> {
    // ...
    val typeTok = expect(TokenType.IDENT, "参数需要类型")
    checkDataType(typeTok.lexeme, typeTok.span)  // ✓ Check parameter type
    val nameTok = expect(TokenType.IDENT, "参数需要名称")
    // nameTok is user-defined, don't check it  // ✓ Skip parameter name
    // ...
}
```

**Examples:**
- `PROC Test(num speed, robtarget target)` → Checks "num" and "robtarget", skips "speed" and "target"

##### 5. Record Declarations
```kotlin
private fun parseRecordDecl(): RecordDecl {
    // ...
    val nameTok = expect(TokenType.IDENT, "RECORD 后需要名称")
    // nameTok is user-defined, don't check it  // ✓ Skip record name
    // ...
    val typeTok = expect(TokenType.IDENT, "RECORD 字段需要类型")
    checkDataType(typeTok.lexeme, typeTok.span)  // ✓ Check field type
    val fieldTok = expect(TokenType.IDENT, "RECORD 字段需要名字")
    // fieldTok is user-defined, don't check it  // ✓ Skip field name
    // ...
}
```

### 2. ABBParser.kt Changes

Enhanced `checkIncompleteKeywords()` to analyze line context:

```kotlin
// Determine context from first token
val firstWord = matches.first().value.uppercase()

when {
    // VAR/PERS/CONST: check positions 0 (keyword) and 1 (type)
    firstWord in setOf("VAR", "PERS", "CONST") -> {
        positionsToCheck.add(0)  // The keyword
        if (matches.size >= 2) {
            positionsToCheck.add(1)  // The data type
        }
        // Position 2 is variable name (skip)
    }
    
    // PROC/TRAP: check position 0 only
    firstWord in setOf("PROC", "TRAP") -> {
        positionsToCheck.add(0)  // The keyword
        // Position 1 is procedure/trap name (skip)
    }
    
    // FUNC: check positions 0 and 1
    firstWord == "FUNC" -> {
        positionsToCheck.add(0)  // The keyword
        if (matches.size >= 2) {
            positionsToCheck.add(1)  // The return type
        }
        // Position 2 is function name (skip)
    }
    
    // Instruction calls: check position 0
    else -> {
        val firstToken = matches.first().value
        if (firstToken.isNotEmpty() && firstToken[0].isUpperCase()) {
            positionsToCheck.add(0)  // Potential instruction
        }
    }
}
```

## Test Cases (测试用例)

### ✓ Should NOT Flag (user-defined names)
```rapid
PROC Main()                           // "Main" is procedure name
PROC Initialize()                     // "Initialize" is procedure name
VAR num counter;                      // "counter" is variable name
VAR robtarget Target_10;             // "Target_10" is variable name
FUNC num CheckDistance()              // "CheckDistance" is function name
PROC TestParams(num speed)            // "speed" is parameter name
RECORD MyRecord(num value);          // "MyRecord" is record name, "value" is field name
```

### ✗ Should Flag (incomplete keywords/instructions)
```rapid
VA num test1;                        // "VA" incomplete (should be "VAR")
PER num test2;                       // "PER" incomplete (should be "PERS")
CONS num test3 := 10;                // "CONS" incomplete (should be "CONST")
WaitTim 1.0;                         // "WaitTim" incomplete (should be "WaitTime")
TPWrit "Test";                       // "TPWrit" incomplete (should be "TPWrite")
MoveAbs Home, v100, z50, tool0;      // "MoveAbs" incomplete (should be "MoveAbsJ")
VAR nu value;                        // "nu" incomplete (should be "num")
FUNC nu GetValue()                   // "nu" incomplete (should be "num")
```

## Benefits (优势)

1. **Precision**: Only checks what should be checked, not user-defined names
2. **Fewer False Positives**: User's creative naming won't trigger warnings
3. **Better User Experience**: Developers can name their procedures, variables, etc. freely
4. **Correct Problem Detection**: Still catches real typos in keywords and instructions

## Technical Approach (技术方法)

The key insight is that **context matters**:

```
Position in code:              Should check?
-----------------              -------------
PROC [name]                    ✗ No  (user-defined procedure name)
VAR [type] [name]             ✓ Yes (type), ✗ No (name)
FUNC [type] [name]            ✓ Yes (type), ✗ No (name)
[instruction] args;           ✓ Yes (instruction name at statement start)
```

We achieve this by:
1. **RapidCompiler**: Checking during parsing when we know the grammatical role of each identifier
2. **ABBParser**: Analyzing line structure to determine which word positions should be checked

## Conclusion (结论)

This implementation fulfills the requirement from the problem statement: **只对真实的指令进行语义检查** (only do semantic checking on real instructions), not on user-defined names.
