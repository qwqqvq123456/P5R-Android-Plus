#!/usr/bin/env python3
"""检查启动器里还有没有没翻译成简体中文的文案。

用法（仓库根目录）：python3 tools/check_zh.py
把 t(es, en) / Lang.f(es, en, ...) 的英文原文，以及 Option 的英文名、英文说明、英文取值，
拿去查 LangZh.java（我们维护的简体中文表）和 Lang.java 的 zh-Hans 列；两边都没有的列出来，并以 1 退出。

同步上游（.github/workflows/sync-upstream.yml）之后跑一次，就知道上游又加了哪些待翻译的文案：
把它们补进 LangZh.java 即可，不用改上游的 Lang.java。
"""
import ast
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
JAVA = os.path.join(ROOT, "games", "p5r", "java", "io", "github", "p5rrecomp")
LIT = re.compile(r'"(?:[^"\\]|\\.)*"', re.S)
# 本来就不翻译的：数值、分辨率、倍率、比例、帧率，以及语言名和品牌名
SKIP = {
    "English", "Español", "Français", "Deutsch", "Italiano", "Português",
    "日本語", "한국어", "简体中文", "繁體中文",
    "CAS", "FSR 1.0", "XBOX / PC", "P5RP · synndras",
}
SKIP_RE = re.compile(
    r"^(?:\d+(?:\.\d+)?%?[pPxX]?|\d+:\d+|\d+\s?FPS(?: TARGET)?|P5RP.*)$"
)


def split_args(text):
    """把调用括号里的内容按顶层逗号切开（字符串里的逗号不算）。"""
    args, depth, quoted, escape, cur = [], 0, False, False, ""
    for ch in text:
        if quoted:
            cur += ch
            if escape:
                escape = False
            elif ch == "\\":
                escape = True
            elif ch == '"':
                quoted = False
            continue
        if ch == '"':
            quoted = True
            cur += ch
        elif ch in "([{":
            depth += 1
            cur += ch
        elif ch in ")]}":
            if depth == 0:
                break
            depth -= 1
            cur += ch
        elif ch == "," and depth == 0:
            args.append(cur.strip())
            cur = ""
        else:
            cur += ch
    args.append(cur.strip())
    return args


def literals(expr):
    out = []
    for m in LIT.finditer(expr):
        try:
            out.append(ast.literal_eval(m.group(0)))
        except Exception:
            pass
    return out


def english_of(expr):
    """参数表达式 -> 英文原文（只由字符串字面量和 + 组成时拼接）。"""
    parts = literals(expr)
    if not parts:
        return None
    rest = re.sub(r"[\s+]", "", LIT.sub("", expr))
    if rest == "":
        return "".join(parts)
    return parts[0]


def keys_from_java(path):
    """一个源文件里用到的全部英文文案。"""
    src = open(path, encoding="utf-8").read()
    keys = []
    for m in re.finditer(r"(?:^|[^\w.])(?:t|f)\(", src):
        args = split_args(src[m.end():])
        if len(args) >= 2:
            s = english_of(args[1])
            if s:
                keys.append(s)
    # Option(tab, key, es, en, descEs, descEn, values, labelsEs, labelsEn, pick, modal)
    for m in re.finditer(r"new Option\(", src):
        args = split_args(src[m.end():])
        if len(args) >= 11:
            for idx in (3, 5):            # 名称、说明
                s = english_of(args[idx])
                if s:
                    keys.append(s)
            for v in literals(args[8]):   # 取值标签
                if v:
                    keys.append(v)
    return keys


def table_keys(path, english_index, chinese_index):
    """Lang.java / LangZh.java 里作为翻译的英文键。"""
    if not os.path.exists(path):
        return set()
    src = open(path, encoding="utf-8").read()
    out = set()
    for m in re.finditer(r"\n\s*put\(", src):
        args = split_args(src[m.end():])
        if len(args) > chinese_index:
            en = english_of(args[english_index])
            zh = english_of(args[chinese_index])
            if en and zh:
                out.add(en)
    return out


def main():
    # LangZh：put(英文, 中文)；Lang：put(英文, fr, de, it, pt, ja, ko, zh-Hans, zh-Hant)
    translated = table_keys(os.path.join(JAVA, "LangZh.java"), 0, 1)
    translated |= table_keys(os.path.join(JAVA, "Lang.java"), 0, 7)

    used, missing = set(), {}
    for name in sorted(os.listdir(JAVA)):
        if not name.endswith(".java") or name.startswith("Lang"):
            continue
        for text in keys_from_java(os.path.join(JAVA, name)):
            if not re.search(r"[A-Za-z]", text) or len(text) <= 2:
                continue
            if text in SKIP or SKIP_RE.match(text):
                continue
            used.add(text)
            if text not in translated:
                missing.setdefault(text, name)

    print("界面文案 {} 条，已翻译 {} 条，待翻译 {} 条。".format(
        len(used), len(used) - len(missing), len(missing)))
    for text, where in sorted(missing.items()):
        print("  [{}] {}".format(where, text.replace("\n", "\\n")))
    return 1 if missing else 0


if __name__ == "__main__":
    sys.exit(main())
