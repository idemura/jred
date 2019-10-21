#! /bin/bash -e

mkdir -p $HOME/.jred
nohup java -XX:+UseG1GC -jar $(dirname $0)/${LOCAL}jred-12-all.jar server start \
    < /dev/null \
    > $HOME/.jred/server.log 2>&1 &
