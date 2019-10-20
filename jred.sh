#! /bin/bash -e

java -XX:+UseG1GC -jar $(dirname $0)/jred-11-all.jar "$@"
