#!/usr/bin/env python3
"""把上游的 Release 更新日志抓进仓库，并在发布时提取最新一版。

用法：
    python3 tools/upstream_notes.py --out docs/upstream-release-notes.md   # 抓取并写入（内容没变不落盘）
    python3 tools/upstream_notes.py --latest docs/upstream-release-notes.md  # 打印最新一版正文

上游的 Release 说明是西班牙语原文，这里只做搬运和排版，不翻译——翻译会失真，
而且上游每次改文案都要重新对一遍。想读中文的看本仓库的发布说明。
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
import urllib.error
import urllib.request
from datetime import datetime, timezone

UPSTREAM = "synndras/P5R-Android-Plus"
API = f"https://api.github.com/repos/{UPSTREAM}/releases"
PER_PAGE = 30
# 一版说明最多取这么多字符，避免个别超长说明把发布页顶穿
LATEST_LIMIT = 1800
VOLATILE_PREFIX = "> 由 `tools/upstream_notes.py`"


def fetch_releases() -> list[dict]:
    req = urllib.request.Request(f"{API}?per_page={PER_PAGE}")
    req.add_header("Accept", "application/vnd.github+json")
    req.add_header("User-Agent", "p5r-android-cn-sync")
    token = os.environ.get("GITHUB_TOKEN") or os.environ.get("GH_TOKEN")
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    with urllib.request.urlopen(req, timeout=30) as r:
        data = json.load(r)
    if not isinstance(data, list):
        raise RuntimeError(f"unexpected response: {str(data)[:200]}")
    # 只保留正式发布，跳过草稿与预发布
    return [x for x in data if not x.get("draft") and not x.get("prerelease")]


def fmt_date(value: str) -> str:
    try:
        dt = datetime.fromisoformat(value.replace("Z", "+00:00")).astimezone(timezone.utc)
        return dt.strftime("%Y-%m-%d")
    except Exception:
        return value or ""


def demote(body: str) -> str:
    """上游正文自带 "## Cambios" 这类标题，放进版本标题下会把结构顶平，整体降一级。"""
    out = []
    for line in body.splitlines():
        m = re.match(r"^(#{1,6})\s+(.*)$", line)
        if m:
            # 上游各级标题（#/##/###）统一成版本下的三级标题，看起来才一致
            level = 3 if len(m.group(1)) <= 3 else 4
            out.append("#" * level + " " + m.group(2))
        else:
            out.append(line)
    return "\n".join(out)


def render(releases: list[dict]) -> str:
    now = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC")
    head = [
        "# 上游更新日志",
        "",
        f"> 自动同步自 [{UPSTREAM}](https://github.com/{UPSTREAM}) 的 GitHub Release 说明，"
        "原文为西班牙语，未做翻译。",
        f"{VOLATILE_PREFIX} 生成，请勿手工编辑。最后同步：{now}。",
        "",
        f"共 {len(releases)} 个版本" + (f"，最新 {releases[0]['tag_name']}。" if releases else "。"),
        "",
        "本仓库对应的版本号是「上游版本-cn.N」，上游写了什么，这里就是什么；"
        "简体中文界面的改动见「最近提交」与发布说明。",
        "",
    ]
    blocks = []
    for r in releases:
        tag = r.get("tag_name") or "?"
        name = (r.get("name") or "").strip()
        raw = (r.get("body") or "").strip()
        # 上游正文末尾那句 "APK Android arm64-v8a (NCE): p5r.apk." 指的是它自己的资产名，
        # 放在本仓库的发布页会让人以为要下 p5r.apk，去掉。
        raw = "\n".join(l for l in raw.splitlines() if not re.match(r"^APK\b.*\.apk", l.strip()))
        body = demote(raw).strip()
        date = fmt_date(r.get("published_at") or "")
        ver = tag.lstrip("vV")
        # 名字里已经带着版本号（"P5RP 0.5.31"）就不再重复一遍
        redundant = not name or name.replace(" ", "").lower().endswith(ver.lower())
        title = f"## {tag}" + ("" if redundant else f" · {name}")
        if date:
            title += f" · {date}"
        block = [title, ""]
        block.append(body if body else "_上游这个版本没有写更新说明。_")
        block += ["", f"[在 GitHub 查看此版本](https://github.com/{UPSTREAM}/releases/tag/{tag})", "", "---", ""]
        blocks.append("\n".join(block))
    return "\n".join(head + blocks).rstrip() + "\n"


def latest_section(path: str) -> str:
    """文件里最新一版的正文（去掉标题行和「在 GitHub 查看」那行）。"""
    try:
        text = open(path, encoding="utf-8").read()
    except OSError:
        return ""
    m = re.search(r"^## (v[^\n]*)\n(.*?)(?:\n---\s*$|\Z)", text, re.S | re.M)
    if not m:
        return ""
    version, body = m.group(1).split(" · ")[0], m.group(2)
    lines = [ln for ln in body.strip().splitlines() if not ln.startswith("[在 GitHub 查看此版本]")]
    body = "\n".join(lines).strip()
    if len(body) > LATEST_LIMIT:
        body = body[:LATEST_LIMIT].rstrip() + "\n\n…（完整内容见 `docs/upstream-release-notes.md`）"
    # 版本号由调用方（发布说明的小标题）给出，这里只回正文，避免重复一遍
    return body or "_上游这个版本没有写更新说明。_"


def strip_volatile(text: str) -> str:
    return "\n".join(ln for ln in text.splitlines() if not ln.startswith(VOLATILE_PREFIX))


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", help="写入这个路径（内容没变就不动文件）")
    ap.add_argument("--latest", help="从这个文件里打印最新一版正文")
    args = ap.parse_args()

    if args.latest:
        section = latest_section(args.latest)
        if not section:
            return 1
        print(section)
        return 0

    if not args.out:
        ap.error("要么给 --out，要么给 --latest")

    try:
        text = render(fetch_releases())
    except (urllib.error.URLError, RuntimeError, TimeoutError) as e:
        print(f"抓取上游更新日志失败：{e}", file=sys.stderr)
        return 1

    old = ""
    if os.path.exists(args.out):
        old = open(args.out, encoding="utf-8").read()
    if strip_volatile(old).strip() == strip_volatile(text).strip():
        print("上游更新日志没有变化")
        return 0

    os.makedirs(os.path.dirname(args.out) or ".", exist_ok=True)
    with open(args.out, "w", encoding="utf-8") as f:
        f.write(text)
    print(f"已更新 {args.out}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
