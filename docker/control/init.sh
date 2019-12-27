#!/bin/sh

: "${SSH_PRIVATE_KEY?SSH_PRIVATE_KEY is empty, please use up.sh}"
: "${SSH_PUBLIC_KEY?SSH_PUBLIC_KEY is empty, please use up.sh}"

if [ ! -f ~/.ssh/known_hosts ]; then
    mkdir -m 700 ~/.ssh
    echo $SSH_PRIVATE_KEY | perl -p -e 's/↩/\n/g' > ~/.ssh/id_rsa
    chmod 600 ~/.ssh/id_rsa
    echo $SSH_PUBLIC_KEY > ~/.ssh/id_rsa.pub
    echo > ~/.ssh/known_hosts
    for f in $(seq 1 5);do
	ssh-keyscan -t rsa n$f >> ~/.ssh/known_hosts
    done
fi

# TODO: assert that SSH_PRIVATE_KEY==~/.ssh/id_rsa

cat <<EOF 

==================================
Welcome to Nebula-Jepsen on Docker

-t <NAME> or --test <NAME> to select a test
--nemesis <NAME> to select a nemesis
--time-limit <TIME> to set test time
Tests: register cas-register multi-register
Nemesises: noop(default) kill-node partition-random-node
Example:
lein run test -t register --nemesis kill-node --time-limit 60
==================================

EOF