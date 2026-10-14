#!/usr/bin/env python3
"""Document package-private and inner-class methods (tab-indented) that lack Javadoc."""

import re
import sys
from pathlib import Path

METHOD = re.compile(r"^(\t+)([\w<>\[\],\s]+)\s+(\w+)\s*\([^;]*\)\s*\{\s*$")


def has_doc(lines, idx):
    j = idx - 1
    while j >= 0 and lines[j].strip() == "":
        j -= 1
    if j >= 0 and lines[j].strip().startswith("//"):
        return True
    if j >= 0 and lines[j].strip().endswith("*/"):
        return True
    return False


def process(path: Path) -> bool:
    lines = path.read_text(encoding="utf-8").splitlines(keepends=True)
    out = []
    changed = False
    for i, line in enumerate(lines):
        m = METHOD.match(line.rstrip("\n"))
        if m and not has_doc(lines, i):
            name = m.group(3)
            if name not in ("if", "for", "while"):
                indent = m.group(1)
                out.append(f"{indent}/** Performs the {name} step in the TDES pipeline. */\n")
                changed = True
        out.append(line)
    if changed:
        path.write_text("".join(out), encoding="utf-8")
    return changed


if __name__ == "__main__":
    root = Path(sys.argv[1])
    for java in root.rglob("*.java"):
        if process(java):
            print(java)
