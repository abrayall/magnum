#!/bin/bash

ORIG_DIRECTORY=`pwd`
BIN_DIRECTORY=`dirname $0`
BIN_PATH=`cd $BIN_DIRECTORY && pwd`
FULL_PATH="${BIN_PATH/bin/}"
ROOT_DIRECTORY=`dirname $BIN_DIRECTORY`

java -cp $FULL_PATH/lib/rameses-0.4.0.jar:$FULL_PATH/lib/cilantro-0.1.0.jar:$FULL_PATH/lib/magnum.jar magnum.Main $*

