#!/bin/bash
PROC='com.lukemerrick.loopbody.processors.NotNullCheckAdderProcessor'
INPUT='spoonable_examples/SimpleLoopExample/src'
OUTPUT="${PWD}/spooned"
echo
echo "-------------- RUNNING THE FOLLOWING -----------"
echo "\"./runspoon.sh $PROC $INPUT $OUTPUT\""
echo
./runspoon.sh $PROC $INPUT $OUTPUT
