#!/bin/bash
PROC='spoon.examples.notnull.NotNullCheckAdderProcessor'
INPUT='spoonable/SimpleLoopExample/src'
OUTPUT="${PWD}/spooned"
echo
echo "-------------- RUNNING THE FOLLOWING -----------"
echo "\"./runspoon.sh $PROC $INPUT $OUTPUT\""
echo
./runspoon.sh $PROC $INPUT $OUTPUT
