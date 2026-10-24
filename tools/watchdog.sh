#!/bin/sh
# Hang watchdog for automatic Minecraft runs (client scenes, GameTests, dev and production servers).
# Guide: testing/hang-watchdog.md. Source it from the project's test scripts:   . path/to/watchdog.sh
#
#   run_watched LOG TIMEOUT CMD...   runs CMD (e.g. ./gradlew :neoforge:runScene --console=plain) into LOG; stops it
#                                    on a crash report, on TIMEOUT seconds, or when LOG stops growing for $STALL
#                                    seconds once the game runs ("HUNG", exit 124). Prints "exit N", returns N.
#   wait_started LOG [PATTERN]       waits for a server started in the background: "Done (", a crash, a failed build
#                                    or PATTERN (grep -E) -> 0; no output for $STALL s before that -> "HUNG", 124;
#                                    10 min -> "TIMEOUT", 124.
#
# STALL: seconds of silence that count as a hang, default 90 (a healthy client scene is silent for up to ~30 s;
# a server that waits for players is silent forever, so servers only get wait_started, until they are up).
# kill_dev: if the caller defines it (pkill of the project's game processes), it is called on a crash or a hang;
# the watched process tree is stopped in any case.

_wd_kill_tree() {
    for child in $(pgrep -P "$1" 2>/dev/null); do _wd_kill_tree "$child"; done
    kill "$1" 2>/dev/null
}

_wd_kill_game() {
    command -v kill_dev >/dev/null 2>&1 && kill_dev
}

run_watched() {
    log=$1; timeout=$2; shift 2
    stall=${STALL:-90}
    perl -e "alarm $timeout; exec @ARGV" "$@" > "$log" 2>&1 &
    pid=$!
    last_size=-1; last_change=$(date +%s); hung=0
    while kill -0 $pid 2>/dev/null; do
        if grep -q -- '---- Minecraft Crash Report ----' "$log" 2>/dev/null; then
            sleep 3
            _wd_kill_game
        fi
        size=$(wc -c < "$log" 2>/dev/null || echo 0)
        now=$(date +%s)
        if [ "$size" != "$last_size" ]; then
            last_size=$size; last_change=$now
        # the game is running (the build and the start before it may be quiet for longer)
        elif [ $((now - last_change)) -ge "$stall" ] && grep -qE 'Render thread|Server thread|\[scene\]' "$log" 2>/dev/null; then
            hung=1
            tail_lines=$(grep -vE '^\s*$' "$log" | tail -4 | cut -c1-300)
            echo "HUNG: no log output for ${stall}s, stopping the run. Last lines:" | tee -a "$log"
            echo "$tail_lines"
            _wd_kill_game; sleep 3; _wd_kill_tree $pid
            break
        fi
        sleep 2
    done
    wait $pid 2>/dev/null
    code=$?
    [ $hung = 1 ] && code=124
    echo "exit $code"
    return $code
}

wait_started() {
    log=$1; pattern='Done \(|Minecraft Crash Report|crash report|Crash report|BUILD FAILED|Failed to start|Exception in server tick'
    [ -n "$2" ] && pattern="$pattern|$2"
    stall=${STALL:-90}
    start=$(date +%s); last_size=-1; last_change=$start
    while :; do
        grep -qE "$pattern" "$log" 2>/dev/null && return 0
        size=$(wc -c < "$log" 2>/dev/null || echo 0)
        now=$(date +%s)
        if [ "$size" != "$last_size" ]; then
            last_size=$size; last_change=$now
        elif [ $((now - last_change)) -ge "$stall" ] && grep -qE 'Server thread|main/' "$log" 2>/dev/null; then
            tail_lines=$(grep -vE '^\s*$' "$log" | tail -4 | cut -c1-300)
            echo "HUNG: the server wrote nothing for ${stall}s before it was up. Last lines:" | tee -a "$log"
            echo "$tail_lines"
            return 124
        fi
        if [ $((now - start)) -ge 600 ]; then
            echo "TIMEOUT: the server was not up in 10 min" | tee -a "$log"
            return 124
        fi
        sleep 2
    done
}
