#!/usr/bin/env python3
import os, sys
from PIL import Image

RULES = {
  "app-icon": {"exact": (512, 512), "max_bytes": 1*1024*1024, "count_range": (1,1)},
  "feature-graphic": {"exact": (1024, 500), "max_bytes": 15*1024*1024, "count_range": (1,1)},
  "phone-screenshots": {"ratio": [(16,9),(9,16)], "min_side":320, "max_side":3840, "max_bytes":8*1024*1024, "count_range": (2,8)},
  "tablet7-screenshots": {"ratio": [(16,9),(9,16)], "min_side":320, "max_side":3840, "max_bytes":8*1024*1024, "count_range": (0,8)},
  "tablet10-screenshots": {"ratio": [(16,9),(9,16)], "min_side":1080, "max_side":7680, "max_bytes":8*1024*1024, "count_range": (0,8)},
}

VALID_EXT = {".png", ".jpg", ".jpeg"}

def check_ratio(w,h, allowed):
    for a,b in allowed:
        if w * b == h * a:
            return True
    return False


def main(root="release-assets"):
    errors = []
    for folder, rule in RULES.items():
        path = os.path.join(root, folder)
        if not os.path.isdir(path):
            errors.append(f"[{folder}] 目录缺失: {path}")
            continue
        files = [f for f in os.listdir(path) if os.path.isfile(os.path.join(path,f))]
        image_files = [f for f in files if os.path.splitext(f.lower())[1] in VALID_EXT]
        count = len(image_files)
        min_c, max_c = rule["count_range"]
        if not (min_c <= count <= max_c):
            errors.append(f"[{folder}] 图片数量 {count} 不在要求范围 {min_c}-{max_c}")
        for f in image_files:
            fp = os.path.join(path,f)
            size_bytes = os.path.getsize(fp)
            if size_bytes > rule.get("max_bytes", 10**12):
                errors.append(f"[{folder}] {f} 超过大小限制 {rule['max_bytes']} bytes (当前 {size_bytes})")
            try:
                with Image.open(fp) as img:
                    w, h = img.size
            except Exception as e:
                errors.append(f"[{folder}] {f} 不能打开: {e}")
                continue
            if "exact" in rule:
                ew, eh = rule["exact"]
                if (w,h)!=(ew,eh):
                    errors.append(f"[{folder}] {f} 尺寸 {w}x{h} 非 {ew}x{eh}")
            else:
                if not check_ratio(w,h, rule["ratio"]):
                    errors.append(f"[{folder}] {f} 宽高比 {w}:{h} 不符合 16:9 或 9:16")
                if not (rule["min_side"] <= w <= rule["max_side"]) or not (rule["min_side"] <= h <= rule["max_side"]):
                    errors.append(f"[{folder}] {f} 边长不在范围 {rule['min_side']}-{rule['max_side']}")
    if errors:
        print("校验失败：")
        for e in errors:
            print(" -", e)
        sys.exit(1)
    print("所有资源通过校验。")

if __name__ == "__main__":
    main()
