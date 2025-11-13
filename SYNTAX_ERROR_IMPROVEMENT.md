# 语法错误检测改进 / Syntax Error Detection Improvement

## 问题描述 / Problem Description

**中文:**
当 PROC 与 ENDPROC 区块之间存在语法错误时，之前的实现只能报告区块级别的错误（如"PROC 区块未关闭，起始于第 X 行"），而不能精确定位到具体的错误位置。用户需要在整个 PROC 区块中手动搜索才能找到实际的错误。

**English:**
When syntax errors existed between PROC and ENDPROC blocks, the previous implementation could only report block-level errors (like "Unclosed PROC block starting at line X"), instead of pinpointing the exact error location. Users had to manually search through the entire PROC block to find the actual error.

---

## 改进对比 / Before vs After Comparison

### 场景 1: 字符串未闭合 / Scenario 1: Unclosed String

#### 错误代码 / Error Code
```rapid
PROC test_unclosed_string()
    TPWrite "This string is not closed;
    TPWrite "This is fine";
ENDPROC
```

#### 之前 / Before
```
错误: Unclosed PROC block starting at line 1
```
❌ 只知道 PROC 区块有问题
❌ 需要检查第 1-4 行的所有代码
❌ 没有列号信息

#### 现在 / After
```
错误: Unclosed string at line 2, column 13
```
✅ 精确指向第 2 行第 13 列
✅ 点击错误直接跳转到引号位置
✅ 明确告诉是字符串未闭合

---

### 场景 2: 括号不匹配 / Scenario 2: Unmatched Parenthesis

#### 错误代码 / Error Code
```rapid
PROC calculate_result()
    VAR num result;
    result := myFunction(param1, param2;
    TPWrite "Done";
ENDPROC
```

#### 之前 / Before
```
错误: Unclosed PROC block starting at line 1
或者根本不报错 / Or no error reported at all
```
❌ 不清楚是什么问题
❌ 需要逐行检查

#### 现在 / After
```
错误: Unclosed parenthesis at line 3, column 22
```
✅ 明确指出括号未闭合
✅ 精确定位到左括号位置
✅ 点击跳转到第 3 行第 22 列

---

### 场景 3: IF 缺少 THEN / Scenario 3: IF Missing THEN

#### 错误代码 / Error Code
```rapid
PROC test_condition()
    IF counter > 10
        TPWrite "Too many";
    ENDIF
ENDPROC
```

#### 之前 / Before
```
错误: IF statement at line 2 is missing THEN keyword
```
⚠️ 有行号但没有列号

#### 现在 / After
```
错误: IF statement at line 2, column 5 is missing THEN keyword
```
✅ 包含行号和列号
✅ 点击可以精确跳转到 IF 关键字

---

### 场景 4: PROC 声明不完整 / Scenario 4: Incomplete PROC Declaration

#### 错误代码 / Error Code
```rapid
MODULE TestModule
    PROC
        TPWrite "Missing procedure name";
    ENDPROC
ENDMODULE
```

#### 之前 / Before
```
错误: Unclosed MODULE block starting at line 1
或者根本不报错 / Or no error reported at all
```
❌ 无法定位到真正的问题

#### 现在 / After
```
错误: Incomplete PROC declaration at line 2, column 5 - missing procedure name
```
✅ 明确指出 PROC 声明不完整
✅ 精确定位到 PROC 关键字
✅ 说明缺少过程名称

---

### 场景 5: 函数缺少返回值 / Scenario 5: FUNC Missing Return Value

#### 错误代码 / Error Code
```rapid
FUNC num calculate_total()
    TPWrite "Calculating";
    RETURN
ENDFUNC
```

#### 之前 / Before
```
根本不报错 / No error reported at all
```
❌ 运行时才会发现问题

#### 现在 / After
```
错误: RETURN statement at line 3, column 5 is missing return value - FUNC requires a return value
```
✅ 上下文感知（知道在 FUNC 中）
✅ 明确说明 FUNC 必须返回值
✅ 精确定位到 RETURN 语句

---

## 新增错误检测 / New Error Detections

