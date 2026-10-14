#!/usr/bin/env python3
"""Add a one-line Javadoc comment above class-level methods that lack documentation."""

import re
import sys
from pathlib import Path

DISALLOWED_PREFIX = re.compile(
    r"^\s*(if|for|while|switch|catch|throw|return|else|case|try|do|synchronized)\b"
)
METHOD = re.compile(
    r"^(\s{4})(public|protected|private|static|\s)*"
    r"([\w<>\[\],\s?]+)\s+(\w+)\s*\([^;]*\)\s*(\{|throws|\s*$)"
)
CTOR = re.compile(
    r"^(\s{4})(public|protected|private)\s+(\w+)\s*\([^;]*\)\s*(\{|throws|\s*$)"
)
ANNOTATION = re.compile(r"^\s*@\w+")


def describe(name: str, is_ctor: bool, class_name: str) -> str:
    if is_ctor:
        return f"Constructs a new {class_name} instance."
    if name.startswith("get") and len(name) > 3:
        prop = re.sub("([A-Z])", r" \1", name[3:]).strip().lower()
        return f"Returns the {prop}."
    if name.startswith("set") and len(name) > 3:
        prop = re.sub("([A-Z])", r" \1", name[3:]).strip().lower()
        return f"Sets the {prop}."
    if name.startswith("is") and len(name) > 2:
        prop = re.sub("([A-Z])", r" \1", name[2:]).strip().lower()
        return f"Indicates whether {prop}."
    return f"Executes the {name} gateway operation."


def has_doc(lines: list[str], idx: int) -> bool:
    j = idx - 1
    while j >= 0 and lines[j].strip() == "":
        j -= 1
    while j >= 0 and ANNOTATION.match(lines[j]):
        j -= 1
        while j >= 0 and lines[j].strip() == "":
            j -= 1
    if j >= 0 and (lines[j].strip().endswith("*/") or lines[j].strip().startswith("//")):
        return True
    return False


def class_name(content: str, fallback: str) -> str:
    m = re.search(r"\bclass\s+(\w+)", content)
    return m.group(1) if m else fallback


def process(path: Path) -> bool:
    lines = path.read_text(encoding="utf-8").splitlines(keepends=True)
    cls = class_name("".join(lines), path.stem)
    out: list[str] = []
    changed = False
    for i, line in enumerate(lines):
        if DISALLOWED_PREFIX.match(line):
            out.append(line)
            continue
        if not has_doc(lines, i):
            m = METHOD.match(line)
            if m and m.group(4) not in ("class", "interface", "enum", "record"):
                indent = m.group(1)
                out.append(f"{indent}/** {describe(m.group(4), False, cls)} */\n")
                changed = True
            else:
                c = CTOR.match(line)
                if c and c.group(3) == cls:
                    indent = c.group(1)
                    out.append(f"{indent}/** {describe(cls, True, cls)} */\n")
                    changed = True
        out.append(line)
    if changed:
        path.write_text("".join(out), encoding="utf-8")
    return changed


def main() -> int:
    root = Path(sys.argv[1] if len(sys.argv) > 1 else "src/main/java")
    updated = sum(1 for p in sorted(root.rglob("*.java")) if process(p))
    print(f"Updated {updated} files")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
