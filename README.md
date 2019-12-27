# nebula-jepsen
Jepsen testing framework for Nebula Graph

## Introduction:
Jepsen is an effort to improve the safety of distributed databases, queues, consensus systems, etc.

Nebula graph collaborates with Jepsen runs in docker environment. The cluster is formed by 8 docker containers, one meta node, one graph node, one control node and 5 storage nodes to achieve the test for storage kv API. Jepsen will be running in control node when testing, control the operations in 5 storage nodes.

## Get started:
The entire cluster will be set up by ```up.sh``` script in ```nebula-jepsen/docker``` directory. It pulls lastest images of storage, meta and graph. Then it adds some dependencies required by Jepsen environment into storage image and build a new one.

Firstly, clone the repo:

``` git clone https://github.com/vesoft-inc/nebula-jepsen```

Secondly, change to ```nebula-jepsen/docker/``` directory and run the script:

```cd nebula-jepsen/docker/```

```./up.sh start|remove|help```  

(arguments are:

​	**start**: start the cluster.

​	**remove**: remove all the related ongoing containers,

​	**help**: operations help)

Then the script will set up the test environment automatically, allowing ssh log in to root without password, using docker-compose to set up the cluster with a test space(partition_num=5, replica_factor=3). When the script ends, we will be in the container of control node.

After that, use ```lein run test``` with some parameters to start testing in ```/jepsen/nebula/``` dirctory.

```bash
-t <NAME> or --test <NAME> 
#which test to run (*required，register or cas-register or multi-key)
--nemesis <NAME> 
#which nemesis to use (optional, kill-node or partition-random-node. no nemesis as default)
--time-limit <TIME>
#how long will the test run
#example:
#lein run test -t register --nemesis kill-node --time-limit 60
```

## Timing Test with crontab  
You can also deploy the cluster in a machine and set a crontab to test all test types with all nemesis types.  

Inside the nebula-control 
```bash
crontab /jepsen/jepsencron
service cron start
```

That's it! Make sure of your machine and docker alive.

