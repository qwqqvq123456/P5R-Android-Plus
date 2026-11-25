#!/usr/bin/env python3
"""从上游 README 生成本仓库的英文版说明文档 README.en.md。

本仓库的仓库主页默认显示中文版 README.md（由本仓库维护）；
英文/西班牙语原文来自上游，每次同步后由 .github/workflows/sync-upstream.yml
调用本脚本刷新，并自动在标题下方插入中英文切换链接。

用法:
    python3 tools/sync_readme.py                        # 默认取 upstream/main
    python3 tools/sync_readme.py --upstream-ref upstream/Chakeyy-Launcher-Rework
    python3 tools/sync_readme.py --source path/to/README.md
"""

from __future__ import annotations

import argparse
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUTPUT = ROOT / "README.en.md"

# 英文版顶部的语言切换条：当前语言加粗，另一边是链接
LANG_BAR = (
    '<p align="center">\n'
    '  <b>English / Español</b> &nbsp;·&nbsp; <a href="README.md">简体中文</a>\n'
    "</p>"
)

# 用来判断文件里是否已经有切换条，避免重复插入
BAR_MARKER = '<a href="README.md">简体中文</a>'


def read_source(ref: str | None, source: Path | None) -> str:
    if source is not None:
        return source.read_text(encoding="utf-8")
    if ref is None:
        ref = "upstream/main"
    try:
        # 上游仓库可能没有该分支，也可能文件不存在
        return subprocess.run(
            ["git", "show", f"{ref}:README.md"],
            cwd=ROOT,
            check=True,
            capture_output=True,
            text=True,
        ).stdout
    except subprocess.CalledProcessError:
        raise SystemExit(f"取不到 {ref}:README.md（上游没有这个分支或文件）")


def with_lang_bar(text: str) -> str:
    lines = text.splitlines()
    if any(BAR_MARKER in line for line in lines[:15]):
        return text

    # 插在第一个一级标题后面；没有标题就放最前面
    insert_at = 0
    for i, line in enumerate(lines):
        if line.startswith("# "):
            insert_at = i + 1
            break

    head = lines[:insert_at]
    tail = lines[insert_at:]
    while tail and not tail[0].strip():
        tail.pop(0)

    parts = head + ["", LANG_BAR, ""]
    return "\n".join(parts + tail).rstrip("\n") + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--upstream-ref", default=None, help="上游分支引用，如 upstream/main")
    parser.add_argument("--source", default=None, help="直接读本地文件（调试用）")
    args = parser.parse_args()

    text = read_source(args.upstream_ref, Path(args.source) if args.source else None)
    OUTPUT.write_text(with_lang_bar(text), encoding="utf-8")
    print(f"已生成 {OUTPUT.relative_to(ROOT)}（{len(text.splitlines())} 行）")
    return 0


if __name__ == "__main__":
    sys.exit(main())
