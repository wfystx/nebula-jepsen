#!/bin/bash

cd /jepsen/nebula/
lein run test --test multi-register --nemesis kill-node --time-limit 18000