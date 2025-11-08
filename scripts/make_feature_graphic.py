#!/usr/bin/env python3
"""
Feature Graphic Generator for ABB Robot Program Reader
Generates a 1024x500 PNG image for Google Play Store Feature Graphic requirement.
"""

import os
import sys
from PIL import Image, ImageDraw, ImageFont

# Configuration
OUTPUT_PATH = "release-assets/feature-graphic/feature_1024x500.png"
WIDTH = 1024
HEIGHT = 500

# Color scheme
BG_COLOR = "#0B0F14"  # Deep tech dark background
TOP_BAR_COLOR = "#00ADFF"  # Cyan-blue
BOTTOM_BAR_COLOR = "#FF4757"  # Red
CODE_BG_COLOR = "#121E28"  # Code panel background
GRID_COLOR = "#1A2128"  # Subtle grid lines

# Code highlighting colors
CODE_COLORS = {
    "module": "#6AD0FF",  # Light blue for MODULE
    "proc": "#A0C8FF",  # Pale blue for PROC
    "movej": "#00ADFF",  # Bright blue for MoveJ
    "waittime": "#00DCA0",  # Green for WaitTime
    "endproc": "#6AD0FF",  # Same as MODULE
}

# Text content
TITLE_TEXT = "ABB Robot Program Reader"
SUBTITLE_TEXT = "读取与浏览 ABB RAPID 程序"
META_TEXT = "Open Source • MIT License"

# Code snippet
CODE_LINES = [
    ("MODULE MainModule", "module"),
    ("  PROC main()", "proc"),
    ("    MoveJ [[600,0,600],[1,0,0,0],...];", "movej"),
    ("    WaitTime 2;", "waittime"),
    ("  ENDPROC", "endproc"),
    ("ENDMODULE", "module"),
]

# Font paths to try (in priority order)
FONT_PATHS = {
    "sans": [
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
        "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf",
        "/usr/share/fonts/truetype/lato/Lato-Bold.ttf",
        "/System/Library/Fonts/Helvetica.ttc",
        "C:\\Windows\\Fonts\\arial.ttf",
    ],
    "sans_regular": [
        "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
        "/usr/share/fonts/truetype/lato/Lato-Regular.ttf",
        "/System/Library/Fonts/Helvetica.ttc",
        "C:\\Windows\\Fonts\\arial.ttf",
    ],
    "mono": [
        "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf",
        "/usr/share/fonts/truetype/liberation/LiberationMono-Regular.ttf",
        "/System/Library/Fonts/Courier.ttc",
        "C:\\Windows\\Fonts\\consola.ttf",
    ],
}


def find_font(font_type, size):
    """Find and load a font, with fallback to default."""
    paths = FONT_PATHS.get(font_type, FONT_PATHS["sans_regular"])
    
    for path in paths:
        if os.path.exists(path):
            try:
                return ImageFont.truetype(path, size)
            except Exception as e:
                print(f"Warning: Could not load font {path}: {e}")
                continue
    
    # Fallback to default font
    print(f"Warning: No TrueType font found for {font_type}, using default font")
    return ImageFont.load_default()


def draw_grid_pattern(draw, width, height, color, spacing=40):
    """Draw a subtle grid pattern on the background."""
    # Vertical lines
    for x in range(0, width, spacing):
        draw.line([(x, 0), (x, height)], fill=color, width=1)
    
    # Horizontal lines
    for y in range(0, height, spacing):
        draw.line([(0, y), (width, y)], fill=color, width=1)


def draw_color_bars(draw, width, height, bar_height=8):
    """Draw colored bars at top and bottom."""
    # Top bar
    draw.rectangle([(0, 0), (width, bar_height)], fill=TOP_BAR_COLOR)
    
    # Bottom bar
    draw.rectangle([(0, height - bar_height), (width, height)], fill=BOTTOM_BAR_COLOR)


