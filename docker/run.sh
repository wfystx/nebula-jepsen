#!/bin/bash
:<<!
if [ ! -f "/etc/ssh/ssh_host_rsa_key" ]; then
    ssh-keygen -t rsa -N "" -f /etc/ssh/ssh_host_rsa_key
fi
if [ ! -f "/etc/ssh/ssh_host_ecdsa_key" ]; then
    ssh-keygen -t ecdsa -N "" -f /etc/ssh/ssh_host_ecdsa_key
fi
if [ ! -f "/etc/ssh/ssh_host_ed25519_key" ]; then
    ssh-keygen -t ed25519 -N "" -f /etc/ssh/ssh_host_ed25519_key
fi
!

if [ "${AUTHORIZED_KEYS}" != "**None**" ]; then
    echo "=> Found authorized keys"
    mkdir -p /root/.ssh
    chmod 700 /root/.ssh
    touch /root/.ssh/authorized_keys
    chmod 600 /root/.ssh/authorized_keys
    IFS=$'\n'
    arr=$(echo ${AUTHORIZED_KEYS} | tr "," "\n")
    for x in $arr
    do
        x=$(echo $x |sed -e 's/^ *//' -e 's/ *$//')
        cat /root/.ssh/authorized_keys | grep "$x" >/dev/null 2>&1
        if [ $? -ne 0 ]; then
            echo "=> Adding public key to /root/.ssh/authorized_keys: $x"
            echo "$x" >> /root/.ssh/authorized_keys
        fi
    done
fi

echo 'root' | passwd root --stdin

/usr/local/nebula/scripts/nebula-storaged.service -c /usr/local/nebula/etc/nebula-storaged.conf start

exec /usr/sbin/sshd -D

tail -f /dev/null