#! /bin/bash -e

mkdir -p $HOME/.jred
nohup java -XX:+UseG1GC -jar $(dirname $0)/${LOCAL}jred-12-all.jar "$@" server \
    < /dev/null \
    > $HOME/.jred/server.log 2>&1 &

cat $HOME/.jred/server.log
