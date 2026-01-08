#!/bin/bash

find . -iname antlr4 -type d | grep java | xargs rm -rf

if [ ! -z $1 ]; then
  echo $1
  mvn antlr4:antlr4 -pl :$1
else
  mvn antlr4:antlr4
fi
