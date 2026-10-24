#!/bin/sh
# The scripted client scene under the hang watchdog:  LOADER=forge|fabric sh tools/scenetest.sh [-PsceneFrom=N]
# Screenshots: <loader>/run/screenshots/scene_*.png
cd "$(dirname "$0")/.." || exit 1
LOADER=${LOADER:-forge}
export JAVA_HOME=${JAVA_HOME:-$(/usr/libexec/java_home -v 21)}
kill_dev() { pkill -f "$(pwd).*(net.minecraftforge|fml|loom-cache|net.fabricmc)" 2>/dev/null; }
. tools/watchdog.sh
LOG=build-scene-$LOADER.log
rm -f "$LOADER"/run/screenshots/scene_*.png
run_watched "$LOG" 1500 ./gradlew ":$LOADER:runScene" --console=plain "$@"
echo "shots: $(ls "$LOADER"/run/screenshots/scene_*.png 2>/dev/null | wc -l)"
grep -E "Crash Report|HUNG|\[scene\].*(FAIL|error)|Exception" "$LOG" | head -20
