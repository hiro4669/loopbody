#!/bin/bash

# CONFIG:
BIN="bin"
SPOON_JAR="lib/spoon.jar"
OUTPUT="exposed_java"
PROCESSOR="com.lukemerrick.loopbody.processors.LoopExposer"

# INPUT: the path to the directory containting the files to process
input="$1"

echo "Processing files in $input"
echo

java \
-classpath "${BIN}:${SPOON_JAR}" \
spoon.Launcher \
-i "${input}" \
-d "${OUTPUT}" \
-p "${PROCESSOR}"
