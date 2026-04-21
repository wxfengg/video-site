#!/usr/bin/env python3
"""
Faster-Whisper 音频转录脚本
用于视频站点智能化分析流水线

安装依赖:
    pip install faster-whisper

用法:
    python transcribe.py --audio input.wav --model small --language zh
"""

import argparse
import sys


def main():
    parser = argparse.ArgumentParser(description="音频转文本 (faster-whisper)")
    parser.add_argument("--audio", required=True, help="输入音频文件路径")
    parser.add_argument("--model", default="small", help="模型大小: tiny/base/small/medium/large")
    parser.add_argument("--language", default="zh", help="语言代码，如 zh/en/ja")
    args = parser.parse_args()

    try:
        from faster_whisper import WhisperModel
    except ImportError:
        print("错误: 未安装 faster-whisper。请执行: pip install faster-whisper", file=sys.stderr)
        sys.exit(1)

    model = WhisperModel(args.model, device="cpu", compute_type="int8")
    segments, info = model.transcribe(args.audio, language=args.language)

    text_parts = []
    for segment in segments:
        text_parts.append(segment.text.strip())

    result = " ".join(text_parts).strip()
    print(result)


if __name__ == "__main__":
    main()
