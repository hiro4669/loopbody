#!/bin/bash
# Input the short (not qualified) name for the processor
PROC="com.lukemerrick.loopbody.processors.${1}"
INPUT='spoonable_examples/SimpleLoopExample/src'
OUTPUT="${PWD}/spooned"
echo
echo "-------------- RUNNING THE FOLLOWING -----------"
echo "\"./runspoon.sh $PROC $INPUT $OUTPUT\""
echo
./runspoon.sh $PROC $INPUT $OUTPUT