| 错误类型<br>Error Type | 示例<br>Example | 定位精度<br>Precision |
|---|---|---|
| 未闭合的字符串<br>Unclosed string | `TPWrite "text` | ✅ 精确到引号位置<br>Exact quote position |
| 不匹配的括号<br>Unmatched parenthesis | `func(a, b` | ✅ 精确到左括号<br>Exact opening paren |
| 不匹配的方括号<br>Unmatched bracket | `array[1, 2` | ✅ 精确到左方括号<br>Exact opening bracket |
| 不匹配的花括号<br>Unmatched brace | `data{x, y` | ✅ 精确到左花括号<br>Exact opening brace |
| ~~无效的分号~~<br>~~Invalid semicolon~~ | ~~`x := 10;`~~ | ~~已移除：分号在 RAPID 中是有效的~~<br>~~Removed: Semicolons are valid in RAPID~~ |
| 不完整的 PROC<br>Incomplete PROC | `PROC` | ✅ 精确到关键字<br>Exact keyword |
| 不完整的 FUNC<br>Incomplete FUNC | `FUNC num` | ✅ 精确到关键字<br>Exact keyword |
| 不完整的变量声明<br>Incomplete VAR | `VAR` | ✅ 精确到关键字<br>Exact keyword |
| FUNC 缺少返回值<br>FUNC missing return | `RETURN` in FUNC | ✅ 精确到 RETURN 语句<br>Exact RETURN statement |

---

## 使用流程 / Usage Flow

### 之前 / Before
1. 点击"检查语法" / Click "Check Syntax"
2. 看到错误: "Unclosed PROC block starting at line 10"
3. ❌ 手动翻到第 10 行
4. ❌ 逐行检查第 10 行到 ENDPROC 之间的所有代码
5. ❌ 花费大量时间寻找具体错误

### 现在 / After
1. 点击"检查语法" / Click "Check Syntax"  
2. 看到错误: "Unclosed string at line 15, column 20"
3. ✅ 点击错误
4. ✅ 自动跳转到第 15 行第 20 列
5. ✅ 立即看到并修复错误

---

## 技术优势 / Technical Advantages

### 1. 精确定位 / Precise Location
- 每个错误都包含行号和列号
- Every error includes line and column numbers
- 0-based column index for accurate positioning

### 2. 上下文感知 / Context-Aware
- 区分 PROC（无返回值）和 FUNC（需要返回值）
- Distinguishes between PROC (no return) and FUNC (needs return)
- 知道何时需要返回值
- Knows when return value is required

### 3. 避免误报 / Avoids False Positives
- 忽略注释中的内容
- Ignores content in comments
- 忽略字符串中的内容
- Ignores content in strings
- 使用 `removeStringsAndComments()` 辅助函数
- Uses `removeStringsAndComments()` helper function

### 4. 专业级体验 / Professional Experience
- 类似 Visual Studio Code、IntelliJ IDEA 的行为
- Behavior similar to Visual Studio Code, IntelliJ IDEA
- 点击跳转功能
- Click-to-jump functionality
- 清晰的错误消息
- Clear error messages

---

## 实现细节 / Implementation Details

### 修改的文件 / Modified File
- `app/src/main/kotlin/com/omocv/abb/ABBParser.kt`

### 新增代码量 / Lines Added
- ~200 行增强的语法检查代码
- ~200 lines of enhanced syntax checking

### 向后兼容性 / Backward Compatibility
- ✅ 零破坏性变更
- ✅ Zero breaking changes
- ✅ 所有现有功能继续工作
- ✅ All existing features continue to work
- ✅ 不需要新的依赖
- ✅ No new dependencies required

---

## 测试验证 / Testing Validation

### 错误场景测试 / Error Scenarios
✅ 未闭合字符串 - 检测到精确位置
✅ 不匹配括号 - 检测到精确位置
✅ IF 缺少 THEN - 检测到精确位置
✅ 不完整声明 - 检测到精确位置

### 正常代码测试 / Normal Code
✅ 正确的 RAPID 程序不报错
✅ Correct RAPID programs report no errors
✅ 无误报
✅ No false positives

### 安全检查 / Security Check
✅ CodeQL 扫描通过
✅ CodeQL scan passed
✅ 无安全问题
✅ No security issues

---

## 总结 / Summary

这次改进大幅提升了语法错误检测的精确度，从"告诉你哪个区块有问题"提升到"告诉你具体哪一行哪一列有什么问题"。用户可以直接点击错误跳转到具体位置，极大提升了开发效率。

This improvement significantly enhances the precision of syntax error detection, upgrading from "telling you which block has a problem" to "telling you exactly which line and column has what problem". Users can directly click on errors to jump to specific locations, greatly improving development efficiency.
