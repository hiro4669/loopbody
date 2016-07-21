#!/bin/bash
# INPUT: 
### 1) the fully qualified name of the processor to use
### 2) the location of the source code to be spooned
### 3) the destination for the spooned output source code
processor="$1"
input="$2"
output="$3"
processor_src="src/${processor//./\/}.java"

echo "1. Building processor $processor..."
echo

javac -classpath "lib/spoon.jar" -d "bin" "${processor_src}"

echo "2. Running spoon (default output) on the Simple Loop example, using $processor"
echo

java \
-classpath "bin:lib/spoon.jar" \
-jar "lib/spoon.jar" \
-i "${input}" \
-d "${output}" \
-p "${processor}"
