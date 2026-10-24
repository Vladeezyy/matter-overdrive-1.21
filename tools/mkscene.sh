#!/bin/sh
# Makes the scene world run/saves/mo_scene (flat, creative) with the loader's dev server:  LOADER=forge|fabric sh tools/mkscene.sh
cd "$(dirname "$0")/.." || exit 1
LOADER=${LOADER:-forge}
export JAVA_HOME=${JAVA_HOME:-$(/usr/libexec/java_home -v 21)}
kill_dev() { pkill -f "$(pwd).*(net.minecraftforge|fml|loom-cache|net.fabricmc)" 2>/dev/null; }
. tools/watchdog.sh
RUN=$LOADER/run
mkdir -p "$RUN/saves"
rm -rf "$RUN/saves/mo_scene"
cp "$RUN/server.properties" "$RUN/server.properties.bak" 2>/dev/null
printf 'level-name=saves/mo_scene\nlevel-type=minecraft\\:flat\ngamemode=creative\ngenerate-structures=false\nonline-mode=false\nspawn-monsters=false\nserver-port=25601\n' > "$RUN/server.properties"
echo eula=true > "$RUN/eula.txt"
LOG=build-mkscene-$LOADER.log
./gradlew ":$LOADER:runServer" --console=plain > "$LOG" 2>&1 &
wait_started "$LOG"; code=$?
sleep 5
kill_dev
wait
[ -f "$RUN/server.properties.bak" ] && mv "$RUN/server.properties.bak" "$RUN/server.properties"
ls "$RUN/saves/mo_scene" >/dev/null 2>&1 && echo "world made" || echo "no world"
exit $code
