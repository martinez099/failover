# failover
This is an implementation of a failover mechanism for multiple Redis server instances in an active-passive setup.

## Prerequisites

* Java 1.8
* Maven 3
* Docker 18

## Setup

1. Compile the client:

   ```mvn clean compile```

## Run
   
1. Start the docker containers:

   ```docker-compose up```
   
2. Run the client; the last argument is the timeout in seconds, i.e. the time period after the failover is triggered

   ```mvn exec:java -Dexec.mainClass="com.redislabs.demo.failover.Failover" -Dexec.args="redis1:6379:localhost:6379 redis2:6379:localhost:6380 redis3:6379:localhost:6381 10" -Dexec.classpathScope=runtime```

3. Stop a Redis instance to see the failover working:

   ```docker-compose stop redis1```
   
4. Stop the next current Redis instance, the mechanism will failover to the next Redis instance:

   ```docker-compose stop redis2```

5. Start Redis instances again, the mechanism will try to failover in a round-robin style:

   ```docker-compose start redis1```

6. At the end don't forget to stop the docker containers:

   ```docker-compose down```
