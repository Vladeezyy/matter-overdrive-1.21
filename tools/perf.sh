#!/bin/sh
# Runs the PerfScene under the hang watchdog (max 15 min):  LOADER=forge|fabric sh tools/perf.sh stress|hands [-Pjfr]
# Prints the [perf] lines and the errors; screenshots of "hands" in <loader>/run/screenshots/hand_*.png.
cd "$(dirname "$0")/.." || exit 1
LOADER=${LOADER:-forge}
MODE=${1:-stress}; shift
export JAVA_HOME=${JAVA_HOME:-$(/usr/libexec/java_home -v 21)}
kill_dev() { pkill -f "$(pwd).*(net.minecraftforge|fml|loom-cache|net.fabricmc)" 2>/dev/null; }
. tools/watchdog.sh
LOG=build-perf-$LOADER.log
rm -f "$LOADER/run/perf.jfr" "$LOADER"/run/screenshots/hand_*.png
run_watched "$LOG" 900 ./gradlew ":$LOADER:runPerf" -Pperf="$MODE" --console=plain "$@"
grep -E '\[perf\] (PERF|CHECK|DONE)' "$LOG" | sed -E 's/^.*\[perf\] //'
echo "--- errors"
grep -E '/(ERROR|FATAL)\]|Caused by|Exception' "$LOG" | sed -E 's/^\[[^]]*\] //' | cut -c1-250 | sort | uniq -c | sort -rn | head -20