def draw_left_text(draw, fonts):
    """Draw the left side text content."""
    x_start = 50
    y_start = 140
    
    # Title
    title_font = fonts["title"]
    draw.text((x_start, y_start), TITLE_TEXT, fill="#FFFFFF", font=title_font)
    
    # Subtitle
    subtitle_font = fonts["subtitle"]
    y_start += 70
    draw.text((x_start, y_start), SUBTITLE_TEXT, fill="#B0B8C0", font=subtitle_font)
    
    # Meta info
    meta_font = fonts["meta"]
    y_start += 50
    draw.text((x_start, y_start), META_TEXT, fill="#6B7280", font=meta_font)


def draw_code_panel(draw, fonts, width, height):
    """Draw the code snippet panel on the right side."""
    # Panel dimensions
    panel_width = 460
    panel_height = 320
    panel_x = width - panel_width - 40
    panel_y = (height - panel_height) // 2
    corner_radius = 12
    
    # Draw rounded rectangle for code panel
    draw.rounded_rectangle(
        [(panel_x, panel_y), (panel_x + panel_width, panel_y + panel_height)],
        radius=corner_radius,
        fill=CODE_BG_COLOR
    )
    
    # Draw window control dots (macOS style)
    dot_y = panel_y + 15
    dot_colors = ["#FF5F57", "#FEBC2E", "#28C840"]
    for i, color in enumerate(dot_colors):
        dot_x = panel_x + 15 + (i * 18)
        draw.ellipse(
            [(dot_x, dot_y), (dot_x + 10, dot_y + 10)],
            fill=color
        )
    
    # Draw code lines
    code_font = fonts["code"]
    line_height = 32
    code_x = panel_x + 20
    code_y = panel_y + 50
    
    for line_text, color_key in CODE_LINES:
        color = CODE_COLORS.get(color_key, "#FFFFFF")
        draw.text((code_x, code_y), line_text, fill=color, font=code_font)
        code_y += line_height


def generate_feature_graphic():
    """Generate the feature graphic image."""
    print(f"Generating Feature Graphic ({WIDTH}x{HEIGHT})...")
    
    # Create base image
    img = Image.new("RGB", (WIDTH, HEIGHT), color=BG_COLOR)
    draw = ImageDraw.Draw(img)
    
    # Load fonts
    fonts = {
        "title": find_font("sans", 48),
        "subtitle": find_font("sans_regular", 28),
        "meta": find_font("sans_regular", 18),
        "code": find_font("mono", 20),
    }
    
    # Draw elements
    draw_grid_pattern(draw, WIDTH, HEIGHT, GRID_COLOR)
    draw_color_bars(draw, WIDTH, HEIGHT)
    draw_left_text(draw, fonts)
    draw_code_panel(draw, fonts, WIDTH, HEIGHT)
    
    # Ensure output directory exists
    output_dir = os.path.dirname(OUTPUT_PATH)
    if output_dir and not os.path.exists(output_dir):
        os.makedirs(output_dir, exist_ok=True)
    
    # Save the image with optimization
    img.save(OUTPUT_PATH, "PNG", optimize=True)
    
    # Report file size
    file_size = os.path.getsize(OUTPUT_PATH)
    file_size_kb = file_size / 1024
    file_size_mb = file_size / (1024 * 1024)
    
    print(f"✓ Feature Graphic saved to: {OUTPUT_PATH}")
    print(f"✓ Image size: {WIDTH}x{HEIGHT} pixels")
    print(f"✓ File size: {file_size_kb:.1f} KB ({file_size_mb:.2f} MB)")
    
    if file_size > 15 * 1024 * 1024:
        print("⚠ Warning: File size exceeds 15 MB limit!")
        return False
    
    print("✓ File size is within the 15 MB limit")
    return True


def main():
    """Main entry point."""
    try:
        success = generate_feature_graphic()
        if success:
            print("\n✓ Feature Graphic generated successfully!")
            sys.exit(0)
        else:
            print("\n✗ Feature Graphic generation completed with warnings")
            sys.exit(1)
    except Exception as e:
        print(f"\n✗ Error generating feature graphic: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)


if __name__ == "__main__":
    main()
