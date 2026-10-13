#!/usr/bin/env sh
# Compiles the game into out/ and packages restaurant-rush.jar. Needs JDK 17 or newer.
set -e
cd "$(dirname "$0")"
rm -rf out
mkdir -p out
javac --release 17 -encoding UTF-8 -d out $(find src -name '*.java')
printf 'Main-Class: rush.Main\n' > out/MANIFEST.MF
jar cfm restaurant-rush.jar out/MANIFEST.MF -C out rush
echo "Built restaurant-rush.jar"
