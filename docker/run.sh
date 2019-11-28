#!/bin/bash

cd /
if [ ! -e /usr/bin/start-stop-daemon ];
then
tar -xf dpkg_1.17.27.tar.xz
cd dpkg-1.17.27 && ./configure
make -j4
cp utils/start-stop-daemon /usr/bin/start-stop-daemon;
fi
service sshd restart