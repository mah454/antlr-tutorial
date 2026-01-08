#!/bin/bash
set -x

if [ ! -z $1 ]; then
  echo $1
  mvn antlr4:antlr4 -pl :$1
else
  mvn antlr4:antlr4
fi
