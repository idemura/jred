#! /bin/bash -e

java -XX:+UseG1GC -jar $(dirname $0)/${LOCAL}jred-13-all.jar "$@"
