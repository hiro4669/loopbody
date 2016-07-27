#!/bin/bash

# CONFIG:
BIN="bin"
SPOON_JAR="lib/spoon.jar"

# INPUT: 
### 1) the fully qualified name of the processor to use
### 2) the location of the source code to be spooned
### 3) the destination for the spooned output source code
processor="$1"
input="$2"
output="$3"
processor_src="src/${processor//./\/}.java"

# STEP 1: COMPILE THE PROCESSOR

echo "1. Building processor $processor..."
echo

# Attempts to compile the processor, terminates script if an error is thrown
if ! javac -classpath "${SPOON_JAR}" -d "${BIN}" "${processor_src}"; then
echo;
echo "Error compiling, Spoon aborted.";
exit;
fi

# STEP 2: RUN SPOON USING THE PROCESSOR

echo "2. Running spoon (default output) on the Simple Loop example, using $processor"
echo


java \
-classpath "${BIN}:${SPOON_JAR}" \
spoon.Launcher \
-i "${input}" \
-d "${output}" \
-p "${processor}"

# Optional. Insures that the output files are properly owned by the user even if this command is run with sudo.
# chown -R $USER: ${output}