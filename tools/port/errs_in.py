"""Errors of one file (or a path substring) from build-compile.log: line, message, symbol, code line."""
import re
import sys

log = open('build-compile.log').read().split('* What went wrong')[0]
key = sys.argv[1]
for f, l, m, code, s in re.findall(r'(/[^\n]+?\.java):(\d+): error: ([^\n]+)\n([^\n]*)\n[^\n]*\n(?:\s+symbol:\s+([^\n]+))?', log):
    if key in f:
        print(f'{f.split("/rats/")[-1]}:{l}: {m}{" | " + s.strip() if s else ""}\n    {code.strip()[:220]}')
