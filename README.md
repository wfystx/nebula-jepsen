# nebula-jepsen
Jepsen testing framework for Nebula Graph

## Get started
- Run docker containers
  - Change directory to ```nebula-jepsen/```, run ```./up.sh``` to start
  - A space named 'test' will be automatically created, with default setting of ```partition_num=5, replica_factor=3```, can be changed by editting ```nebula-jepsen/docker/create_space.txt```
  - Wait until all nodes started and you are now in the container of control node. 


- Run the test 
  ```cd nebula``` 
  ```lein run test ```  

