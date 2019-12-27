#!/bin/bash

cd /jepsen/nebula/
lein run test --test cas-register --nemesis kill-node --time-limit 18000