#!/bin/sh
set -e
rm -rf out
mkdir -p out/main out/test
javac -d out/main $(find src/main -name '*.java')
javac -d out/test -cp out/main $(find src/test -name '*.java')
java -cp out/main:out/test fairplay.TestMain
jar --create --file out/fairplay-sim.jar --main-class fairplay.Main -C out/main .
