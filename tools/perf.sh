#!/bin/sh
# Runs the PerfScene (max 15 min): tools/perf.sh stress|hands [-Pjfr]. Prints the [perf] lines and the errors; screenshots
# of "hands" in run/screenshots/hand_*.png, the flight recording of -Pjfr in run/perf.jfr.
cd "$(dirname "$0")/.."
MODE=${1:-stress}; shift
rm -f build-perf.log run/perf.jfr run/screenshots/hand_*.png
perl -e 'alarm 900; exec @ARGV' ./gradlew runPerf -Pperf=$MODE --console=plain "$@" > build-perf.log 2>&1
echo "exit $?"
grep -E '\[perf\] (PERF|CHECK|DONE)' build-perf.log | sed -E 's/^.*\[perf\] //'
echo "--- errors"
grep -E '/(ERROR|FATAL)\]|Caused by|Exception' build-perf.log | sed -E 's/^\[[^]]*\] //' | cut -c1-250 | sort | uniq -c | sort -rn | head -20
