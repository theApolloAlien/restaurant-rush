#!/usr/bin/env sh
# Compiles the game and the rule checks, then runs them.
set -e
cd "$(dirname "$0")"
rm -rf out-test
mkdir -p out-test
javac --release 17 -encoding UTF-8 -d out-test $(find src test -name '*.java')
java -cp out-test rush.RulesTest
