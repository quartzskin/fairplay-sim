@echo off
rmdir /s /q out 2>nul
mkdir out\main out\test
dir /s /b src\main\*.java > out\main.txt
dir /s /b src\test\*.java > out\test.txt
javac -d out\main @out\main.txt || exit /b 1
javac -d out\test -cp out\main @out\test.txt || exit /b 1
java -cp out\main;out\test fairplay.TestMain || exit /b 1
jar --create --file out\fairplay-sim.jar --main-class fairplay.Main -C out\main .
