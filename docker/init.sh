#!/bin/bash
password=root
echo 'y' | ssh-keygen -f $HOME/.ssh/id_rsa -t rsa -N ''

echo "" > ~/.ssh/known_hosts

for f in $(seq 1 5)
  do
    ssh-keyscan -t rsa n$f >> ~/.ssh/known_hosts
    expect <<-EOF
    set timeout 5
    spawn ssh-copy-id -i n$f
    expect {
    "yes/no" { send "yes\n";exp_continue }
    "password:" { send "$password\n" }
    }
  interact
  expect eof
EOF
done