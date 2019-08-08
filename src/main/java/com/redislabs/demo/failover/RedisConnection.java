package com.redislabs.demo.failover;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.event.Event;
import io.lettuce.core.event.EventBus;

import java.nio.charset.Charset;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;

public class RedisConnection {

    private String internalHost;

    private int internalPort;

    private String externalHost;

    private int externalPort;

    private String url;

    private RedisClient client;

    private StatefulRedisConnection<String, String> connection;

    private RedisCommands<String, String> syncCommands;

    public RedisConnection(String intHost, int intPort, String extHost, int extPort, int timeout) {
        this.internalHost = intHost;
        this.internalPort = intPort;
        this.externalHost = extHost;
        this.externalPort = extPort;
        this.url = "redis://" + externalHost + ':' + externalPort + "?timeout=" + timeout + 's';
        this.client = RedisClient.create(this.url);
        this.client.setOptions(ClientOptions.builder()
            .autoReconnect(true)
            .build());
        this.connect();
    }

    public void addEventHandler(Consumer<? super Event> handler) {
        this.client.getResources().eventBus().get().subscribe(handler);
    }

    public String getInternalHost() { return this.internalHost; }

    public int getInternalPort() { return this.internalPort; }

    public String getExternalHost() { return this.externalHost; }

    public int getExternalPort() { return this.externalPort; }

    public String getUrl() { return this.url; }

    public RedisCommands<String, String> exec() {
        if (!this.connection.isOpen()) {
            this.connect();
        }
        return this.syncCommands;
    }

    public void putTraffic() {
        if (!this.connection.isOpen()) {
            this.connect();
        }
        while(true) {
            String key = getRandomString(12);
            this.syncCommands.set(key, getRandomString(32));
            this.syncCommands.get(key);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    public void close() {
        connection.close();
    }

    public void shutdown() {
        client.shutdown();
    }

    private void connect() {
        this.connection = this.client.connect();
        this.syncCommands = this.connection.sync();
    }

    private String getRandomString(int length) {
        byte[] array = new byte[length];
        new Random().nextBytes(array);
        return new String(array, Charset.forName("UTF-8"));
    }
}
