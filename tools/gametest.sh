#!/bin/sh
# GameTests of one loader under the hang watchdog:  LOADER=forge|fabric sh tools/gametest.sh
cd "$(dirname "$0")/.." || exit 1
LOADER=${LOADER:-forge}
export JAVA_HOME=${JAVA_HOME:-$(/usr/libexec/java_home -v 21)}
kill_dev() { pkill -f "$(pwd).*(net.minecraftforge|fml|loom-cache|net.fabricmc)" 2>/dev/null; }
. tools/watchdog.sh
LOG=build-gametest-$LOADER.log
if [ "$LOADER" = fabric ]; then TASK=:fabric:runGametest; else TASK=:forge:runGameTestServer; fi
run_watched "$LOG" 1500 ./gradlew "$TASK" --console=plain "$@"
grep -E "tests? (passed|failed)|All [0-9]+ required tests passed|required tests failed|::|Crash Report" "$LOG" | tail -40
