#! /bin/bash -e

java -XX:+UseG1GC -jar $(dirname $0)/${LOCAL}jred-12-all.jar "$@"
