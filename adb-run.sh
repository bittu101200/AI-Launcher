#!/bin/bash

if [ -z "$1" ]; then
    echo "Usage: ./adb-run.sh 'command or AI prompt'"
    exit 1
fi

# Send command via broadcast
# We wrap the whole command in quotes for adb shell to prevent 'am' from misparsing flags like -l
adb shell "am broadcast -a bhupendra.ai.launcher.main_exec --es cmd \"$1\"" > /dev/null
echo "Sent: $1"
echo "--- Output (Streaming) ---"

# Stream the logcat
# -T 1 ensures we only see logs generated AFTER this command starts
# We use a subshell to manage the logcat process
adb logcat -v raw -T 1 -s AI_OUTPUT | while read -r line; do
    # Check for the finish signals
    if [[ "$line" == *"AI_TURN_FINISHED"* ]]; then
        echo "--- Finished (AI_TURN_FINISHED) ---"
        pkill -P $$ adb 2>/dev/null
        break
    fi
    if [[ "$line" == *"CMD_FINISHED"* ]]; then
        echo "--- Finished (CMD_FINISHED) ---"
        pkill -P $$ adb 2>/dev/null
        break
    fi
    echo "$line"
done

exit 0
