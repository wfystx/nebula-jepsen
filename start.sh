#!/bin/bash

function start() {
    cnt=0
    for f in $(seq 1 5);do
        if docker ps -a | grep -qw nebula-n$f; then
            docker ps | grep -qw nebula-n$f || docker start nebula-n$f
            cnt=$((${cnt} + 1))
        fi
    done 
    if docker ps -a | grep -qw nebula-metad;then
        docker ps | grep -qw nebula-metad || docker start nebula-metad
        cnt=$((${cnt} + 1))
    fi
    if docker ps -a | grep -qw nebula-graphd;then
        docker ps | grep -qw nebula-graphd || docker start nebula-graphd
        cnt=$((${cnt} + 1))
    fi
    
    if [ "$cnt" -ne 7 ];then
        if [ "$cnt" -eq 0 ];then
            init
        else
            stop
            remove
            init
        fi
    fi

    if ! docker ps -a | grep -qw nebula-control; then
        docker run --name nebula-control --privileged=true -it --net docker_nebula-net \
        --link nebula-n1:n1 --link nebula-n2:n2 --link nebula-n3:n3 --link nebula-n4:n4 \
        --link nebula-n5:n5 -v $(pwd)/:/jepsen_dev wfystx/nebula-control:latest /bin/bash
    else 
        docker ps | grep -qw nebula-control || docker start nebula-control
        docker exec -t -i nebula-control /bin/bash
    fi
}

function remove() {
    for f in $(seq 1 5);do
        echo "remove nebula-jepsen storage node $f" 
        docker rm -f nebula-n$f
    done
    echo "remove nebula-jepsen meta node" 
    docker rm -f nebula-metad
    echo "remove nebula-jepsen graph node" 
    docker rm -f nebula-graphd
    echo "remove nebula-jepsen control node"
    docker rm -f nebula-control
}

function stop() {
    for f in $(seq 1 5);do
        docker stop nebula-n$f
    done
    docker stop nebula-metad
    docker stop nebula-graphd
    docker stop nebula-control
}

function init() {
    if [ -d "docker/data/" ];then
        rm -rf docker/data/
    fi
    if [ -d "docker/logs/" ];then
        rm -rf docker/data/
    fi
    docker-compose -f docker/docker-compose.yaml up -d
    for f in $(seq 1 5);do
        docker exec nebula-n$f /run.sh
    done
    docker run -idt --name nebula-console --network=host vesoft/nebula-console:nightly --addr=127.0.0.1 --port=3699
    cat create_space.txt | docker exec -i nebula-console /bin/bash
    docker stop nebula-console
    docker rm nebula-console
}

function restart() {
    stop
    remove
    init
    docker run --name nebula-control --privileged=true -it --net docker_nebula-net \
    --link nebula-n1:n1 --link nebula-n2:n2 --link nebula-n3:n3 --link nebula-n4:n4 \
    --link nebula-n5:n5 -v $(pwd)/:/jepsen_dev wfystx/nebula-control:latest /bin/bash
}

case $1 in 
    "start")
        start
    ;;
    "stop") 
        stop
    ;;
    "remove")
        remove
    ;;
    "restart")
        restart
    ;;
    "help")
        echo "start.sh [start|restart|stop|remove]"
    ;;
    *) 
        echo "start.sh [start|restart|stop|remove]"
    ;;
esac