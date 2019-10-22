#! /bin/bash -e

java -XX:+UseG1GC -jar $(dirname $0)/${JRED_PATH}jred-all.jar "$@"
