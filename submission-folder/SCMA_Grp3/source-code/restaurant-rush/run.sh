#!/usr/bin/env sh
# Builds (if needed) and starts the game. Extra arguments are passed on, e.g.
#   ./run.sh --script scripts/stage1-victory.txt
#   ./run.sh --plain
set -e
cd "$(dirname "$0")"
[ -f restaurant-rush.jar ] || ./build.sh > /dev/null
exec java -jar restaurant-rush.jar "$@"
