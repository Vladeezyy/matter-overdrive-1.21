"""Summary of build-compile.log: errors grouped by message and missing symbol (first copy of the javac output)."""
import collections
import re
import sys

log = open(sys.argv[1] if len(sys.argv) > 1 else 'build-compile.log').read().split('* What went wrong')[0]
errs = re.findall(r'(/[^\n]+?\.java):(\d+): error: ([^\n]+)\n([^\n]*)\n[^\n]*\n(?:\s+symbol:\s+([^\n]+))?', log)
print(len(errs), 'errors in', len({e[0] for e in errs}), 'files')
c = collections.Counter((m + (' | ' + s.strip() if s else ''))[:150] for f, l, m, code, s in errs)
for k, v in c.most_common(int(sys.argv[2]) if len(sys.argv) > 2 else 60):
    print(v, k)
