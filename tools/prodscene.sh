#!/bin/sh
# Fabric: the scripted scene on the release jar in a production client (Loom ClientProductionRunTask), under the watchdog
cd "$(dirname "$0")/.." || exit 1
export JAVA_HOME=${JAVA_HOME:-$(/usr/libexec/java_home -v 21)}
kill_dev() { pkill -f "$(pwd).*(loom-cache|net.fabricmc|run-prod)" 2>/dev/null; }
. tools/watchdog.sh
rm -f fabric/run-prod/screenshots/scene_*.png
run_watched build-prodscene.log 1800 ./gradlew :fabric:prodScene --console=plain
echo "shots: $(ls fabric/run-prod/screenshots/scene_*.png 2>/dev/null | wc -l)"
grep -E "Crash Report|HUNG|Mixin apply.*failed" build-prodscene.log | head
