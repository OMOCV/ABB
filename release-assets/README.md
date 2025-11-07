# 发布素材要求与目录

本目录用于存放应用商店上架所需的图片资源。请按以下要求准备素材，并将文件放入对应子目录后运行校验脚本进行检查。

目录结构（请勿改动目录名）：

release-assets/
  app-icon/
  feature-graphic/
  phone-screenshots/
  tablet7-screenshots/
  tablet10-screenshots/

命名建议（可保持默认示例名或自定义有序命名）：
- app-icon/ic_launcher_512x512.png
- feature-graphic/feature_1024x500.png
- phone-screenshots/phone_01.png … phone_08.png
- tablet7-screenshots/tablet7_01.png … tablet7_08.png
- tablet10-screenshots/tablet10_01.png … tablet10_08.png

素材要求：
1. 应用图标（必填）
   - 尺寸：512 x 512 像素（固定尺寸）
   - 格式：PNG 或 JPEG
   - 文件大小：≤ 1 MB

2. 置顶大图（Feature Graphic，必填）
   - 尺寸：1,024 x 500 像素（固定尺寸）
   - 格式：PNG 或 JPEG
   - 文件大小：≤ 15 MB

3. 手机屏幕截图（2–8 张）
   - 数量：2 到 8 张
   - 格式：PNG 或 JPEG
   - 文件大小：≤ 8 MB/张
   - 宽高比：16:9 或 9:16（必须严格匹配）
   - 尺寸范围：任意一边介于 320–3840 像素

4. 7 英寸平板截图（0–8 张）
   - 数量：最多 8 张
   - 格式：PNG 或 JPEG
   - 文件大小：≤ 8 MB/张
   - 宽高比：16:9 或 9:16（必须严格匹配）
   - 尺寸范围：任意一边介于 320–3840 像素

5. 10 英寸平板截图（0–8 张）
   - 数量：最多 8 张
   - 格式：PNG 或 JPEG
   - 文件大小：≤ 8 MB/张
   - 宽高比：16:9 或 9:16（必须严格匹配）
   - 尺寸范围：任意一边介于 1080–7680 像素

使用方法：
1) 将素材文件放入上述对应子目录；
2) 在项目根目录执行：
   - 安装依赖：pip install pillow
   - 运行校验：python3 scripts/validate_assets.py
3) 通过校验后再提交应用商店。

提示：
- 截图建议使用真实数据内容，避免占位文本；
- PNG 适用于含透明或线条清晰的 UI，JPEG 适合复杂场景以减小体积；
- 确保无黑边、内容未被裁切；
- 若支持深色模式，可挑选亮/暗各若干张以展示适配。