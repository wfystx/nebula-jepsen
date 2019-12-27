#!/bin/bash

cd /jepsen/nebula/
lein run test --test multi-register --nemesis partition-random-node --time-limit 18000