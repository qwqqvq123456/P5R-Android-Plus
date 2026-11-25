#!/usr/bin/env python3
"""确保 games/p5r/AndroidManifest.xml 声明了 VIBRATE 权限。

震动（屏幕虚拟按键的点击反馈 NX_TOUCH_HAPTICS，以及游戏自身的 rumble）都要经过
Vibrator.vibrate()。VIBRATE 是普通权限，但必须写进清单里，安装时才由系统授予；
漏掉这一行，vibrate() 会抛 SecurityException 被上层吞掉，表现就是"完全不震"
（Android 10 到 17 都一样，跟系统版本无关）。

上游 main 分支有这个权限，`Chakeyy-Launcher-Rework` 分支和同步合并都可能把它丢掉，
所以构建前和同步后都跑一遍本脚本；已有时不做任何改动。

用法:
    python3 tools/ensure_manifest.py            # 修 games/p5r/AndroidManifest.xml
    python3 tools/ensure_manifest.py --check    # 只检查，缺了以退出码 1 结束
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MANIFEST = ROOT / "games" / "p5r" / "AndroidManifest.xml"

PERMISSION = "android.permission.VIBRATE"
LINE = f'    <uses-permission android:name="{PERMISSION}" />\n'


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="只检查，不写入")
    parser.add_argument("--path", default=str(MANIFEST), help="清单文件路径")
    args = parser.parse_args()

    path = Path(args.path)
    if not path.is_file():
        print(f"找不到清单文件：{path}", file=sys.stderr)
        return 1

    text = path.read_text(encoding="utf-8")
    if PERMISSION in text:
        print(f"清单已声明 {PERMISSION}")
        return 0

    if args.check:
        print(f"清单缺少 {PERMISSION}", file=sys.stderr)
        return 1

    # 优先插在最后一条 uses-permission 后面，其次是 uses-sdk 后面，最后退回 </manifest> 之前
    lines = text.splitlines(keepends=True)
    insert_at = None
    for i, line in enumerate(lines):
        if "<uses-permission" in line:
            insert_at = i + 1
    if insert_at is None:
        for i, line in enumerate(lines):
            if "<uses-sdk" in line:
                insert_at = i + 1
                break
    if insert_at is None:
        for i, line in enumerate(lines):
            if "</manifest>" in line:
                insert_at = i
                break
    if insert_at is None:
        print("清单结构不认识（没有 uses-permission / uses-sdk / </manifest>）", file=sys.stderr)
        return 1

    # 保持缩进；注释放在权限行前面，说明为什么不能删
    note = [
        "    <!-- Vibrator.vibrate() (touch haptics, the game's rumble) needs this permission: without it the calls\n"
        "         fail silently and the phone never vibrates. -->\n"
    ]
    lines[insert_at:insert_at] = note + [LINE]
    path.write_text("".join(lines), encoding="utf-8")
    print(f"已在 {path.name} 补上 {PERMISSION}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
