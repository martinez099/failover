package com.redislabs.demo.failover;

import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisException;
import io.lettuce.core.event.Event;
import io.lettuce.core.event.connection.ConnectionActivatedEvent;

import java.net.SocketAddress;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class Failover {

    private static Logger logger = Logger.getLogger(Failover.class.getName());

    private RedisConnection[] connections;

    private int currentConnectionIdx = 0;

    public static void main(String[] args) {

        Failover fo = new Failover();

        fo.createConnections(args);

        fo.configureReplication();

        // clear database
        fo.connections[fo.currentConnectionIdx].exec().flushall();

        fo.runComputation();

        for (RedisConnection conn : fo.connections) {
            logger.info("shutting down " + conn.getUrl());
            conn.close();
            conn.shutdown();
        }

    }

    public void createConnections(String[] args) {
        this.connections = new RedisConnection[args.length - 1];
        for (int i = 0; i < args.length - 1; i++) {
            String[] parts  = args[i].split(":", 4);
            this.connections[i] = new RedisConnection(
                    parts[0],
                    Integer.parseInt(parts[1]),
                    parts[2],
                    Integer.parseInt(parts[3]),
                    Integer.parseInt(args[args.length - 1])
            );
        }
    }

    public void configureReplication() {
        for (int i = 0; i < this.connections.length; i++) {
            if (i == currentConnectionIdx) {
                try {
                    this.connections[i].exec().slaveofNoOne();
                } catch (RedisException e) {
                    logger.severe(e.toString());
                    this.performFailover();
                    break;
                }
            } else {
                try {
                    this.connections[i].exec().slaveof(
                            this.connections[this.currentConnectionIdx].getInternalHost(),
                            this.connections[this.currentConnectionIdx].getInternalPort()
                    );
                } catch (RedisException e) {
                    logger.severe(e.toString());
                    continue;
                }
            }
        }
    }

    public void runComputation() {
        while(true) {
            logger.info("running against connection " + this.connections[this.currentConnectionIdx].getUrl());
            try {
                this.connections[this.currentConnectionIdx].putTraffic();
            } catch (RedisCommandTimeoutException e) {
                logger.severe(e.toString());
                this.performFailover();
            }
        }
    }

    public void performFailover() {
        this.currentConnectionIdx = (this.currentConnectionIdx + 1) % this.connections.length;
        logger.warning("failing over to connection " + this.connections[this.currentConnectionIdx].getUrl());
        this.configureReplication();
    }

}
