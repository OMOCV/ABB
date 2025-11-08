# Feature Graphic 使用说明

## 概述

本目录包含应用商店置顶大图（Feature Graphic），这是 Google Play 商店展示应用时使用的横幅图片。

## 文件说明

- **feature_1024x500.png**: 正式的特性图，尺寸 1024×500 像素，符合 Google Play 规范。

## 设计元素

### 配色方案

- **背景色**: `#0B0F14` - 深色科技风格，营造专业感
- **网格色**: `#1A2128` - 细微网格纹理，增加科技感
- **顶部色条**: `#00ADFF` - 青蓝色，代表科技与创新
- **底部色条**: `#FF4757` - 红色，增加视觉对比
- **代码面板背景**: `#121E28` - 深色面板，突出代码内容

### 布局结构

**左侧区域** (文字信息):
- **标题**: "ABB Robot Program Reader" (英文，白色，48pt)
- **副标题**: "读取与浏览 ABB RAPID 程序" (中文，灰色，28pt)
- **元信息**: "Open Source • MIT License" (英文，浅灰，18pt)

**右侧区域** (代码展示面板):
- 半圆角矩形背景，仿终端窗口设计
- 顶部三色圆点（红、黄、绿），模仿 macOS 窗口控制按钮
- RAPID 代码示例，带语法高亮：
  - `MODULE` / `ENDMODULE`: `#6AD0FF` (浅蓝)
  - `PROC` / `ENDPROC`: `#A0C8FF` (淡蓝)
  - `MoveJ`: `#00ADFF` (亮蓝，强调机器人运动指令)
  - `WaitTime`: `#00DCA0` (绿色)

### 代码示例内容

```rapid
MODULE MainModule
  PROC main()
    MoveJ [[600,0,600],[1,0,0,0],...];
    WaitTime 2;
  ENDPROC
ENDMODULE
```

## 生成与再生产

### 前置要求

确保已安装 Python 3 和 Pillow 库：

```bash
python3 -m pip install pillow
```

### 生成特性图

在项目根目录执行以下命令：

```bash
python3 scripts/make_feature_graphic.py
```

脚本将自动生成 `release-assets/feature-graphic/feature_1024x500.png` 文件。

### 输出规格

- **尺寸**: 1024 × 500 像素（固定）
- **格式**: PNG
- **文件大小**: 约 28 KB（远低于 15 MB 限制）
- **优化**: 启用 PNG 优化以减小文件体积

### 验证生成结果

运行项目验证脚本以确保符合规范：

```bash
python3 scripts/validate_assets.py
```

验证项包括：
- 尺寸精确为 1024 × 500 像素
- 文件大小 ≤ 15 MB
- 文件格式为 PNG 或 JPEG

## 字体配置

### 默认字体路径

脚本会自动尝试以下字体（按优先级）：

**无衬线字体** (标题与文字):
- DejaVu Sans (Bold/Regular)
- Liberation Sans (Bold/Regular)
- Lato (Bold/Regular)
- Helvetica (macOS)
- Arial (Windows)

**等宽字体** (代码):
- DejaVu Sans Mono
- Liberation Mono
- Courier (macOS)
- Consolas (Windows)

### 自定义字体

如需使用其他字体（如 Noto Sans），可修改 `scripts/make_feature_graphic.py` 中的 `FONT_PATHS` 配置：

```python
FONT_PATHS = {
    "sans": [
        "/path/to/NotoSans-Bold.ttf",  # 添加自定义路径
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
        # ... 其他回退路径
    ],
    "sans_regular": [
        "/path/to/NotoSans-Regular.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        # ...
    ],
    "mono": [
        "/path/to/NotoSansMono-Regular.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf",
        # ...
    ],
}
```

### 字体回退机制

如果找不到指定的 TrueType 字体，脚本会自动使用系统默认字体，并输出警告信息。这确保在不同环境下都能正常生成图片。

## 自定义设计

### 修改颜色

在 `scripts/make_feature_graphic.py` 中找到颜色配置部分：

```python
# Color scheme
BG_COLOR = "#0B0F14"           # 背景色
TOP_BAR_COLOR = "#00ADFF"      # 顶部色条
BOTTOM_BAR_COLOR = "#FF4757"   # 底部色条
CODE_BG_COLOR = "#121E28"      # 代码面板背景
GRID_COLOR = "#1A2128"         # 网格线颜色

# Code highlighting colors
CODE_COLORS = {
    "module": "#6AD0FF",       # MODULE 关键字
    "proc": "#A0C8FF",         # PROC 关键字
    "movej": "#00ADFF",        # MoveJ 指令
    "waittime": "#00DCA0",     # WaitTime 指令
    "endproc": "#6AD0FF",      # ENDPROC 关键字
}
```

### 修改文字内容

在同一文件中修改文本配置：

```python
TITLE_TEXT = "ABB Robot Program Reader"
SUBTITLE_TEXT = "读取与浏览 ABB RAPID 程序"
META_TEXT = "Open Source • MIT License"
```

### 修改代码示例

修改 `CODE_LINES` 列表以更改显示的代码内容：

```python
CODE_LINES = [
    ("MODULE MainModule", "module"),
    ("  PROC main()", "proc"),
    ("    MoveJ [[600,0,600],[1,0,0,0],...];", "movej"),
    ("    WaitTime 2;", "waittime"),
    ("  ENDPROC", "endproc"),
    ("ENDMODULE", "module"),
]
```

每个元组的格式为 `(代码文本, 颜色键)`，颜色键对应 `CODE_COLORS` 中的配置。

## 故障排除

### 问题：脚本运行失败

**解决方案**:
1. 确认 Python 版本 ≥ 3.6：`python3 --version`
2. 确认 Pillow 已安装：`python3 -c "import PIL; print(PIL.__version__)"`
3. 如果缺少 Pillow，运行：`pip install pillow`

### 问题：字体警告

如果看到 "No TrueType font found" 警告，说明系统缺少首选字体。脚本会使用默认字体，但可能影响视觉效果。

**解决方案**:
- **Ubuntu/Debian**: `sudo apt-get install fonts-dejavu fonts-liberation`
- **Fedora/RHEL**: `sudo dnf install dejavu-sans-fonts liberation-fonts`
- **macOS/Windows**: 系统自带的字体应该足够

### 问题：生成的图片尺寸不对

脚本硬编码为 1024×500 像素。如果需要其他尺寸，请修改 `scripts/make_feature_graphic.py` 中的 `WIDTH` 和 `HEIGHT` 常量，但注意这可能不符合 Google Play 规范。

### 问题：文件大小超过限制

当前生成的 PNG 文件约 28 KB，远低于 15 MB 限制。如果添加复杂图案或高分辨率图像导致体积增大，可以：
- 减少颜色复杂度
- 使用 JPEG 格式（质量 85-95）
- 使用外部工具进一步压缩（如 `pngquant`）

## 版本历史

- **v1.0** (2025-11-08): 初始版本，实现基础设计与生成功能

## 参考资料

- [Google Play 图形资源规范](https://support.google.com/googleplay/android-developer/answer/9866151)
- [Pillow 文档](https://pillow.readthedocs.io/)
- [ABB RAPID 编程手册](https://new.abb.com/products/robotics/robotstudio/downloads)
