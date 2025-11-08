#!/usr/bin/env python3
"""生成应用商店素材占位图片。运行后自动在 release-assets 下生成 PNG，占位用，后续请替换为真实素材。"""
import os
from PIL import Image, ImageDraw, ImageFont

SPEC = [
    ("release-assets/app-icon/ic_launcher_512x512.png", 512, 512, "#3F51B5", "APP ICON 512x512"),
    ("release-assets/feature-graphic/feature_1024x500.png", 1024, 500, "#009688", "FEATURE 1024x500"),
    ("release-assets/phone-screenshots/phone_01.png", 1080, 1920, "#607D8B", "PHONE 1080x1920"),
    ("release-assets/phone-screenshots/phone_02.png", 1920, 1080, "#795548", "PHONE 1920x1080"),
    ("release-assets/tablet7-screenshots/tablet7_01.png", 1280, 720, "#9C27B0", "TABLET7 1280x720"),
    ("release-assets/tablet10-screenshots/tablet10_01.png", 1920, 1080, "#673AB7", "TABLET10 1920x1080"),
]

def draw_text_center(img, text):
    draw = ImageDraw.Draw(img)
    try:
        font = ImageFont.load_default()
    except Exception:
        font = None
    w, h = img.size
    if font:
        tw, th = draw.textsize(text, font=font)
    else:
        tw, th = draw.textsize(text)
    draw.text(((w - tw)/2, (h - th)/2), text, fill="white", font=font)


def main():
    for path, w, h, color, text in SPEC:
        os.makedirs(os.path.dirname(path), exist_ok=True)
        img = Image.new("RGB", (w, h), color)
        draw_text_center(img, text)
        img.save(path, optimize=True)
        print(f"生成: {path} ({w}x{h})")
    print("占位图片全部生成完成。")

if __name__ == "__main__":
    main()