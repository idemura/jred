#! /bin/bash -e

mkdir -p $HOME/.jred
nohup java -XX:+UseG1GC -jar $(dirname $0)/jred-11-all.jar server start \
    < /dev/null \
    > $HOME/.jred/server.log 2>&1 &
