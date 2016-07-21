#!/bin/bash
# INPUT: 
### 1) the fully qualified name of the processor to use
### 2) the location of the source code to be spooned
### 3) the destination for the spooned output source code
proc="$1"
input="$2"
output="$3"
proc_src="processors/${proc//./\/}.java"

echo "1. Building processor $proc..."
echo

javac -cp "lib/*" -d "lib" "${proc_src}"

echo "2. Running spoon (default output) on the Simple Loop example, using $proc"
echo

binary_loc="lib/spoon"
echo "${binary_loc}:lib/spoon.jar"

java \
-classpath "${binary_loc}:lib/spoon.jar" \
-jar "lib/spoon.jar" \
-i "${input}" \
-d "${output}" \
-p "${proc}"
