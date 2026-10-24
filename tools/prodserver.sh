#!/bin/sh
# The release jar on a real server (outside dev):  LOADER=forge sh tools/prodserver.sh
# Forge: forge/run-prod-server (installed with the official installer --installServer); a new normal world each run,
# waits for "Done", stops the server with "stop", prints the errors.
cd "$(dirname "$0")/.." || exit 1
LOADER=${LOADER:-forge}
JAVA17=${JAVA17:-$(/usr/libexec/java_home -v 17)}
. tools/watchdog.sh
DIR=$LOADER/run-prod-server
JAR=$(ls $LOADER/build/libs/matteroverdrive-$LOADER-*.jar | grep -v -- '-sources\|-dev' | head -1)
[ -n "$JAR" ] || { echo "no release jar: ./gradlew :$LOADER:build"; exit 1; }
mkdir -p "$DIR/mods"; rm -f "$DIR"/mods/matteroverdrive-*.jar; cp "$JAR" "$DIR/mods/"
rm -rf "$DIR/world"; echo eula=true > "$DIR/eula.txt"
printf 'online-mode=false\nserver-port=25602\nspawn-protection=0\n' > "$DIR/server.properties"
LOG=build-prodserver-$LOADER.log
rm -f "$DIR/console.in"; mkfifo "$DIR/console.in"
( cd "$DIR" && sleep 100000 > console.in & cd "$DIR" && PATH="$JAVA17/bin:$PATH" sh run.sh nogui < console.in ) > "$LOG" 2>&1 &
wait_started "$LOG"; code=$?
echo "jar: $JAR"
grep -E "Done \(" "$LOG" | tail -1
sleep 10
echo stop > "$DIR/console.in"
sleep 15
pkill -f "$(pwd)/$DIR.*sleep 100000" 2>/dev/null; pkill -f "sleep 100000" 2>/dev/null
rm -f "$DIR/console.in"
echo "--- errors"
grep -E '/(ERROR|FATAL)\]|Exception|Caused by' "$LOG" | grep -v 'Failed to load properties' | cut -c1-220 | sort | uniq -c | sort -rn | head -20
exit $code
